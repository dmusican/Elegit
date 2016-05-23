package elegit.treefx;

import javafx.scene.Node;
import javafx.scene.shape.Shape;

/**
 * A subclass of Cell that is drawn with a dashed line and transparent fill
 */
public class InvisibleCell extends Cell{

    public InvisibleCell(String cellId, long time, Cell parent1, Cell parent2){
        super(cellId, time, parent1, parent2);
    }

    @Override
    protected Node getBaseView(){
        Shape node = DEFAULT_SHAPE.get();
        node.setFill(null);
        node.setStyle("-fx-stroke: " + CellState.STANDARD.getCssStringKey());
        node.getStyleClass().setAll("cell", "invisCell");
        return node;
    }

    @Override
    public synchronized void setShape(CellShape newShape){
        if(view == null){
            setView(getBaseView());
        }
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
