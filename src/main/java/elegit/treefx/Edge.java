package elegit.treefx;

import elegit.Main;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.binding.When;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Group;
import org.apache.http.annotation.ThreadSafe;

/**
 * Connects two cells in the TreeGraph using a DirectedPath
 *
 * A Edge extends Group, so it IS a JavaFX node and should be treated as one.
 *
 * This code should have no Platform.runLater code in it whatsoever. The Edge becomes part of the scene graph.
 * Could aspects of be done off thread? Sure, but that gets really complicated, and it's why the FX thread is single-
 * threaded to start with. Some code in here is called from off-thread, but it's the job of them to make sure they
 * call Platform.runLater (or whatever) and deal with the consequences of the timing. From the perspective of this
 * class, it's all on FX thread. Don't take any of the asserts out without a complete rethinking of the philosophy.
 *
 */
@ThreadSafe
// but critically only because of all the asserts requiring this be done only in the FX thread. Without that, it
// isn't threadsafe.
public class Edge  {

    // Whether or not this edge is visible
    private final BooleanProperty visible = new SimpleBooleanProperty(false);

    // The endpoints of this edge
    private final Cell source;
    private final Cell target;

    // The path that will be drawn to represent this edge
    public final DirectedPath path;

    // Whether extra points between the start and endpoints have been added
    private boolean addedMidPoints;

    // The y value to draw the mid points at
    private final DoubleProperty midLineX = new SimpleDoubleProperty(0);

    /**
     * Constructs a directed line between the source and target cells and binds
     * properties to handle relocation smoothly
     * @param source the source (parent) cell
     * @param target the target (child) cell
     */
    public Edge(Cell source, Cell target) {
        Main.assertFxThread();
        this.source = source;
        this.target = target;
        this.addedMidPoints = false;

        DoubleBinding endX = source.translateXProperty().add(source.widthProperty().divide(2.0));
        DoubleBinding endY = source.translateYProperty().add(0);//source.heightProperty());

        DoubleBinding startX = target.translateXProperty().add(target.widthProperty().divide(2.0));
        DoubleBinding startY = target.translateYProperty().add(source.heightProperty());

        path = new DirectedPath(startX, startY, endX, endY, source.getCellId(), target.getCellId()); // SLOW-ISH
        checkAndAddMidPoints(startY, endY);
        path.addPoint(endX, endY.add(-TreeLayout.V_SPACING / 4.));

        source.translateXProperty().addListener((observable, oldValue, newValue) -> {
            checkAndAddMidPoints(startY, endY);
        });

        target.translateYProperty().addListener((observable, oldValue, newValue) -> {
            checkAndAddMidPoints(startY, endY);
        });

        source.translateXProperty().addListener((observable, oldValue, newValue) -> {
            checkAndAddMidPoints(startY, endY);
        });
        target.translateYProperty().addListener((observable, oldValue, newValue) -> {
            checkAndAddMidPoints(startY, endY);
        });

        // Change the X of the midpoints depending on whether the target is above, below, or at the same
        // level as the source
        midLineX.bind(new When(target.rowLocationProperty.subtract(source.rowLocationProperty).lessThan(0))
                .then(new When(target.columnLocationProperty.subtract(source.columnLocationProperty).greaterThan(0))
                        .then(endX.add(TreeLayout.H_SPACING / 2.))
                        .otherwise(new When(target.columnLocationProperty.subtract(source.columnLocationProperty).lessThan(0))
                                .then(startX.add(TreeLayout.H_SPACING / 2.))
                                .otherwise(startX)))
                .otherwise(new When(target.columnLocationProperty.subtract(source.columnLocationProperty).greaterThan(0))
                        .then(endX.add(TreeLayout.H_SPACING / 2.))
                        .otherwise(new When(target.columnLocationProperty.subtract(source.columnLocationProperty).lessThan(0))
                                .then(startX.add(TreeLayout.H_SPACING / 2.))
                                .otherwise(startX))));

        if(source.getCellType() != Cell.CellType.BOTH || target.getCellType() != Cell.CellType.BOTH){
            path.setDashed(true);
        }
        //getChildren().add(path);

//        visibleProperty().bind(source.visibleProperty().and(target.visibleProperty())
//                .and(allVisible.or(visible)));

        source.addEdge(this);
        target.addEdge(this);
    }

    /**
     * Checks the start and endpoints to see if any midpoints are necessary for drawing the line
     * between them correctly. If the endpoints are more than 1 column apart and, the midpoints
     * are added at the calculated x value
     * @param startY the starting y coordinate of this edge
     * @param endY the ending y coordinate of this edge
     */
    private void checkAndAddMidPoints(DoubleBinding startY, DoubleBinding endY){
        Main.assertFxThread();
        if(source.rowLocationProperty.get() - target.rowLocationProperty.get() > 1
                || source.rowLocationProperty.get() - target.rowLocationProperty.get() < 0){
            if(!addedMidPoints){
                path.addPoint(midLineX.add(0), startY.add(TreeLayout.V_SPACING/3.), 1);
                path.addPoint(midLineX.add(0), endY.subtract(TreeLayout.V_SPACING/2.), 2);
                this.addedMidPoints = true;
            }
        }else{
            if(addedMidPoints){
                path.removePoint(2);
                path.removePoint(1);
                this.addedMidPoints = false;
            }
        }
    }

    /**
     * @param enable whether to set this edge as visible or not
     */
    public void setHighlighted(boolean enable){
        Main.assertFxThread();
        this.visible.set(enable);
    }

    public Cell getSource() {
        Main.assertFxThread();
        return this.source;
    }

    public Cell getTarget() {
        Main.assertFxThread();
        return this.target;
    }

    public void resetDashed() {
        Main.assertFxThread();
        if(source.getCellType() != Cell.CellType.BOTH || target.getCellType() != Cell.CellType.BOTH)
            path.setDashed(true);
        else
            path.setDashed(false);
    }

}
