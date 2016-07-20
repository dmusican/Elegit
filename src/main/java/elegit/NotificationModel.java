package elegit;

import javafx.scene.control.Label;

import java.util.ArrayList;
import java.util.List;

/**
 * Model for notifications. Contains a list of past notifications.
 */
public class NotificationModel {

    public List<Label> notificationList;

    /**
     * Default constructor
     */
    public NotificationModel() {
        this.notificationList = new ArrayList<>();
    }

    public void addNotification(Label notification) {
        // Items are added to the beginning
        notificationList.add(0,notification);
    }

    public void removeNotification(Label notification) {
        notificationList.remove(notification);
    }
}
