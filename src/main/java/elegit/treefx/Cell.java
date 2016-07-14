package elegit.treefx;

import de.jensd.fx.glyphs.GlyphsDude;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Background;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Shape;
import javafx.scene.text.Text;
import javafx.util.Duration;
import elegit.CommitTreeController;
import elegit.MatchedScrollPane;
import org.controlsfx.glyphfont.FontAwesome;

import java.util.ArrayList;
import java.util.List;

/**
 * A class that represents a node in a TreeGraph
 */
public class Cell extends Pane{

    // Base shapes for different types of cells
    public static final CellShape DEFAULT_SHAPE = CellShape.SQUARE;
    public static final CellShape UNTRACKED_BRANCH_HEAD_SHAPE = CellShape.CIRCLE;
    public static final CellShape TRACKED_BRANCH_HEAD_SHAPE = CellShape.TRIANGLE_DOWN;

    // The size of the rectangle being drawn
    public static final int BOX_SIZE = 30;

    //The height of the shift for the cells;
    public static final int BOX_SHIFT = 20;

    // Limits on animation so the app doesn't begin to stutter
    private static final int MAX_NUM_CELLS_TO_ANIMATE = 5;
    private static int numCellsBeingAnimated = 0;

    // The displayed view
    Node view;
    private CellShape shape;
    private CellType type;
    // The tooltip shown on hover
    Tooltip tooltip;

    // The unique ID of this cell
    private final String cellId;
    // The assigned time of this commit
    private final long time;
    // The label displayed for this cell
    private String label;

    private ContextMenu contextMenu;

    private LabelCell refLabel;

    private boolean animate;

    private boolean useParentAsSource;

    // The list of children of this cell
    List<Cell> children = new ArrayList<>();

    // The parent object that holds the parents of this cell
    ParentCell parents;

    // All edges that have this cell as an endpoint
    List<Edge> edges = new ArrayList<>();

    // The row and column location of this cell
    public IntegerProperty columnLocationProperty, rowLocationProperty;

    // Whether this cell has been moved to its appropriate location
    public BooleanProperty hasUpdatedPosition;

    public Cell(String s) {
        this.cellId = s;
        this.time = 0;
    }

    /**
     * Constructs a node with the given ID and a single parent node
     * @param cellId the ID of this node
     * @param parent the parent of this node
     */
    public Cell(String cellId, long time, Cell parent, CellType type){
        this(cellId, time, parent, null, type);
    }

