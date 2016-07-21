package elegit;

import de.jensd.fx.glyphs.GlyphsDude;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Line;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Controller class for notifications in a given window
 */
public class NotificationController {

    private final static int MIN_SCROLLPANE_HEIGHT = 20;
    private final static int BUTTON_WIDTH = 85;

    // FXML elements
    @FXML StackPane notificationPane;
    @FXML AnchorPane latestNotification;
    @FXML StackPane notificationListPane;
    @FXML ScrollPane notificationList;
    @FXML AnchorPane notificationListUI;

    @FXML Line resizeLine;
    @FXML Line separatorLine;
    @FXML Button minimizeButton;
    @FXML Label notificationNum;
    @FXML Label latestNotificationLabel;

    static final Logger logger = LogManager.getLogger(SessionController.class);

    /**
     * Initializes the environment and sets up event handlers. Called
     * automatically by JavaFX
     */
    public void initialize() {
        this.notificationListPane.setVisible(false);
        this.notificationListPane.setMouseTransparent(true);

        this.latestNotification.setOnMouseClicked(this::handleNotificationPane);
        this.resizeLine.setOnMouseDragged(this::handleLineDragged);
        this.minimizeButton.setOnMouseClicked(event -> hideNotificationList());

        this.minimizeButton.boundsInParentProperty().addListener((observable, oldValue, newValue) ->
            this.resizeLine.setEndX(newValue.getMinX()+BUTTON_WIDTH));
        //this.resizeLine.endXProperty().bind(this.minimizeButton.boundsInParentProperty().add(BUTTON_WIDTH));
        this.separatorLine.endXProperty().bind(this.resizeLine.endXProperty());

        this.notificationListUI.setPickOnBounds(false);
        this.notificationPane.setPickOnBounds(false);
    }

    /**
     * Handler method for the notification pane
     * @param e the mouse event to handle
     */
    private void handleNotificationPane(MouseEvent e) {
        if (e.getTarget().toString().contains("ImageView")) {
            toggleNotificationList();
        } else if (e.getClickCount()>1) {
            toggleNotificationList();
        }
    }

    /**
     * Handler method for resizing the extended notification window
     * @param e mouse event for dragging
     */
    private void handleLineDragged(MouseEvent e) {
        if (notificationList.getHeight()<MIN_SCROLLPANE_HEIGHT || e.getSceneY()>(notificationList.getScene().getHeight()-MIN_SCROLLPANE_HEIGHT)) {
            notificationList.setPrefHeight(MIN_SCROLLPANE_HEIGHT);
            hideNotificationList();
            return;
        } else if (e.getSceneY()<(notificationList.getScene().getHeight()-MIN_SCROLLPANE_HEIGHT)) {
            showNotificationList();
        }
        notificationList.setPrefHeight(notificationList.getHeight()-e.getY());
    }

    /**
     * Toggles between the basic view of one notification to the extended list view
     */
    void toggleNotificationList() {
        this.notificationListPane.setVisible(!this.notificationListPane.isVisible());
        this.notificationListPane.setMouseTransparent(!this.notificationListPane.isMouseTransparent());
        this.latestNotification.setVisible(!this.latestNotification.isVisible());
    }

    /**
     * Helper method to hide the extended notification list if it is showing
     */
    private void hideNotificationList() {
        if (isListPaneVisible()) toggleNotificationList();
    }

    /**
     * Helper method to show the extended notification list if isn't showing
     */
    private void showNotificationList() {
        if (!isListPaneVisible()) toggleNotificationList();
    }

    /**
     * @return true if the extended notification list is showing
     */
    boolean isListPaneVisible() {
        return this.notificationListPane.isVisible();
    }

    /**
     * Adds a notification to the list of notifications
     * @param notification the notification string to add
     */
    void addNotification(String notification) {
        Label line = new Label(notification);
        line.setWrapText(true);
        line.setId("notification");
        line.setGraphic(GlyphsDude.createIcon(FontAwesomeIcon.TIMES_CIRCLE));
        line.setOnMouseClicked(event -> {
            if (event.getTarget().equals(line.getGraphic()))
                removeNotification(line);
        });

        setLatestNotificationText(notification);

        VBox vBox = (VBox) this.notificationList.getContent();
        vBox.getChildren().add(0,line);

        setNotificationNum();
    }

    /**
     * Removes a given notification
     * @param notification the notification label to remove
     */
    private void removeNotification(Label notification) {
        VBox vBox = (VBox) this.notificationList.getContent();

        // Reset the latest notification text if needed
        if (vBox.getChildren().indexOf(notification)==0) {
            if (vBox.getChildren().size() > 1)
                setLatestNotificationText(((Label) vBox.getChildren().get(1)).getText());
            else
                setLatestNotificationText("");
        }

        vBox.getChildren().remove(notification);

        setNotificationNum();
    }

    /**
     * Helper method that clears all notifications
     */
    @FXML
    private void clearAllNotifications() {
        VBox vBox = (VBox) this.notificationList.getContent();

        vBox.getChildren().clear();
        setLatestNotificationText("");

        setNotificationNum();
    }

    /**
     * Helper method to set the latest notification text
     * @param notificationText the string to set the latest notification text to
     */
    private void setLatestNotificationText(String notificationText) {
        latestNotificationLabel.setText(notificationText);
    }

    /**
     * Helper method to set the number on the notifications
     */
    private void setNotificationNum() {
        int num = ((VBox)this.notificationList.getContent()).getChildren().size();
        if (num>0) this.notificationNum.setText(num+"");
        else this.notificationNum.setText("");
    }
}
