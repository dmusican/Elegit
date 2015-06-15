package edugit.treefx;

import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.Group;
import javafx.scene.shape.Line;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;

/**
 * Created by makik on 6/11/15.
 *
 * Represents a line with an arrow at the end. The arrow is constructed using a three-point Path
 * object that has its vertices bound to the end of the line.
 */
public class DirectedLine extends Group{

    public static final IntegerProperty ARROW_LENGTH = new SimpleIntegerProperty(Cell.BOX_SIZE / 3);
    public static final IntegerProperty ARROW_VERTICAL_OFFSET = new SimpleIntegerProperty(Cell.BOX_SIZE / 2);

    private final DoubleProperty startX;
    private final DoubleProperty startY;
    private final DoubleProperty endX;
    private final DoubleProperty endY;

    private final DoubleProperty rise;
    private final DoubleProperty run;
    private final DoubleProperty slope;

    private final DoubleProperty tipX;
    private final DoubleProperty tipY;
    private final DoubleProperty arrowOffsetX;
    private final DoubleProperty arrowOffsetY;

    Line line;
    Path arrow;

    /**
     * Constructs and binds the appropriate properties for the line and
     * the arrow
     */
    public DirectedLine(){
        this.line = new Line();
        this.getChildren().add(line);

        startX = line.startXProperty();
        startY = line.startYProperty();
        endX = line.endXProperty();
        endY = line.endYProperty();

        slope = new SimpleDoubleProperty(0.0);
        rise = new SimpleDoubleProperty(0.0);
        run = new SimpleDoubleProperty(0.0);

        rise.bind(endY.subtract(startY));
        run.bind(endX.subtract(startX));
        slope.bind(rise.divide(run));

        tipX = new SimpleDoubleProperty();
        tipY = new SimpleDoubleProperty();

        tipX.bind(endX.add(ARROW_VERTICAL_OFFSET.divide(slope)));
        tipY.bind(endY.add(ARROW_VERTICAL_OFFSET));

        arrowOffsetX = new SimpleDoubleProperty(ARROW_LENGTH.doubleValue());
        arrowOffsetY = new SimpleDoubleProperty(ARROW_LENGTH.doubleValue());

        // http://www.dbp-consulting.com/tutorials/canvas/CanvasArrow.html
        // Position of the endpoints of the arrow on either side are given respectively by
        //
        // x = tipX+Math.cos((3*pi/4)+atan2(slope))*arrow_length
        // y = tipY+Math.sin((3*pi/4)+atan2(slope))*arrow_length
        //
        //and
        //
        // x = tipX+Math.cos((5*pi/4)+atan2(slope))*arrow_length
        // y = tipY+Math.sin((5*pi/4)+atan2(slope))*arrow_length

        this.arrow = new Path();

        MoveTo left = new MoveTo();
        left.xProperty().bind(tipX.add(new CosBinding(new ArcTanBinding(rise, run).add(Math.PI + Math.PI / 4)).multiply(ARROW_LENGTH)));
        left.yProperty().bind(tipY.add(new SinBinding(new ArcTanBinding(rise, run).add(Math.PI + Math.PI / 4)).multiply(ARROW_LENGTH)));

        LineTo tip = new LineTo();
        tip.xProperty().bind(tipX);
        tip.yProperty().bind(tipY);

        LineTo right = new LineTo();
        right.xProperty().bind(tipX.add(new CosBinding(new ArcTanBinding(rise, run).add(Math.PI - Math.PI / 4)).multiply(ARROW_LENGTH)));
        right.yProperty().bind(tipY.add(new SinBinding(new ArcTanBinding(rise, run).add(Math.PI - Math.PI / 4)).multiply(ARROW_LENGTH)));

        arrow.getElements().add(left);
        arrow.getElements().add(tip);
        arrow.getElements().add(right);

        this.getChildren().add(arrow);
    }

    public DoubleProperty startXProperty(){
        return startX;
    }

    public DoubleProperty startYProperty(){
        return startY;
    }

    public DoubleProperty endXProperty(){
        return endX;
    }

    public DoubleProperty endYProperty(){
        return endY;
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
