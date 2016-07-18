package elegit.treefx;

import javafx.beans.binding.DoubleBinding;
import javafx.beans.binding.When;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Group;

/**
 * Connects two cells in the TreeGraph using a DirectedPath
 */
public class Edge extends Group {

    // Determines whether all edges are set to be visible or not
    public static BooleanProperty allVisible = new SimpleBooleanProperty(true);

    // Whether or not this edge is visible
    private BooleanProperty visible;

    // The endpoints of this edge
    private Cell source;
    private Cell target;

    // The path that will be drawn to represent this edge
    private DirectedPath path;

    // Whether extra points between the start and endpoints have been added
    private boolean addedMidPoints;

    // The y value to draw the mid points at
    private DoubleProperty midLineX;

    /**
     * Constructs a directed line between the source and target cells and binds
     * properties to handle relocation smoothly
     * @param source the source (parent) cell
     * @param target the target (child) cell
     */
    public Edge(Cell source, Cell target) {

        this.source = source;
        this.target = target;
        this.addedMidPoints = false;
        midLineX = new SimpleDoubleProperty(0);

        DoubleBinding endX = source.translateXProperty().add(source.widthProperty().divide(2.0));
        DoubleBinding endY = source.translateYProperty().add(source.heightProperty());

        DoubleBinding startX = target.translateXProperty().add(target.widthProperty().divide(2.0));
        DoubleBinding startY = target.translateYProperty().add(0);

        path = new DirectedPath(startX, startY, endX, endY);
        checkAndAddMidPoints(startY, endY);
        path.addPoint(endX, endY.add(TreeLayout.V_SPACING / 4.));

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

        if(source instanceof InvisibleCell || target instanceof InvisibleCell){
            path.setDashed(true);
        }
        getChildren().add(path);

        visible = new SimpleBooleanProperty(false);
        visibleProperty().bind(source.visibleProperty().and(target.visibleProperty())
                .and(allVisible.or(visible)));

        source.edges.add(this);
        target.edges.add(this);

    }

    /**
     * Checks the start and endpoints to see if any midpoints are necessary for drawing the line
     * between them correctly. If the endpoints are more than 1 column apart and, the midpoints
     * are added at the calculated x value
     * @param startY the starting y coordinate of this edge
     * @param endY the ending y coordinate of this edge
     */
    private void checkAndAddMidPoints(DoubleBinding startY, DoubleBinding endY){
        if(target.rowLocationProperty.get() - source.rowLocationProperty.get() > 1
                || target.rowLocationProperty.get() - source.rowLocationProperty.get() < 0){
            if(!addedMidPoints){
                path.addPoint(midLineX.add(0), startY.subtract(TreeLayout.V_SPACING/3.), 1);
                path.addPoint(midLineX.add(0), endY.add(TreeLayout.V_SPACING/2.), 2);
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
        this.visible.set(enable);
    }

    public Cell getSource() { return this.source; }
    public Cell getTarget() { return this.target; }

}
