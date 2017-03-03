package elegit;

import elegit.exceptions.MissingRepoException;
import elegit.treefx.*;
import elegit.treefx.Cell;
import javafx.scene.control.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    private List<CommitHelper> localCommitsInModel;
    private List<CommitHelper> remoteCommitsInModel;
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
        this.localCommitsInModel = new ArrayList<>();
        this.remoteCommitsInModel = new ArrayList<>();
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
        // Handles rare edge case with the RepositoryMonitor and removing repos
        if(this.sessionModel.getCurrentRepoHelper() != null){
            // Get the changes between this model and the repo after updating the repo
            this.sessionModel.getCurrentRepoHelper().updateModel();
            UpdateModel updates = this.getChanges();

            if (!updates.hasChanges()) return;

            this.removeCommitsFromTree(updates.getCommitsToRemove());
            this.addCommitsToTree(updates.getCommitsToAdd());
            this.updateCommitFills(updates.getCommitsToUpdate());
            this.sessionModel.getCurrentRepoHelper().getBranchModel().updateAllBranches();
            this.resetBranchHeads();
            this.updateAllRefLabels();

            TreeLayout.stopMovingCells();
            this.updateView();
        }
    }


    /**
     * Helper method that checks for differences between a commit tree model and a repo model
     *
     * @return an update model that has all the differences between these
     */
    public UpdateModel getChanges() throws IOException {
        UpdateModel updateModel = new UpdateModel();
        RepoHelper repo = this.sessionModel.getCurrentRepoHelper();

        // Added commits are all commits in the current repo helper that aren't in the model's list
        List<CommitHelper> commitsToAdd = new ArrayList<>(this.getAllCommits(this.sessionModel.getCurrentRepoHelper()));
        commitsToAdd.removeAll(this.getCommitsInModel());
        updateModel.setCommitsToAdd(commitsToAdd);

        // Removed commits are those in the model, but not in the current repo helper
        List<CommitHelper> commitsToRemove = new ArrayList<>(this.commitsInModel);
        commitsToRemove.removeAll(this.getAllCommits(this.sessionModel.getCurrentRepoHelper()));
        updateModel.setCommitsToRemove(commitsToRemove);

        // Updated commits are ones that have changed whether they are tracked locally
        // or uploaded to the server.
        // (remote-model's remote)+(model's remote-remote)+(local-model's local)+(model's local-local)
        List<CommitHelper> commitsToUpdate = new ArrayList<>(this.localCommitsInModel);
        commitsToUpdate.removeAll(repo.getLocalCommits());
        updateModel.updateCommits(commitsToUpdate);
        commitsToUpdate = new ArrayList<>(repo.getLocalCommits());
        commitsToUpdate.removeAll(this.localCommitsInModel);
        updateModel.updateCommits(commitsToUpdate);
        commitsToUpdate = new ArrayList<>(this.remoteCommitsInModel);
        commitsToUpdate.removeAll(repo.getRemoteCommits());
        updateModel.updateCommits(commitsToUpdate);
        commitsToUpdate = new ArrayList<>(repo.getRemoteCommits());
        commitsToUpdate.removeAll(this.remoteCommitsInModel);
        updateModel.updateCommits(commitsToUpdate);

        commitsToUpdate = updateModel.getCommitsToUpdate();
        commitsToUpdate.removeAll(commitsToRemove);
        updateModel.setCommitsToUpdate(commitsToUpdate);

        /* ************************ BRANCHES ************************ */

        List<BranchHelper> branchesToUpdate = new ArrayList<>(this.sessionModel.getCurrentRepoHelper().getBranchModel().getAllBranches());
        Map<String, BranchHelper> currentBranchMap = new HashMap<>();
        Map<String, BranchHelper> updateBranchMap = new HashMap<>();

        for (BranchHelper branch : this.branchesInModel) {
            currentBranchMap.put(branch.getRefName(), branch);
        }
        for (BranchHelper branch : branchesToUpdate)
            updateBranchMap.put(branch.getRefName(), branch);

        // Check for added and changed branches
        for (BranchHelper branch : branchesToUpdate) {
            if (currentBranchMap.containsKey(branch.getRefName())){
                if(currentBranchMap.get(branch.getRefName()).getCommit().getId().equals(branch.getHeadId().getName())){
                    continue;
                }
            }
            updateModel.addBranch(branch);
        }
        // Check if there are removed branches
        for (BranchHelper branch : this.branchesInModel) {
            if (!updateBranchMap.containsKey(branch.getRefName()))
                updateModel.addBranch(branch);
        }

        /* ************************ TAGS ************************ */
        List<TagHelper> tagsInRepo = new ArrayList<>(this.sessionModel.getCurrentRepoHelper().getTagModel().getAllTags());

        // Check for added tags
        for (TagHelper tag : tagsInRepo)
            if (!this.tagsInModel.contains(tag))
                updateModel.addTag(tag);

        // Check for removed tags
        for (TagHelper tag : this.tagsInModel)
            if (!tagsInRepo.contains(tag))
                updateModel.addTag(tag);


        return updateModel;
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
            this.addCommitToTree(curCommitHelper, parents, treeGraph.treeGraphModel);
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

    private boolean updateCommitFills(List<CommitHelper> commits) {
        if(commits.size() == 0) return false;

        for(CommitHelper curCommitHelper : commits)
            this.updateCommitFill(curCommitHelper, treeGraph.treeGraphModel, this.sessionModel.getCurrentRepoHelper());

        return true;
    }

    /**
     * Adds a single commit to the tree with the given parents. Ensures the given parents are
     * already added to the tree, and if they aren't, adds them
     * @param commitHelper the commit to be added
     * @param parents a list of this commit's parents
     * @param graphModel the treeGraphModel to add the commit to
     */
    private void addCommitToTree(CommitHelper commitHelper, List<CommitHelper> parents, TreeGraphModel graphModel){
        List<String> parentIds = new ArrayList<>(parents.size());

        RepoHelper repo = sessionModel.getCurrentRepoHelper();
        String displayLabel = repo.getCommitDescriptorString(commitHelper, false);
        List<String> branchLabels = repo.getBranchModel().getBranchesWithHead(commitHelper);
        List<RefHelper> refLabels = repo.getRefsForCommit(commitHelper);
        Cell.CellType computedType = repo.getCommitType(commitHelper);

        this.commitsInModel.add(commitHelper);
        if (computedType == Cell.CellType.BOTH || computedType == Cell.CellType.LOCAL)
            this.localCommitsInModel.add(commitHelper);
        if (computedType == Cell.CellType.BOTH || computedType == Cell.CellType.REMOTE)
            this.remoteCommitsInModel.add(commitHelper);

        for(CommitHelper parent : parents){
            if(!graphModel.containsID(RepoHelper.getCommitId(parent))){
                addCommitToTree(parent, parent.getParents(), graphModel);
            }
            parentIds.add(RepoHelper.getCommitId(parent));
        }

        String commitID = RepoHelper.getCommitId(commitHelper);
        if(graphModel.containsID(commitID)){
            graphModel.setCellType(commitID, computedType);
            return;
        }

        graphModel.addCell(commitID, commitHelper.getWhen().getTime(), displayLabel, refLabels, getContextMenu(commitHelper), parentIds, computedType);
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
        this.localCommitsInModel.remove(commitHelper);
        this.remoteCommitsInModel.remove(commitHelper);

        graphModel.removeCell(commitID);
    }

    private void updateCommitFill(CommitHelper helper, TreeGraphModel graphModel, RepoHelper repo) {
        Cell.CellType type = (repo.getLocalCommits().contains(helper)) ?
                (repo.getRemoteCommits().contains(helper)) ? Cell.CellType.BOTH : Cell.CellType.LOCAL : Cell.CellType.REMOTE;
        this.localCommitsInModel.remove(helper);
        this.remoteCommitsInModel.remove(helper);
        switch (type) {
            case LOCAL:
                this.localCommitsInModel.add(helper);
                break;
            case REMOTE:
                this.remoteCommitsInModel.add(helper);
                break;
            case BOTH:
                this.localCommitsInModel.add(helper);
                this.remoteCommitsInModel.add(helper);
                break;
            default:
                break;
        }
        graphModel.setCellType(helper.getId(), type);
    }

    /**
     * Constructs and returns a context menu corresponding to the given tag. Will
     * be shown on right click on the tag label
     * @param tagHelper the tag that this context menu will refer to
     * @return the context menu with a delete option
     */
    public ContextMenu getTagLabelMenu(TagHelper tagHelper) {
        ContextMenu contextMenu = new ContextMenu();

        MenuItem deleteitem = new MenuItem("Delete");
        deleteitem.setOnAction(event -> {
            logger.info("Delete tag dialog started.");
            if (tagHelper.presentDeleteDialog()) {
                try {
                    sessionModel.getCurrentRepoHelper().getTagModel().deleteTag(tagHelper.getRefName());
                    update();
                } catch (GitAPIException | MissingRepoException | IOException e) {
                    e.printStackTrace();
                }
            }
        });
        contextMenu.getItems().addAll(deleteitem);

        return contextMenu;
    }

    /**
     * Constructs and returns a context menu corresponding to the given branch. Will
     * be shown on right click on the branch label
     * @param branch the branch to have a menu for
     * @return the context menu with various options related to branches
     */
    private ContextMenu getBranchLabelMenu(BranchHelper branch) {
        ContextMenu contextMenu = new ContextMenu();

        MenuItem checkoutItem = new MenuItem("Checkout");
        checkoutItem.setOnAction(event -> CommitTreeController.sessionController.checkoutBranch(branch) );

        MenuItem deleteitem = new MenuItem("Delete");
        deleteitem.setOnAction(event -> CommitTreeController.sessionController.deleteBranch(branch) );

        contextMenu.getItems().addAll(checkoutItem, deleteitem);

        return contextMenu;
    }


    /**
     * Constructs and returns a context menu corresponding to the given commit. Will
     * be shown on right click in the tree diagram
     * @param commit the commit for which this context menu is for
     * @return the context menu for the commit
     */
    private ContextMenu getContextMenu(CommitHelper commit){
        ContextMenu contextMenu = new ContextMenu();

        MenuItem checkoutItem = new MenuItem("Checkout files...");
        checkoutItem.setOnAction(event -> {
            logger.info("Checkout files from commit button clicked");
            CommitTreeController.sessionController.handleCheckoutFilesButton(commit);
        });
        Menu relativesMenu = getRelativesMenu(commit);
        Menu revertMenu = getRevertMenu(commit);
        Menu resetMenu = getResetMenu(commit);

        contextMenu.getItems().addAll(revertMenu, resetMenu, checkoutItem, new SeparatorMenuItem(), relativesMenu);

        return contextMenu;
    }

    /**
     * Helper method for getContextMenu that gets the relativesMenu
     * @param commit CommitHelper
     * @return relativesMenu
     */
    private Menu getRelativesMenu(CommitHelper commit) {
        Menu relativesMenu = new Menu("Show Relatives");

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

        relativesMenu.getItems().setAll(parentsItem, childrenItem, parentsAndChildrenItem);

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
        revertMultipleItem.disableProperty().bind(CommitTreeController.multipleNotSelectedProperty);
        MenuItem helpItem = new MenuItem("Help");

        revertItem.setOnAction(event -> CommitTreeController.sessionController.handleRevertButton(commit));

        revertMultipleItem.setOnAction(event -> {
            // Some fancy lambda syntax and collect call
            List<CommitHelper> commits = commitsInModel.stream().filter(commitHelper ->
                    CommitTreeController.getSelectedIds().contains(commitHelper.getName())).collect(Collectors.toList());
            CommitTreeController.sessionController.handleRevertMultipleButton(commits);
        });

        helpItem.setOnAction(event -> PopUpWindows.showRevertHelpAlert());

        revertMenu.getItems().setAll(revertItem, revertMultipleItem, helpItem);

        return revertMenu;
    }

    private Menu getResetMenu(CommitHelper commit) {
        Menu resetMenu = new Menu("Reset...");
        MenuItem resetItem = new MenuItem("Reset to this commit");
        MenuItem helpItem = new MenuItem("Help");
        Menu advancedMenu = getAdvancedResetMenu(commit);

        resetItem.setOnAction(event -> CommitTreeController.sessionController.handleResetButton(commit));

        helpItem.setOnAction(event -> PopUpWindows.showResetHelpAlert());

        resetMenu.getItems().setAll(resetItem, advancedMenu, helpItem);

        return resetMenu;
    }

    private Menu getAdvancedResetMenu(CommitHelper commit) {
        Menu resetMenu = new Menu("Advanced");
        MenuItem hardItem = new MenuItem("reset --hard");
        MenuItem mixedItem = new MenuItem("reset --mixed");
        MenuItem softItem = new MenuItem("reset --soft");

        hardItem.setOnAction(event ->
                CommitTreeController.sessionController.handleAdvancedResetButton(commit, ResetCommand.ResetType.HARD));
        mixedItem.setOnAction(event ->
                CommitTreeController.sessionController.handleAdvancedResetButton(commit, ResetCommand.ResetType.MIXED));
        softItem.setOnAction(event ->
                CommitTreeController.sessionController.handleAdvancedResetButton(commit, ResetCommand.ResetType.SOFT));

        resetMenu.getItems().setAll(hardItem, mixedItem, softItem);

        return resetMenu;
    }

    /**
     * Updates the corresponding view if possible
     */
    public void updateView() throws IOException{
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
        /*
        RepoHelper repo = sessionModel.getCurrentRepoHelper();
        String displayLabel = repo.getCommitDescriptorString(commitId, false);
        List<String> branchLabels = repo.getBranchModel().getBranchesWithHead(commitId);
        treeGraph.treeGraphModel.setCellLabels(commitId, displayLabel, branchLabels);
        treeGraph.treeGraphModel.setCurrentCellLabels(commitId,this.sessionModel.getCurrentRepoHelper().getBranchModel().getCurrentBranches());*/
    }

    /**
     * Looks for all ref labels, then adds them to the commit tree graph
     */
    public void updateAllRefLabels() {
        RepoHelper repo = sessionModel.getCurrentRepoHelper();

        List<RefHelper> refHelpers = new ArrayList<>();
        refHelpers.addAll(repo.getBranchModel().getAllBranches());
        refHelpers.addAll(repo.getTagModel().getAllTags());

        List<RemoteBranchHelper> remotes = repo.getBranchModel().getRemoteBranchesTyped();

        Map<RefHelper, ContextMenu> menuMap = new HashMap<>();
        List<String> remoteBranches = new ArrayList<>();

        this.tagsInModel = repo.getTagModel().getAllTags();

        Map<String, List<RefHelper>> commitLabelMap = new HashMap<>();

        addCommitRefMaps(refHelpers, commitLabelMap, menuMap);

        for (RemoteBranchHelper helper : remotes) {
            remoteBranches.add(helper.getRefName());
        }

        // Set the labels
        for (String commit : commitLabelMap.keySet()) {
            if(this.sessionModel.getCurrentRepoHelper().getCommit(commit) != null) {
                String displayLabel = repo.getCommitDescriptorString(commit, false);
                treeGraph.treeGraphModel.setCellLabels(commit, displayLabel, commitLabelMap.get(commit));
                treeGraph.treeGraphModel.setCurrentCellLabels(commit, this.sessionModel.getCurrentRepoHelper().getBranchModel().getCurrentAbbrevBranches());

                treeGraph.treeGraphModel.setLabelMenus(commit, menuMap);
                treeGraph.treeGraphModel.setRemoteBranchCells(commit, remoteBranches);
            }
        }
    }

    private void addCommitRefMaps(List<RefHelper> helpers, Map<String, List<RefHelper>> commitLabelMap,
                                                          Map<RefHelper, ContextMenu> menuMap) {
        String commitId;
        for (RefHelper helper : helpers) {
            commitId = helper.getCommit().getId();

            if (helper instanceof TagHelper)
                menuMap.put(helper, getTagLabelMenu((TagHelper)helper));
            else
                menuMap.put(helper, getBranchLabelMenu((BranchHelper)helper));

            if (commitLabelMap.containsKey(commitId))
                commitLabelMap.get(commitId).add(helper);
            else {
                List<RefHelper> newList = new ArrayList<>();
                newList.add(helper);
                commitLabelMap.put(commitId, newList);
            }
        }
    }

    /**
     * Forgets information about tracked/untracked branch heads in the tree and updates the model
     */
    public void resetBranchHeads(){
        List<String> resetIDs = treeGraph.treeGraphModel.resetCellShapes();
        RepoHelper repo = sessionModel.getCurrentRepoHelper();
        this.branchesInModel = repo.getBranchModel().getAllBranches();
        for(String id : resetIDs){
            if(this.sessionModel.getCurrentRepoHelper().getCommit(id) != null) {
                String displayLabel = repo.getCommitDescriptorString(id, false);
                List<RefHelper> branchLabels = new ArrayList<>();
                treeGraph.treeGraphModel.setCellLabels(id, displayLabel, branchLabels);
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
