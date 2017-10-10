package elegit.controllers;

import elegit.SessionModel;
import javafx.fxml.FXML;
import javafx.scene.text.Text;
import org.apache.http.annotation.GuardedBy;
import org.apache.http.annotation.ThreadSafe;

/**
 *
 * A controller for the Help Page view that provides information about
 * what all of the
 *
 */
@ThreadSafe
public class AboutController {
    @FXML
    @GuardedBy("this")
    private Text version;

    public synchronized void setVersion(String version) {
        this.version.setText(this.version.getText()+" "+version);
    }
}