package elegit.treefx;

import elegit.Main;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.ObservableList;
import javafx.scene.Group;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;
import org.apache.http.annotation.ThreadSafe;

/**
 * Represents a line with an arrow at the end. The arrow is constructed using a three-point Path
 * object that has its vertices bound to the end of the line.
 */
@ThreadSafe
// but critically only because of all the asserts requiring this be done only in the FX thread. Without that, it
// isn't threadsafe, not least because of the bindings / listeners that are done.
public class DirectedPath extends Group{

    // The length of the arrow
    private static final IntegerProperty ARROW_LENGTH = new SimpleIntegerProperty(Cell.BOX_SIZE / 3);

    public Path arrow;
    public Path path;

    /**
     * Constructs and binds the appropriate properties for the line and
     * the arrow
     */
    DirectedPath(DoubleBinding startX, DoubleBinding startY,
                 DoubleBinding endX, DoubleBinding endY){
        Main.assertFxThread();

        this.path = new Path();

        MoveTo start = new MoveTo();
        start.xProperty().bind(startX);
        start.yProperty().bind(startY);

        LineTo end = new LineTo();
        end.xProperty().bind(endX);
        end.yProperty().bind(endY);

        path.getElements().add(start);
        path.getElements().add(end);

        this.arrow = getArrow();

        this.getChildren().add(path);
        this.getChildren().add(arrow);

        this.path.getStyleClass().setAll("edge");
    }

    /**
     * @param isDashed whether to draw this line as a dashed or solid line
     */
    void setDashed(boolean isDashed){
        Main.assertFxThread();
        if(isDashed){
            this.path.getStyleClass().setAll("edge", "invisEdge");
        }else{
            this.path.getStyleClass().setAll("edge");
        }
    }

    /**
     * Adds a point to the line at the given index, and updates the path and arrow
     * appropriately
     * @param newX the x of the new point
     * @param newY the y of the new point
     * @param index the index to add the point in
     */
    void addPoint(DoubleBinding newX, DoubleBinding newY, int index){
        Main.assertFxThread();
        this.getChildren().remove(path);
        this.getChildren().remove(arrow);

        LineTo newLine = new LineTo();
        newLine.xProperty().bind(newX);
        newLine.yProperty().bind(newY);

        path.getElements().add(index, newLine);
        this.arrow = getArrow();

        this.getChildren().add(path);
        this.getChildren().add(arrow);
    }

    /**
     * Adds a point to the line at the index just before the endpoint
     * @param newX the x of the new point
     * @param newY the y of the new point
     */
    void addPoint(DoubleBinding newX, DoubleBinding newY){
        Main.assertFxThread();
        this.addPoint(newX, newY, path.getElements().size() - 1);
    }

    /**
     * Removes the point at the given index from the line
     * @param index the index of the point to remove
     */
    void removePoint(int index){
        Main.assertFxThread();
        this.getChildren().remove(path);
        this.getChildren().remove(arrow);

        path.getElements().remove(index);

        this.getChildren().add(path);
        this.getChildren().add(arrow);
    }

    /**
     * http://www.dbp-consulting.com/tutorials/canvas/CanvasArrow.html
     * Position of the endpoints of the arrow on either side are given respectively by
     * x = tipX+Math.cos((3*pi/4)+atan2(slope))*arrow_length
     * y = tipY+Math.sin((3*pi/4)+atan2(slope))*arrow_length
     * and
     * x = tipX+Math.cos((5*pi/4)+atan2(slope))*arrow_length
     * y = tipY+Math.sin((5*pi/4)+atan2(slope))*arrow_length
     *
     * @return the path that will draw an arrow at the end of the line
     */
    private Path getArrow(){
        Main.assertFxThread();
        ObservableList<PathElement> list =  this.path.getElements();
        DoubleBinding tipX = ((LineTo) list.get(list.size()-1)).xProperty().add(0);
        DoubleBinding tipY = ((LineTo) list.get(list.size()-1)).yProperty().add(0);

        DoubleBinding buttX;
        DoubleBinding buttY;
        if(list.size()>2){
            buttX = ((LineTo) list.get(list.size()-2)).xProperty().add(0);
            buttY = ((LineTo) list.get(list.size()-2)).yProperty().add(0);
        }else{
            buttX = ((MoveTo) list.get(list.size()-2)).xProperty().add(0);
            buttY = ((MoveTo) list.get(list.size()-2)).yProperty().add(0);
        }

        DoubleProperty rise = new SimpleDoubleProperty();
        DoubleProperty run = new SimpleDoubleProperty();
        rise.bind(tipY.subtract(buttY));
        run.bind(tipX.subtract(buttX));

        MoveTo left = new MoveTo();
        left.xProperty().bind(tipX.add(new CosBinding(new ArcTanBinding(rise, run).add(Math.PI + Math.PI / 4)).multiply(ARROW_LENGTH)));
        left.yProperty().bind(tipY.add(new SinBinding(new ArcTanBinding(rise, run).add(Math.PI + Math.PI / 4)).multiply(ARROW_LENGTH)));

        LineTo tip = new LineTo();
        tip.xProperty().bind(tipX);
        tip.yProperty().bind(tipY);

        LineTo right = new LineTo();
        right.xProperty().bind(tipX.add(new CosBinding(new ArcTanBinding(rise, run).add(Math.PI - Math.PI / 4)).multiply(ARROW_LENGTH)));
        right.yProperty().bind(tipY.add(new SinBinding(new ArcTanBinding(rise, run).add(Math.PI - Math.PI / 4)).multiply(ARROW_LENGTH)));

        Path temp = new Path();
        temp.getElements().add(left);
        temp.getElements().add(tip);
        temp.getElements().add(right);

        temp.getStyleClass().setAll("edge");
        return temp;
    }

    /**
     * Helper class that provides a binding to the ArcTan of two values
     */
    private class ArcTanBinding extends DoubleBinding{
        private final DoubleProperty x, y;

        ArcTanBinding(DoubleProperty x, DoubleProperty y){
            Main.assertFxThread();
            this.x = x;
            this.y = y;
            super.bind(this.x);
            super.bind(this.y);
        }

        @Override
        protected double computeValue(){
            Main.assertFxThread();
            return Math.atan2(this.x.get(), this.y.get());
        }
    }

    /**
     * Helper class that provides a binding to the Sin of a value
     */
    private class SinBinding extends DoubleBinding{
        private final DoubleBinding theta;

        SinBinding(DoubleBinding theta){
            Main.assertFxThread();
            this.theta = theta;
            super.bind(this.theta);
        }

        @Override
        protected double computeValue(){
            Main.assertFxThread();
            return Math.sin(this.theta.get());
        }
    }

    /**
     * Helper class that provides a binding to the Cos of a value
     */
    private class CosBinding extends DoubleBinding{
        private final DoubleBinding theta;

        CosBinding(DoubleBinding theta){
            Main.assertFxThread();
            this.theta = theta;
            super.bind(this.theta);
        }

        @Override
        protected double computeValue(){
            Main.assertFxThread();
            return Math.cos(this.theta.get());
        }
    }
}
