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
 * Label class for the ref labels that go next to cells
 */
public class CellLabel extends Pane {
    private final int MAX_COL_PER_ROW=6, MAX_CHAR_PER_LABEL=25;


    Pane basic;
    List<Node> basicLabels;
    List<Node> extendedLabels;

    void translate(double x, double y) {
        setTranslateX(x+BOX_SIZE+10);
        setTranslateY(y+BOX_SIZE);
    }

    void addToolTip(Label l, String text) {
        Tooltip tooltip = new Tooltip(text);
        tooltip.setWrapText(true);
        tooltip.setMaxWidth(350);
        Tooltip.install(l, tooltip);
    }

    void setLabels(List<String> labels, Cell cell) {
        Platform.runLater(() -> getChildren().clear());
        if (labels.size() < 1) {
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
            currentLabel.getStyleClass().clear();
            currentLabel.getStyleClass().add("cell-label");
            currentLabel.setId("regular");

            ImageView img = new ImageView(new Image("elegit/images/branch.png"));
            img.setFitHeight(15);
            img.setPreserveRatio(true);

            // Label arrow
            Text pointer = GlyphsDude.createIcon(FontAwesomeIcon.CHEVRON_LEFT);
            pointer.setFill(Color.web("#333333"));

            // Box to contain both items
            HBox box = new HBox(0, pointer);
            box.getChildren().add(currentLabel);
            box.getChildren().add(img);
            HBox.setMargin(pointer, new Insets(5,2,0,5));
            HBox.setMargin(currentLabel, new Insets(0,5,0,0));
            HBox.setMargin(img, new Insets(2,0,0,0));
            GridPane.setColumnIndex(box, col);
            GridPane.setMargin(box, new Insets(0,0,5,5));

            box.getStyleClass().clear();
            box.getStyleClass().add("cell-label-box");
            box.setId("regular");

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

        this.setMaxHeight(20);
        this.setRotationAxis(Rotate.X_AXIS);
        this.setRotate(180);

        this.visibleProperty().bind(cell.visibleProperty());

        Platform.runLater(() -> {
            getChildren().clear();
            getChildren().add(basic);
            getChildren().add(showExtended);
        });
    }

    void setCurrentLabels(List<String> labels) {
        for (Node n : basic.getChildren()) {
            Label l = (Label) ((HBox)n).getChildren().get(1);
            if (labels.contains(l.getText())) {
                n.getStyleClass().clear();
                n.getStyleClass().add("cell-label-box");
                n.setId("current");

                l.getStyleClass().clear();
                l.getStyleClass().add("cell-label");
                l.setId("current");
                ((Text)((HBox) n).getChildren().get(0)).setFill(Color.web("#FFFFFF"));
            }
        }
    }
}