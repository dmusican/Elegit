package elegit.treefx;

import de.jensd.fx.glyphs.GlyphsDude;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import elegit.RefHelper;
import elegit.TagHelper;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.transform.Rotate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Class for the container that shows all the labels for a given cell
 */
public class CellLabelContainer extends GridPane {
    private final int MAX_COL_PER_ROW=4;


    /**
     * Default constructor. Doesn't do anything, but it's nice to have
     * for blank cell creation.
     */
    public CellLabelContainer() { }

    /**
     * Translates the container to a given location
     * @param x the x coordinate of the new location
     * @param y the y coordinate of the new location
     */
    public void translate(double x, double y) {
        assert Platform.isFxApplicationThread();
        //setTranslateX(x+Cell.BOX_SIZE+10);
        //setTranslateY(y+Cell.BOX_SIZE-5-(this.getHeight()-25));
        setTranslateX(x);
        setTranslateY(y);
    }

    /**
     * Creates cell labels and structures them accordingly
     *
     * @param refHelpers the labels to create and place in this container
     * @param cell the cell these labels are associated with
     */
    // THREAD
    void setLabels(List<RefHelper> refHelpers, Cell cell) {
        assert Platform.isFxApplicationThread();
        getChildren().clear();
        if (refHelpers.size() < 1) {
            return;
        }

        HBox firstRowLabels = new HBox(5);
        List<HBox> extendedLabels = new ArrayList<>();

        int rowCount = addLabelsToContainerInRows(refHelpers, firstRowLabels, extendedLabels);
        Label extendedDropArrow = setupExtendedDropArrow(extendedLabels, rowCount);

        // We rotate the labels because it's more efficient than having our tree
        // upside down and moving everything around often.
        this.setMaxHeight(20);
        this.setRotationAxis(Rotate.X_AXIS);
        this.setRotate(180);
        this.visibleProperty().bind(cell.visibleProperty());

        getChildren().addAll(firstRowLabels);
        getChildren().addAll(extendedLabels);
        getChildren().add(extendedDropArrow);
        this.setPickOnBounds(false);
    }

    private int addLabelsToContainerInRows(List<RefHelper> refHelpers, HBox basicLabels, List<HBox> extendedLabels) {
        int col=0;
        int row=0;

        GridPane.setMargin(basicLabels, new Insets(0,0,5,5));
        basicLabels.setPickOnBounds(false);
        for (RefHelper helper : refHelpers) {
            if (col>=MAX_COL_PER_ROW) {
                row++;
                col=0;
                HBox newLine = new HBox(5);
                GridPane.setMargin(newLine, new Insets(0,0,5,5));
                GridPane.setRowIndex(newLine, row);
                newLine.setVisible(false);
                newLine.setPickOnBounds(false);
                extendedLabels.add(newLine);
            }
            CellLabel label;
            if (helper instanceof TagHelper) {
                label = new TagCellLabel(helper, false);
            }
            else
                label = new BranchCellLabel(helper, false);

            if (row>0) {
                extendedLabels.get(row-1).getChildren().add(label);
            } else {
                basicLabels.getChildren().add(label);
            }
            col++;
        }
        int rowCount = row + 1;
        return rowCount;
    }

    private Label setupExtendedDropArrow(List<HBox> extendedLabels, int rowCount) {
        Label extendedDropArrow = new Label();
        extendedDropArrow.setVisible(false);
        if (rowCount>1) {
            extendedDropArrow.setVisible(true);
            extendedDropArrow.setTranslateX(-6);
            extendedDropArrow.setTranslateY(-3);
            Node down = GlyphsDude.createIcon(FontAwesomeIcon.CARET_DOWN);
            Node up = GlyphsDude.createIcon(FontAwesomeIcon.CARET_UP);
            extendedDropArrow.setGraphic(down);
            extendedDropArrow.setOnMouseClicked(event -> {
                if(extendedDropArrow.getGraphic().equals(down)) {
                    extendedDropArrow.setGraphic(up);
                }else {
                    extendedDropArrow.setGraphic(down);
                }
                for (Node n : extendedLabels) {
                    n.setVisible(!n.isVisible());
                }
            });
        }
        return extendedDropArrow;
    }

    /**
     * Helper method to set the current cell labels
     * @param labels the labels that refer to the current refs
     */
    // THREAD
    void setCurrentLabels(List<String> labels) {
        assert Platform.isFxApplicationThread();
        for (Node m : getChildren()) {
            if (m instanceof HBox) {
                for (Node n : ((HBox) m).getChildren()) {
                    if (n instanceof CellLabel) {
                        if (labels.contains(((CellLabel) n).getLabelName().getText()))
                            ((CellLabel) n).setCurrent(true);
                    }
                }
            }
        }
    }

    /**
     * Helper method to set the context menus on the ref labels
     * @param menuMap a map between ref helpers and context menus
     */
    // THREAD
    void setLabelMenus(Map<RefHelper, ContextMenu> menuMap) {
        assert Platform.isFxApplicationThread();
        for (Node m : getChildren()) {
            if (m instanceof HBox) {
                for (Node n : ((HBox) m).getChildren()) {
                    if (n instanceof CellLabel && menuMap.keySet().contains(((CellLabel) n).getRefHelper())) {
                        ((CellLabel) n).setContextMenu(menuMap.get(((CellLabel) n).getRefHelper()));
                    }
                }
            }
        }
    }

    /**
     * Helper method to set the remote branch cell icons
     * @param labels the labels to set as remote
     */
    void setRemoteLabels(List<String>  labels) {
        assert Platform.isFxApplicationThread();
        for (Node m : getChildren()) {
            if (m instanceof HBox) {
                for (Node n : ((HBox) m).getChildren()) {
                    if (n instanceof CellLabel) {
                        if (labels.contains(((CellLabel) n).getLabelName().getText()))
                            ((CellLabel) n).setRemote(true);
                    }
                }
            }
        }
    }
}