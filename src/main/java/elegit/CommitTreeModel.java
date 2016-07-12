package elegit;

import elegit.treefx.*;
import elegit.treefx.Cell;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.SymbolicRef;
import org.eclipse.jgit.util.IO;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles the conversion/creation of a list of commit helpers into a nice
 * tree structure. It also takes care of updating the view its given to
 * display the new tree whenever the graph is updated.
 */
public abstract class CommitTreeModel{

    // The view corresponding to this model
    CommitTreePanelView view;

    // The model from which this class pulls its commits
    SessionModel sessionModel;
    // The graph corresponding to this model
    TreeGraph treeGraph;

    // A list of commits in this model
    private List<CommitHelper> commitsInModel;
    private List<BranchHelper> branchesInModel;
    private List<TagHelper> tagsInModel;

    // A list of tags that haven't been pushed yet
    public List<TagHelper> tagsToBePushed;

    static final Logger logger = LogManager.getLogger();

    /**
     * Constructs a new commit tree model that supplies the data for the given
     * view
     * @param model the model with which this class accesses the commits
     * @param view the view that will be updated with the new graph
     */
    public CommitTreeModel(SessionModel model, CommitTreePanelView view){
        this.sessionModel = model;
        this.view = view;
        this.view.setName("Generic commit tree");
        CommitTreeController.allCommitTreeModels.add(this);
        this.commitsInModel = new ArrayList<>();
        this.branchesInModel = new ArrayList<>();
    }

    /**
     * @param repoHelper the repository to get the branches from
     * @return a list of all branches tracked by this model
     */
    protected abstract List<BranchHelper> getAllBranches(RepoHelper repoHelper);

    /**
     * @param repoHelper the repository to get the commits from
     * @return a list of all commits tracked by this model
     */
    protected abstract List<CommitHelper> getAllCommits(RepoHelper repoHelper);

    /**
     * @param id the id to check
     * @return true if the given id corresponds to a commit in the tree, false otherwise
     */
    public boolean containsID(String id){
        return treeGraph != null && treeGraph.treeGraphModel.containsID(id);
    }

    /**
     * Initializes the treeGraph, unselects any previously selected commit,
     * and then adds all commits tracked by this model to the tree
     */
    public synchronized void init(){
        treeGraph = this.createNewTreeGraph();

        CommitTreeController.resetSelection();

        if (this.sessionModel.getCurrentRepoHelper() != null) {
            this.addAllCommitsToTree();
            //this.branchesInModel = getAllBranches(this.sessionModel.getCurrentRepoHelper());
            this.branchesInModel = this.sessionModel.getCurrentRepoHelper().getBranchModel().getAllBranches();
        }

        this.initView();
    }

    public synchronized void update() throws GitAPIException, IOException {
        // Get the changes between this model and the repo after updating the repo
        this.sessionModel.getCurrentRepoHelper().updateModel();
        UpdateModel updates = this.getChanges();

        if (!updates.hasChanges()) return;

        this.addCommitsToTree(updates.getCommitsToAdd());
        this.removeCommitsFromTree(updates.getCommitsToRemove());

        TreeLayout.stopMovingCells();
        this.updateView();

        CommitTreeController.setBranchHeads(this, this.sessionModel.getCurrentRepoHelper());
    }


