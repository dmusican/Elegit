package edugit;

import edugit.treefx.TreeGraph;
import edugit.treefx.TreeGraphModel;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by makik on 6/12/15.
 *
 * Handles the conversion/creation of a list of commit helpers into a nice
 * tree structure. It also takes care of updating the view its given to
 * display the new tree whenever the graph is updated.
 */
public abstract class CommitTreeModel{

    CommitTreePanelView view;

    SessionModel sessionModel;
    TreeGraph treeGraph;

    /**
     * @param model the model with which this class accesses the commits
     * @param view the view that will be updated with the new graph
     */
    public CommitTreeModel(SessionModel model, CommitTreePanelView view){
        this.sessionModel = model;
        this.view = view;
        this.init();
        CommitTreeController.allCommitTreeModels.add(this);
    }

    /**
     * @return a list of the commits to be put into a tree
     */
    protected abstract List<CommitHelper> getAllCommits();

    protected abstract List<CommitHelper> getNewCommits() throws GitAPIException, IOException;

    public boolean containsID(String id){
        return treeGraph != null && treeGraph.getTreeGraphModel().containsID(id);
    }

    public void init(){
        treeGraph = this.createNewTreeGraph();

        CommitTreeController.resetSelection();

        this.addAllCommitsToTree();
        this.updateView();
    }

    public void update() throws GitAPIException, IOException{
        if(this.addNewCommitsToTree()){
            this.updateView();
        }
    }

    public void addInvisibleCommit(String id){
        CommitHelper invisCommit = sessionModel.currentRepoHelper.getCommit(id);
        for(CommitHelper c : invisCommit.getParents()){
            if(!treeGraph.getTreeGraphModel().containsID(c.getId())){
                addInvisibleCommit(c.getId());
            }
        }
        this.addCommitToTree(invisCommit, invisCommit.getParents(), treeGraph.getTreeGraphModel(), false);
    }

    /**
     * The main function for transforming a simple list into a tree. Builds a
     * tree up iteratively from the first commit, making sure the parent/child
     * relations are preserved
     * @return true if the tree was updated, otherwise false
     */
    private boolean addAllCommitsToTree(){
        return this.addCommitsToTree(this.getAllCommits());
    }

    private boolean addNewCommitsToTree() throws GitAPIException, IOException{
        return this.addCommitsToTree(this.getNewCommits());
    }

    private boolean addCommitsToTree(List<CommitHelper> commits){
        if(commits.size() == 0) return false;

        for(int i = 0; i < commits.size(); i++){
            CommitHelper curCommitHelper = commits.get(i);
            ArrayList<CommitHelper> parents = curCommitHelper.getParents();
            this.addCommitToTree(curCommitHelper, parents, treeGraph.getTreeGraphModel(), true);
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
        if(this.sessionModel != null && this.sessionModel.currentRepoHelper != null){
            CommitTreeController.update(sessionModel.currentRepoHelper);
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
        return commitHelper.getFormattedWhen()+"\n"+
                commitHelper.getAuthorName()+"\n"+
                commitHelper.getMessage(false);
    }
}
