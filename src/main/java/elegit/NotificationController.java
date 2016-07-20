package elegit;

import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Controller class for notifications in a given window
 */
public class NotificationController {

    private final static int MIN_SCROLLPANE_HEIGHT = 37;
    private final static int DEFAULT_SCROLLPANE_HEIGHT = 100;

    // FXML elements
    StackPane notificationPane;
    AnchorPane latestNotification;
    StackPane notificationListPane;
    ScrollPane notificationList;

    private NotificationModel notificationModel;

    static final Logger logger = LogManager.getLogger(SessionController.class);

    /**
     * Constructor method, gives the controller access to its various panes
     * @param notificationPane the pane for all notification elements
     * @param latestNotification the bar at the bottom for the latest notification
     * @param notificationListPane the pane with all elements of the expanded view of notifications
     * @param notificationList the expanded pane to scroll through all notifications
     */
    public NotificationController(StackPane notificationPane, AnchorPane latestNotification, StackPane notificationListPane, ScrollPane notificationList) {
        this.notificationPane=notificationPane;
        this.latestNotification=latestNotification;
        this.notificationListPane=notificationListPane;
        this.notificationList=notificationList;
    }

    /**
     * Initializes the environment
     */
    public void initialize() {
        this.notificationModel = new NotificationModel();

        this.notificationListPane.setVisible(false);
        this.notificationListPane.setMouseTransparent(true);
    }

    /**
     * Handler method for the notification pane
     * @param e the mouse event to handle
     */
    public void handleNotificationPane(MouseEvent e) {
        if (e.getTarget().toString().contains("Line")) {
            switch (e.getEventType().toString()) {
                case "MOUSE_DRAGGED":
                    if (notificationList.getHeight()<MIN_SCROLLPANE_HEIGHT || e.getSceneY()>(notificationList.getScene().getHeight()-MIN_SCROLLPANE_HEIGHT)) {
                        notificationList.setPrefHeight(MIN_SCROLLPANE_HEIGHT);
                        hideNotificationList();
                        break;
                    } else if (!notificationListPane.isVisible() && e.getSceneY()<(notificationList.getScene().getHeight()-MIN_SCROLLPANE_HEIGHT)) {
                        toggleNotificationList();
                    }
                    notificationList.setPrefHeight(notificationList.getHeight()-e.getY());
                    break;
                default:
                    break;
            }
        }
        else if (e.getTarget().toString().contains("ImageView") || e.getTarget().toString().contains("Minimize")) {
            toggleNotificationList();
        }
    }

    public void toggleNotificationList() {
        this.notificationListPane.setVisible(!this.notificationListPane.isVisible());
        this.notificationListPane.setMouseTransparent(!this.notificationListPane.isMouseTransparent());
        this.latestNotification.setVisible(!this.latestNotification.isVisible());
    }

    public void hideNotificationList() {
        this.notificationListPane.setVisible(false);
        this.notificationListPane.setMouseTransparent(false);
        this.latestNotification.setVisible(true);
    }
}
