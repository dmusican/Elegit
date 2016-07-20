package elegit;

import javafx.scene.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Model for notifications. Contains a list of past notifications.
 */
public class NotificationModel {

    public List<Text> notificationList;

    /**
     * Default constructor
     */
    public NotificationModel() {
        this.notificationList = new ArrayList<>();
    }

    public void addNotification(Text notification) {
        // Items are added to the beginning
        notificationList.add(0,notification);
    }

    public void removeNotification(Text notification) {
        notificationList.remove(notification);
    }
}