    /**
     * Constructs a node with the given ID and the two given parent nodes
     * @param cellId the ID of this node
     * @param parent1 the first parent of this node
     * @param parent2 the second parent of this node
     */
    public Cell(String cellId, long time, Cell parent1, Cell parent2, CellType type){
        this.cellId = cellId;
        this.time = time;
        this.parents = new ParentCell(this, parent1, parent2);
        this.refLabel = new LabelCell();
        this.type = type;

        setShape(DEFAULT_SHAPE);

        this.columnLocationProperty = new SimpleIntegerProperty(-1);
        this.rowLocationProperty = new SimpleIntegerProperty(-1);

        this.hasUpdatedPosition = new SimpleBooleanProperty(false);
        visibleProperty().bind(this.hasUpdatedPosition);

        columnLocationProperty.addListener((observable, oldValue, newValue) -> hasUpdatedPosition.set(oldValue.intValue()==newValue.intValue()));
        rowLocationProperty.addListener((observable, oldValue, newValue) ->
                hasUpdatedPosition.set(oldValue.intValue()==newValue.intValue()));

        tooltip = new Tooltip(cellId);
        tooltip.setWrapText(true);
        tooltip.setMaxWidth(300);
        Tooltip.install(this, tooltip);

        this.setOnMouseClicked(event -> {
            if(event.getButton() == MouseButton.PRIMARY){
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
    }

    /**
     * Moves this cell to the given x and y coordinates
     * @param x the x coordinate to move to
     * @param y the y coordinate to move to
     * @param animate whether to animate the transition from the old position
     * @param emphasize whether to have the Highlighter class emphasize this cell while it moves
     */
    public void moveTo(double x, double y, boolean animate, boolean emphasize){
        if(animate && numCellsBeingAnimated < MAX_NUM_CELLS_TO_ANIMATE){
            numCellsBeingAnimated++;
            MatchedScrollPane.scrollTo(-1);

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
        this.refLabel.translate(x,y);
        this.hasUpdatedPosition.set(true);
    }

    /**
     * @return the basic view for this cell
     */
    protected Node getBaseView(){
        Node node = DEFAULT_SHAPE.get();
        node.setStyle("-fx-fill: " + CellState.STANDARD.getCssStringKey());
        node.getStyleClass().setAll("cell");
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

        newView.setStyle(this.view.getStyle());
        newView.getStyleClass().setAll(this.view.getStyleClass());

        this.view = newView;
        Platform.runLater(() -> {
            getChildren().clear();
            getChildren().add(this.view);
        });
    }

    /**
     * Set the shape of this cell
     * @param newShape the new shape
     */
    public synchronized void setShape(CellShape newShape){
        if(this.shape == newShape) return;
        setView(newShape.get());
        this.shape = newShape;
    }

    /**
     * Sets the tooltip to display the given text
     * @param label the text to display
     */
    public void setDisplayLabel(String label){
        tooltip.setText(label);
        this.label = label;
    }

    public void setRefLabel(List<String> refs){
        this.refLabel.setLabels(refs);
    }

    public void setCurrentRefLabel(List<String> refs) {
        this.refLabel.setCurrentLabels(refs);
    }

    public void setLabels(String displayLabel, List<String> refLabels){
        setDisplayLabel(displayLabel);
        setRefLabel(refLabels);
    }

    public void setCurrentLabels(List<String> refLabels) {
        setCurrentRefLabel(refLabels);
    }

    public void setAnimate(boolean animate) {this.animate = animate;}

    public void setUseParentAsSource(boolean useParentAsSource) {this.useParentAsSource = useParentAsSource;}

    public void setContextMenu(ContextMenu contextMenu){
        this.contextMenu = contextMenu;
    }

    /**
     * Adds a child to this cell
     * @param cell the new child
     */
    public void addCellChild(Cell cell) {
        children.add(cell);
    }

    /**
     * @return the list of the children of this cell
     */
    public List<Cell> getCellChildren() {
        return children;
    }

    /**
     * @return the list of the parents of this cell
     */
    public List<Cell> getCellParents(){
        return parents.toList();
    }

    /**
     * @return whether or not this cell wants to be animated in the next transition
     */
    public boolean getAnimate() { return this.animate; }

    /**
     * @return whether or not to use the parent to base the animation off of
     */
    public boolean getUseParentAsSource() { return this.useParentAsSource; }

    /**
     * Removes the given cell from the children of this cell
     * @param cell the cell to remove
     */
    public void removeCellChild(Cell cell) {
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
    public boolean isChild(Cell cell, int depth){
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
    public void setCellState(CellState state){
        Platform.runLater(() -> view.setStyle("-fx-fill: "+state.getCssStringKey()));
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
    public long getTime(){
        return time;
    }

    public Node getLabel() { return this.refLabel; }

    @Override
    public String toString(){
        return cellId;
    }



    /**
     * Sets the fill type of a shape based on this cell's type
     * @param n the shape to set the fill of
     */
    protected void setFillType(Shape n) {
        Stop[] stops = new Stop[] { new Stop(0, Color.web("#52B3D9")),new Stop(0.499, Color.web("#52B3D9")),new Stop(0.501, Color.web("#F4F4F4")), new Stop(1, Color.web("#F4F4F4"))};
        LinearGradient gradient;
        switch(this.type) {
            case LOCAL:
                gradient = new LinearGradient(0,0,0,3,false, CycleMethod.REFLECT, stops);
                break;
            case REMOTE:
                gradient = new LinearGradient(0,0,3,0,false, CycleMethod.REFLECT, stops);
                break;
            case BOTH:
            default:
                gradient = new LinearGradient(0,0,2,2,false, CycleMethod.REFLECT, stops[0]);
        }
        n.setFill(gradient);
    }

    public enum CellType {
        BOTH,
        LOCAL,
        REMOTE;
    }

    /**
     * A class that holds the parents of a cell
     */
    private class ParentCell{

        private ArrayList<Cell> parents;

        /**
         * Sets the given child to have the given parents
         * @param child the child cell
         * @param mom the first parent
         * @param dad the second parent
         */
        public ParentCell(Cell child, Cell mom, Cell dad){
            parents = new ArrayList<>();
            if(mom != null) parents.add(mom);
            if(dad != null) parents.add(dad);
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
        public ArrayList<Cell> toList(){
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

    private class LabelCell extends Pane {
        private final int MAX_COL_PER_ROW=8, MAX_CHAR_PER_LABEL=25;
        private final String CURRENT_BOX_STYLE = "-fx-background-color: #1E90FF; -fx-background-radius: 5;";
        private final String BOX_STYLE = "-fx-background-color: #CCCCCC; -fx-background-radius: 5;";
        private final String CURRENT_LABEL_STYLE = "-fx-text-fill: #FFFFFF; -fx-font-size: 14px; -fx-font-weight: bold;";
        private final String LABEL_STYLE = "-fx-text-fill: #333333; -fx-font-size: 14px; -fx-font-weight: bold;";


        Pane basic;
        List<Node> basicLabels;
        List<Node> extendedLabels;

        public void translate(double x, double y) {
            setTranslateX(x+BOX_SIZE+10);
            setTranslateY(y+BOX_SIZE-5);
        }

        public void addToolTip(Label l, String text) {
            tooltip = new Tooltip(text);
            tooltip.setWrapText(true);
            tooltip.setMaxWidth(350);
            Tooltip.install(l, tooltip);
        }

        public void setLabels(List<String> labels) {
            if (labels.size() < 1) {
                Platform.runLater(() -> getChildren().clear());
                return;
            }

            basic = new GridPane();
            Button showExtended = new Button();
            basicLabels = new ArrayList<>();
            extendedLabels = new ArrayList<>();

            int col=0;
            int row=0;
            for (String label : labels) {

                // Label text
                Label currentLabel = new Label();
                currentLabel.getStyleClass().remove(0);
                currentLabel.getStyleClass().add("branch-label");
                if (label.length()>MAX_CHAR_PER_LABEL) {
                    addToolTip(currentLabel, label);
                    label = label.substring(0,24)+"...";
                }
                if (col>MAX_COL_PER_ROW) {
                    row++;
                    col=0;
                }
                currentLabel.setText(label);
                currentLabel.setStyle(LABEL_STYLE);

                // Label arrow
                Text pointer = GlyphsDude.createIcon(FontAwesomeIcon.CHEVRON_LEFT);
                pointer.setFill(Color.web("#333333"));

                // Box to contain both items
                HBox box = new HBox(0, pointer);
                box.getChildren().add(currentLabel);
                HBox.setMargin(pointer, new Insets(5,2,0,5));
                HBox.setMargin(currentLabel, new Insets(0,5,0,0));
                GridPane.setColumnIndex(box, col);
                GridPane.setMargin(box, new Insets(0,0,5,5));
                box.setStyle(BOX_STYLE);

                if (row>0) {
                    GridPane.setRowIndex(box, row);
                    box.setVisible(false);
                    extendedLabels.add(box);
                } else {
                    basicLabels.add(box);
                }

                col++;
            }
            basic.getChildren().addAll(basicLabels);
            basic.getChildren().addAll(extendedLabels);
            basic.setVisible(true);

            showExtended.setVisible(false);
            if (row>0) {
                showExtended.setVisible(true);
                showExtended.setTranslateX(-5);
                showExtended.setText("\u22EE");
                showExtended.setStyle("-fx-background-color: rgba(244,244,244,100); -fx-padding: -3 0 0 0;" +
                        "-fx-font-size:18px; -fx-font-weight:bold;");
                showExtended.setOnMouseClicked(event -> {
                    for (Node n : extendedLabels) {
                        n.setVisible(!n.isVisible());
                    }
                });
            }

            this.setMaxHeight(20);

            Platform.runLater(() -> {
                getChildren().clear();
                getChildren().add(basic);
                getChildren().add(showExtended);
            });
        }

        public void setCurrentLabels(List<String> labels) {
            for (Node n : basic.getChildren()) {
                Label l = (Label) ((HBox)n).getChildren().get(1);
                if (labels.contains(l.getText())) {
                    n.setStyle(CURRENT_BOX_STYLE);
                    l.setStyle(CURRENT_LABEL_STYLE);
                    ((Text)((HBox) n).getChildren().get(0)).setFill(Color.web("#FFFFFF"));
                }
            }
        }
    }
}
