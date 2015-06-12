package edugit;

import edugit.treefx.Layout;
import edugit.treefx.TreeGraph;
import edugit.treefx.TreeGraphModel;
import edugit.treefx.TreeLayout;
import javafx.scene.Group;
import javafx.scene.control.ScrollPane;

import java.util.ArrayList;

/**
 * Created by makik on 6/10/15.
 *
 * Super class for the local and remote panel views that handles the common functionality,
 * namely the drawing of a tree structure
 */
public abstract class TreePanelView extends Group{

    SessionModel model;
    TreeGraph treeGraph;

    public abstract void drawTreeFromCurrentRepo();

    public void setSessionModel(SessionModel model){
        this.model = model;
        if(this.model != null && this.model.currentRepoHelper != null){
            this.drawTreeFromCurrentRepo();
        }
    }

    public void addCommitsToTree(ArrayList<CommitHelper> commits){
        CommitHelper root = commits.get(0);

        TreeGraphModel graphModel = new TreeGraphModel(root.getName()+" "+root.getMessage(false));

        treeGraph = new TreeGraph(graphModel);

        treeGraph.beginUpdate();
        for(int i = 1; i < commits.size(); i++){
            CommitHelper curCommitHelper = commits.get(i);
            ArrayList<CommitHelper> parents = curCommitHelper.getParents();
            this.addCommitToTree(curCommitHelper, parents, graphModel);
        }
        treeGraph.endUpdate();
    }

    private void addCommitToTree(CommitHelper commitHelper, ArrayList<CommitHelper> parents, TreeGraphModel graphModel){
        switch(parents.size()){
            case 1:
                graphModel.addCell(this.getTreeLabel(commitHelper), this.getTreeLabel(parents.get(0)));
                break;
            case 2:
                graphModel.addCell(this.getTreeLabel(commitHelper), this.getTreeLabel(parents.get(0)), this.getTreeLabel(parents.get(1)));
                break;
            default:
                graphModel.addCell(this.getTreeLabel(commitHelper));
        }
    }

    private String getTreeLabel(CommitHelper commitHelper){
        return commitHelper.getName() + " " + commitHelper.getMessage(false);
    }

    public void displayTreeGraph(){
        ScrollPane sp = treeGraph.getScrollPane();
        sp.setPannable(true);
        sp.setPrefSize(200, 600);
        this.getChildren().add(sp);

        Layout layout = new TreeLayout(treeGraph);
        layout.execute();
    }

    private void drawTestGraph(){
        TreeGraphModel treeGraphModel = new TreeGraphModel("root");

        treeGraph = new TreeGraph(treeGraphModel);

        ScrollPane sp = treeGraph.getScrollPane();
        sp.setPannable(true);
        sp.setPrefSize(200, 600);
        this.getChildren().add(sp);

        treeGraph.beginUpdate();

        treeGraphModel.addCell("A");
        treeGraphModel.addCell("B", "root");
        treeGraphModel.addCell("C");
        treeGraphModel.addCell("D", "A");
        treeGraphModel.addCell("E");
        treeGraphModel.addCell("F","D");

        treeGraphModel.addCell("G");
        treeGraphModel.addCell("H");

        treeGraphModel.addCell("I","G");
        treeGraphModel.addCell("J");
        treeGraphModel.addCell("K","I");

        treeGraphModel.addCell("L","I");
        treeGraphModel.addCell("M");
        treeGraphModel.addCell("N","M","K");
        treeGraphModel.addCell("O");

        treeGraphModel.addCell("P","root");
        treeGraphModel.addCell("Q");

        treeGraphModel.addCell("R","J");
        treeGraphModel.addCell("S");
        treeGraphModel.addCell("T");

        treeGraphModel.addCell("U","O","T");
        treeGraphModel.addCell("V");

        treeGraph.endUpdate();

        Layout layout = new TreeLayout(treeGraph);
        layout.execute();
    }
}
