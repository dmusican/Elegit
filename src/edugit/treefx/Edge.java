package edugit.treefx;

import javafx.beans.binding.DoubleBinding;
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

    protected Cell source;
    protected Cell target;

    private DirectedPath path;

    private boolean addedMidPoints;
    private boolean visible;

    private DoubleProperty midPointOffset;

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
        midPointOffset = new SimpleDoubleProperty(0);

        DoubleBinding startX = source.layoutXProperty().add(source.getBoundsInParent().getWidth() / 2.0);
        DoubleBinding startY = source.layoutYProperty().add(0);

        DoubleBinding endX = target.layoutXProperty().add(target.getBoundsInParent().getWidth() / 2.0);
        DoubleBinding endY = target.layoutYProperty().add(target.getBoundsInParent().getHeight());

        path = new DirectedPath(startX, startY, endX, endY);
        checkAndAddMidPoints(startX, startY, endX, endY);
        path.addPoint(endX, endY.add(TreeLayout.V_SPACING / 4.));

        target.heightProperty.addListener((observable, oldValue, newValue) -> {
            checkAndAddMidPoints(startX, startY, endX, endY);
        });
        source.heightProperty.addListener((observable, oldValue, newValue) -> {
            checkAndAddMidPoints(startX, startY, endX, endY);
        });

        startX.addListener((observable, oldValue, newValue) -> {
            checkMidPointOffset(endX.get(), newValue.doubleValue());
        });

        endX.addListener((observable, oldValue, newValue) -> {
            checkMidPointOffset(newValue.doubleValue(), startX.get());
        });

        getChildren().add(path);

        allVisible.addListener((observable, oldValue, newValue) -> checkVisible());

        source.edges.add(this);
        target.edges.add(this);
    }

    private void checkMidPointOffset(double startX, double endX){
        if(startX > endX){
            midPointOffset.set(TreeLayout.H_SPACING / -2.);
        }else if(startX < endX){
            midPointOffset.set(TreeLayout.H_SPACING / 2.);
        }else{
            midPointOffset.set(0);
        }
    }

    private void checkAndAddMidPoints(DoubleBinding startX, DoubleBinding startY, DoubleBinding endX, DoubleBinding endY){
        if(source.height - target.height > 1){
            if(!addedMidPoints){
                path.addPoint(endX.add(midPointOffset), startY.subtract(TreeLayout.V_SPACING / 3.), 1);
                path.addPoint(endX.add(midPointOffset), endY.add(TreeLayout.V_SPACING / 2.), 2);
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
        this.visible = enable;
        checkVisible();
    }

    private void checkVisible(){
        if(allVisible.get()){
            this.setVisible(true);
        }else{
            if(this.visible){
                this.setVisible(true);
            }else{
                this.setVisible(false);
            }
        }
    }

    public Cell getSource() {
        return source;
    }

    public Cell getTarget() {
        return target;
    }

}
