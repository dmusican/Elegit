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
        this.parent = new Parent(this, parent1, parent2);

        setView(new Rectangle(10, 10, Color.BLUE));
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
        int i = Double.compare(c.height, this.height);
        if(i != 0){
            return i;
        }
        return i;
//        int childMaxHeight = 0;
//        for(Cell child : this.getCellChildren()){
//            childMaxHeight = Math.max(childMaxHeight, child.height);
//        }
//
//        int cChildMaxHeight = 0;
//        for(Cell child : this.getCellChildren()){
//            cChildMaxHeight = Math.max(cChildMaxHeight, child.height);
//        }
//        return Integer.compare(cChildMaxHeight, childMaxHeight);
    }

    private class Parent{

        private Cell mom,dad;

        public Parent(Cell child, Cell mom, Cell dad){
            this.mom = mom;
            this.dad = dad;
            this.setChild(child);
        }

        public void updateHeight(){
            if(this.mom != null){
                this.mom.updateHeight();
            }
            if(this.dad != null){
                this.dad.updateHeight();
            }
        }

        public void setChild(Cell cell){
            if(this.mom != null){
                this.mom.addCellChild(cell);
            }
            if(this.dad != null){
                this.dad.addCellChild(cell);
            }
        }
    }
}
