// REFACTORED BY DAVE: RETHREADING
package elegit.treefx;

import de.jensd.fx.glyphs.GlyphsDude;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import elegit.RefHelper;
import javafx.application.Platform;
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

    private Text commitIndicator;
    private Label label;
    private ImageView image;

    private ContextMenu contextMenu;
    public static final int MAX_CHAR_PER_LABEL=25;

    CellLabel(RefHelper refHelper, boolean isCurrent, boolean isTag) {
        assert Platform.isFxApplicationThread();
        this.refHelper = refHelper;
        this.name = refHelper.getAbbrevName();
        this.isCurrent = isCurrent;
        this.isTag = isTag;
        this.isRemote = false;

        // Set up the actual contents of the label
        setCommitIndicator();
        setLabelName();
        setImage();

        // Add label contents to JavaFX model for this label
        this.getChildren().add(commitIndicator);
        this.getChildren().add(label);
        this.getChildren().add(image);

        // Add margins to the label contents
        HBox.setMargin(commitIndicator, new Insets(5, 2, 0, 5));
        HBox.setMargin(image, new Insets(2, 2, 0, 0));
        HBox.setMargin(label, new Insets(0, 5, 0, 0));

        // Set the style class
        this.getStyleClass().clear();
        this.getStyleClass().add("cell-label-box");
        this.setId(isCurrent ? "current" : isTag ? "tag" : "regular");
    }

    /**
     * assign commitIndicator with right color
     */
    private void setCommitIndicator() {
        assert Platform.isFxApplicationThread();
        commitIndicator = GlyphsDude.createIcon(FontAwesomeIcon.CHEVRON_LEFT);
        commitIndicator.setFill(Color.web(isCurrent ? "#FFFFFF" : "333333"));
    }

    protected void setLabelName() {
        assert Platform.isFxApplicationThread();
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
    }

    protected Label getLabelName() {
        return label;
    }

    private void setImage() {
        assert Platform.isFxApplicationThread();
        image = new ImageView(new Image(isTag ? "elegit/images/tag.png" : isCurrent ? "elegit/images/branch_white.png" : "elegit/images/branch.png"));
        image.setFitHeight(15);
        image.setPreserveRatio(true);
    }

    /**
     * Sets the context menu and clicking
     * @param menu the menu for this label
     */
    public void setContextMenu(ContextMenu menu) {
        assert Platform.isFxApplicationThread();
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
        assert Platform.isFxApplicationThread();
        this.isRemote = true;
        refreshIcon();
    }

    /**
     * Helper method to add a tool tip to a label
     * @param l the label to add a tooltip to
     * @param text the text of the tooltip
     */
    private void addToolTip(Label l, String text) {
        assert Platform.isFxApplicationThread();
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
        assert Platform.isFxApplicationThread();
        this.isCurrent = current;

        this.getChildren().get(1).setId(isCurrent ? "current" : "regular");
        ((ImageView) this.getChildren().get(2)).setImage(new Image(isCurrent ? "elegit/images/branch_white.png" : "elegit/images/branch.png"));
        commitIndicator.setFill(Color.web(isCurrent ? "#FFFFFF" : "333333"));
        this.setId(isCurrent ? "current" : isTag ? "tag" : "regular");
    }

    /**
     * Sets the various items to have the tag style
     * @param tag whether or not this label is a tag
     */
    void setTag(boolean tag) {
        assert Platform.isFxApplicationThread();
        this.isTag = tag;

        ((ImageView) this.getChildren().get(2)).setImage(new Image(isTag ? "elegit/images/tag.png" : "elegit/images/branch.png"));
        this.setId(isCurrent ? "current" : isTag ? "tag" : "regular");
    }

    String getName() {
        return this.name;
    }

    /**
     * Refreshes the icon based on various boolean values
     */
    void refreshIcon() {
        assert Platform.isFxApplicationThread();
        String image = "elegit/images/";
        if (isTag) {
            image += "tag.png";
        } else if (isCurrent) {
            if (isRemote)
                image += "remote_white.png";
            else
                image += "remote.png";
        } else {
            image += "branch.png";
        }
        ((ImageView) this.getChildren().get(2)).setImage(new Image(image));
    }

    RefHelper getRefHelper() {
        return this.refHelper;
    }
}
