package elegit.treefx;

import elegit.CommitTreeController;
import elegit.Main;
import elegit.models.RefHelper;
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
import org.apache.http.annotation.ThreadSafe;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A class that represents a node in a TreeGraph
 *
 * A Cell extends Pane, so it IS a JavaFX node and should be treated as one.
 *
 * This code should have no Platform.runLater code in it whatsoever. The Cell becomes part of the scene graph.
 * Could aspects of be done off thread? Sure, but that gets really complicated, and it's why the FX thread is single-
 * threaded to start with. Some code in here is called from off-thread, but it's the job of them to make sure they
 * call Platform.runLater (or whatever) and deal with the consequences of the timing. From the perspective of this
 * class, it's all on FX thread. Don't take any of the asserts out without a complete rethinking of the philosophy.
 *
 */
// YIKES, NOT THREADSAFE due to non-private non-final variables
// YIKES, ALSO NOT THREADSAFE due to leaking returns of private data, check that out too
// but critically only because of all the asserts requiring this be done only in the FX thread. Without that, it
// isn't threadsafe.
// TODO: If this sticks, take out the atomic references etc I put in, they aren't necessary; ditto synchronized
public class Cell extends Pane {

    // Base shapes for different types of cells
    private static final CellShape DEFAULT_SHAPE = CellShape.SQUARE;
    private static final String BACKGROUND_COLOR = "#F4F4F4";

    // Limits on animation so the app doesn't begin to stutter
    private static final int MAX_NUM_CELLS_TO_ANIMATE = 5;
    private static AtomicInteger numCellsBeingAnimated = new AtomicInteger(0);

    // The tooltip shown on hover
    private final Tooltip tooltip;

    // The unique ID of this cell
    private final String cellId;

    // The assigned time of this commit
    private final long time;


    private final AtomicBoolean animate = new AtomicBoolean();
    private final AtomicBoolean useParentAsSource = new AtomicBoolean();

    private CellShape shape;
    private CellType type;

    private ContextMenu contextMenu;
    private CellLabelContainer refLabels;

    // The list of children of this cell
    private final List<Cell> childrenList = new ArrayList<>();

    // The parent object that holds the parents of this cell
    private final ParentCell parents;


    // Constants
    public static final CellShape UNTRACKED_BRANCH_HEAD_SHAPE = CellShape.CIRCLE;
    public static final CellShape TRACKED_BRANCH_HEAD_SHAPE = CellShape.TRIANGLE_UP;
    // The size of the rectangle being drawn
    public static final int BOX_SIZE = 20;
    //The height of the shift for the cells;
    private static final int BOX_SHIFT = 20;

    // Whether this cell has been moved to its appropriate location
    private BooleanProperty hasUpdatedPosition;

    // All edges that have this cell as an endpoint
    private List<Edge> edges = new ArrayList<>();

    // hard
    // There's a lot in here that's hard. This is because it's unclear what's happening on the FX thread
    // and what's not. Said differently, it's unclear what's view, and what's model. Get that disentangled, and
    // everything else should hopefully fall into place.

    // The displayed view
    Node view;

    // The row and column location of this cell
    IntegerProperty columnLocationProperty, rowLocationProperty;



