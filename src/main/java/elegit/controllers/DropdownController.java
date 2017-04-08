package elegit.controllers;

import de.jensd.fx.glyphs.GlyphsDude;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import elegit.RepoHelper;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.text.Text;

/**
 * Created by dmusicant on 4/8/17.
 */
public class DropdownController {

    private SessionController sessionController;
    public ComboBox<RepoHelper> repoDropdownSelector;

    @FXML public Button openRepoDirButton;
    @FXML public Button loadNewRepoButton;
    @FXML public Button removeRecentReposButton;
    @FXML public MenuItem cloneOption;
    @FXML public MenuItem existingOption;
    @FXML public ContextMenu newRepoOptionsMenu;


    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public void initialize() {
        openRepoDirButton.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
        Text openExternallyIcon = GlyphsDude.createIcon(FontAwesomeIcon.EXTERNAL_LINK);
        this.openRepoDirButton.setGraphic(openExternallyIcon);
        this.openRepoDirButton.setTooltip(new Tooltip("Open repository directory"));

        final int REPO_DROPDOWN_MAX_WIDTH = 147;
        repoDropdownSelector.setMaxWidth(REPO_DROPDOWN_MAX_WIDTH);

        Text plusIcon = GlyphsDude.createIcon(FontAwesomeIcon.PLUS);
        this.loadNewRepoButton.setGraphic(plusIcon);

        Text minusIcon = GlyphsDude.createIcon(FontAwesomeIcon.MINUS);
        this.removeRecentReposButton.setGraphic(minusIcon);

        this.removeRecentReposButton.setTooltip(new Tooltip("Clear shortcuts to recently opened repos"));
        Text downloadIcon = GlyphsDude.createIcon(FontAwesomeIcon.CLOUD_DOWNLOAD);
        cloneOption.setGraphic(downloadIcon);

        Text folderOpenIcon = GlyphsDude.createIcon(FontAwesomeIcon.FOLDER_OPEN);
        existingOption.setGraphic(folderOpenIcon);


    }

    public void loadSelectedRepo() {
        sessionController.loadSelectedRepo();
    }

    public void openRepoDirectory() {
        sessionController.openRepoDirectory();
    }

    public void handleLoadNewRepoButton() {
        sessionController.handleLoadNewRepoButton();
    }

    public void handleCloneNewRepoOption() {
        sessionController.handleCloneNewRepoOption();
    }

    public void handleLoadExistingRepoOption() {
        sessionController.handleLoadExistingRepoOption();
    }

    public void chooseRecentReposToDelete() {
        sessionController.chooseRecentReposToDelete();
    }
}
