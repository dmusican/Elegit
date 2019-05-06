package elegit.controllers;

import de.jensd.fx.glyphs.GlyphsDude;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import javafx.fxml.FXML;
import javafx.geometry.Side;
import javafx.scene.control.*;
import javafx.scene.text.Text;
import net.jcip.annotations.GuardedBy;

/**
 * Commit info subview of the main view controller
 */
public class CommitInfoController {

    @FXML private TextArea commitInfoMessageText;
    @FXML private MenuItem commitInfoNameCopyButton;
    @FXML private MenuItem commitInfoGoToButton;
    @FXML private Button commitInfoButton;
    @FXML private ContextMenu commitInfoContextMenu;

    @GuardedBy("this")
    private SessionController sessionController;

    public void initialize() {
        commitInfoButton.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);

        Text clipboardIcon = GlyphsDude.createIcon(FontAwesomeIcon.CLIPBOARD);
        this.commitInfoNameCopyButton.setGraphic(clipboardIcon);

        Text goToIcon = GlyphsDude.createIcon(FontAwesomeIcon.ARROW_CIRCLE_LEFT);
        this.commitInfoGoToButton.setGraphic(goToIcon);

        Text barsIcon = GlyphsDude.createIcon(FontAwesomeIcon.BARS);
        this.commitInfoButton.setGraphic(barsIcon);
    }

    /**
     * Called when the commitInfoButton gets pushed, shows a menu of options
     */
    public void handleCommitInfoButton() {
        commitInfoContextMenu.show(this.commitInfoButton, Side.BOTTOM, -150, 0);
    }

    void setCommitInfoMessageText(String text) {
        commitInfoMessageText.setVisible(true);
        commitInfoNameCopyButton.setVisible(true);
        commitInfoGoToButton.setVisible(true);
        commitInfoButton.setVisible(true);
        commitInfoMessageText.setText(text);
    }

    public synchronized void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public synchronized void handleGoToCommitButton() {
        sessionController.handleGoToCommitButton();
    }

    public synchronized void handleCommitNameCopyButton() {
        sessionController.handleCommitNameCopyButton();
    }

    void clearCommit() {
        commitInfoMessageText.setText("");
        commitInfoMessageText.setVisible(false);
        commitInfoNameCopyButton.setVisible(false);
        commitInfoGoToButton.setVisible(false);
        commitInfoButton.setVisible(false);
    }
}
