package elegit.treefx;

import elegit.Main;
import elegit.gui.PopUpWindows;
import elegit.models.SessionModel;
import elegit.exceptions.MissingRepoException;
import elegit.models.*;
import javafx.scene.control.*;
import org.apache.http.annotation.ThreadSafe;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;


/**
 * Handles the conversion/creation of a list of commit helpers into a nice
 * tree structure. It also takes care of updating the view its given to
 * display the new tree whenever the graph is updated.
 *
 * As a singleton class, there will be only one object. To ensure complete threadsafety,
 * use the "monitor" pattern; all methods are synchronized.
 */
@ThreadSafe
// but critically only because of all the asserts requiring this be done only in the FX thread. Without that, it
// isn't threadsafe. This is currently very tangled up with the TreeGraph, which ultimately keeps track of hierarchy
// in the FX data. There are also a lot of methods in here that should really be in the controller, not in the model.
// But again, these are really knotted together currently.
public class CommitTreeModel{

    // The singleton reference
    private static final CommitTreeModel commitTreeModel = new CommitTreeModel();

    private static final Logger logger = LogManager.getLogger();

    // The view corresponding to this model
    private final AtomicReference<CommitTreePanelView> view = new AtomicReference<>();

    // The graph corresponding to this model
    private final TreeGraph treeGraph = new TreeGraph(new TreeGraphModel());

    // A list of commits in this model
    private final Set<CommitHelper> commitsInModel = ConcurrentHashMap.newKeySet();
    private final Set<CommitHelper> localCommitsInModel = ConcurrentHashMap.newKeySet();
    private final Set<CommitHelper> remoteCommitsInModel = ConcurrentHashMap.newKeySet();
    private final Set<BranchHelper> branchesInModel = ConcurrentHashMap.newKeySet();
    private final Set<TagHelper> tagsInModel = ConcurrentHashMap.newKeySet();


    public synchronized static CommitTreeModel getCommitTreeModel() {
        return commitTreeModel;
    }

    public synchronized void setView(CommitTreePanelView view) {
        Main.assertFxThread();
        this.view.set(view);
    }

    public synchronized CommitTreePanelView getView() {
        Main.assertFxThread();
        return view.get();
    }

    public synchronized TreeGraph getTreeGraph() {
        Main.assertFxThread();
        return treeGraph;
    }

    /**
     * @param repoHelper the repository to get the commits from
     * @return a list of all commits tracked by this model
     */
    private synchronized Set<CommitHelper> getAllCommits(RepoHelper repoHelper) {
        return repoHelper.getAllCommits();
    }

    /**
     * @param id the id to check
     * @return true if the given id corresponds to a commit in the tree, false otherwise
     */
    public synchronized boolean containsID(String id){
        Main.assertFxThread();
        return treeGraph.treeGraphModel.containsID(id);
    }

    /**
     * Initializes the treeGraph, unselects any previously selected commit,
     * and then adds all commits tracked by this model to the tree
     */
    public synchronized void init(){
        Main.assertFxThread();

        CommitTreeController.resetSelection();

        if (SessionModel.getSessionModel().getCurrentRepoHelper() != null) {
            this.addAllCommitsToTree();
            this.branchesInModel.clear();
            this.branchesInModel.addAll(SessionModel.getSessionModel().getCurrentRepoHelper().getBranchModel().getAllBranches());
        }

        this.initView();
    }

    public synchronized void update() throws GitAPIException, IOException {
        Main.assertFxThread();
        // Handles rare edge case with the RepositoryMonitor and removing repos
        if(SessionModel.getSessionModel().getCurrentRepoHelper() != null){
            // Get the changes between this model and the repo after updating the repo
            SessionModel.getSessionModel().getCurrentRepoHelper().updateModel();
            UpdateModel updates = this.getChanges();

            if (!updates.hasChanges()) return;

            System.out.println("CommitTreeModel.update before 1");
            this.removeCommitsFromTree(updates.getCommitsToRemove());
            System.out.println("CommitTreeModel.update before 2");
            this.addCommitsToTree(updates.getCommitsToAdd());
            System.out.println("CommitTreeModel.update before 3");
            this.updateCommitFills(updates.getCommitsToUpdate());  // SLOW
            System.out.println("CommitTreeModel.update before 4");
            SessionModel.getSessionModel().getCurrentRepoHelper().getBranchModel().updateAllBranches();
            this.resetBranchHeads();
            this.updateAllRefLabels();
            TreeLayout.stopMovingCells();
            this.updateView();  // SLOW
            System.out.println("CommitTreeModel.update after");
        }
    }


