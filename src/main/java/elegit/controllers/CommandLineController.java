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
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import net.jcip.annotations.GuardedBy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Created by grenche on 6/7/18.
 */

public class CommandLineController {
    @GuardedBy("this")
    private CommandLineHistoryController commandLineHistoryController;

    @FXML
    private Text currentCommand;
    @FXML
    private Button commandLineMenuButton;
    @FXML
    private ContextMenu commandLineMenu;

    private boolean allowUpdates = true;

    private static final Logger logger = LogManager.getLogger();

    public synchronized void setCommandLineHistoryController() {
        this.commandLineHistoryController = new CommandLineHistoryController();
    }

    public void initialize() {
        setCommandLineHistoryController();
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
        commandLineHistoryController.handleSeeHistoryOption();
    }

    public synchronized void handleExportHistoryOption() {
        commandLineHistoryController.handleExportHistoryOption();
    }

    public synchronized void handleClearLogOption() {
        TranscriptHelper.clear();
        currentCommand.setText("");
    }

    public synchronized void updateCommandText(String command) {
        TranscriptHelper.post(command);
        if (allowUpdates) {
            currentCommand.setText(command);
        }
    }
}
