package edugit.treefx;

import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.Group;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;

/**
 * Created by makik on 6/11/15.
 *
 * Represents a line with an arrow at the end. The arrow is constructed using a three-point Path
 * object that has its vertices bound to the end of the line.
 */
public class DirectedPath extends Group{

    public static final IntegerProperty ARROW_LENGTH = new SimpleIntegerProperty(Cell.BOX_SIZE / 3);

    Path arrow;
    Path path;

    /**
     * Constructs and binds the appropriate properties for the line and
     * the arrow
     */
    public DirectedPath(DoubleBinding x1, DoubleBinding y1,
                        DoubleBinding x2, DoubleBinding y2,
                        DoubleBinding x3,DoubleBinding y3){

        this.path = new Path();

        MoveTo start = new MoveTo();
        start.xProperty().bind(x1);
        start.yProperty().bind(y1);

        LineTo mid = new LineTo();
        mid.xProperty().bind(x2);
        mid.yProperty().bind(y2);

        LineTo end = new LineTo();
        end.xProperty().bind(x3);
        end.yProperty().bind(y3);

        path.getElements().add(start);
        path.getElements().add(mid);
        path.getElements().add(end);

        this.arrow = getArrow(x2, y2, x3, y3);

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
    */
    private Path getArrow(DoubleBinding buttX, DoubleBinding buttY, DoubleBinding tipX, DoubleBinding tipY){
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

        return temp;
    }

    private class ArcTanBinding extends DoubleBinding{
        private final DoubleProperty x, y;

        public ArcTanBinding(DoubleProperty x, DoubleProperty y){
            this.x = x;
            this.y = y;
            super.bind(this.x);
            super.bind(this.y);
        }

        @Override
        protected double computeValue(){
            return Math.atan2(this.x.get(), this.y.get());
        }
    }

    private class SinBinding extends DoubleBinding{
        private final DoubleBinding theta;

        public SinBinding(DoubleBinding theta){
            this.theta = theta;
            super.bind(this.theta);
        }

        @Override
        protected double computeValue(){
            return Math.sin(this.theta.get());
        }
    }

    private class CosBinding extends DoubleBinding{
        private final DoubleBinding theta;

        public CosBinding(DoubleBinding theta){
            this.theta = theta;
            super.bind(this.theta);
        }

        @Override
        protected double computeValue(){
            return Math.cos(this.theta.get());
        }
    }
}
