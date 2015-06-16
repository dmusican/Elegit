package edugit.treefx;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.Node;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by makik on 6/10/15.
 *
 * A class that represents a node in a TreeGraph
 */
public class Cell extends Pane implements Comparable<Cell>{

    // The size of the rectangle being drawn
    public static final int BOX_SIZE = 20;
    private static final Color STANDARD_COLOR = Color.BLUE;
    private static final Color HIGHLIGHT_COLOR = Color.RED;

    // The displayed view
    Node view;
    // The tooltip shown on hover
    Tooltip tooltip;

    // The unique ID of this cell
    private final String cellId;

    // The list of children of this cell
    List<Cell> children = new ArrayList<>();

    // The parent object that holds the parents of this cell
    ParentCell parent;

    List<Edge> edges = new ArrayList<>();

    // The number of generations away from the furthest leaf cell
    int height;
    IntegerProperty heightProperty;

    /**
     * Constructs a node with the given ID and a single parent node
     * @param cellId the ID of this node
     * @param parent the parent of this node
     */
    public Cell(String cellId, Cell parent){
        this(cellId, parent, null);
    }

    /**
     * Constructs a node with the given ID and the two given parent nodes
     * @param cellId the ID of this node
     * @param parent1 the first parent of this node
     * @param parent2 the second parent of this node
     */
    public Cell(String cellId, Cell parent1, Cell parent2){
        this.cellId = cellId;
        this.parent = new ParentCell(this, parent1, parent2);

        setView(new Rectangle(BOX_SIZE, BOX_SIZE, STANDARD_COLOR));
//        setView(new Text(cellId));

        this.height = 0;
        this.heightProperty = new SimpleIntegerProperty(this.height);

        updateHeight();

        this.setOnMouseClicked(event -> System.out.println("ID: " + cellId + "\n" + "Height: " + this.height + "\n" + tooltip.getText()));

        tooltip = new Tooltip(cellId);
        tooltip.setWrapText(true);
        tooltip.setMaxWidth(200);
        Tooltip.install(this, tooltip);

        this.setOnMouseEntered(event -> highlight(true, true));
        this.setOnMouseExited(event -> highlight(false, true));
    }

    /**
     * Sets the tooltip to display the given text
     * @param label the text to display
     */
    public void setDisplayLabel(String label){
        tooltip.setText(label);
    }

    /**
     * Updates the height of this cell based on the height of its children, then tells
     * its parents to update
     */
    public void updateHeight(){
        for(Cell c : children){
            this.height = (this.height <= c.height) ? (c.height + 1) : this.height;
            this.heightProperty.set(this.height);
        }
        parent.updateHeight();
    }

    /**
     * Adds a child to this cell
     * @param cell the new child
     */
    public void addCellChild(Cell cell) {
        children.add(cell);
        updateHeight();
    }

    /**
     * @return the list of the children of this cell
     */
    public List<Cell> getCellChildren() {
        return children;
    }

    /**
     * Removes the given cell from the children of this cell
     * @param cell the cell to remove
     */
    public void removeCellChild(Cell cell) {
        children.remove(cell);
    }

    /**
     * Sets the look of this cell
     * @param view the new view
     */
    public void setView(Node view) {
        this.view = view;
        getChildren().add(view);
    }

    public void highlight(boolean enable, boolean enableRelatives){
        if(enableRelatives){
            Edge.allVisible.set(!enable);
            this.highlightEdges(enable);
            parent.highlight(enable);
            for(Cell child : children){
                child.highlight(enable, false);
            }
        }
        Shape s = (Shape) view;
        if(enable){
            s.setFill(HIGHLIGHT_COLOR);
        }else{
            s.setFill(STANDARD_COLOR);
        }
    }

    private void highlightEdges(boolean enable){
        for(Edge e : edges){
            e.setHighlighted(enable);
        }
    }

    /**
     * @return the unique ID of this cell
     */
    public String getCellId() {
        return cellId;
    }

    @Override
    public int compareTo(Cell c){
        int i = Double.compare(this.height, c.height);
        if(i != 0){
            return i;
        }
        int minHeightCChild = c.height;
        for(Cell child : c.getCellChildren()){
            if(child.height < minHeightCChild){
                minHeightCChild = child.height;
            }
        }

        int minHeightChild = this.height;
        for(Cell child : this.getCellChildren()){
            if(child.height < minHeightChild){
                minHeightChild = child.height;
            }
        }
        i = Integer.compare(minHeightCChild, minHeightChild);
        return i;
    }

    /**
     * A class that holds the parents of a cell
     */
    private class ParentCell{

        private Cell mom,dad;

        /**
         * Sets the given child to have the given parents
         * @param child the child cell
         * @param mom the first parent
         * @param dad the second parent
         */
        public ParentCell(Cell child, Cell mom, Cell dad){
            this.mom = mom;
            this.dad = dad;
            this.setChild(child);
        }

        /**
         * Updates the heights of each held parent cell
         */
        public void updateHeight(){
            if(this.mom != null){
                this.mom.updateHeight();
            }
            if(this.dad != null){
                this.dad.updateHeight();
            }
        }

        /**
         * Sets the given sell to be the child of each non-null parent
         * @param cell the child to add
         */
        private void setChild(Cell cell){
            if(this.mom != null){
                this.mom.addCellChild(cell);
            }
            if(this.dad != null){
                this.dad.addCellChild(cell);
            }
        }

        public void highlight(boolean enable){
            if(this.mom != null){
                this.mom.highlight(enable, false);
            }
            if(this.dad != null){
                this.dad.highlight(enable, false);
            }
        }
    }
}
