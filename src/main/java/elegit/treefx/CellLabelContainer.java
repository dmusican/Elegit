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

import static elegit.treefx.Cell.BOX_SIZE;

/**
 * Class for the container that shows all the labels for a given cell
 */
public class CellLabelContainer extends GridPane {
    private final int MAX_COL_PER_ROW=4;

    HBox basicLabels;
    List<HBox> extendedLabels;

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
        setTranslateX(x+BOX_SIZE+10);
        setTranslateY(y+BOX_SIZE-5-(this.getHeight()-25));
    }

    /**
     * Helper method to add tooltips to labels that are too long
     * @param l the label to add a tooltip for
     * @param text the text of the tooltip
     */
    void addToolTip(Label l, String text) {
        assert Platform.isFxApplicationThread();
        Tooltip tooltip = new Tooltip(text);
        tooltip.setWrapText(true);
        tooltip.setMaxWidth(350);
        Tooltip.install(l, tooltip);
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

        Label showExtended = new Label();
        basicLabels = new HBox(5);
        extendedLabels = new ArrayList<>();

        int col=0;
        int row=0;

        GridPane.setMargin(basicLabels, new Insets(0,0,5,5));
        basicLabels.setPickOnBounds(false);
        for (RefHelper helper : refHelpers) {
            if (col>MAX_COL_PER_ROW) {
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

        showExtended.setVisible(false);
        if (row>0) {
            showExtended.setVisible(true);
            showExtended.setTranslateX(-6);
            showExtended.setTranslateY(-3);
            Node down = GlyphsDude.createIcon(FontAwesomeIcon.CARET_DOWN);
            Node up = GlyphsDude.createIcon(FontAwesomeIcon.CARET_UP);
            showExtended.setGraphic(down);
            showExtended.setOnMouseClicked(event -> {
                if(showExtended.getGraphic().equals(down)) {
                    showExtended.setGraphic(up);
                }else {
                    showExtended.setGraphic(down);
                }
                for (Node n : extendedLabels) {
                    n.setVisible(!n.isVisible());
                }
            });
        }

        // We rotate the labels because it's more efficient than having our tree
        // upside down and moving everything around often.
        this.setMaxHeight(20);
        this.setRotationAxis(Rotate.X_AXIS);
        this.setRotate(180);
        this.visibleProperty().bind(cell.visibleProperty());

        getChildren().clear();
        getChildren().addAll(basicLabels);
        getChildren().addAll(extendedLabels);
        getChildren().add(showExtended);
        this.setPickOnBounds(false);
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
                    if (n instanceof CellLabel)
                        if (labels.contains(((CellLabel) n).setLabelName().getText()))
                            ((CellLabel) n).setCurrent(true);
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
                    if (n instanceof CellLabel)
                        if (labels.contains(((CellLabel) n).setLabelName().getText())) {
                            ((CellLabel) n).setRemote(true);
                        }
                }
            }
        }
    }
}