    /**
     * Constructs a node with the given ID and the given parents
     * @param cellId the ID of this node
     * @param parents the parent(s) of this cell
     * @param type the type of cell to add
     */
    public Cell(String cellId, long time, List<Cell> parents, CellType type){
        Main.assertFxThread();
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

    public List<Edge> getEdges() {
        Main.assertFxThread();
        return Collections.unmodifiableList(new ArrayList<>(edges));
    }

    public void addEdge(Edge e) {
        Main.assertFxThread();
        edges.add(e);
    }

    public void removeEdge(Edge e) {
        Main.assertFxThread();
        edges.remove(e);
    }

    /**
     * Moves this cell to the given x and y coordinates
     * @param x the x coordinate to move to
     * @param y the y coordinate to move to
     * @param animate whether to animate the transition from the old position
     * @param emphasize whether to have the Highlighter class emphasize this cell while it moves
     */
    void moveTo(double x, double y, boolean animate, boolean emphasize){
        // In principle, there's a compare-and-set bug below, in getting numCellsBeingAnimated, testing its value,
        // and then conditionally incrementing, but this is all running the FX thread anyway. Leave that assert
        // below in there, it's critical.
        Main.assertFxThread();
        if(animate && numCellsBeingAnimated.get() < MAX_NUM_CELLS_TO_ANIMATE){
            numCellsBeingAnimated.getAndIncrement();

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
                numCellsBeingAnimated.getAndDecrement();
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
        Main.assertFxThread();
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
        Main.assertFxThread();
        if(this.view == null){
            this.view = getBaseView();
        }

        newView.getStyleClass().setAll(this.view.getStyleClass());
        newView.setId(this.view.getId());

        this.view = newView;
        getChildren().clear();
        getChildren().add(this.view);
        setFillType((Shape) this.view, CellState.STANDARD);
    }

    /**
     * Set the shape of this cell
     * @param newShape the new shape
     */
    public synchronized void setShape(CellShape newShape){
        Main.assertFxThread();
        if(this.shape == newShape) return;
        setView(newShape.getType(this.type));
        this.shape = newShape;
    }

    public void setShapeDefault() {
        Main.assertFxThread();
        setShape(DEFAULT_SHAPE);
    }

    /**
     * Sets the tooltip to display the given text
     * @param label the text to display
     */
    private void setCommitDescriptor(String label){
        Main.assertFxThread();
        tooltip.setText(label);
    }

    private void setRefLabels(List<RefHelper> refs){
        Main.assertFxThread();
        this.refLabels.setLabels(refs, this);
    }

    private void setCurrentRefLabels(Set<String> refs) {
        Main.assertFxThread();
        this.refLabels.setCurrentLabels(refs);
    }

    void setLabels(String commitDescriptor, List<RefHelper> refLabels){
        Main.assertFxThread();
        setCommitDescriptor(commitDescriptor);
        setRefLabels(refLabels);
    }

    void setCurrentLabels(Set<String> refLabels) {
        Main.assertFxThread();
        setCurrentRefLabels(refLabels);
    }

    void setLabelMenus(Map<RefHelper, ContextMenu> menuMap) {
        Main.assertFxThread();
        this.refLabels.setLabelMenus(menuMap);
    }

    void setRemoteLabels(List<String> branchLabels) {
        Main.assertFxThread();
        this.refLabels.setRemoteLabels(branchLabels);
    }

    void setAnimate(boolean animate) {
        Main.assertFxThread();
        this.animate.set(animate);}

    void setUseParentAsSource(boolean useParentAsSource) {
        Main.assertFxThread();
        this.useParentAsSource.set(useParentAsSource);}

    void setContextMenu(ContextMenu contextMenu) {
        Main.assertFxThread();
        this.contextMenu = contextMenu;
    }

    /**
     * Adds a child to this cell
     * @param cell the new child
     */
    private void addCellChild(Cell cell) {
        Main.assertFxThread();
        childrenList.add(cell);
    }

    /**
     * @return the list of the childrenList of this cell
     */
    List<Cell> getCellChildren() {
        Main.assertFxThread();
        return Collections.unmodifiableList(childrenList);
    }

    /**
     * @return the list of the parents of this cell
     */
    List<Cell> getCellParents(){
        Main.assertFxThread();
        return parents.toList();
    }

    /**
     * @return whether or not this cell wants to be animated in the next transition
     */
    boolean getAnimate() {
        Main.assertFxThread();
        return this.animate.get(); }

    /**
     * @return whether or not to use the parent to base the animation off of
     */
    boolean getUseParentAsSource() {
        Main.assertFxThread();
        return this.useParentAsSource.get(); }

    /**
     * Removes the given cell from the childrenList of this cell
     * @param cell the cell to remove
     */
    void removeCellChild(Cell cell) {
        Main.assertFxThread();
        childrenList.remove(cell);
    }

    /**
     * Sets the state of this cell and adjusts the style accordingly
     * @param state the new state of the cell
     */
    void setCellState(CellState state){
        Main.assertFxThread();
        setFillType((Shape) view, state);
    }

    /**
     * Sets the cell type to local, both or remote and resets edges accordingly
     * @param type the type of the cell
     */
    void setCellType(CellType type) {
        Main.assertFxThread();
        this.type = type;
        setFillType((Shape) view, CellState.STANDARD);
        for (Edge e : edges) {
            e.resetDashed();
        }
    }

    CellType getCellType() {
        Main.assertFxThread();
        return this.type;
    }

    /**
     * @return the unique ID of this cell
     */
    public String getCellId() {
        Main.assertFxThread();
        return cellId;
    }

    /**
     * @return the time of this cell
     */
    long getTime(){
        Main.assertFxThread();
        return time;
    }

    public Node getLabel() {
        Main.assertFxThread();
        return this.refLabels;
    }

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
        Main.assertFxThread();
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
            Main.assertFxThread();
            ArrayList<Cell> buildingParents = new ArrayList<>();
            buildingParents.addAll(parents);
            this.parents = Collections.unmodifiableList(buildingParents);
            this.setChild(child);
        }

        /**
         * @return the stored parent commits in list form
         */
        List<Cell> toList(){
            Main.assertFxThread();
            return parents;
        }

        /**
         * Sets the given sell to be the child of each non-null parent
         * @param cell the child to add
         */
        private void setChild(Cell cell){
            Main.assertFxThread();
            for(Cell parent : parents) {
                if(parent != null) {
                    parent.addCellChild(cell);
                }
            }
        }
    }
}