    /**
     * Helper method that checks for differences between a commit tree model and a repo model
     *
     * @return an update model that has all the differences between these
     *
     * TODO: tags
     */
    public UpdateModel getChanges() throws IOException {
        UpdateModel updateModel = new UpdateModel();

        // Added commits are all commits in the current repo helper that aren't in the model's list
        List<CommitHelper> commitsToAdd = new ArrayList<>(this.getAllCommits(this.sessionModel.getCurrentRepoHelper()));
        commitsToAdd.removeAll(this.getCommitsInModel());
        updateModel.setCommitsToAdd(commitsToAdd);

        // Removed commits are those in the model, but not in the current repo helper
        List<CommitHelper> commitsToRemove = new ArrayList<>(this.commitsInModel);
        commitsToRemove.removeAll(this.getAllCommits(this.sessionModel.getCurrentRepoHelper()));
        updateModel.setCommitsToRemove(commitsToRemove);

        List<BranchHelper> branchesToUpdate = new ArrayList<>(this.sessionModel.getCurrentRepoHelper().getBranchModel().getAllBranches());
        Map<String, BranchHelper> currentBranchMap = new HashMap<>();
        Map<String, BranchHelper> updateBranchMap = new HashMap<>();

        for (BranchHelper branch : this.branchesInModel)
            currentBranchMap.put(branch.getBranchName(), branch);
        for (BranchHelper branch : branchesToUpdate)
            updateBranchMap.put(branch.getBranchName(), branch);

        // Check for added and changed branches
        for (BranchHelper branch : branchesToUpdate) {
            if (currentBranchMap.containsKey(branch.getBranchName()) &&
                    currentBranchMap.get(branch.getBranchName()).getHead().getId().equals(branch.getHeadId().getName()))
                continue;
            updateModel.addBranch(branch);
        }
        // Check if there are removed branches
        for (BranchHelper branch : this.branchesInModel) {
            if (!updateBranchMap.containsKey(branch.getBranchName()))
                updateModel.addBranch(branch);
        }


        return updateModel;
    }

    /**
     * Adds a pseudo-cell of type InvisibleCell to the treeGraph.
     * @param id the id of the cell to add
     */
    public void addInvisibleCommit(String id){
        CommitHelper invisCommit = sessionModel.getCurrentRepoHelper().getCommit(id);

        if (invisCommit != null) {

            for(CommitHelper c : invisCommit.getParents()){
                if(!treeGraph.treeGraphModel.containsID(c.getId())){
                    addInvisibleCommit(c.getId());
                }
            }

            this.addCommitToTree(invisCommit, invisCommit.getParents(),
                    treeGraph.treeGraphModel, false);

            // If there are tags in the repo that haven't been pushed, allow them to be pushed
            if (invisCommit.getTags() != null) {
                if (tagsToBePushed == null)
                    tagsToBePushed = new ArrayList<>();
                tagsToBePushed.addAll(invisCommit.getTags());
            }
        }
    }

    /**
     * Gets all commits tracked by this model and adds them to the tree
     * @return true if the tree was updated, otherwise false
     */
    private boolean addAllCommitsToTree() {
        return this.addCommitsToTree(this.getAllCommits(this.sessionModel.getCurrentRepoHelper()));
    }

    /**
     * Creates a new TreeGraph with a new model. Updates the list
     * of all models accordingly
     * @return the newly created graph
     */
    private TreeGraph createNewTreeGraph(){
        TreeGraphModel graphModel = new TreeGraphModel();
        treeGraph = new TreeGraph(graphModel);
        return treeGraph;
    }

    /**
     * Adds the given list of commits to the treeGraph
     * @param commits the commits to add
     * @return true if commits where added, else false
     */
    private boolean addCommitsToTree(List<CommitHelper> commits){
        if(commits.size() == 0) return false;

        for(CommitHelper curCommitHelper : commits){
            List<CommitHelper> parents = curCommitHelper.getParents();
            this.addCommitToTree(curCommitHelper, parents, treeGraph.treeGraphModel, true);
        }

        return true;
    }

    /**
     * Removes the given list of commits from the treeGraph
     * @param commits the commits to remove
     * @return true if commits were removed, else false
     */
    private boolean removeCommitsFromTree(List<CommitHelper> commits){
        if(commits.size() == 0) return false;

        for(CommitHelper curCommitHelper : commits)
            this.removeCommitFromTree(curCommitHelper, treeGraph.treeGraphModel);

        return true;
    }

