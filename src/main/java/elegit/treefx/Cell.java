package elegit.treefx;

import elegit.Main;
import elegit.models.RefHelper;
import javafx.animation.TranslateTransition;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Shape;
import javafx.util.Duration;
import org.apache.http.annotation.GuardedBy;
import org.apache.http.annotation.ThreadSafe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A class that represents a node in a TreeGraph
 *
 * A Cell extends Pane, so it IS a JavaFX node and should be treated as one.
 *
 * The reason we use a Pane instead of just the shape directly itself is that we want it to be clickable, be able
 * to set styling, etc; and we sometimes swap out one shape for another depending on the features of the commit.
 * So the Cell represents everything bout that node in the graph, whereas the shape is just one feature.
 *
 */
@ThreadSafe
// but critically only because of all the asserts requiring this be done only in the FX thread. Without that, it
// isn't threadsafe. In some cases, non FX-thread specific information can be done off-thread, and that's been
// carefully checked. Beware of making updates here without being similarly careful.
public class Cell extends Pane {

    //// Class variables; constants and such
    // Base shapes for different types of cells
    public static final CellShape DEFAULT_SHAPE = CellShape.SQUARE;
    public static final CellShape UNTRACKED_BRANCH_HEAD_SHAPE = CellShape.CIRCLE;
    public static final CellShape TRACKED_BRANCH_HEAD_SHAPE = CellShape.TRIANGLE_DOWN;
    private static final String BACKGROUND_COLOR = "#F4F4F4";

    // Limits on animation so the app doesn't begin to stutter
    private static final int MAX_NUM_CELLS_TO_ANIMATE = 5;
    private static AtomicInteger numCellsBeingAnimated = new AtomicInteger(0);

    // The size of the rectangle being drawn
    public static final int BOX_SIZE = 20;
    //The height of the shift for the cells;
    private static final int BOX_SHIFT = 20;


    ///// Instance variables, specific for a particular cell
    // The tooltip shown on hover
    @GuardedBy("this") private final Tooltip tooltip;

    // The unique ID of this cell
    private final String cellId;

    // The assigned time of this commit
    private final long time;

    // animate represents whether or not this cell should be animated when it moves
    // useParentAsSource seems to be a secondary animate variable.
    // TODO: clean up useParentAsSource. Make it more understandable and determine if it is serving a necessary purpose.
    private final AtomicBoolean animate = new AtomicBoolean();
    private final AtomicBoolean useParentAsSource = new AtomicBoolean();

    // Whether this cell has been moved to its appropriate location
    private final BooleanProperty hasUpdatedPosition;

    // Actual shape of the cell itself (triangle, circle, etc.)
    private CellShape shape;

    // Whether the cell represents a local commit, a remote commit, or both (they're rendered differently)
    @GuardedBy("this") private CellType localOrRemote;

    // The right-click context menu that is attached to the cell
    @GuardedBy("this") private ContextMenu contextMenu;

    // All of the reference labels attached to the cell (branch name, tags, etc.)
    // TODO: Make sure refLabels is appropriately handled from a threading perspective.
    private CellLabelContainer refLabels;

    // The list of children of this cell
    @GuardedBy("this") private final List<Cell> childrenList = new ArrayList<>();

    // The parent object that holds the parents of this cell
    @GuardedBy("this") private final ParentCell parents;

    // Used to keep track of a cell's "permanent" state; it may transiently change as the result of an animation
    @GuardedBy("this") private CellState persistentCellState;

    // All edges that have this cell as an endpoint
    @GuardedBy("this") private final List<Edge> edges = new ArrayList<>();


    // The following are kept public so that other aspects of the view can bind and work with them. It is critical
    // that they only be accessed from the FX thread.

    // The displayed view. Don't touch this except on FX thread!
    Shape fxShapeObject;

    // The row and column location of this cell. Don't touch these except on FX thread!
    IntegerProperty columnLocationProperty, rowLocationProperty;


