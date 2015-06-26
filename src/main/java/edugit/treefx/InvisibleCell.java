package main.java.edugit.treefx;

import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeType;

/**
 * A subclass of Cell that is drawn with a dashed line and transparent fill
 */
public class InvisibleCell extends Cell{

    public InvisibleCell(String cellId, long time, Cell parent){
        super(cellId, time, parent);
    }

    public InvisibleCell(String cellId, long time, Cell parent1, Cell parent2){
        super(cellId, time, parent1, parent2);
    }

    @Override
    protected Node getBaseView(){
        Shape s = new Rectangle(BOX_SIZE, BOX_SIZE);
        s.setFill(null);
        s.setStroke(Highlighter.STANDARD_COLOR);
        s.setStrokeType(StrokeType.INSIDE);
        s.getStrokeDashArray().addAll(2., 3.);
        s.setStrokeDashOffset(1.);
        s.setStrokeLineCap(StrokeLineCap.BUTT);
        return s;
    }

    @Override
    public void setColor(Color color){
        Shape s = (Shape) view;
        s.setStroke(color);
        if(color.equals(Highlighter.STANDARD_COLOR)){
            s.getStrokeDashArray().addAll(2., 3.);
        }else{
            s.getStrokeDashArray().clear();
        }
    }
}
