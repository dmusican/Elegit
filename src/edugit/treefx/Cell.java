package edugit.treefx;

import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by makik on 6/10/15.
 */
public class Cell extends Pane {

    String cellId;

    List<Cell> children = new ArrayList<>();
    Cell parent;

    Node view;

    public Cell(String cellId, Cell parent){
        this.cellId = cellId;
        this.parent = parent;

        setView(new Rectangle(10,10, Color.BLUE));
    }

    public void addCellChild(Cell cell) {
        children.add(cell);
    }

    public List<Cell> getCellChildren() {
        return children;
    }

    public void addCellParent(Cell cell) {
        this.parent = cell;
    }

    public Cell getCellParent() {
        return parent;
    }

    public void removeCellChild(Cell cell) {
        children.remove(cell);
    }

    public void setView(Node view) {

        this.view = view;
        getChildren().add(view);

    }

    public Node getView() {
        return this.view;
    }

    public String getCellId() {
        return cellId;
    }
}