    /**
     * Constructs a node with the given ID and the given parents
     * @param cellId the ID of this node
     * @param parents the parent(s) of this cell
     * @param type the localOrRemote of cell to add
     */
    // Can be done off FX thread since doesn't actually affect anything on the FX scene graph
    public Cell(String cellId, long time, List<Cell> parents, CellType type){
        this.cellId = cellId;
        this.time = time;
        this.parents = new ParentCell(this, parents);
        this.refLabels = new CellLabelContainer();
        this.localOrRemote = type;

        setShape(DEFAULT_SHAPE);

        this.columnLocationProperty = new SimpleIntegerProperty(-1);
        this.rowLocationProperty = new SimpleIntegerProperty(-1);

        // These are all FX safe because they are in the constructor, and hence no one else has access to them yet
        // ... so bindings are ok at this point in time
        // https://docs.oracle.com/javase/specs/jls/se8/html/jls-8.html#jls-8.8.3
        // https://bruceeckel.github.io/2017/01/13/constructors-are-not-thread-safe/
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
            Main.assertFxThread();
            // synchronized for contextMenu
            synchronized (this) {
                if (event.getButton() == MouseButton.PRIMARY) {
                    if (event.isShiftDown())
                        CommitTreeController.handleMouseClickedShift(this);
                    else
                        CommitTreeController.handleMouseClicked(this.cellId);
                } else if (event.getButton() == MouseButton.SECONDARY) {
                    if (contextMenu != null) {
                        contextMenu.show(this, event.getScreenX(), event.getScreenY());
                    }
                }
                event.consume();
            }
        });
        this.setOnMouseEntered(event -> CommitTreeController.handleMouseover(this, true));
        this.setOnMouseExited(event -> CommitTreeController.handleMouseover(this, false));

        this.persistentCellState = CellState.STANDARD;
    }

    // Can be done off FX thread since synchronized, and doesn't make any actual GUI changes. Critical to note
    // that if someone USES the edge in a nefarious way off the FX thread, that's a bad bug. Be very careful about
    // using this list of edges.
    public List<Edge> getEdges() {
        return Collections.unmodifiableList(new ArrayList<>(edges));
    }

    // Can be done off FX thread since synchronized, and doesn't make any actual GUI changes
    public synchronized void addEdge(Edge e) {
        edges.add(e);
    }

    // Can be done off FX thread since synchronized, and doesn't make any actual GUI changes
    public synchronized void removeEdge(Edge e) {
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

            Shape placeHolder = getBaseView();
            placeHolder.setTranslateX(x+TreeLayout.H_PAD);
            placeHolder.setTranslateY(y+BOX_SHIFT);
            placeHolder.setOpacity(0.0);
            CommitTreeModel.getCommitTreeModel().getTreeGraph().getCellLayerPane().getChildren().add(placeHolder);

            TranslateTransition t = new TranslateTransition(Duration.millis(3000), this);
            t.setToX(x);
            t.setToY(y+BOX_SHIFT);
            t.setCycleCount(1);
            t.setOnFinished(event -> {
                numCellsBeingAnimated.getAndDecrement();
                CommitTreeModel.getCommitTreeModel().getTreeGraph().getCellLayerPane().getChildren().remove(placeHolder);
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
    // synchronized for this.localOrRemote
    // Doesn't need to be on FX thread; creates a node, but doesn't have to be in scene graph (yet)
    // TODO: straighten this out
    private synchronized Shape getBaseView(){
        Shape node = DEFAULT_SHAPE.getNewFxShapeObject();
        setFillType(node, CellState.STANDARD);
        node.getStyleClass().setAll("cell");
        node.setId("tree-cell");
        return node;
    }

    /**
     * Set the shape of this cell. The FX shape object needs to retain the styling of the previous one,
     * so this method creates a new FX shape object, then transfers the styling of the old one to it.
     * @param newShape the new shape
     */
    // Doesn't need to be on FX thread; manipulates FX nodes, but doesn't have to be in scene graph (yet)
    // synchronized for this.localOrRemote and this.shape
    public synchronized void setShape(CellShape newShape){

        // If no change in shape, do nothing
        if(this.shape == newShape)
            return;

        Shape newFxShapeObject = newShape.getNewFxShapeObject();

        if(this.fxShapeObject == null){
            this.fxShapeObject = getBaseView();
        }

        // All styling to this shape object (could include labels, etc) should remain, and so they need to be applied
        // to the new object.
        newFxShapeObject.getStyleClass().setAll(this.fxShapeObject.getStyleClass());
        newFxShapeObject.setId(this.fxShapeObject.getId());

        this.shape = newShape;
        this.fxShapeObject = newFxShapeObject;

        // Remove old fx object from pane, and add new one
        getChildren().clear();
        getChildren().add(this.fxShapeObject);

        setFillType(this.fxShapeObject, CellState.STANDARD);

    }

    /**
     * Sets the tooltip to display the given text
     * @param label the text to display
     */
    // Can potentially be done off FX thread since synchronized, and might be used on something off the scene graph.
    // It's the responsibility of the caller to verify that.
    private synchronized void setCommitDescriptor(String label){
        tooltip.setText(label);
    }

    // Can potentially be done off FX thread since synchronized, and might be used on something off the scene graph.
    // It's the responsibility of the caller to verify that.
    // synchronized for "this" reference; need to make sure properties of Cell aren't being accessed simultaneously
    private void setRefLabels(List<RefHelper> refs){
        this.refLabels.setLabels(refs, this);
    }

    private void setCurrentRefLabels(Set<String> refs) {
        Main.assertFxThread();
        this.refLabels.setCurrentLabels(refs);
    }

    // Can potentially be done off FX thread since synchronized, and might be used on something off the scene graph.
    synchronized void setLabels(String commitDescriptor, List<RefHelper> refLabels){
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

    // Can potentially be done off FX thread since synchronized, and might be used on something off the scene graph.
    // synchronized for contextMenu
    synchronized void setContextMenu(ContextMenu contextMenu) {
        this.contextMenu = contextMenu;
    }

    /**
     * Adds a child to this cell
     * @param cell the new child
     */
    // Doesn't need to be on FX thread; this is simply data, not attached to FX viewing
    // synchronized for childrenList
    private synchronized void addCellChild(Cell cell) {
        childrenList.add(cell);
    }

    /**
     * @return the list of the childrenList of this cell
     */
    // Doesn't need to be on FX thread; this is simply data, not attached to FX viewing
    // synchronized for childrenList
    List<Cell> getCellChildren() {
        return Collections.unmodifiableList(new ArrayList<>(childrenList));
    }

    /**
     * @return the list of the parents of this cell
     */
    // Doesn't need to be on FX thread; this is simply data, not attached to FX viewing
    // synchronized for parents
    List<Cell> getCellParents(){
        return parents.toList();
    }

    /**
     * @return whether or not this cell wants to be animated in the next transition
     */
    boolean getAnimate() {
        Main.assertFxThread();
        return this.animate.get();
    }

    /**
     * @return whether or not to use the parent to base the animation off of
     */
    boolean getUseParentAsSource() {
        Main.assertFxThread();
        return this.useParentAsSource.get();
    }

    /**
     * Removes the given cell from the childrenList of this cell
     * @param cell the cell to remove
     */
    // Doesn't need to be on FX thread; this is simply data, not attached to FX viewing
    // synchronized for childrenList
    void removeCellChild(Cell cell) {
        childrenList.remove(cell);
    }

    /**
     * Sets the state of this cell and adjusts the style accordingly
     * @param state the new state of the cell
     */
    void setCellState(CellState state){
        Main.assertFxThread();
        setFillType(fxShapeObject, state);
    }

    /**
     * Sets the cell localOrRemote to local, both or remote and resets edges accordingly
     * @param type the localOrRemote of the cell
     */
    // synchronized for use of edges and this.localOrRemote
    // This can be done off the FX thread in general, as a Cell might be constructed off and used later. However,
    // it is critical that this method not be used on a Cell that is already on the scene graph from off thread.
    synchronized void setCellType(CellType type) {
        this.localOrRemote = type;
        setFillType(fxShapeObject, CellState.STANDARD);
        for (Edge e : edges) {
            e.resetDashed();
        }
    }

    // synchronized for this.localOrRemote
    // This just returns a constant enum variable, safe to use off FX thread
    synchronized CellType getCellType() {
        return this.localOrRemote;
    }

    /**
     * @return the unique ID of this cell
     */
    // cellId is final and an immutable String, so it is safe to return it across threads
    public String getCellId() {
        return cellId;
    }

    /**
     * @return the time of this cell
     */
    long getTime(){
        Main.assertFxThread();
        return time;
    }

    public CellLabelContainer getLabel() {
        Main.assertFxThread();
        return this.refLabels;
    }

    @Override
    public String toString(){
        return cellId;
    }



    /**
     * Sets the fill localOrRemote of a shape based on this cell's localOrRemote
     * @param n the shape to set the fill of
     * @param state the state of the cell, determines coloring
     */
    // synchronized for this.localOrRemote
    // This can be done off the FX thread in general, as a Cell might be constructed off and used later. However,
    // it is critical that this method not be used on a Cell that is already on the scene graph from off thread.
    public synchronized void setFillType(Shape n, CellState state) {
        Color baseColor = Color.web(state.getBackgroundColor());
        switch(this.localOrRemote) {
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


    public void setPersistentCellState(CellState state) {
        persistentCellState = state;
    }

    public CellState getPersistentCellState() {
        return persistentCellState;
    }

    public Shape getFxShapeObject() {
        return fxShapeObject;
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
        // Doesn't need to be on FX thread; this is simply data, not attached to FX viewing
        // synchronized for childrenList
        ParentCell(Cell child, List<Cell> parents) {
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
            return Collections.unmodifiableList(new ArrayList<>(parents));
        }

        /**
         * Sets the given sell to be the child of each non-null parent
         * @param cell the child to add
         */
        // Doesn't need to be on FX thread; this is simply data, not attached to FX viewing
        private void setChild(Cell cell){
            for(Cell parent : parents) {
                if(parent != null) {
                    parent.addCellChild(cell);
                }
            }
        }


    }
}
