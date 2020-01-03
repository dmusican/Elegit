package elegit.controllers;

import de.jensd.fx.glyphs.GlyphsDude;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import elegit.Main;
import javafx.application.Platform;
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
import javafx.util.Duration;
import net.jcip.annotations.GuardedBy;
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

    @GuardedBy("this") private PopOver notificationAlert;
    private TimerTask hideBubbleTask;
    private Stage anchor;

    private static final Logger logger = LogManager.getLogger(SessionController.class);
    private static final Logger console = LogManager.getLogger("briefconsolelogger");

    /**
     * Initializes the environment and sets up event handlers. Called
     * automatically by JavaFX
     */
    public synchronized void initialize() {
        this.notificationListPane.setVisible(false);
        this.notificationListPane.setMouseTransparent(true);

        this.latestNotification.setOnMouseClicked(this::handleNotificationPane);
        this.resizeLine.setOnMouseDragged(this::handleLineDragged);
        this.minimizeButton.setOnMouseClicked(event -> hideNotificationList());

        this.notificationListPane.widthProperty().addListener((observable, oldValue, newValue) ->
                                                                  this.resizeLine.setEndX(100d));

        this.separatorLine.endXProperty().bind(this.resizeLine.endXProperty());

        this.notificationListUI.setPickOnBounds(false);
        this.notificationPane.setPickOnBounds(false);

        this.notificationNum.setPickOnBounds(false);

        this.notificationAlert = new PopOver();


    }

    public synchronized void setAnchor(Stage stage) {
        this.anchor = stage;
    }

    /**
     * Updates the notification alert
     * @param notification new alert
     */
    private synchronized void updateNotificationBubble(String notification) {
        Main.assertFxThread();
        Text notificationText = new Text(notification);
        notificationText.setWrappingWidth(230);
        notificationText.setStyle("-fx-font-weight: bold");

        HBox hBox = new HBox(notificationText);
        hBox.setPadding(new Insets(0, 5, 0, 5));
        hBox.setOnMouseClicked(event -> showNotificationList());

        notificationAlert.setContentNode(hBox);
        notificationAlert.setTitle("New Notification");
        notificationAlert.setArrowLocation(PopOver.ArrowLocation.BOTTOM_RIGHT);
        notificationAlert.setAnchorLocation(PopupWindow.AnchorLocation.CONTENT_BOTTOM_RIGHT);
    }

    /**
     * Handler method for the notification pane
     * @param e the mouse event to handle
     */
    private synchronized void handleNotificationPane(MouseEvent e) {
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
    private synchronized void handleLineDragged(MouseEvent e) {
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
    synchronized void toggleNotificationList() {
        Main.assertFxThread();
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
    private synchronized void hideNotificationList() {
        if (isListPaneVisible()) toggleNotificationList();
    }

    /**
     * Helper method to show the extended notification list if isn't showing
     */
    private synchronized void showNotificationList() {
        if (!isListPaneVisible()) {
            toggleNotificationList();
            hideBubble();
        }
    }

    /**
     * @return true if the extended notification list is showing
     */
    synchronized boolean isListPaneVisible() {
        return this.notificationListPane.isVisible();
    }

    /**
     * Adds a notification to the list of notifications
     * @param notification the notification string to add
     */
    synchronized void addNotification(String notification) {
        Label line = makeNotificationLabel(notification);
        setLatestNotification(makeLatestNotificationLabel(line));
        showBubble(notification);

        VBox vBox = (VBox) this.notificationList.getContent();
        vBox.getChildren().add(0,line);

        setNotificationNum();
        console.info("Notification being added: " + notification);
    }

    /**
     * Adds a notification with an action
     * @param notification the message to show on the notification
     * @param actionText the text to show on the action button
     * @param handler the handler for clicking the button
     */
    synchronized void addNotification(String notification, String actionText, EventHandler<MouseEvent> handler) {
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

    private synchronized Label makeNotificationLabel(String notification) {
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

    private synchronized Label makeLatestNotificationLabel(Label notification) {
        Label line = new Label(notification.getText());
        line.setWrapText(true);
        line.setId("notification");
        return line;
    }

    private synchronized Button makeLatestNotificationButton(Button action) {
        Button latestActionButton = new Button(action.getText());
        latestActionButton.setId("notification");
        latestActionButton.setOnMouseClicked(action.getOnMouseClicked());
        return latestActionButton;
    }

    /**
     * Binds the max height of the notification pane to a little below the height of the parent anchor pane
     * @param parentHeight the height property of the parent pane
     */
    synchronized void bindParentBounds(ReadOnlyDoubleProperty parentHeight) {
        this.notificationPane.maxHeightProperty().bind(parentHeight.divide(1.1));
    }

    /**
     * Helper method that clears all notifications
     */
    @FXML
    synchronized void clearAllNotifications() {
        VBox vBox = (VBox) this.notificationList.getContent();

        vBox.getChildren().clear();
        setLatestNotification(null);

        setNotificationNum();
    }

    /**
     * Removes a given notification
     * @param notification the notification label (with or without an action) to remove
     */
    private synchronized void removeNotification(Region notification) {
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
    private synchronized void setLatestNotification(Label notificationLabel) {
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
    private synchronized void setLatestNotification(Label notificationLabel, Button notificationButton) {
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
    private synchronized void setNotificationNum() {
        int num = ((VBox)this.notificationList.getContent()).getChildren().size();
        if (num>0) this.notificationNum.setText(num+"");
        else this.notificationNum.setText("");
    }

    public synchronized int getNotificationNum() {
        int num = ((VBox)this.notificationList.getContent()).getChildren().size();
        return num;
    }

    /**
     * Alerts the user that there's a new notification by popping up a little bubble in the lower right corner
     *
     * @param notification to put in window
     */
    private synchronized void showBubble(String notification) {
        Main.assertFxThread();
        if(!Main.isAppClosed.get() && !isListPaneVisible()) {
            updateNotificationBubble(notification);
            notificationAlert.show(this.anchor, anchor.getX() + anchor.getWidth() - 15, anchor.getY() + anchor.getHeight() - 15);
            notificationAlert.detach();
            //notificationAlert.setAutoHide(true);
            hideBubbleTask();
        }
    }

    /**
     * Hides the notification alert 4 seconds after the most recent alert
     */
    private synchronized void hideBubbleTask(){
        if(hideBubbleTask != null) hideBubbleTask.cancel();
        Timer timer = new Timer(true);
        hideBubbleTask = new TimerTask() {
            @Override
            public void run() {
                // Hiding the bubble from the timer thread might be possible, but do it from the FX thread to avoid
                // race conditions
                Platform.runLater(() -> hideBubble());
            }
        };
        timer.schedule(hideBubbleTask, 4000);
    }

    /**
     * Hides bubble instantly, as well as stops any task that may be trying to hide it later
     */
    private synchronized void hideBubble() {
        if (hideBubbleTask != null) hideBubbleTask.cancel();
        if(notificationAlert != null) notificationAlert.hide();
    }

    /**
     * Hides bubble instantly, as well as stops any task that may be trying to hide it later
     */
    public synchronized void hideBubbleInstantly() {
        if (hideBubbleTask != null) hideBubbleTask.cancel();
        if(notificationAlert != null) notificationAlert.hide(Duration.ZERO);
    }

}
