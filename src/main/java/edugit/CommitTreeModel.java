package main.java.edugit;

import main.java.edugit.treefx.TreeGraph;
import main.java.edugit.treefx.TreeGraphModel;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

    /**
     * @param model the model with which this class accesses the commits
     * @param view the view that will be updated with the new graph
     */
    public CommitTreeModel(SessionModel model, CommitTreePanelView view){
        this.sessionModel = model;
        this.view = view;
        this.view.setName("Generic commit tree");
        CommitTreeController.allCommitTreeModels.add(this);
        this.init();
    }

    /**
     * @return a list of all commits tracked by this model
     */
    protected abstract List<CommitHelper> getAllCommits();

    /**
     * @return a list of all commits tracked by this model that haven't been added to the tree
     * @throws GitAPIException
     * @throws IOException
     */
    protected abstract List<CommitHelper> getNewCommits() throws GitAPIException, IOException;

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
    public void init(){
        treeGraph = this.createNewTreeGraph();

        CommitTreeController.resetSelection();

        this.addAllCommitsToTree();
        this.initView();
    }

    /**
     * Checks for new commits to add to the tree, and notifies the
     * CommitTreeController that an update is needed if there are any
     * @throws GitAPIException
     * @throws IOException
     */
    public void update() throws GitAPIException, IOException{
        if(this.addNewCommitsToTree()){
            this.updateView();
        }
    }

    /**
     * Adds a pseudo-cell of type InvisibleCell to the treeGraph.
     * @param id the id of the cell to add
     */
    public void addInvisibleCommit(String id){
        CommitHelper invisCommit = sessionModel.getCurrentRepoHelper().getCommit(id);
        for(CommitHelper c : invisCommit.getParents()){
            if(!treeGraph.treeGraphModel.containsID(c.getId())){
                addInvisibleCommit(c.getId());
            }
        }
        this.addCommitToTree(invisCommit, invisCommit.getParents(), treeGraph.treeGraphModel, false);
    }

    /**
     * Gets all commits tracked by this model and adds them to the tree
     * @return true if the tree was updated, otherwise false
     */
    private boolean addAllCommitsToTree() {
        return this.addCommitsToTree(this.getAllCommits());
    }

    /**
     * Gets all commits tracked by this model that haven't been added to the tree,
     * and adds them
     * @return true if the tree was updated, otherwise false
     * @throws GitAPIException
     * @throws IOException
     */
    private boolean addNewCommitsToTree() throws GitAPIException, IOException{
        return this.addCommitsToTree(this.getNewCommits());
    }

    /**
     * Adds the given list of commits to the treeGraph
     * @param commits the commits to add
     * @return true if commits where added, else false
     */
    private boolean addCommitsToTree(List<CommitHelper> commits){
        if(commits.size() == 0) return false;

        for(int i = 0; i < commits.size(); i++){
            CommitHelper curCommitHelper = commits.get(i);
            ArrayList<CommitHelper> parents = curCommitHelper.getParents();
            this.addCommitToTree(curCommitHelper, parents, treeGraph.treeGraphModel, true);
        }

        treeGraph.update();
        return true;
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
     * Adds a single commit to the tree using one of the three methods available depending on the number of parents
     * @param commitHelper the commit to be added
     * @param parents a list of this commit's parents
     * @param graphModel the treeGraphModel to add the commit to
     */
    private void addCommitToTree(CommitHelper commitHelper, ArrayList<CommitHelper> parents, TreeGraphModel graphModel, boolean visible){
        for(CommitHelper parent : parents){
            if(!graphModel.containsID(getId(parent))){
                addCommitToTree(parent, parent.getParents(), graphModel, visible);
            }
        }
        String commitID = getId(commitHelper);
        if(graphModel.containsID(commitID) && graphModel.isVisible(commitID)){
            return;
        }
        switch(parents.size()){
            case 1:
                graphModel.addCell(commitID, commitHelper.getWhen().getTime(), getTreeCellLabel(commitHelper), getId(parents.get(0)), visible);
                break;
            case 2:
                graphModel.addCell(commitID, commitHelper.getWhen().getTime(), getTreeCellLabel(commitHelper), getId(parents.get(0)), getId(parents.get(1)), visible);
                break;
            default:
                graphModel.addCell(commitID, commitHelper.getWhen().getTime(), getTreeCellLabel(commitHelper), visible);
        }
    }

    /**
     * Updates the corresponding view if possible
     */
    private void updateView(){
        if(this.sessionModel != null && this.sessionModel.getCurrentRepoHelper() != null){
            CommitTreeController.update(sessionModel.getCurrentRepoHelper());
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
     * Returns a unique identifier that will never be shown
     * @param commitHelper the commit to get an ID for
     * @return a unique identifying string to be used as a key in the tree's map
     */
    public static String getId(CommitHelper commitHelper){
        return commitHelper.getName();
    }

    /**
     * Returns a string that will be displayed to the user to identify this commit
     * @param commitHelper the commit to get a label for
     * @return the display label for this commit
     */
    private static String getTreeCellLabel(CommitHelper commitHelper){
        return commitHelper.getAuthorName()+ "\n"+
                commitHelper.getFormattedWhen()+"\n"+
                commitHelper.getName()+"\n\n"+
                commitHelper.getMessage(false);
    }
}
