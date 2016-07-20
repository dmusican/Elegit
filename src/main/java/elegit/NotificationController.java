package elegit;

import javafx.fxml.FXML;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Controller class for notifications in a given window
 */
public class NotificationController {

    // FXML elements
    StackPane notificationPane;
    AnchorPane latestNotification;
    AnchorPane notificationList;

    private NotificationModel notificationModel;

    static final Logger logger = LogManager.getLogger(SessionController.class);

    /**
     * Constructor method, gives the controller access to its various panes
     * @param notificationPane the pane for all notification elements
     * @param latestNotification the bar at the bottom for the latest notification
     * @param notificationList the expanded pane to scroll through all notifications
     */
    public NotificationController(StackPane notificationPane, AnchorPane latestNotification, AnchorPane notificationList) {
        this.notificationPane=notificationPane;
        this.latestNotification=latestNotification;
        this.notificationList=notificationList;
    }

    /**
     * Initializes the environment
     */
    public void initialize() {
        this.notificationModel = new NotificationModel();

        this.notificationList.setVisible(false);
        this.notificationList.setMouseTransparent(true);
    }

    /**
     * Handler method for the notification pane
     * @param e the mouse event to handle
     */
    public void handleNotificationPane(MouseEvent e) {
        if (e.getTarget().toString().contains("Rectangle")) {
            switch (e.getEventType().toString()) {
                case "MOUSE_DRAGGED":
                    System.out.println(e.getY()+" "+notificationList.isResizable());
                    notificationList.resize(notificationList.getWidth(), notificationList.getHeight()-e.getY());
                    System.out.println(notificationList.getHeight());
                    break;
                default:
                    break;
            }
        }
        else if (e.getTarget().toString().contains("ImageView") || e.getTarget().toString().contains("ScrollPane")) {
            toggleNotificationList();
        }
    }

    public void toggleNotificationList() {
        this.notificationList.setVisible(!this.notificationList.isVisible());
        this.notificationList.setMouseTransparent(!this.notificationList.isMouseTransparent());
        this.latestNotification.setVisible(!this.latestNotification.isVisible());
    }
}