    /**
     * Helper method that checks for differences between a commit tree model and a repo model
     *
     * @return an update model that has all the differences between these
     */
    public synchronized UpdateModel getChanges() throws IOException {
        UpdateModel updateModel = new UpdateModel();
        RepoHelper repo = SessionModel.getSessionModel().getCurrentRepoHelper();

        // Added commits are all commits in the current repo helper that aren't in the model's list
        List<CommitHelper> commitsToAdd = new ArrayList<>(this.getAllCommits(SessionModel.getSessionModel().getCurrentRepoHelper()));
        commitsToAdd.removeAll(this.getCommitsInModel());
        updateModel.setCommitsToAdd(commitsToAdd);

        // Removed commits are those in the model, but not in the current repo helper
        List<CommitHelper> commitsToRemove = new ArrayList<>(this.commitsInModel);
        commitsToRemove.removeAll(this.getAllCommits(SessionModel.getSessionModel().getCurrentRepoHelper()));
        updateModel.setCommitsToRemove(commitsToRemove);

        // Updated commits are ones that have changed whether they are tracked locally
        // or uploaded to the server.
        // (remote-model's remote)+(model's remote-remote)+(local-model's local)+(model's local-local)
        Set<CommitHelper> commitsToUpdate = new HashSet<>(this.localCommitsInModel);
        commitsToUpdate.removeAll(repo.getLocalCommits());
        updateModel.updateCommits(commitsToUpdate);
        commitsToUpdate = new HashSet<>(repo.getLocalCommits());
        commitsToUpdate.removeAll(this.localCommitsInModel);
        updateModel.updateCommits(commitsToUpdate);
        commitsToUpdate = new HashSet<>(this.remoteCommitsInModel);
        commitsToUpdate.removeAll(repo.getRemoteCommits());
        updateModel.updateCommits(commitsToUpdate);
        commitsToUpdate = new HashSet<>(repo.getRemoteCommits());
        commitsToUpdate.removeAll(this.remoteCommitsInModel);
        updateModel.updateCommits(commitsToUpdate);

        commitsToUpdate = new HashSet<>(updateModel.getCommitsToUpdate());
        commitsToUpdate.removeAll(commitsToRemove);
        updateModel.setCommitsToUpdate(commitsToUpdate);

        /* ************************ BRANCHES ************************ */

        List<BranchHelper> branchesToUpdate = new ArrayList<>(SessionModel.getSessionModel().getCurrentRepoHelper().getBranchModel().getAllBranches());
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
                if(currentBranchMap.get(branch.getRefName()).getCommit().getName().equals(branch.getHeadId().getName())){
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
        List<TagHelper> tagsInRepo = new ArrayList<>(SessionModel.getSessionModel().getCurrentRepoHelper().getTagModel().getAllTags());

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
     * Class to hold updates for a commit tree model
     */
    private class UpdateModel {
        private final Set<CommitHelper> commitsToAdd = ConcurrentHashMap.newKeySet();
        private final Set<CommitHelper> commitsToRemove = ConcurrentHashMap.newKeySet();
        private final Set<CommitHelper> commitsToUpdate = ConcurrentHashMap.newKeySet();
        private final Set<BranchHelper> branchesToUpdate = ConcurrentHashMap.newKeySet();
        private final Set<TagHelper> tagsToUpdate = ConcurrentHashMap.newKeySet();

        /**
         * Method to see if there are changes in the update model
         * @return if there are any changes that need to be processed
         */
        public boolean hasChanges() {
            return this.commitsToAdd.size()+this.commitsToRemove.size()+this.commitsToUpdate.size()
                    +this.branchesToUpdate.size()+this.tagsToUpdate.size()>0;
        }

        void updateCommits(Set<CommitHelper> commits) { this.commitsToUpdate.addAll(commits); }
        void addBranch(BranchHelper branch) { this.branchesToUpdate.add(branch); }
        void addTag(TagHelper tag) { this.tagsToUpdate.add(tag); }


        // ********************* GETTERS AND SETTERS ************************

        Set<CommitHelper> getCommitsToAdd() {
            return Collections.unmodifiableSet(this.commitsToAdd);
        }

        Set<CommitHelper> getCommitsToRemove() {
            return Collections.unmodifiableSet(this.commitsToRemove);
        }

        Set<CommitHelper> getCommitsToUpdate() {
            return Collections.unmodifiableSet(this.commitsToUpdate);
        }

        void setCommitsToAdd(List<CommitHelper> commitsToAdd) {
            this.commitsToAdd.clear();
            this.commitsToAdd.addAll(commitsToAdd);
        }
        void setCommitsToRemove(List<CommitHelper> commitsToRemove) {
            this.commitsToRemove.clear();
            this.commitsToRemove.addAll(commitsToRemove);
        }

        void setCommitsToUpdate(Set<CommitHelper> commitsToUpdate) {
            this.commitsToUpdate.clear();
            this.commitsToUpdate.addAll(commitsToUpdate);
        }
    }


    /**
     * Gets all commits tracked by this model and adds them to the tree
     * @return true if the tree was updated, otherwise false
     */
    private synchronized boolean addAllCommitsToTree() {
        return this.addCommitsToTree(this.getAllCommits(SessionModel.getSessionModel().getCurrentRepoHelper()));
    }

    /**
     * Adds the given list of commits to the treeGraph
     * @param commits the commits to add
     * @return true if commits where added, else false
     */
    private synchronized boolean addCommitsToTree(Set<CommitHelper> commits){
        if(commits.size() == 0) return false;

        Main.startTime = 0;
        for(CommitHelper curCommitHelper : commits){
            this.addCommitToTree(curCommitHelper, treeGraph.treeGraphModel);
        }

        return true;
    }

    /**
     * Removes the given list of commits from the treeGraph
     * @param commits the commits to remove
     */
    private synchronized void removeCommitsFromTree(Set<CommitHelper> commits){
        if(commits.size() == 0) return;

        for(CommitHelper curCommitHelper : commits)
            this.removeCommitFromTree(curCommitHelper, treeGraph.treeGraphModel);

    }

    private synchronized void updateCommitFills(Set<CommitHelper> commits) {
        if(commits.size() == 0) return;

        RepoHelper repoHelper = SessionModel.getSessionModel().getCurrentRepoHelper();
        for(CommitHelper curCommitHelper : commits)
            this.updateCommitFill(curCommitHelper, treeGraph.treeGraphModel, repoHelper);

    }

    /**
     * Adds a single commit to the tree with the given parents. Ensures the given parents are
     * already added to the tree, and if they aren't, adds them
     * @param commitHelper the commit to be added
     * @param graphModel the treeGraphModel to add the commit to
     */
    private synchronized void addCommitToTree(CommitHelper commitHelper, TreeGraphModel graphModel) {
        Main.assertFxThread();

        ArrayDeque<CommitHelper> traversalStack = new ArrayDeque<>();

        ArrayDeque<CommitHelper> queue = new ArrayDeque<>();
        HashSet<String> idsAlreadySeen = new HashSet<>();

        traversalStack.push(commitHelper);
        idsAlreadySeen.add(commitHelper.getName());

        // Parents need to be entered in first, so do a breadth-first traversal of all aneso
        while (!traversalStack.isEmpty()) {

            CommitHelper nextOneUp = traversalStack.peek();

            List<CommitHelper> parents = nextOneUp.getParents();

            boolean addedParents = false;
            for (CommitHelper parent : parents) {
                if (!idsAlreadySeen.contains(parent.getName()) && !graphModel.containsID(parent.getName()))
                {
                    traversalStack.push(parent);
                    addedParents = true;
                }

            }
            if (!addedParents) {
                CommitHelper toAdd = traversalStack.pop();
                queue.offer(toAdd);
                idsAlreadySeen.add(toAdd.getName());
            }
        }

        RepoHelper repo = SessionModel.getSessionModel().getCurrentRepoHelper();
        while (!queue.isEmpty()) {

            CommitHelper commitToAdd = queue.poll();
            this.commitsInModel.add(commitToAdd);

            Cell.CellType computedType = repo.getCommitType(commitToAdd);
            if (computedType == Cell.CellType.BOTH || computedType == Cell.CellType.LOCAL)
                this.localCommitsInModel.add(commitToAdd);
            if (computedType == Cell.CellType.BOTH || computedType == Cell.CellType.REMOTE)
                this.remoteCommitsInModel.add(commitToAdd);

            String commitID = commitToAdd.getName();
            if (graphModel.containsID(commitID)) {
                graphModel.setCellType(commitToAdd);
            } else {
                graphModel.addCell(commitToAdd);
            }
        }
    }


    /**
     * Removes a single commit from the tree
     *
     * @param commitHelper the commit to remove
     * @param graphModel the graph model to remove the commit from
     */
    private synchronized void removeCommitFromTree(CommitHelper commitHelper, TreeGraphModel graphModel){
        Main.assertFxThread();
        String commitID = commitHelper.getName();

        this.commitsInModel.remove(commitHelper);
        this.localCommitsInModel.remove(commitHelper);
        this.remoteCommitsInModel.remove(commitHelper);

        graphModel.removeCell(commitID);
    }

    private synchronized void updateCommitFill(CommitHelper helper, TreeGraphModel graphModel, RepoHelper repo) {
        Main.assertFxThread();
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
        graphModel.setCellType(helper);
    }

    /**
     * Constructs and returns a context menu corresponding to the given tag. Will
     * be shown on right click on the tag label
     * @param tagHelper the tag that this context menu will refer to
     * @return the context menu with a delete option
     */
    public synchronized ContextMenu getTagLabelMenu(TagHelper tagHelper) {
        Main.assertFxThread();
        ContextMenu contextMenu = new ContextMenu();

        MenuItem deleteitem = new MenuItem("Delete");
        deleteitem.setOnAction(event -> {
            logger.info("Delete tag dialog started.");
            if (presentDeleteDialog(tagHelper)) {
                try {
                    SessionModel.getSessionModel().getCurrentRepoHelper().getTagModel().deleteTag(tagHelper.getRefName());
                    update();
                } catch (GitAPIException | MissingRepoException | IOException e) {
                    e.printStackTrace();
                }
            }
        });
        contextMenu.getItems().addAll(deleteitem);

        return contextMenu;
    }

    private synchronized boolean presentDeleteDialog(TagHelper tagHelper) {
        Main.assertFxThread();
        //Create the dialog
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Delete Tag");
        dialog.setHeaderText("Are you sure you want to delete tag "+ tagHelper.getRefName() +"?");

        ButtonType confirm = new ButtonType("Yes", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(confirm, ButtonType.CANCEL);

        dialog.setResultConverter(dialogButton -> dialogButton == confirm);

        Optional<Boolean> result = dialog.showAndWait();

        return result.orElse(false);
    }

    /**
     * Constructs and returns a context menu corresponding to the given branch. Will
     * be shown on right click on the branch label
     * @param branch the branch to have a menu for
     * @return the context menu with various options related to branches
     */
    private synchronized ContextMenu getBranchLabelMenu(BranchHelper branch) {
        Main.assertFxThread();
        ContextMenu contextMenu = new ContextMenu();

        MenuItem checkoutItem = new MenuItem("Checkout");
        checkoutItem.setOnAction(event -> CommitTreeController.getSessionController().checkoutBranch(branch) );

        MenuItem deleteitem = new MenuItem("Delete");
        deleteitem.setOnAction(event -> CommitTreeController.getSessionController().deleteBranch(branch) );

        contextMenu.getItems().addAll(checkoutItem, deleteitem);

        return contextMenu;
    }


    /**
     * Updates the corresponding view if possible
     */
    // TODO: This happens off FX thread when called from somewhere (gitStatus?) but happens on the thread when called from SessionController.handleCommitSortToggle. Fix.
    public synchronized void updateView() throws IOException{
        Main.assertFxThread();
        if(SessionModel.getSessionModel().getCurrentRepoHelper() != null){
            CommitTreeController.update(this);
        }else{
            getView().displayEmptyView();
        }
    }

    /**
     * Initializes the corresponding view if possible
     */
    private synchronized void initView(){
        Main.assertFxThread();
        if(SessionModel.getSessionModel().getCurrentRepoHelper() != null){
            CommitTreeController.init(this);
        }else{
            getView().displayEmptyView();
        }
    }

    /**
     * Sets the shape and ref labels for a cell based on the current repo status
     *
     * @param helper the commit helper that we want to set as a branch head
     * @param tracked whether or not the commit is the head of a tracked branch
     */
    public synchronized void setCommitAsBranchHead(CommitHelper helper, boolean tracked) {
        Main.assertFxThread();
        String commitId;
        commitId = helper.getName();
        CellShape shape = (tracked) ? Cell.TRACKED_BRANCH_HEAD_SHAPE : Cell.UNTRACKED_BRANCH_HEAD_SHAPE;

        treeGraph.treeGraphModel.setCellShape(commitId, shape);
    }

    /**
     * Looks for all ref labels, then adds them to the commit tree graph
     */
    public synchronized void updateAllRefLabels() {
        Main.assertFxThread();
        RepoHelper repo = SessionModel.getSessionModel().getCurrentRepoHelper();

        List<RefHelper> refHelpers = new ArrayList<>();
        refHelpers.addAll(repo.getBranchModel().getAllBranches());
        refHelpers.addAll(repo.getTagModel().getAllTags());

        List<RemoteBranchHelper> remotes = repo.getBranchModel().getRemoteBranchesTyped();

        Map<RefHelper, ContextMenu> menuMap = new HashMap<>();
        List<String> remoteBranches = new ArrayList<>();

        this.tagsInModel.clear();
        this.tagsInModel.addAll(repo.getTagModel().getAllTags());

        Map<String, List<RefHelper>> commitLabelMap = new HashMap<>();

        addCommitRefMaps(refHelpers, commitLabelMap, menuMap);

        for (RemoteBranchHelper helper : remotes) {
            remoteBranches.add(helper.getRefName());
        }

        // Set the labels
        RepoHelper repoHelper = SessionModel.getSessionModel().getCurrentRepoHelper();
        Set<String> currentAbbrevBranches = repoHelper.getBranchModel().getCurrentAbbrevBranches();
        for (String commit : commitLabelMap.keySet()) {
            if(repoHelper.getCommit(commit) != null) {
                if (!treeGraph.treeGraphModel.containsID(commit)) {
                    // TODO make this not a banaid fix...
                    //System.out.println("Does not yet contain "+commit);
                    continue;
                }
                String commitDescriptor = repo.getCommitDescriptorString(commit, false);
                treeGraph.treeGraphModel.setCellLabels(commit, commitDescriptor, commitLabelMap.get(commit));
                treeGraph.treeGraphModel.setCurrentCellLabels(commit, currentAbbrevBranches);

                treeGraph.treeGraphModel.setLabelMenus(commit, menuMap);
                treeGraph.treeGraphModel.setRemoteBranchCells(commit, remoteBranches);
            }
        }
    }

    private synchronized void addCommitRefMaps(List<RefHelper> helpers, Map<String, List<RefHelper>> commitLabelMap,
                                  Map<RefHelper, ContextMenu> menuMap) {
        Main.assertFxThread();
        String commitId;
        for (RefHelper helper : helpers) {
            commitId = helper.getCommit().getName();

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
    public synchronized void resetBranchHeads(){
        Main.assertFxThread();
        List<String> resetIDs = treeGraph.treeGraphModel.resetCellShapes();
        RepoHelper repo = SessionModel.getSessionModel().getCurrentRepoHelper();
        this.branchesInModel.clear();
        this.branchesInModel.addAll(repo.getBranchModel().getAllBranches());
        for(String id : resetIDs){
            if(SessionModel.getSessionModel().getCurrentRepoHelper().getCommit(id) != null) {
                String displayLabel = repo.getCommitDescriptorString(id, false);
                List<RefHelper> branchLabels = new ArrayList<>();
                treeGraph.treeGraphModel.setCellLabels(id, displayLabel, branchLabels);
            }
        }
    }


    public synchronized Set<CommitHelper> getCommitsInModel() {
        return Collections.unmodifiableSet(this.commitsInModel); }

}
