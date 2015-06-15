package edugit;

import edugit.treefx.TreeGraph;
import edugit.treefx.TreeGraphModel;

import java.util.ArrayList;

/**
 * Created by makik on 6/12/15.
 *
 * Handles the conversion/creation of a list of commit helpers into a nice
 * tree structure. It also takes care of updating the view its given to
 * display the new tree whenever the graph is updated.
 */
public abstract class CommitTreeModel{

    CommitTreePanelView view;

    SessionModel model;
    TreeGraph treeGraph;

    /**
     * @param model the model with which this class accesses the commits
     * @param view the view that will be updated with the new graph
     */
    public CommitTreeModel(SessionModel model, CommitTreePanelView view){
        this.model = model;
        this.view = view;
        this.update();
    }

    /**
     * @return a list of the commits to be put into a tree
     */
    public abstract ArrayList<CommitHelper> getCommits();

    public void update(){
        if(this.addCommitsToTree()){
            this.updateView();
        }
    }

    /**
     * The main function for transforming a simple list into a tree. Builds a
     * tree up iteratively from the first commit, making sure the parent/child
     * relations are preserved
     * @return true if the tree was updated, otherwise false
     */
    private boolean addCommitsToTree(){
        ArrayList<CommitHelper> commits = this.getCommits();

        if(commits.size() == 0) return false;

        CommitHelper root = commits.get(0);

        TreeGraphModel graphModel = new TreeGraphModel(this.getTreeCellId(root), this.getTreeCellLabel(root));

        treeGraph = new TreeGraph(graphModel);

        treeGraph.beginUpdate();
        for(int i = 1; i < commits.size(); i++){
            CommitHelper curCommitHelper = commits.get(i);
            ArrayList<CommitHelper> parents = curCommitHelper.getParents();
            this.addCommitToTree(curCommitHelper, parents, graphModel);
        }
        treeGraph.endUpdate();

        return true;
    }

    /**
     * Adds a single commit to the tree using one of the three methods available depending on the number of parents
     * @param commitHelper the commit to be added
     * @param parents a list of this commit's parents
     * @param graphModel the treeGraphModel to add the commit to
     */
    private void addCommitToTree(CommitHelper commitHelper, ArrayList<CommitHelper> parents, TreeGraphModel graphModel){
        switch(parents.size()){
            case 1:
                graphModel.addCell(this.getTreeCellId(commitHelper), this.getTreeCellLabel(commitHelper), this.getTreeCellId(parents.get(0)));
                break;
            case 2:
                graphModel.addCell(this.getTreeCellId(commitHelper), this.getTreeCellLabel(commitHelper), this.getTreeCellId(parents.get(0)), this.getTreeCellId(parents.get(1)));
                break;
            default:
                graphModel.addCell(this.getTreeCellId(commitHelper), this.getTreeCellLabel(commitHelper));
        }
    }

    /**
     * Returns a unique identifier that will never be shown
     * @param commitHelper the commit to get an ID for
     * @return a unique identifying string to be used as a key in the tree's map
     */
    private String getTreeCellId(CommitHelper commitHelper){
        return commitHelper.getName();
    }

    /**
     * Returns a string that will be displayed to the user to identify this commit
     * @param commitHelper the commit to get a label for
     * @return the display label for this commit
     */
    private String getTreeCellLabel(CommitHelper commitHelper){
        return commitHelper.getFormattedWhen()+"\n"+
                commitHelper.getAuthorName()+"\n"+
                commitHelper.getMessage(false);
    }

    /**
     * Updates the corresponding view if possible
     */
    private void updateView(){
        if(this.model != null && this.model.currentRepoHelper != null){
            view.displayTreeGraph(treeGraph);
        }
    }
}
