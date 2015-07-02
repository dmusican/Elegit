package main.java.edugit.treefx;

import javafx.scene.Node;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;

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
        s.setStyle("-fx-stroke: " + CellState.STANDARD.getCssStringKey());
        s.getStyleClass().setAll("cell", "invisCell");
        return s;
    }

    @Override
    public void setCellState(CellState state){
        Shape s = (Shape) view;
        s.setStyle("-fx-stroke: " + state.getCssStringKey());
        if(state == CellState.STANDARD){
            s.getStyleClass().setAll("cell", "invisCell");
        }else{
            s.getStyleClass().setAll("cell");
        }
    }
}
