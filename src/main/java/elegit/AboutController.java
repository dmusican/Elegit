package elegit;

import javafx.fxml.FXML;
import javafx.scene.text.Text;

/**
 *
 * A controller for the Help Page view that provides information about
 * what all of the
 *
 */
public class AboutController {
    @FXML
    private Text version;

    private SessionModel sessionModel;

    public void initialize() throws Exception {
        this.sessionModel = SessionModel.getSessionModel();
    }

    public void setVersion(String version) {
        this.version.setText(this.version.getText()+" "+version);
    }
}