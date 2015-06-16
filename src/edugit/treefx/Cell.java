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
    private static final Color[] HIGHLIGHT_COLORS = {Color.RED, Color.DARKGREEN, Color.VIOLET, Color.BLACK, Color.ORANGE};

    private Color currentColor;

    // The displayed view
    Node view;
    // The tooltip shown on hover
    Tooltip tooltip;

    // The unique ID of this cell
    private final String cellId;

    // The list of children of this cell
    List<Cell> children = new ArrayList<>();

    // The parent object that holds the parents of this cell
    ParentCell parents;

    List<Edge> edges = new ArrayList<>();

    // The number of generations away from the furthest leaf cell
    int height;
    IntegerProperty heightProperty;

    private boolean isSelected = false;
    private static int selectedCount = 0;

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
        this.parents = new ParentCell(this, parent1, parent2);

        currentColor = STANDARD_COLOR;
        setView(new Rectangle(BOX_SIZE, BOX_SIZE, currentColor));
//        setView(new Text(cellId));

        this.height = 0;
        this.heightProperty = new SimpleIntegerProperty(this.height);

        updateHeight();

        tooltip = new Tooltip(cellId);
        tooltip.setWrapText(true);
        tooltip.setMaxWidth(200);
        Tooltip.install(this, tooltip);

        this.setOnMouseClicked(event -> handleMouseClicked());
//        this.setOnMouseEntered(event -> highlight(true, true));
//        this.setOnMouseExited(event -> highlight(false, true));
    }

    private void handleMouseClicked(){
        System.out.println("ID: " + cellId + "\n" + "Height: " + this.height + "\n" + tooltip.getText());

        isSelected = !isSelected;

        if(isSelected){
            this.currentColor = HIGHLIGHT_COLORS[selectedCount%HIGHLIGHT_COLORS.length];
            selectedCount++;
            highlight(true, true);
        }else{
            selectedCount--;
            this.currentColor = STANDARD_COLOR;
            highlight(false, true);
        }
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
        parents.updateHeight();
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
        boolean isHighlighted =  enable || isSelected;
        if(enableRelatives){
            Edge.allVisible.set(selectedCount == 0);
            this.highlightEdges(isHighlighted);
            parents.highlight(isHighlighted);
            for(Cell child : children){
                child.highlight(isHighlighted, false);
            }
        }
        if(!isSelected && isRelativeSelected()){
            this.currentColor = getRelativesColor();
        }else if(!isHighlighted){
            this.currentColor = STANDARD_COLOR;
        }
        Shape s = (Shape) view;
        s.setFill(this.currentColor);
    }

    private Color getRelativesColor(){
        Color c = parents.getColor();
        if(c.equals(STANDARD_COLOR)){
            for(Cell child : children){
                Color childColor = child.currentColor;
                if(!childColor.equals(STANDARD_COLOR)){
                    return childColor;
                }
            }
        }
        return c;
    }

    private boolean isRelativeSelected(){
        boolean childSelected = false;
        for(Cell c : children){
            childSelected = childSelected || c.isSelected;
        }
        return parents.isSelected() || childSelected;
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
        int i = Integer.compare(this.height, c.height);
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
        if(i != 0){
            return i;
        }

        int cParentCount = c.parents.count();
        return Integer.compare(parents.count(), cParentCount);
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
         * @return the number of parent commits associated with this object
         */
        public int count(){
            int count = 0;
            if(mom != null) count++;
            if(dad != null) count++;
            return count;
        }

        /**
         * @return the stored parent commits in list form
         */
        public ArrayList<Cell> toList(){
            ArrayList<Cell> list = new ArrayList<>(2);
            if(mom != null) list.add(mom);
            if(dad != null) list.add(dad);
            return list;
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

        public void highlight(boolean enable){
            if(this.mom != null){
                this.mom.highlight(enable, false);
            }
            if(this.dad != null){
                this.dad.highlight(enable, false);
            }
        }

        public boolean isSelected(){
            return (this.mom != null && this.mom.isSelected) || (this.dad != null && this.dad.isSelected);
        }

        public Color getColor(){
            if(this.mom != null && this.mom.isSelected){
                return this.mom.currentColor;
            }
            if(this.dad != null && this.dad.isSelected){
                return this.dad.currentColor;
            }
            return STANDARD_COLOR;
        }
    }
}
