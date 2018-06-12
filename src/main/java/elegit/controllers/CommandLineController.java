package elegit.controllers;

import de.jensd.fx.glyphs.GlyphsDude;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import elegit.models.TranscriptHelper;
import javafx.fxml.FXML;
import javafx.geometry.Side;
import javafx.scene.control.*;
import javafx.scene.text.Text;
import net.jcip.annotations.GuardedBy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by grenche on 6/7/18.
 */

public class CommandLineController {
    @GuardedBy("this")
    private SessionController sessionController;

    @FXML
    private TextArea currentCommand;
    @FXML
    private Button commandLineMenuButton;
    @FXML
    private ContextMenu commandLineMenu;
    @FXML
    private MenuItem disableOption;
    @FXML
    private ScrollPane commandBar;

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
        currentCommand.setEditable(false);
        resetScrollPane();
        commandBar.setFitToHeight(true);
    }

    /**
     * Called when the commandLineMenuButton gets pushed, shows a menu of relevant options
     */
    public synchronized void handleCommandLineMenuButton() {
        // Allows the menu to stay within the window (opens bottom-left of button) when clicked.
        commandLineMenu.show(commandLineMenuButton, Side.BOTTOM, -162, 3);
    }

    public synchronized void handleDisableOption() {
        if (allowUpdates) { // Entered when the user wants to disable the tool and allows them to reenable it.
            allowUpdates = false;
            currentCommand.setText("Disabled");
            disableOption.setText("Enable terminal commands");
            resetScrollPane();
        } else { // Entered initially and when the user wants to enable the tool (allowing them to disable next).
            allowUpdates = true;
            currentCommand.setText("");
            disableOption.setText("Disable terminal commands");
            resetScrollPane();
        }
    }

    //Currently doesn't update with actual history
    public synchronized void handleSeeHistoryOption() {
        sessionController.handleSeeHistoryOption();
    }

    public synchronized void handleExportHistoryOption() {
        sessionController.handleExportHistoryOption();
    }

    public synchronized void handleClearLogOption() {
        TranscriptHelper.clear();
        currentCommand.setText("");
        resetScrollPane();
    }

    public synchronized void updateCommandText(String command) {
        TranscriptHelper.post(command);
        if (allowUpdates) {
            currentCommand.setText(command);
            setTextAreaWidth();
        }
    }

    /*
     * If a command is too long to fit in the visible ScrollPane, allow the text area to get as long as it needs to
     * and make it so the text appears slightly higher so it is not covered by the scroll bar.
     */
    private void setTextAreaWidth() {
        // Numbers are pretty arbitrary, but seem to adjust relatively well to any give text.
        int length = (currentCommand.getText().length() + 1) * 12;
        System.out.println(length);
        if (length > 244) {
            currentCommand.setPrefWidth(length);
            commandBar.setVvalue(0.335);
        } else {
            resetScrollPane();
        }
    }

    // Makes it so the scroll bar does not appear when text is short and that text appears in the middle.
    private synchronized void resetScrollPane() {
        currentCommand.setPrefWidth(244);
        commandBar.setVvalue(0.125);
    }
}
