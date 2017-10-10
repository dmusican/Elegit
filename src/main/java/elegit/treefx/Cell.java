package elegit.treefx;

import elegit.CommitTreeController;
import elegit.Main;
import elegit.RefHelper;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Shape;
import javafx.util.Duration;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A class that represents a node in a TreeGraph
 */
public class Cell extends Pane{

    // Base shapes for different types of cells
    static final CellShape DEFAULT_SHAPE = CellShape.SQUARE;
    public static final CellShape UNTRACKED_BRANCH_HEAD_SHAPE = CellShape.CIRCLE;
    public static final CellShape TRACKED_BRANCH_HEAD_SHAPE = CellShape.TRIANGLE_UP;

    // The size of the rectangle being drawn
    public static final int BOX_SIZE = 20;

    //The height of the shift for the cells;
    private static final int BOX_SHIFT = 20;

    // The inset for the background;
    static final int BOX_INSET = 1;
    static final int BOX_INSIDE = 2;

    private static final String BACKGROUND_COLOR = "#F4F4F4";

    // Limits on animation so the app doesn't begin to stutter
    private static final int MAX_NUM_CELLS_TO_ANIMATE = 5;
    private static int numCellsBeingAnimated = 0;

    // The displayed view
    Node view;
    private CellShape shape;
    private CellType type;
    // The tooltip shown on hover
    private Tooltip tooltip;

    // The unique ID of this cell
    private final String cellId;
    // The assigned time of this commit
    private final long time;

    private ContextMenu contextMenu;

    private CellLabelContainer refLabels;

    private final AtomicBoolean animate = new AtomicBoolean();
    private final AtomicBoolean useParentAsSource = new AtomicBoolean();

    // The list of children of this cell
    private final List<Cell> children = new ArrayList<>();

    // The parent object that holds the parents of this cell
    private final ParentCell parents;

    // All edges that have this cell as an endpoint
    List<Edge> edges = new ArrayList<>();

    // The row and column location of this cell
    IntegerProperty columnLocationProperty, rowLocationProperty;

    // Whether this cell has been moved to its appropriate location
    private BooleanProperty hasUpdatedPosition;

    /**
     * Constructs a node with the given ID and the given parents
     * @param cellId the ID of this node
     * @param parents the parent(s) of this cell
     * @param type the type of cell to add
     */
    public Cell(String cellId, long time, List<Cell> parents, CellType type){
        this.cellId = cellId;
        this.time = time;
        this.parents = new ParentCell(this, parents);
        this.refLabels = new CellLabelContainer();
        this.type = type;

        setShape(DEFAULT_SHAPE);

        this.columnLocationProperty = new SimpleIntegerProperty(-1);
        this.rowLocationProperty = new SimpleIntegerProperty(-1);

        this.hasUpdatedPosition = new SimpleBooleanProperty(false);
        visibleProperty().bind(this.hasUpdatedPosition);

        columnLocationProperty.addListener((observable, oldValue, newValue) ->
                hasUpdatedPosition.set(oldValue.intValue()==newValue.intValue() || (newValue.intValue()>-1)&&oldValue.intValue()>-1));
        rowLocationProperty.addListener((observable, oldValue, newValue) ->
                hasUpdatedPosition.set(oldValue.intValue()==newValue.intValue() || (newValue.intValue()>-1)&&oldValue.intValue()>-1));

        // This next line is slow for me (Java 1.8.0_101), but is supposedly fixed in Java 9:
        // https://bugs.openjdk.java.net/browse/JDK-8143033
        tooltip = new Tooltip(cellId);
        tooltip.setWrapText(true);
        tooltip.setMaxWidth(300);
        Tooltip.install(this, tooltip);

        this.setOnMouseClicked(event -> {
            if(event.getButton() == MouseButton.PRIMARY){
                if (event.isShiftDown())
                    CommitTreeController.handleMouseClickedShift(this);
                else
                    CommitTreeController.handleMouseClicked(this.cellId);
            }else if(event.getButton() == MouseButton.SECONDARY){
                if(contextMenu != null){
                    contextMenu.show(this, event.getScreenX(), event.getScreenY());
                }
            }
            event.consume();
        });
        this.setOnMouseEntered(event -> CommitTreeController.handleMouseover(this, true));
        this.setOnMouseExited(event -> CommitTreeController.handleMouseover(this, false));

        this.view=getBaseView();
    }

