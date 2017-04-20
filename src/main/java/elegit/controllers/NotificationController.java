package elegit.controllers;

import de.jensd.fx.glyphs.GlyphsDude;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import elegit.controllers.SessionController;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.event.EventHandler;
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
    @FXML HBox latestNotificationBox;

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
        Label line = makeNotificationLabel(notification);
        setLatestNotification(makeLatestNotificationLabel(line));
        showBubble(notification);

        VBox vBox = (VBox) this.notificationList.getContent();
        vBox.getChildren().add(0,line);

        setNotificationNum();
    }

    /**
     * Adds a notification with an action
     * @param notification the message to show on the notification
     * @param actionText the text to show on the action button
     * @param handler the handler for clicking the button
     */
    void addNotification(String notification, String actionText, EventHandler handler) {
        // HBox to hold the label of the notification and the action button
        HBox box = new HBox();

        Label line = makeNotificationLabel(notification);
        showBubble(notification);

        Button actionButton = new Button(actionText);
        actionButton.setId("notification");
        actionButton.setOnMouseClicked(handler);

        setLatestNotification(makeLatestNotificationLabel(line), makeLatestNotificationButton(actionButton));

        // Make the x remove the whole hbox
        line.setOnMouseClicked(event -> {
            if (event.getTarget().equals(line.getGraphic()))
                removeNotification(box);
        });

        box.setSpacing(5);
        box.getChildren().add(line);
        box.getChildren().add(actionButton);

        VBox vBox = (VBox) this.notificationList.getContent();
        vBox.getChildren().add(0,box);

        setNotificationNum();
    }

    private Label makeNotificationLabel(String notification) {
        Label line = new Label(notification);
        line.setWrapText(true);
        line.setId("notification");
        line.setGraphic(GlyphsDude.createIcon(FontAwesomeIcon.TIMES_CIRCLE));
        line.setOnMouseClicked(event -> {
            if (event.getTarget().equals(line.getGraphic()))
                removeNotification(line);
        });
        return line;
    }

    private Label makeLatestNotificationLabel(Label notification) {
        Label line = new Label(notification.getText());
        line.setWrapText(true);
        line.setId("notification");
        return line;
    }

    private Button makeLatestNotificationButton(Button action) {
        Button latestActionButton = new Button(action.getText());
        latestActionButton.setId("notification");
        latestActionButton.setOnMouseClicked(action.getOnMouseClicked());
        return latestActionButton;
    }

    /**
     * Binds the max height of the notification pane to a little below the height of the parent anchor pane
     * @param parentHeight the height property of the parent pane
     */
    void bindParentBounds(ReadOnlyDoubleProperty parentHeight) {
        this.notificationPane.maxHeightProperty().bind(parentHeight.divide(1.1));
    }

    /**
     * Helper method that clears all notifications
     */
    @FXML
    void clearAllNotifications() {
        VBox vBox = (VBox) this.notificationList.getContent();

        vBox.getChildren().clear();
        setLatestNotification(null);

        setNotificationNum();
    }

    /**
     * Removes a given notification
     * @param notification the notification label (with or without an action) to remove
     */
    private void removeNotification(Region notification) {
        VBox vBox = (VBox) this.notificationList.getContent();

        // Reset the latest notification text if needed
        if (vBox.getChildren().indexOf(notification)==0) {
            if (vBox.getChildren().size() > 1) {
                if (vBox.getChildren().get(1) instanceof HBox) {
                    HBox box = (HBox) vBox.getChildren().get(1);
                    setLatestNotification(makeLatestNotificationLabel(((Label) box.getChildren().get(0))),
                            makeLatestNotificationButton((Button) box.getChildren().get(1)));
                } else
                    setLatestNotification(makeLatestNotificationLabel((Label) vBox.getChildren().get(1)));
            }
            else
                setLatestNotification(null);
        }

        vBox.getChildren().remove(notification);

        setNotificationNum();
    }

    /**
     * Sets the latest notification
     * @param notificationLabel the label to add
     */
    private void setLatestNotification(Label notificationLabel) {
        if (notificationLabel != null) {
            latestNotificationBox.getChildren().setAll(notificationLabel);
        } else {
            latestNotificationBox.getChildren().clear();
        }
    }

    /**
     * Sets the latest notification with an action
     * @param notificationLabel the label for the notification
     * @param notificationButton the button with an action
     */
    private void setLatestNotification(Label notificationLabel, Button notificationButton) {
        try {
            latestNotificationBox.getChildren().setAll(notificationLabel, notificationButton);
        } catch (Exception e) {
            e.printStackTrace();
        }
        //latestNotificationBox.getChildren().setAll(notificationLabel, notificationButton);
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
