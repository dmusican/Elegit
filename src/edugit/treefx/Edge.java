package edugit.treefx;

import javafx.beans.binding.DoubleBinding;
import javafx.beans.binding.When;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Group;

/**
 * Created by makik on 6/10/15.
 *
 * Connects two cells in the TreeGraph using a DirectedPath
 */
public class Edge extends Group {

    public static BooleanProperty allVisible = new SimpleBooleanProperty(true);
    private BooleanProperty visible;

    private Cell source;
    private Cell target;

    private DirectedPath path;

    private boolean addedMidPoints;

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

        DoubleBinding startX = source.translateXProperty().add(source.getBoundsInParent().getWidth());
        DoubleBinding startY = source.translateYProperty().add(source.getBoundsInParent().getHeight() / 2.0);

        DoubleBinding endX = target.translateXProperty().add(0);
        DoubleBinding endY = target.translateYProperty().add(target.getBoundsInParent().getHeight() / 2.0);

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

    public void setHighlighted(boolean enable){
        this.visible.set(enable);
    }

    public Cell getSource() {
        return source;
    }

    public Cell getTarget() {
        return target;
    }

}
