package elegit;

import javafx.fxml.FXML;
import org.controlsfx.control.NotificationPane;

/**
 *
 * A controller for the Help Page view that provides information about
 * what all of the
 *
 */
public class LegendController {
    @FXML
    private NotificationPane notificationPane;

    private SessionModel sessionModel;
    private LegendModel legendModel;

    public void initialize() throws Exception {
        this.sessionModel = SessionModel.getSessionModel();
    }

    /// BEGIN: ERROR NOTIFICATIONS:

    private void showGenericErrorNotification() {
        this.notificationPane.setText("Sorry, there was an error.");

        this.notificationPane.getActions().clear();
        this.notificationPane.show();
    }

    /// END: ERROR NOTIFICATIONS ^^^
}
