package edugit;

import edugit.treefx.TreeGraph;
import edugit.treefx.TreeGraphModel;

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
        this.update();
        CommitTreeController.allCommitTreeModels.add(this);
    }

    /**
     * @return a list of the commits to be put into a tree
     */
    protected abstract List<CommitHelper> getCommits();

    public List<String> getCommitIDs(){
        if(treeGraph != null){
            return treeGraph.getTreeGraphModel().getCellIDs();
        }else{
            return new ArrayList<>();
        }
    }

    public boolean containsID(String id){
        return treeGraph != null && treeGraph.getTreeGraphModel().containsID(id);
    }

    public void update(){
        this.addCommitsToTree();
        this.updateView();
    }

    public void addInvisibleCommit(String id){
        CommitHelper invisComimt = sessionModel.currentRepoHelper.getCommit(id);
        for(CommitHelper c : invisComimt.getParents()){
            if(!treeGraph.getTreeGraphModel().containsID(c.getId())){
                addInvisibleCommit(c.getId());
            }
        }
        addCommitToTree(invisComimt, invisComimt.getParents(), treeGraph.getTreeGraphModel(), false);
    }

    /**
     * The main function for transforming a simple list into a tree. Builds a
     * tree up iteratively from the first commit, making sure the parent/child
     * relations are preserved
     * @return true if the tree was updated, otherwise false
     */
    private boolean addCommitsToTree(){
        List<CommitHelper> commits = this.getCommits();

        if(commits.size() == 0) return false;

        CommitHelper root = commits.get(0);

        treeGraph = this.createNewTreeGraph(root);

        for(int i = 1; i < commits.size(); i++){
            CommitHelper curCommitHelper = commits.get(i);
            ArrayList<CommitHelper> parents = curCommitHelper.getParents();
            this.addCommitToTree(curCommitHelper, parents, treeGraph.getTreeGraphModel(), true);
        }
        treeGraph.update();

        CommitTreeController.update(sessionModel.currentRepoHelper);
        return true;
    }

    /**
     * Creates a new TreeGraph with a new model starting at the given root commit. Updates the list
     * of all models accordingly
     * @param root the root of the new graph
     * @return the newly created graph
     */
    private TreeGraph createNewTreeGraph(CommitHelper root){
        TreeGraphModel graphModel = new TreeGraphModel(getId(root), getTreeCellLabel(root));
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
        switch(parents.size()){
            case 1:
                graphModel.addCell(getId(commitHelper), getTreeCellLabel(commitHelper), getId(parents.get(0)), visible);
                break;
            case 2:
                graphModel.addCell(getId(commitHelper), getTreeCellLabel(commitHelper), getId(parents.get(0)), getId(parents.get(1)), visible);
                break;
            default:
                graphModel.addCell(getId(commitHelper), getTreeCellLabel(commitHelper), visible);
        }
    }

    /**
     * Updates the corresponding view if possible
     */
    private void updateView(){
        if(this.sessionModel != null && this.sessionModel.currentRepoHelper != null){
            view.displayTreeGraph(treeGraph);
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
