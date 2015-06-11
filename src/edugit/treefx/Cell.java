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
    Parent parent;
    int height;

    Node view;

    public Cell(String cellId, Cell parent){
        this(cellId, parent, null);
    }

    public Cell(String cellId, Cell parent1, Cell parent2){
        this.cellId = cellId;
        this.parent = new Parent(parent1, parent2);

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
            this.height = (this.height <= c.height) ? (c.height + 1) : this.height;
        }
        parent.updateHeight();
    }

    public void addCellChild(Cell cell) {
        children.add(cell);
    }

    public List<Cell> getCellChildren() {
        return children;
    }

    public void addCellParent(Cell cell) {
        parent.add(cell);
    }

    public Parent getCellParent() {
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

    private class Parent{

        private Cell mom,dad;

        public Parent(Cell mom, Cell dad){
            this.mom = mom;
            this.dad = dad;
        }

        public void updateHeight(){
            if(mom != null){
                mom.updateHeight();
            }
            if(dad != null){
                dad.updateHeight();
            }
        }

        public void add(Cell cell){
            if(mom != null){
                this.mom = cell;
            }else if(dad != null){
                this.dad = cell;
            }
        }
    }
}
