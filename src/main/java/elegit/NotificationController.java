package elegit;

import de.jensd.fx.glyphs.GlyphsDude;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;
import javafx.stage.PopupWindow;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.controlsfx.control.PopOver;

import java.util.Timer;
import java.util.TimerTask;

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

    private PopOver notificationAlert;
    private TimerTask hideBubbleTask;
    private Stage anchor;

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
        this.separatorLine.endXProperty().bind(this.resizeLine.endXProperty());

        this.notificationListUI.setPickOnBounds(false);
        this.notificationPane.setPickOnBounds(false);

        this.notificationNum.setPickOnBounds(false);

        notificationList.setMaxWidth(1200);
        notificationListPane.setMaxWidth(1200);
    }

    public void setAnchor(Stage stage) {
        this.anchor = stage;
    }

    /**
     * Updates the notification alert
     * @param notification new alert
     * @return new PopOver
     */
    private PopOver updateNotificationBubble(String notification) {
        Text notifcationText = new Text(notification);
        notifcationText.setWrappingWidth(230);
        notifcationText.setStyle("-fx-font-weight: bold");

        HBox hBox = new HBox(notifcationText);
        hBox.setPadding(new Insets(0, 5, 0, 5));
        hBox.setOnMouseClicked(event -> showNotificationList());

        PopOver popOver = new PopOver(hBox);
        popOver.setTitle("New Notification");
        popOver.setArrowLocation(PopOver.ArrowLocation.BOTTOM_RIGHT);
        popOver.setAnchorLocation(PopupWindow.AnchorLocation.CONTENT_BOTTOM_RIGHT);

        return popOver;
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
        if(notificationListPane.isVisible()) {
            hideBubble();
        }
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
        if (!isListPaneVisible()) {
            toggleNotificationList();
            hideBubble();
        }
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
        showBubble(notification);

        VBox vBox = (VBox) this.notificationList.getContent();
        vBox.getChildren().add(0,line);

        setNotificationNum();
    }

    /**
     * Binds the max height of the notification pane to a little below the height of the parent anchor pane
     * @param parentHeight the height property of the parent pane
     */
    void bindParentBounds(ReadOnlyDoubleProperty parentHeight) {
        this.notificationPane.maxHeightProperty().bind(parentHeight.divide(1.1));
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

    /**
     * Alerts the user that there's a new notification by popping up a little bubble in the lower right corner
     *
     * @param notification to put in window
     */
    private void showBubble(String notification) {
        if(!isListPaneVisible()) {
            notificationAlert = updateNotificationBubble(notification);
            notificationAlert.show(this.anchor, anchor.getX() + anchor.getWidth() - 15, anchor.getY() + anchor.getHeight() - 15);
            notificationAlert.detach();
            notificationAlert.setAutoHide(true);

            hideBubbleTask();
        }
    }

    /**
     * Hides the notification alert 4 seconds after the most recent alert
     */
    private void hideBubbleTask(){
        if(hideBubbleTask != null) hideBubbleTask.cancel();
        Timer timer = new Timer(true);
        hideBubbleTask = new TimerTask() {
            @Override
            public void run() {
                hideBubble();
            }
        };
        timer.schedule(hideBubbleTask, 4000);
    }

    /**
     * Helper method that hides the notification Alert
     */
    private void hideBubble() {
        if(notificationAlert != null) notificationAlert.hide();
    }
}
