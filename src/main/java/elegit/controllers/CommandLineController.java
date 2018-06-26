package elegit.controllers;

import de.jensd.fx.glyphs.GlyphsDude;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import elegit.models.TranscriptHelper;
import javafx.fxml.FXML;
import javafx.geometry.Side;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.text.Text;
import net.jcip.annotations.GuardedBy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by grenche on 6/7/18.
 * Controls the GUI portion of the terminal command tool and send the information to the model to be used in other ways
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
    @FXML
    private ContextMenu commandRightClickMenu;

    private boolean allowUpdates = true;

    // Used for testing that the updateCommandText method has run before checking the text in currentCommand
    public static final AtomicBoolean methodCalled = new AtomicBoolean(false);

    private static final Logger logger = LogManager.getLogger();

    public synchronized void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public void initialize() {
        commandLineMenuButton.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
        Text barsIcon = GlyphsDude.createIcon(FontAwesomeIcon.BARS);
        commandLineMenuButton.setGraphic(barsIcon);
        commandLineMenuButton.setTooltip(new Tooltip("Command line tool menu"));
        currentCommand.setEditable(false);
        resetScrollPane();
        commandBar.setVvalue(0.125);
    }

    /**
     * Updates the TextArea with the git command that would have just occurred.
     */
    public synchronized void updateCommandText(String command) {
        // Sends it to be added to the log file in case the user wants to see/export the full history
        TranscriptHelper.post(command);
        if (allowUpdates) {
            currentCommand.setText(command);
            setTextAreaWidth();
        }
        // The text in currentCommand can now be tested
        methodCalled.set(true);
    }

    /*
     * If a command is too long to fit in the visible ScrollPane, allow the text area to get as long as it needs to
     * and make it so the text appears slightly higher so it is not covered by the scroll bar.
     */
    private void setTextAreaWidth() {
        // Numbers are pretty arbitrary, but seem to adjust relatively well to any give text.
        int length = (currentCommand.getText().length() + 1) * 12;
        if (length > 244) { // If it needs to scroll
            adjustScrollPane(length);
        } else {
            resetScrollPane();
        }
    }

    // Resizes the TextArea to be long enough to hold the command and prevents vertical scrolling.
    private void adjustScrollPane(int length) {
        currentCommand.setPrefWidth(length);
        commandBar.setVmax(0.6);
        commandBar.setVmin(0.1);
    }

    // Makes it so the scroll bar does not appear when text is short and that text appears in the middle.
    private synchronized void resetScrollPane() {
        currentCommand.setPrefWidth(244);
        commandBar.setVmax(0.5);
        commandBar.setVmin(0);
    }

    public synchronized void handleCommandLineMenuButton() {
        // Allows the menu to stay within the window (opens bottom-left of button) when clicked.
        commandLineMenu.show(commandLineMenuButton, Side.BOTTOM, -162, 3);
    }

    /**
     * Copies the entire command to the clipboard
     */
    public synchronized void handleCopyCommandOption() {
        if (allowUpdates) { // Only copy the command if the terminal window is updating with commands
            logger.info("Command copied");
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent command = new ClipboardContent();
            command.putString(currentCommand.getText());
            clipboard.setContent(command);
        } else {
            sessionController.showNoCommandToCopyNotification();
        }
    }

    public synchronized void handleSeeHistoryOption() { sessionController.handleSeeHistoryOption(); }

    public synchronized void handleExportHistoryOption() {
        sessionController.handleExportHistoryOption();
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

    public synchronized void handleClearLogOption() {
        TranscriptHelper.clear();
        if (allowUpdates) {
            currentCommand.setText("");
            resetScrollPane();
        }
    }

    public synchronized void handleRightClickMenu() {
        // Show the copy option near where the user clicked.
        commandRightClickMenu.show(currentCommand.getClip(), Side.BOTTOM, 0, 0);
    }

    /**
     * Copies only the highlighted portion to the clipboard
     */
    public synchronized void handleCopyOption() {
        logger.info("Portion of command copied");
        currentCommand.copy();
    }

    public static boolean getMethodCalled() {
        return methodCalled.get();
    }

    public static void setMethodCalled(boolean value) {
        methodCalled.set(value);
    }
}