    /**
     * Adds a single commit to the tree with the given parents. Ensures the given parents are
     * already added to the tree, and if they aren't, adds them
     * @param commitHelper the commit to be added
     * @param parents a list of this commit's parents
     * @param graphModel the treeGraphModel to add the commit to
     */
    private void addCommitToTree(CommitHelper commitHelper, List<CommitHelper> parents, TreeGraphModel graphModel, boolean visible){
        List<String> parentIds = new ArrayList<>(parents.size());

        if (visible) this.commitsInModel.add(commitHelper);

        for(CommitHelper parent : parents){
            if(!graphModel.containsID(RepoHelper.getCommitId(parent))){
                addCommitToTree(parent, parent.getParents(), graphModel, visible);
            }
            parentIds.add(RepoHelper.getCommitId(parent));
        }

        String commitID = RepoHelper.getCommitId(commitHelper);
        if(graphModel.containsID(commitID) && graphModel.isVisible(commitID)){
            return;
        }

        RepoHelper repo = sessionModel.getCurrentRepoHelper();
        String displayLabel = repo.getCommitDescriptorString(commitHelper, false);
        List<String> branchLabels = repo.getBranchModel().getBranchesWithHead(commitHelper);

        graphModel.addCell(commitID, commitHelper.getWhen().getTime(), displayLabel, branchLabels, getContextMenu(commitHelper), parentIds, visible);
    }


    /**
     * Removes a single commit from the tree
     *
     * @param commitHelper the commit to remove
     * @param graphModel the graph model to remove the commit from
     */
    private void removeCommitFromTree(CommitHelper commitHelper, TreeGraphModel graphModel){
        String commitID = RepoHelper.getCommitId(commitHelper);

        this.commitsInModel.remove(commitHelper);

        if(graphModel.containsID(commitID) && graphModel.isVisible(commitID))
            graphModel.removeCell(commitID);
    }


    /**
     * Constructs and returns a context menu corresponding to the given commit. Will
     * be shown on right click in the tree diagram
     * @param commit the commit for which this context menu is for
     * @return the context menu for the commit
     */
    private ContextMenu getContextMenu(CommitHelper commit){
        ContextMenu contextMenu = new ContextMenu();

        MenuItem infoItem = new MenuItem("Show Info");
        infoItem.setOnAction(event -> {
            logger.info("Showed info");
            CommitTreeController.selectCommit(commit.getId(), false, false, false);
        });
        infoItem.disableProperty().bind(CommitTreeController.selectedIDProperty().isEqualTo(commit.getId()));

        Menu relativesMenu = getRelativesMenu(commit);
        Menu revertMenu = getRevertMenu(commit);
        Menu resetMenu = getResetMenu(commit);

        contextMenu.getItems().addAll(revertMenu, resetMenu, new SeparatorMenuItem(), infoItem, relativesMenu);

        return contextMenu;
    }

    /**
     * Helper method for getContextMenu that gets the relativesMenu
     * @param commit CommitHelper
     * @return relativesMenu
     */
    private Menu getRelativesMenu(CommitHelper commit) {
        Menu relativesMenu = new Menu("Show Relatives");

        CheckMenuItem showEdgesItem = new CheckMenuItem("Show Only Relatives' Connections");
        showEdgesItem.setDisable(true);

        MenuItem parentsItem = new MenuItem("Parents");
        parentsItem.setOnAction(event -> {
            logger.info("Selected see parents");
            CommitTreeController.selectCommit(commit.getId(), true, false, false);
        });

        MenuItem childrenItem = new MenuItem("Children");
        childrenItem.setOnAction(event -> {
            logger.info("Selected see children");
            CommitTreeController.selectCommit(commit.getId(), false, true, false);
        });

        MenuItem parentsAndChildrenItem = new MenuItem("Both");
        parentsAndChildrenItem.setOnAction(event -> {
            logger.info("Selected see children and parents");
            CommitTreeController.selectCommit(commit.getId(), true, true, false);
        });

        MenuItem allAncestorsItem = new MenuItem("Ancestors");
        allAncestorsItem.setOnAction(event -> {
            logger.info("Selected see ancestors");
            CommitTreeController.selectCommit(commit.getId(), true, false, true);
        });

        MenuItem allDescendantsItem = new MenuItem("Descendants");
        allDescendantsItem.setOnAction(event -> {
            logger.info("Selected see descendants");
            CommitTreeController.selectCommit(commit.getId(), false, true, true);
        });

        relativesMenu.getItems().setAll(parentsItem, childrenItem, parentsAndChildrenItem,
                new SeparatorMenuItem(), allAncestorsItem, allDescendantsItem,
                new SeparatorMenuItem(), showEdgesItem);

        return relativesMenu;
    }

