package elegit;

import de.jensd.fx.glyphs.GlyphsDude;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
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
    StackPane notificationPane;
    AnchorPane latestNotification;
    StackPane notificationListPane;
    ScrollPane notificationList;
    AnchorPane notificationListUI;

    Line resizeLine;
    Line separatorLine;
    Button clearAllButton;
    Button minimizeButton;
    Label notificationNum;

    static final Logger logger = LogManager.getLogger(SessionController.class);

    /**
     * Constructor method, gives the controller access to its various panes
     * @param notificationPane: the pane containing all notifications
     */
    public NotificationController(StackPane notificationPane) {
        this.notificationPane=notificationPane;
        this.latestNotification=(AnchorPane) notificationPane.getChildren().get(0);
        this.notificationListPane=(StackPane) notificationPane.getChildren().get(1);
        this.notificationList=(ScrollPane) this.notificationListPane.getChildren().get(0);
        this.notificationListUI=(AnchorPane)this.notificationListPane.getChildren().get(1);

        this.resizeLine = (Line) this.notificationListUI.getChildren().get(0);
        this.separatorLine = (Line) this.latestNotification.getChildren().get(0);
        this.clearAllButton = (Button) this.notificationListUI.getChildren().get(1);
        this.minimizeButton = (Button) this.notificationListUI.getChildren().get(2);
        this.notificationNum = (Label) this.latestNotification.getChildren().get(3);

        initialize();
    }

    /**
     * Initializes the environment and sets up event handlers
     */
    public void initialize() {
        this.notificationListPane.setVisible(false);
        this.notificationListPane.setMouseTransparent(true);

        this.latestNotification.setOnMouseClicked(event -> handleNotificationPane(event));
        this.resizeLine.setOnMouseDragged(event -> handleLineDragged(event));
        this.minimizeButton.setOnMouseClicked(event -> hideNotificationList());
        this.clearAllButton.setOnMouseClicked(event -> clearAllNotifications());

        this.resizeLine.endXProperty().bind(this.minimizeButton.layoutXProperty().add(BUTTON_WIDTH));
        this.separatorLine.endXProperty().bind(this.resizeLine.endXProperty());

        this.notificationListUI.setPickOnBounds(false);
        this.notificationPane.setPickOnBounds(false);
    }

    /**
     * Handler method for the notification pane
     * @param e the mouse event to handle
     */
    public void handleNotificationPane(MouseEvent e) {
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
    public void handleLineDragged(MouseEvent e) {
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
    public void toggleNotificationList() {
        this.notificationListPane.setVisible(!this.notificationListPane.isVisible());
        this.notificationListPane.setMouseTransparent(!this.notificationListPane.isMouseTransparent());
        this.latestNotification.setVisible(!this.latestNotification.isVisible());
    }

    /**
     * Helper method to hide the extended notification list if it is showing
     */
    public void hideNotificationList() {
        if (isListPaneVisible()) toggleNotificationList();
    }

    /**
     * Helper method to show the extended notification list if isn't showing
     */
    public void showNotificationList() {
        if (!isListPaneVisible()) toggleNotificationList();
    }

    /**
     * @return true if the extended notification list is showing
     */
    public boolean isListPaneVisible() {
        return this.notificationListPane.isVisible();
    }

    /**
     * Adds a notification to the list of notifications
     * @param notification the notification string to add
     */
    public void addNotification(String notification) {
        Label line = new Label(notification);
        line.setId("notification");
        line.setGraphic(GlyphsDude.createIcon(FontAwesomeIcon.TIMES_CIRCLE));
        line.setOnMouseClicked(event -> {
            System.out.println(event);
            if (event.getTarget().equals(line.getGraphic()))
                removeNotification(line);
        });

        ((Label) latestNotification.getChildren().get(1)).setText(notification);

        VBox vBox = (VBox) this.notificationList.getContent();
        vBox.getChildren().add(0,line);

        setNotificationNum();
    }

    /**
     * Removes a given notification
     * @param notification the notification label to remove
     */
    public void removeNotification(Label notification) {
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
        ((Label) latestNotification.getChildren().get(1)).setText(notificationText);
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
