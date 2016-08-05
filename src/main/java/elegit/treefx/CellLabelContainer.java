package elegit.treefx;

import de.jensd.fx.glyphs.GlyphsDude;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.transform.Rotate;

import java.util.ArrayList;
import java.util.List;

import static elegit.treefx.Cell.BOX_SIZE;

/**
 * Class for the container that shows all the labels for a given cell
 */
public class CellLabelContainer extends GridPane {
    private final int MAX_COL_PER_ROW=6;

    List<HBox> basicLabels;
    List<HBox> extendedLabels;

    /**
     * Default constructor. Doesn't do anything, but it's nice to have
     * for blank cell creation.
     */
    public CellLabelContainer() { }

    /**
     * Constructor, adds labels to the container
     * @param labels the labels to add
     * @param cell the cell these labels are associated with
     */
    public CellLabelContainer(List<String> labels, Cell cell) {
        setLabels(labels, cell);
    }

    /**
     * Translates the container to a given location
     * @param x the x coordinate of the new location
     * @param y the y coordinate of the new location
     */
    public void translate(double x, double y) {
        setTranslateX(x+BOX_SIZE+10);
        setTranslateY(y+BOX_SIZE-5);
    }

    /**
     * Helper method to add tooltips to labels that are too long
     * @param l the label to add a tooltip for
     * @param text the text of the tooltip
     */
    void addToolTip(Label l, String text) {
        Tooltip tooltip = new Tooltip(text);
        tooltip.setWrapText(true);
        tooltip.setMaxWidth(350);
        Tooltip.install(l, tooltip);
    }

    /**
     * Creates cell labels and structures them accordingly
     *
     * @param labels the labels to create and place in this container
     * @param cell the cell these labels are associated with
     */
    void setLabels(List<String> labels, Cell cell) {
        Platform.runLater(() -> getChildren().clear());
        if (labels.size() < 1) {
            return;
        }

        Button showExtended = new Button();
        basicLabels = new ArrayList<>();
        extendedLabels = new ArrayList<>();

        int col=0;
        int row=0;

        for (String name : labels) {
            if (col>MAX_COL_PER_ROW) {
                row++;
                col=0;
            }
            CellLabel label = new CellLabel(name, false, false);

            GridPane.setColumnIndex(label, col);
            GridPane.setMargin(label, new Insets(0,0,5,5));

            if (row>0) {
                GridPane.setRowIndex(label, row);
                label.setVisible(false);
                extendedLabels.add(label);
            } else {
                basicLabels.add(label);
            }
            col++;
        }

        showExtended.setVisible(false);
        if (row>0) {
            showExtended.setVisible(true);
            showExtended.setTranslateX(-6);
            showExtended.setText("\u02c5");
            showExtended.setStyle("-fx-background-color: rgba(244,244,244,100); -fx-padding: -3 0 0 0;" +
                    "-fx-font-size:28px; -fx-font-weight:bold;");
            showExtended.setOnMouseClicked(event -> {
                if(showExtended.getText().equals("\u02c5")) {
                    showExtended.setText("\u02c4");
                }else {
                    showExtended.setText("\u02c5");
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

        Platform.runLater(() -> {
            getChildren().addAll(basicLabels);
            getChildren().addAll(extendedLabels);
            getChildren().add(showExtended);
        });
    }

    /**
     * Helper method to set the current cell labels
     * @param labels the labels that refer to the current refs
     */
    void setCurrentLabels(List<String> labels) {
        Platform.runLater(() -> {
            for (Node n : getChildren())
                if (n instanceof CellLabel && labels.contains(((CellLabel) n).getLabel().getText()))
                    ((CellLabel) n).setCurrent(true);
        });
    }

    /**
     * Helper method to set the tag cell labels
     * @param labels the labels that refer to tags
     */
    void setTagLabels(List<String> labels) {
        Platform.runLater(() -> {
            for (Node n : getChildren()) {
                if (n instanceof CellLabel && labels.contains(((CellLabel) n).getLabel().getText()))
                    ((CellLabel) n).setTag(true);
            }
        });
    }
}