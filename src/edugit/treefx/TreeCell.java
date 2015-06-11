package edugit.treefx;

import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by makik on 6/11/15.
 */
public class TreeCell extends Cell{

    Cell parent;
    boolean isRoot;

    public TreeCell(String cellId, Cell parent){
        super(cellId);
        this.parent = parent;
        this.isRoot = false;

        setView(new Rectangle(10,10, Color.BLUE));
//        setView(new Text(cellId));
    }

    @Override
    public void addCellParent(Cell cell){
        this.parent = cell;
    }

    @Override
    public List<Cell> getCellParents(){
        List<Cell> l = new ArrayList<>(1);
        l.add(parent);
        return l;
    }

    public Cell getCellParent(){
        return parent;
    }

    public Cell setRoot(){
        this.isRoot = true;
        Cell temp = parent;
        this.parent = null;
        return temp;
    }
}
