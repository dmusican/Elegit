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
public class Cell extends Pane implements Comparable<Cell>{

    String cellId;

    List<Cell> children = new ArrayList<>();
    Cell parent;
    int height;

    Node view;

    public Cell(String cellId, Cell parent){
        this.cellId = cellId;
        this.parent = parent;

        setView(new Rectangle(10,10, Color.BLUE));
//        setView(new Text(cellId));

        this.height = 0;

        updateHeight();

        this.setOnMouseClicked(event -> {
            System.out.println("Node "+cellId);
        });
    }

    public void updateHeight(){
        for(Cell c : children){
            if(c.height == this.height){
                this.height = c.height+1;
            }
        }
        if(parent != null){
            parent.updateHeight();
        }
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

    @Override
    public int compareTo(Cell c){
        return Double.compare(c.height, this.height);
    }
}
