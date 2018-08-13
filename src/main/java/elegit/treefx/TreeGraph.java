package elegit.treefx;

import elegit.Main;
import io.reactivex.Observable;
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Pane;
import javafx.scene.transform.Rotate;
import net.jcip.annotations.ThreadSafe;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Thanks to RolandC for providing the base graph code structure:
 * http://stackoverflow.com/questions/30679025/graph-visualisation-like-yfiles-in-javafx/30696075#30696075
 *
 * Constructs a scrollable tree graph represented by parent and children cells with directed edges between them
 */
@ThreadSafe
// but critically only because of all the asserts requiring this be done only in the FX thread. Without that, it
// isn't threadsafe, not least because of the bindings that are done.
public class TreeGraph{

    // The scroll pane that holds all drawn elements
    private final CommitTreeScrollPane scrollPane;

    // The underlying model of the graph
    public final TreeGraphModel treeGraphModel;

    // The layer within which the cells will be added
    private final Pane cellLayer;

    /**
     * Constructs a new graph using the given model
     * @param m the model of the graph
     */
    public TreeGraph(TreeGraphModel m) {
        Main.assertFxThread();
        this.treeGraphModel = m;

        cellLayer = new Pane();
        cellLayer.setId("cell layer");
        cellLayer.setPadding(new Insets(0,0,Cell.BOX_SIZE+TreeLayout.V_PAD,0));
        cellLayer.boundsInLocalProperty().addListener((observable, oldValue, newValue) -> cellLayer.setMinWidth(newValue.getMaxX()));
        scrollPane = new CommitTreeScrollPane(cellLayer);

        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        scrollPane.NumItemsProperty.bind(m.getNumCellsProperty());

    }
    /**
     * @return the scroll pane that holds the graph drawing
     */
    public ScrollPane getScrollPane() {
        Main.assertFxThread();
        return this.scrollPane;
    }

    /**
     * Must be called after modifying the underlying model to add and
     * remove the appropriate cells and edges and keep the view up to
     * date
     */
    public synchronized void update() {
        System.out.println("Updating.............gg");
        Main.assertFxThread();
        final List<Node> queuedToAdd = new ArrayList<>();
        final List<Node> queuedToRemove = new ArrayList<>();

        queuedToRemove.addAll(treeGraphModel.getRemovedCells());
        for (Edge edge : treeGraphModel.getRemovedEdges()) {
            queuedToRemove.add(edge.path);
        }

        queuedToAdd.addAll(treeGraphModel.getAddedCells());
        for (Edge edge : treeGraphModel.getAddedEdges()) {
            queuedToAdd.add(edge.path);
        }

        // add components to treeGraph pane
        LinkedList<Node> moreToAdd = new LinkedList<>();
        LinkedList<Node> moreToRemove = new LinkedList<>();

//        for (Node n: queuedToAdd) {
//            if (n instanceof Cell) {
//                CellLabelContainer labels = ((Cell) n).getLabel();
//                if (labels.getChildren().size() > 0) {
//                    moreToAdd.add(labels);
//                }
//            }
//        }

        System.out.println("Alas, the size of labelChangedCells is " + treeGraphModel.getLabelChangedCells().size());
        for (Cell cell: treeGraphModel.getLabelChangedCells()) {
            System.out.println("Found changed labelsssssss ");
            CellLabelContainer labels = cell.getLabel();
            if (labels.getChildren().size() > 0) {
                if (!cell.getRefLabelsInScene()) {
                    cell.setRefLabelsInScene(true);
                }
                moreToAdd.add(labels);
            } else {
                if (cell.getRefLabelsInScene()) {
                    cell.setRefLabelsInScene(false);
                }
                moreToRemove.add(labels);
            }
        }

        // merge added & removed cells with all cells
        treeGraphModel.merge();

        // remove components from treeGraph pane
        for (Node n:queuedToRemove) {
            if (n instanceof Cell)
                moreToRemove.add(((Cell)n).getLabel());
        }

        // Emit 10 at a time, so as to leave space in the FX thread for other events, but not individually
        // so as not to over do it
        Observable.concat(Observable.fromIterable(queuedToAdd),
                Observable.fromIterable(moreToAdd))
                .buffer(10)
                //.observeOn(JavaFxScheduler.platform())
                .subscribe(cellsToAdd -> {
                    cellsToAdd.stream().forEach((n) -> {
                        if (n instanceof CellLabelContainer)
                        System.out.println("iddddddd " + ((CellLabelContainer)n).hashCode());
                    });
                    cellLayer.getChildren().addAll(cellsToAdd);
                });

        Observable.concat(Observable.fromIterable(moreToRemove),
                Observable.fromIterable(queuedToRemove))
                .buffer(10)
                //.observeOn(JavaFxScheduler.platform())
                .subscribe(cellsToAdd -> {
                    cellLayer.getChildren().removeAll(cellsToAdd);
                });
    }

    public Pane getCellLayerPane() {
        Main.assertFxThread();
        return cellLayer;
    }
}
