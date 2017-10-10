package elegit.controllers;

import de.jensd.fx.glyphs.GlyphsDude;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import javafx.fxml.FXML;
import javafx.scene.control.Control;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.text.Text;

/**
 * Commit info subview of the main view controller
 */
public class CommitInfoController {

    @FXML private SessionController sessionController;
    @FXML private TextArea commitInfoMessageText;
    @FXML private Button commitInfoNameCopyButton;
    @FXML private Button commitInfoGoToButton;

    public void initialize() {
        commitInfoNameCopyButton.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
        commitInfoGoToButton.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);

        Text clipboardIcon = GlyphsDude.createIcon(FontAwesomeIcon.CLIPBOARD);
        this.commitInfoNameCopyButton.setGraphic(clipboardIcon);

        Text goToIcon = GlyphsDude.createIcon(FontAwesomeIcon.ARROW_CIRCLE_LEFT);
        this.commitInfoGoToButton.setGraphic(goToIcon);

        this.commitInfoGoToButton.setTooltip(new Tooltip(
                "Go to selected commit"
        ));

        this.commitInfoNameCopyButton.setTooltip(new Tooltip(
                "Copy commit ID"
        ));
    }

    void setCommitInfoMessageText(String text) {
        commitInfoMessageText.setVisible(true);
        commitInfoNameCopyButton.setVisible(true);
        commitInfoGoToButton.setVisible(true);
        commitInfoMessageText.setText(text);
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public void handleGoToCommitButton() {
        sessionController.handleGoToCommitButton();
    }

    public void handleCommitNameCopyButton() {
        sessionController.handleCommitNameCopyButton();
    }

    void clearCommit() {
        commitInfoMessageText.setText("");
        commitInfoMessageText.setVisible(false);
        commitInfoNameCopyButton.setVisible(false);
        commitInfoGoToButton.setVisible(false);
    }
}
