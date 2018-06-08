package elegit.controllers;

import de.jensd.fx.glyphs.GlyphsDude;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import elegit.models.TranscriptHelper;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import net.jcip.annotations.GuardedBy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

/**
 * Created by grenche on 6/7/18.
 */

public class CommandLineController {
    @GuardedBy("this")
    private SessionController sessionController;

    @FXML
    private Text currentCommand;
    @FXML
    private Button commandLineMenuButton;
    @FXML
    private ContextMenu commandLineMenu;

    private boolean allowUpdates = true;

    private static final Logger logger = LogManager.getLogger();

    public synchronized void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public void initialize() {
        commandLineMenuButton.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
        Text barsIcon = GlyphsDude.createIcon(FontAwesomeIcon.BARS);
        this.commandLineMenuButton.setGraphic(barsIcon);
        this.commandLineMenuButton.setTooltip(new Tooltip("Command line tool menu"));
    }

    /**
     * Called when the commandLineMenuButton gets pushed, shows a menu of relevant options
     */
    public synchronized void handleCommandLineMenuButton() {
        commandLineMenu.show(commandLineMenuButton, Side.BOTTOM, -162, 3);
    }

    public synchronized void handleDisableOption() {
        currentCommand.setText("Disabled");
        allowUpdates = false;
    }

    //Currently doesn't update with actual history
    public synchronized void handleSeeHistoryOption() {
        showHistory();
    }

    public synchronized void handleExportHistoryOption() {

    }

    public synchronized void handleClearLogOption() {
        TranscriptHelper.clear();
        currentCommand.setText("");
    }

    //Currently can't work because Model can't send things right now.
    public synchronized void updateCommandText(String command) {
        TranscriptHelper.post(command);
        if (allowUpdates) {
            currentCommand.setText(command);
        }
    }

    /**
     * Opens up a terminal like window and displays history
     */
    private void showHistory() {
        try {
            logger.info("See history clicked");
            // Create and display the Stage:
            ScrollPane fxmlRoot = FXMLLoader.load(getClass().getResource("/elegit/fxml/CommandLineHistory.fxml"));

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
