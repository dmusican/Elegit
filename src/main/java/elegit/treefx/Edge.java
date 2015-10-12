package main.java.elegit.treefx;

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
    private DoubleProperty midLineY;

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
        midLineY = new SimpleDoubleProperty(0);

        DoubleBinding startX = source.translateXProperty().add(source.widthProperty());
        DoubleBinding startY = source.translateYProperty().add(source.heightProperty().divide(2.0));

        DoubleBinding endX = target.translateXProperty().add(0);
        DoubleBinding endY = target.translateYProperty().add(target.heightProperty().divide(2.0));

        path = new DirectedPath(startX, startY, endX, endY);
        checkAndAddMidPoints(startX, endX);
        path.addPoint(endX.subtract(TreeLayout.H_SPACING / 4.), endY);

        source.translateXProperty().addListener((observable, oldValue, newValue) -> {
            checkAndAddMidPoints(startX, endX);
        });

        target.translateYProperty().addListener((observable, oldValue, newValue) -> {
            checkAndAddMidPoints(startX, endX);
        });

        source.translateXProperty().addListener((observable, oldValue, newValue) -> {
            checkAndAddMidPoints(startX, endX);
        });
        target.translateYProperty().addListener((observable, oldValue, newValue) -> {
            checkAndAddMidPoints(startX, endX);
        });

        // Change the Y of the midpoints depending on whether the target is above, below, or at the same
        // level as the source
        midLineY.bind(new When(target.rowLocationProperty.subtract(source.rowLocationProperty).greaterThan(0))
                .then(endY.subtract(TreeLayout.V_SPACING / 2.))
                .otherwise(new When(target.rowLocationProperty.subtract(source.rowLocationProperty).lessThan(0))
                        .then(startY.subtract(TreeLayout.V_SPACING / 2.))
                        .otherwise(startY)));


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
     * are added at the calculated y value
     * @param startX the starting x coordinate of this edge
     * @param endX the ending x coordinate of this edge
     */
    private void checkAndAddMidPoints(DoubleBinding startX, DoubleBinding endX){
        if(target.columnLocationProperty.get() - source.columnLocationProperty.get() > 1){
            if(!addedMidPoints){
                path.addPoint(startX.add(TreeLayout.H_SPACING / 3.), midLineY.add(0), 1);
                path.addPoint(endX.subtract(TreeLayout.H_SPACING / 2.), midLineY.add(0), 2);
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

}
