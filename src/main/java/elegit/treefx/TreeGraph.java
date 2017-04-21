package elegit.treefx;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Pane;
import javafx.scene.transform.Rotate;

import java.util.LinkedList;
import java.util.List;

/**
 * Thanks to RolandC for providing the base graph code structure:
 * http://stackoverflow.com/questions/30679025/graph-visualisation-like-yfiles-in-javafx/30696075#30696075
 *
 * Constructs a scrollable tree graph represented by parent and children cells with directed edges between them
 */
public class TreeGraph{

    // The scroll pane that holds all drawn elements
    private CommitTreeScrollPane scrollPane;

    // The underlying model of the graph
    public TreeGraphModel treeGraphModel;

    // The layer within which the cells will be added
    private Pane cellLayer;

    private volatile List<Node> queuedToAdd;
    private volatile List<Node> queuedToRemove;

    /**
     * Constructs a new graph using the given model
     * @param m the model of the graph
     */
    public TreeGraph(TreeGraphModel m) {
        this.treeGraphModel = m;

        cellLayer = new Pane();
        cellLayer.setRotationAxis(Rotate.X_AXIS);
        cellLayer.setRotate(180);
        cellLayer.setPadding(new Insets(0,0,Cell.BOX_SIZE+TreeLayout.V_PAD,0));
        cellLayer.boundsInLocalProperty().addListener((observable, oldValue, newValue) -> cellLayer.setMinWidth(newValue.getMaxX()));

        scrollPane = new CommitTreeScrollPane(cellLayer);

        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        scrollPane.NumItemsProperty.bind(m.numCellsProperty);

        queuedToAdd = new LinkedList<>();
        queuedToRemove = new LinkedList<>();
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
    // THREAD
    public synchronized void update() {
        assert Platform.isFxApplicationThread();
        queuedToRemove.addAll(treeGraphModel.getRemovedCells());
        queuedToRemove.addAll(treeGraphModel.getRemovedEdges());

        queuedToAdd.addAll(treeGraphModel.getAddedCells());
        queuedToAdd.addAll(treeGraphModel.getAddedEdges());

        // merge added & removed cells with all cells
        treeGraphModel.merge();

        // add components to treeGraph pane
        LinkedList<Node> moreToAdd = new LinkedList<>();
        LinkedList<Node> moreToRemove = new LinkedList<>();
        for (Node n: queuedToAdd) {
            if (n instanceof Cell)
                moreToAdd.add(((Cell)n).getLabel());
        }
        cellLayer.getChildren().addAll(queuedToAdd);
        cellLayer.getChildren().addAll(moreToAdd);

        // remove components from treeGraph pane
        for (Node n:queuedToRemove) {
            if (n instanceof Cell)
                moreToRemove.add(((Cell)n).getLabel());
        }
        cellLayer.getChildren().removeAll(moreToRemove);
        cellLayer.getChildren().removeAll(queuedToRemove);

        queuedToAdd = new LinkedList<>();
        queuedToRemove = new LinkedList<>();
    }

    Pane getCellLayerPane() {
        return cellLayer;
    }
}
