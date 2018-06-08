package elegit.controllers;

import de.jensd.fx.glyphs.GlyphsDude;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import elegit.models.TranscriptHelper;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import net.jcip.annotations.GuardedBy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public class CommandLineHistoryController {

    @GuardedBy("this")
    private SessionController sessionController;

    @FXML
    private Text commandHistory;

    private static final Logger logger = LogManager.getLogger();

    public synchronized void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    //Currently doesn't update with actual history
    public synchronized void handleSeeHistoryOption() {
        commandHistory.setText("");
        showHistory();
    }

    public synchronized void handleExportHistoryOption() {

    }

    /**
     * Opens up a terminal like window and displays history
     */
    private void showHistory() {
        try {
            logger.info("See history clicked");
            // Create and display the Stage:
            ScrollPane fxmlRoot = FXMLLoader.load(getClass().getResource("/elegit/fxml/pop-ups/CommandLineHistory.fxml"));

            Stage stage = new Stage();
            stage.setTitle("Recent Elegit actions as commands");
            stage.setScene(new Scene(fxmlRoot));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setOnCloseRequest(event -> logger.info("Closed history"));
            stage.show();
        } catch (IOException e) {
            sessionController.showGenericErrorNotification(e);
            e.printStackTrace();
        }
    }
}