    /**
     * Helper method for getContextMenu that initializes the revert part of the menu
     * @param commit CommitHelper
     * @return revertMenu
     */
    private Menu getRevertMenu(CommitHelper commit) {
        Menu revertMenu = new Menu("Revert...");
        MenuItem revertItem = new MenuItem("Revert this commit");
        MenuItem revertMultipleItem = new MenuItem("Revert multiple commits...");
        revertMultipleItem.setDisable(true);
        MenuItem helpItem = new MenuItem("Help");

        revertItem.setOnAction(event -> CommitTreeController.sessionController.handleRevertButton(commit));

        revertMultipleItem.setOnAction(event -> {
            //pull up some sort of window
            //PopUpWindows.something
        });

        helpItem.setOnAction(event -> PopUpWindows.showRevertHelpAlert());

        revertMenu.getItems().setAll(revertItem, revertMultipleItem, helpItem);

        return revertMenu;
    }

    private Menu getResetMenu(CommitHelper commit) {
        Menu resetMenu = new Menu("Reset...");
        MenuItem resetItem = new MenuItem("Reset to this commit");
        MenuItem helpItem = new MenuItem("Help");

        resetItem.setOnAction(event -> CommitTreeController.sessionController.handleResetButton(commit));

        helpItem.setOnAction(event -> PopUpWindows.showResetHelpAlert());

        resetMenu.getItems().setAll(resetItem, helpItem);

        return resetMenu;
    }

    /**
     * Updates the corresponding view if possible
     */
    private void updateView() throws IOException{
        if(this.sessionModel != null && this.sessionModel.getCurrentRepoHelper() != null){
            CommitTreeController.update(this);
        }else{
            view.displayEmptyView();
        }
    }

    /**
     * Initializes the corresponding view if possible
     */
    private void initView(){
        if(this.sessionModel != null && this.sessionModel.getCurrentRepoHelper() != null){
            CommitTreeController.init(this);
        }else{
            view.displayEmptyView();
        }
    }

    /**
     * Sets the shape and ref labels for a cell based on the current repo status
     *
     * @param helper the commit helper that we want to set as a branch head
     * @param tracked whether or not the commit is the head of a tracked branch
     */
    public void setCommitAsBranchHead(CommitHelper helper, boolean tracked) {
        String commitId="";
        commitId = helper.getId();
        CellShape shape = (tracked) ? Cell.TRACKED_BRANCH_HEAD_SHAPE : Cell.UNTRACKED_BRANCH_HEAD_SHAPE;

        treeGraph.treeGraphModel.setCellShape(commitId, shape);

        RepoHelper repo = sessionModel.getCurrentRepoHelper();
        String displayLabel = repo.getCommitDescriptorString(commitId, false);
        List<String> branchLabels = repo.getBranchModel().getBranchesWithHead(commitId);
        treeGraph.treeGraphModel.setCellLabels(commitId, displayLabel, branchLabels);
    }

    /**
     * Forgets information about tracked/untracked branch heads in the tree and fills it back in
     */
    public void resetBranchHeads(boolean updateLabels){
        List<String> resetIDs = treeGraph.treeGraphModel.resetCellShapes();
        RepoHelper repo = sessionModel.getCurrentRepoHelper();
        if(updateLabels){
            try {
                this.sessionModel.getCurrentRepoHelper().getBranchModel().updateAllBranches();
            } catch (IOException | GitAPIException e) {
                // Shouldn't happen
            }
            this.branchesInModel = this.sessionModel.getCurrentRepoHelper().getBranchModel().getAllBranches();
            for(String id : resetIDs){
                if(this.sessionModel.getCurrentRepoHelper().getCommit(id) != null) {
                    String displayLabel = repo.getCommitDescriptorString(id, false);
                    List<String> branchLabels = repo.getBranchModel().getBranchesWithHead(id);
                    treeGraph.treeGraphModel.setCellLabels(id, displayLabel, branchLabels);
                }
            }
        }
    }

    public List<TagHelper> getTagsToBePushed() {
        return tagsToBePushed;
    }

    public String getViewName() {
        return this.view.getName();
    }



    public List<CommitHelper> getCommitsInModel() { return this.commitsInModel; }

    public List<BranchHelper> getBranchesInModel() { return this.branchesInModel; }

    public List<TagHelper> getTagsInModel() { return this.tagsInModel; }
}
