package edugit.treefx;

import edugit.MatchedScrollPane;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Pane;

/**
 * Thanks to Roland for providing this graph structure:
 * http://stackoverflow.com/questions/30679025/graph-visualisation-like-yfiles-in-javafx/30696075#30696075
 *
 * Constructs a hold a tree graph represented by parent and children cells with directed edges between them
 *
 */
public class TreeGraph{

    // The scroll pane that holds all drawn elements
    private MatchedScrollPane scrollPane;

    // The underlying model of the graph
    public TreeGraphModel treeGraphModel;

    // The layer within which the cells will be added
    Pane cellLayer;

    /**
     * Constructs a new graph using the given model
     * @param m the model of the graph
     */
    public TreeGraph(TreeGraphModel m) {

        this.treeGraphModel = m;

        Group canvas = new Group();
        cellLayer = new Pane();
        cellLayer.setPadding(new Insets(TreeLayout.V_PAD, TreeLayout.H_PAD, TreeLayout.V_PAD, TreeLayout.H_PAD));

        canvas.getChildren().add(cellLayer);

        scrollPane = new MatchedScrollPane(canvas);

        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        scrollPane.NumItemsProperty.bind(m.numCellsProperty);
    }

    /**
     * @return the scroll pane that holds the graph drawing
     */
    public ScrollPane getScrollPane() {
        return this.scrollPane;
    }

    /**
     * Must be called after modifying the underlying model to add and
     * remove the appropriate cells and edges and keep the view up to
     * date
     */
    public void update() {
        // add components to treeGraph pane
        cellLayer.getChildren().addAll(treeGraphModel.getAddedEdges());
        cellLayer.getChildren().addAll(treeGraphModel.getAddedCells());

        // remove components from treeGraph pane
        cellLayer.getChildren().removeAll(treeGraphModel.getRemovedCells());
        cellLayer.getChildren().removeAll(treeGraphModel.getRemovedEdges());

        // merge added & removed cells with all cells
        treeGraphModel.merge();
    }
}
