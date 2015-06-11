package edugit.treefx;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Group;
import javafx.scene.shape.Line;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;

/**
 * Created by makik on 6/11/15.
 */
public class DirectedLine extends Group{

    private final DoubleProperty startX;
    private final DoubleProperty startY;
    private final DoubleProperty endX;
    private final DoubleProperty endY;

    private final DoubleProperty tipX;
    private final DoubleProperty tipY;

    Line line;
    Path arrow;

    public DirectedLine(){
        this.line = new Line();
        this.getChildren().add(line);

        startX = line.startXProperty();
        startY = line.startYProperty();
        endX = line.endXProperty();
        endY = line.endYProperty();

        tipX = new SimpleDoubleProperty();
        tipY = new SimpleDoubleProperty();

        tipX.bind(endX);
        tipY.bind(endY.add(5));

        this.arrow = new Path();

        MoveTo left = new MoveTo();
        left.xProperty().bind(tipX.add(-5));
        left.yProperty().bind(tipY.add(5));

        LineTo tip = new LineTo();
        tip.xProperty().bind(tipX);
        tip.yProperty().bind(tipY);

        LineTo right = new LineTo();
        right.xProperty().bind(tipX.add(5));
        right.yProperty().bind(tipY.add(5));

        arrow.getElements().add(left);
        arrow.getElements().add(tip);
        arrow.getElements().add(right);

        this.getChildren().add(arrow);
    }

    public double getStartX(){
        return startX.get();
    }

    public DoubleProperty startXProperty(){
        return startX;
    }

    public double getStartY(){
        return startY.get();
    }

    public DoubleProperty startYProperty(){
        return startY;
    }

    public double getEndX(){
        return endX.get();
    }

    public DoubleProperty endXProperty(){
        return endX;
    }

    public double getEndY(){
        return endY.get();
    }

    public DoubleProperty endYProperty(){
        return endY;
    }

    public double getTipX(){
        return tipX.get();
    }

    public DoubleProperty tipXProperty(){
        return tipX;
    }

    public double getTipY(){
        return tipY.get();
    }

    public DoubleProperty tipYProperty(){
        return tipY;
    }
}
