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
 * Constructs a hold a tree graph represented by parent and children cells with directed edges between them
 *
 */
public class TreeGraph{

    // The underlying model of the graph
    public TreeGraphModel treeGraphModel;

    private Group canvas;

    private MatchedScrollPane scrollPane;

    // The layer within which the cells will be added
    Pane cellLayer;

    /**
     * Constructs a new graph using the given model
     * @param m the model of the graph
     */
    public TreeGraph(TreeGraphModel m) {

        this.treeGraphModel = m;

        canvas = new Group();
        cellLayer = new Pane();

        canvas.getChildren().add(cellLayer);

        scrollPane = new MatchedScrollPane(canvas);

        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(false);

        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        scrollPane.vmaxProperty().bind(m.numCellsProperty);
    }

    public ScrollPane getScrollPane() {
        return this.scrollPane;
    }

    public Pane getCellLayer() {
        return this.cellLayer;
    }

    /**
     * Must be called after modifying the underlying model to add and
     * remove the appropriate cells and edges and keep the view up to
     * date
     */
    public void update() {
        // add components to treeGraph pane
        getCellLayer().getChildren().addAll(treeGraphModel.getAddedEdges());
        getCellLayer().getChildren().addAll(treeGraphModel.getAddedCells());

        // remove components from treeGraph pane
        getCellLayer().getChildren().removeAll(treeGraphModel.getRemovedCells());
        getCellLayer().getChildren().removeAll(treeGraphModel.getRemovedEdges());

        // merge added & removed cells with all cells
        treeGraphModel.merge();
    }
}
