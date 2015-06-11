package edugit;

import edugit.treefx.*;
import javafx.scene.Group;
import javafx.scene.control.ScrollPane;

/**
 * Created by makik on 6/10/15.
 *
 * Super class for the local and remote panel views that handles the common functionality,
 * namely the drawing of a tree structure
 */
public abstract class TreePanelView extends Group{

    SessionModel model;
    TreeGraph treeGraph;

    public void setSessionModel(SessionModel model){
        this.model = model;
        if(this.model != null && this.model.currentRepoHelper != null){
            this.drawTreeFromCurrentRepo();
        }
    }

    public abstract void drawTreeFromCurrentRepo();

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
