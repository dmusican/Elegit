package elegit.treefx;

import de.jensd.fx.glyphs.GlyphsDude;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

/**
 * Class for ref labels
 */
public class CellLabel extends HBox {
    private String name;
    boolean isCurrent, isTag;
    Text pointer;
    ImageView image;
    Label label;
    private final int MAX_CHAR_PER_LABEL=25;

    CellLabel(String name, boolean isCurrent, boolean isTag) {
        this.name = name;
        this.isCurrent = isCurrent;
        this.isTag = isTag;

        Text pointer = getPointer();
        Label label = getLabel();
        ImageView img = getImage();

        // Add children to this label
        this.getChildren().add(pointer);
        this.getChildren().add(label);
        this.getChildren().add(img);

        // Add margins to the children
        HBox.setMargin(pointer, new Insets(5, 2, 0, 5));
        HBox.setMargin(img, new Insets(2, 2, 0, 0));
        HBox.setMargin(label, new Insets(0, 5, 0, 0));

        // Set the style class
        this.getStyleClass().clear();
        this.getStyleClass().add("cell-label-box");
        this.setId(isCurrent ? "current" : isTag ? "tag" : "regular");
    }

    /**
     * @return the pointer with the right color based on the
     */
    private Text getPointer() {
        pointer = GlyphsDude.createIcon(FontAwesomeIcon.CHEVRON_LEFT);
        pointer.setFill(Color.web(isCurrent ? "#FFFFFF" : "333333"));
        return pointer;
    }

    protected Label getLabel() {
        label = new Label();
        label.getStyleClass().clear();
        if (name.length() > MAX_CHAR_PER_LABEL) {
            addToolTip(label, name);
            name = name.substring(0, 24) + "...";
        }
        label.setText(name);
        label.getStyleClass().clear();
        label.getStyleClass().add("cell-label");
        label.setId(isCurrent ? "current" : "regular");

        return label;
    }

    /**
     * @return the imageView with the correct image
     */
    protected ImageView getImage() {
        image = new ImageView(new Image(isTag ? "elegit/images/tag.png" : "elegit/images/branch.png"));
        image.setFitHeight(15);
        image.setPreserveRatio(true);
        return image;
    }

    /**
     * Helper method to add a tool tip to a label
     * @param l the label to add a tooltip to
     * @param text the text of the tooltip
     */
    private void addToolTip(Label l, String text) {
        Tooltip tooltip = new Tooltip(text);
        tooltip.setWrapText(true);
        tooltip.setMaxWidth(350);
        Tooltip.install(l, tooltip);
    }

    void setCurrent(boolean current) {
        this.isCurrent = current;

        label.setId(isCurrent ? "current" : "regular");
        pointer.setFill(Color.web(isCurrent ? "#FFFFFF" : "333333"));
        this.setId(isCurrent ? "current" : isTag ? "tag" : "regular");
    }
}
