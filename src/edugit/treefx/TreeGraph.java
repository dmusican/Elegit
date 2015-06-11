package edugit.treefx;

import edugit.MatchedScrollPane;
import javafx.scene.Group;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Pane;

/**
 * Created by makik on 6/10/15.
 *
 * Thanks to Roland for providing this graph structure:
 * http://stackoverflow.com/questions/30679025/graph-visualisation-like-yfiles-in-javafx/30696075#30696075
 *
 */
public class TreeGraph{

    private TreeGraphModel treeGraphModel;

    private Group canvas;

    private MatchedScrollPane scrollPane;

    /**
     * the pane wrapper is necessary or else the scrollpane would always align
     * the top-most and left-most child to the top and left eg when you drag the
     * top child down, the entire scrollpane would move down
     */
    CellLayer cellLayer;

    public TreeGraph(TreeGraphModel m) {

        this.treeGraphModel = m;

        canvas = new Group();
        cellLayer = new CellLayer();

        canvas.getChildren().add(cellLayer);

        scrollPane = new MatchedScrollPane(canvas);

        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(false);

        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
    }

    public ScrollPane getScrollPane() {
        return this.scrollPane;
    }

    public Pane getCellLayer() {
        return this.cellLayer;
    }

    public TreeGraphModel getTreeGraphModel() {
        return treeGraphModel;
    }

    public void beginUpdate() {
    }

    public void endUpdate() {
        // add components to treeGraph pane
        getCellLayer().getChildren().addAll(treeGraphModel.getAddedEdges());
        getCellLayer().getChildren().addAll(treeGraphModel.getAddedCells());

        // remove components from treeGraph pane
        getCellLayer().getChildren().removeAll(treeGraphModel.getRemovedCells());
        getCellLayer().getChildren().removeAll(treeGraphModel.getRemovedEdges());

        // merge added & removed cells with all cells
        getTreeGraphModel().merge();
    }
}