    /**
     * Moves this cell to the given x and y coordinates
     * @param x the x coordinate to move to
     * @param y the y coordinate to move to
     * @param animate whether to animate the transition from the old position
     * @param emphasize whether to have the Highlighter class emphasize this cell while it moves
     */
    void moveTo(double x, double y, boolean animate, boolean emphasize){
        Main.assertFxThread();
        if(animate && numCellsBeingAnimated < MAX_NUM_CELLS_TO_ANIMATE){
            numCellsBeingAnimated++;

            Shape placeHolder = (Shape) getBaseView();
            placeHolder.setTranslateX(x+TreeLayout.H_PAD);
            placeHolder.setTranslateY(y+BOX_SHIFT);
            placeHolder.setOpacity(0.0);
            ((Pane)(this.getParent())).getChildren().add(placeHolder);

            TranslateTransition t = new TranslateTransition(Duration.millis(3000), this);
            t.setToX(x);
            t.setToY(y+BOX_SHIFT);
            t.setCycleCount(1);
            t.setOnFinished(event -> {
                numCellsBeingAnimated--;
                ((Pane)(this.getParent())).getChildren().remove(placeHolder);
            });
            t.play();

            if(emphasize){
                Highlighter.emphasizeCell(this);
            }
        }else{
            setTranslateX(x);
            setTranslateY(y+BOX_SHIFT);
        }
        this.refLabels.translate(x,y);
        this.hasUpdatedPosition.set(true);
        if (!this.refLabels.isVisible())
            this.refLabels.setVisible(true);
    }

    /**
     * @return the basic view for this cell
     */
    private Node getBaseView(){
        Node node = DEFAULT_SHAPE.getType(this.type);
        setFillType((Shape)node, CellState.STANDARD);
        node.getStyleClass().setAll("cell");
        node.setId("tree-cell");
        return node;
    }

    /**
     * Sets the look of this cell
     * @param newView the new view
     */
    public synchronized void setView(Node newView) {
        if(this.view == null){
            this.view = getBaseView();
        }

        //newView.setStyle(this.view.getStyle());
        newView.getStyleClass().setAll(this.view.getStyleClass());
        newView.setId(this.view.getId());

        this.view = newView;
        Platform.runLater(() -> {
            getChildren().clear();
            getChildren().add(this.view);
            setFillType((Shape) this.view, CellState.STANDARD);
        });
    }

    /**
     * Set the shape of this cell
     * @param newShape the new shape
     */
    public synchronized void setShape(CellShape newShape){
        if(this.shape == newShape) return;
        setView(newShape.getType(this.type));
        this.shape = newShape;
    }

    /**
     * Sets the tooltip to display the given text
     * @param label the text to display
     */
    private void setCommitDescriptor(String label){
        tooltip.setText(label);
    }

    private void setRefLabels(List<RefHelper> refs){
        this.refLabels.setLabels(refs, this);
    }

    private void setCurrentRefLabels(HashSet<String> refs) {
        this.refLabels.setCurrentLabels(refs);
    }

    void setLabels(String commitDescriptor, List<RefHelper> refLabels){
        setCommitDescriptor(commitDescriptor);
        setRefLabels(refLabels);
    }

    void setCurrentLabels(HashSet<String> refLabels) {
        setCurrentRefLabels(refLabels);
    }

    void setLabelMenus(Map<RefHelper, ContextMenu> menuMap) {
        this.refLabels.setLabelMenus(menuMap);
    }

    void setRemoteLabels(List<String> branchLabels) {
        this.refLabels.setRemoteLabels(branchLabels);
    }

    void setAnimate(boolean animate) {this.animate.set(animate);}

    void setUseParentAsSource(boolean useParentAsSource) {this.useParentAsSource.set(useParentAsSource);}

