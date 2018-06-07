package elegit.controllers;

import de.jensd.fx.glyphs.GlyphsDude;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
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
    private ScrollPane commands;
    @FXML
    private Button commandLineMenuButton;
    @FXML
    private Button removeRecentReposButton;
    @FXML
    private MenuItem disableOption;
    @FXML
    private MenuItem seeHistoryOption;
    @FXML
    private MenuItem exportHistoryOption;
    @FXML
    private MenuItem existingOption;
    @FXML
    private ContextMenu commandLineMenu;

    private static final Logger logger = LogManager.getLogger();
    private static final Logger console = LogManager.getLogger("briefconsolelogger");

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
    public synchronized void handleCommandLineButton() {
        commandLineMenu.show(commandLineMenuButton, Side.BOTTOM ,0, 0);
    }

    public synchronized void handleDisableOption() {

    }

    public synchronized void handleSeeHistoryOption() {

    }

    public synchronized void handleExportHistoryOption() {

    }

    public synchronized void handleClearLogOption() {

    }
}
