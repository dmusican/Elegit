package elegit.treefx;

import de.jensd.fx.glyphs.GlyphsDude;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import elegit.RefHelper;
import javafx.geometry.Insets;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

/**
 * Class for ref labels
 */
public class CellLabel extends HBox {
    private String name;
    private RefHelper refHelper;
    private boolean isCurrent, isTag, isRemote;
    private Text pointer;
    private ImageView image;
    private Label label;
    private ContextMenu contextMenu;
    public static final int MAX_CHAR_PER_LABEL=25;

    private static Image tagImage = new Image("elegit/images/tag.png");
    private static Image currentImage = new Image("elegit/images/branch_white.png");
    private static Image remoteImage = new Image("elegit/images/remote.png");
    private static Image remoteWhiteImage = new Image("elegit/images/remote_white.png");
    private static Image otherImage = new Image("elegit/images/branch.png");

    CellLabel(RefHelper refHelper, boolean isCurrent, boolean isTag) {
        this.refHelper = refHelper;
        this.name = refHelper.getAbbrevName();
        this.isCurrent = isCurrent;
        this.isTag = isTag;
        this.isRemote = false;

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
        //image = new ImageView(new Image(isTag ? "elegit/images/tag.png" : isCurrent ? "elegit/images/branch_white.png" : "elegit/images/branch.png"));
        image = new ImageView(isTag ? tagImage : isCurrent ? currentImage : otherImage);
        image.setFitHeight(15);
        image.setPreserveRatio(true);
        return image;
    }

    /**
     * Sets the context menu and clicking
     * @param menu the menu for this label
     */
    void setContextMenu(ContextMenu menu) {
        this.contextMenu = menu;
        this.setPickOnBounds(true);

        this.setOnMouseClicked(event -> {
            contextMenu.show(this, event.getScreenX(), event.getScreenY());
            if(event.getButton() == MouseButton.PRIMARY){
                // TODO: do something here?
            }else if(event.getButton() == MouseButton.SECONDARY){
                if(contextMenu != null){
                    contextMenu.show(this, event.getScreenX(), event.getScreenY());
                }
            }
            event.consume();
        });
    }

    /**
     * Sets the cell label to be a remote branch type
     * @param isRemote whether or not the ref label is remote
     */
    void setRemote(boolean isRemote) {
        this.isRemote = true;
        refreshIcon();
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

    /**
     * Sets the various items to have the current style
     * @param current whether or not this label is current
     */
    void setCurrent(boolean current) {
        this.isCurrent = current;

        this.getChildren().get(1).setId(isCurrent ? "current" : "regular");
//        ((ImageView) this.getChildren().get(2)).setImage(new Image(isCurrent ? "elegit/images/branch_white.png" : "elegit/images/branch.png"));
        ((ImageView) this.getChildren().get(2)).setImage(isCurrent ? currentImage : otherImage);
        pointer.setFill(Color.web(isCurrent ? "#FFFFFF" : "333333"));
        this.setId(isCurrent ? "current" : isTag ? "tag" : "regular");
    }

    /**
     * Sets the various items to have the tag style
     * @param tag whether or not this label is a tag
     */
    void setTag(boolean tag) {
        this.isTag = tag;

        //((ImageView) this.getChildren().get(2)).setImage(new Image(isTag ? "elegit/images/tag.png" : "elegit/images/branch.png"));
        ((ImageView) this.getChildren().get(2)).setImage(isTag ? tagImage : otherImage);
        this.setId(isCurrent ? "current" : isTag ? "tag" : "regular");
    }

    String getName() {
        return this.name;
    }

    private void refreshIcon() {
        Image image;
        if (isTag) {
            image = tagImage;
        } else if (isCurrent) {
            if (isRemote)
                image = remoteWhiteImage;
            else
                image = remoteImage;
        } else {
            image = otherImage;
        }
        ((ImageView) this.getChildren().get(2)).setImage(image);
    }

    RefHelper getRefHelper() {
        return this.refHelper;
    }
}
