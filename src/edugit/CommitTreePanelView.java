package edugit;

import edugit.treefx.TreeGraph;
import edugit.treefx.TreeLayout;
import javafx.scene.Group;
import javafx.scene.control.ScrollPane;

/**
 * Created by makik on 6/10/15.
 *
 * Class for the local and remote panel views that handles the drawing of a tree structure
 * from a given treeGraph.
 *
 */
public class CommitTreePanelView extends Group{

    public static int TREE_PANEL_WIDTH = 200;
    public static int TREE_PANEL_HEIGHT = 600;
    /**
     * Handles the layout and display of the treeGraph
     * @param treeGraph the graph to be displayed
     */
    public void displayTreeGraph(TreeGraph treeGraph){
        TreeLayout.doTreeLayout(treeGraph);

        ScrollPane sp = treeGraph.getScrollPane();
        sp.setPannable(true);
        sp.setPrefSize(TREE_PANEL_WIDTH, TREE_PANEL_HEIGHT);
        this.getChildren().clear();
        this.getChildren().add(sp);
    }

    public void displayEmptyView(){
        ScrollPane sp = new ScrollPane();
        sp.setPrefSize(TREE_PANEL_WIDTH, TREE_PANEL_HEIGHT);
        this.getChildren().clear();
        this.getChildren().add(sp);
    }
}