    void setContextMenu(ContextMenu contextMenu){
        this.contextMenu = contextMenu;
    }

    /**
     * Adds a child to this cell
     * @param cell the new child
     */
    private void addCellChild(Cell cell) {
        children.add(cell);
    }

    /**
     * @return the list of the children of this cell
     */
    List<Cell> getCellChildren() {
        return children;
    }

    /**
     * @return the list of the parents of this cell
     */
    List<Cell> getCellParents(){
        return parents.toList();
    }

    /**
     * @return whether or not this cell wants to be animated in the next transition
     */
    boolean getAnimate() { return this.animate.get(); }

    /**
     * @return whether or not to use the parent to base the animation off of
     */
    boolean getUseParentAsSource() { return this.useParentAsSource.get(); }

    /**
     * Removes the given cell from the children of this cell
     * @param cell the cell to remove
     */
    void removeCellChild(Cell cell) {
        children.remove(cell);
    }

    /**
     * Checks to see if the given cell has this cell as an ancestor,
     * up to the given number of generations.
     *
     * Entering zero or a negative number will search all descendants
     *
     * @param cell the commit to check
     * @param depth how many generations down to check
     * @return true if cell is a descendant of this cell, otherwise false
     */
    private boolean isChild(Cell cell, int depth){
        depth--;
        if(children.contains(cell)) return true;
        else if(depth != 0){
            for(Cell child : children){
                if(child.isChild(cell, depth)){
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Sets the state of this cell and adjusts the style accordingly
     * @param state the new state of the cell
     */
    void setCellState(CellState state){
        Platform.runLater(() -> setFillType((Shape) view, state));
    }

    /**
     * Sets the cell type to local, both or remote and resets edges accordingly
     * @param type the type of the cell
     */
    void setCellType(CellType type) {
        this.type = type;
        Platform.runLater(() -> setFillType((Shape) view, CellState.STANDARD));
        for (Edge e : edges) {
            e.resetDashed();
        }
    }

    CellType getCellType() {
        return this.type;
    }

    /**
     * @return the unique ID of this cell
     */
    public String getCellId() {
        return cellId;
    }

    /**
     * @return the time of this cell
     */
    long getTime(){
        return time;
    }

    public Node getLabel() { return this.refLabels; }

    @Override
    public String toString(){
        return cellId;
    }



    /**
     * Sets the fill type of a shape based on this cell's type
     * @param n the shape to set the fill of
     * @param state the state of the cell, determines coloring
     */
    private void setFillType(Shape n, CellState state) {
        Color baseColor = Color.web(state.getBackgroundColor());
        switch(this.type) {
            case LOCAL:
                n.setFill(baseColor);
                break;
            case REMOTE:
                n.setFill(Color.web(BACKGROUND_COLOR));
                n.setStroke(baseColor);
                break;
            case BOTH:
                n.setFill(baseColor);
                n.setStroke(Color.GRAY);
                break;
            default:
                break;
        }
    }

    public enum CellType {
        BOTH,
        LOCAL,
        REMOTE
    }

    /**
     * A class that holds the parents of a cell
     */
    private class ParentCell{

        private final List<Cell> parents;

        /**
         * Sets the given child to have the given parents
         * @param child the child cell
         * @param parents the list of parents
         */
        ParentCell(Cell child, List<Cell> parents) {
            ArrayList<Cell> buildingParents = new ArrayList<>();
            for (Cell parent : parents)
                buildingParents.add(parent);
            this.parents = Collections.unmodifiableList(buildingParents);
            this.setChild(child);
        }

        /**
         * @return the number of parent commits associated with this object
         */
        public int count(){
            return parents.size();
        }

        /**
         * @return the stored parent commits in list form
         */
        List<Cell> toList(){
            return parents;
        }

        /**
         * Sets the given sell to be the child of each non-null parent
         * @param cell the child to add
         */
        private void setChild(Cell cell){
            for(Cell parent : parents) {
                if(parent != null) {
                    parent.addCellChild(cell);
                }
            }
        }
    }
}
