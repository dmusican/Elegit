package elegit.controllers;

import de.jensd.fx.glyphs.GlyphsDude;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import elegit.Main;
import elegit.models.RepoHelper;
import elegit.models.SessionModel;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Side;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import org.apache.http.annotation.GuardedBy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.controlsfx.control.CheckListView;
import org.controlsfx.control.PopOver;

import java.util.List;

/**
 * Created by dmusicant on 4/8/17.
 */
public class DropdownController {

    @GuardedBy("this")
    private SessionController sessionController;

    @FXML private Button openRepoDirButton;
    @FXML private Button loadNewRepoButton;
    @FXML private Button removeRecentReposButton;
    @FXML private MenuItem cloneOption;
    @FXML private MenuItem existingOption;
    @FXML private ContextMenu newRepoOptionsMenu;
    @FXML private ComboBox<RepoHelper> repoDropdownSelector;

    private static final Logger logger = LogManager.getLogger();
    private static final Logger console = LogManager.getLogger("briefconsolelogger");

    public synchronized void setSessionController(SessionController sessionController) {
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
        loadNewRepoButton.setGraphic(plusIcon);

        loadNewRepoButton.setTooltip(new Tooltip(
                "Load a new repository"
        ));

        Text minusIcon = GlyphsDude.createIcon(FontAwesomeIcon.MINUS);
        removeRecentReposButton.setGraphic(minusIcon);

        removeRecentReposButton.setTooltip(new Tooltip("Clear shortcuts to recently opened repos"));
        Text downloadIcon = GlyphsDude.createIcon(FontAwesomeIcon.CLOUD_DOWNLOAD);
        cloneOption.setGraphic(downloadIcon);

        Text folderOpenIcon = GlyphsDude.createIcon(FontAwesomeIcon.FOLDER_OPEN);
        existingOption.setGraphic(folderOpenIcon);



    }

    public void setButtonsDisabled(boolean value) {
        openRepoDirButton.setDisable(value);
        removeRecentReposButton.setDisable(value);
        repoDropdownSelector.setDisable(value);
    }

    public synchronized void loadSelectedRepo() {
        sessionController.loadDesignatedRepo(getCurrentRepo());
    }

    public synchronized void openRepoDirectory() {
        sessionController.openRepoDirectory();
    }

    /**
     * Called when the loadNewRepoButton gets pushed, shows a menu of options
     */
    public synchronized void handleLoadNewRepoButton() {
        newRepoOptionsMenu.show(loadNewRepoButton, Side.BOTTOM ,0, 0);
    }

    public synchronized void handleCloneNewRepoOption() {
        sessionController.handleCloneNewRepoOption();
    }

    public synchronized void handleLoadExistingRepoOption() {
        sessionController.handleLoadExistingRepoOption();
    }

    // TODO: Make sure that RepoHelper is threadsafe
    public synchronized void chooseRecentReposToDelete() {
        logger.info("Remove repos button clicked");

        // creates a CheckListView with all the repos in it
        List<RepoHelper> repoHelpers = SessionModel.getSessionModel().getAllRepoHelpers();
        CheckListView<RepoHelper> repoCheckListView = new CheckListView<>(FXCollections.observableArrayList(repoHelpers));
        repoCheckListView.setId("repoCheckList");

        repoCheckListView.getItemBooleanProperty(0).set(true);
        // creates a popover with the list and a button used to remove repo shortcuts
        Button removeSelectedButton = new Button("Remove repository shortcuts from Elegit");
        removeSelectedButton.setId("reposDeleteRemoveSelectedButton");
        PopOver popover = new PopOver(new VBox(repoCheckListView, removeSelectedButton));
        popover.setTitle("Manage Recent Repositories");

        // shows the popover
        popover.show(removeRecentReposButton);

        removeSelectedButton.setOnAction(e -> {
            sessionController.handleRemoveReposButton(repoCheckListView.getCheckModel().getCheckedItems());
            popover.hide();
        });
    }

    /**
     * When setting the current repo, always reset the list of repos at the same time. This way we can ensure
     * that the repo being set as the current one is also in the list of repos.
     * @param repoHelper The repo to show
     * @param repoHelpers The list of all repos to show
     */
    public void setCurrentRepoWithoutInvokingAction(RepoHelper repoHelper, ObservableList<RepoHelper> repoHelpers) {
        console.info(repoHelper);
        if (!containedInList(repoHelper, repoHelpers)) {
            throw new RuntimeException("Repo being set [" + repoHelper + "] is not in list of repos: " + repoHelpers);
        }
        setAllReposWithoutInvokingAction(repoHelpers);
        EventHandler<ActionEvent> handler = repoDropdownSelector.getOnAction();
        repoDropdownSelector.setOnAction(null);
        repoDropdownSelector.setValue(repoHelper);
        repoDropdownSelector.setOnAction(handler);
        console.info("Finished");
    }

    private boolean containedInList(RepoHelper repoHelper, ObservableList<RepoHelper> repoHelpers) {
        // If the repoHelper is null, we should consider that as "contained," in that it is legitimate as a choice.
        if (repoHelper == null) {
            return true;
        }

        boolean containedInList = false;
        for (RepoHelper repoInList : repoHelpers) {
            if (repoHelper.getLocalPath().equals(repoInList.getLocalPath())) {
                containedInList = true;
                break;
            }
        }
        console.info("Finished: " + containedInList);
        return containedInList;
    }

    public RepoHelper getCurrentRepo() {
        return repoDropdownSelector.getValue();
    }

    private void setAllReposWithoutInvokingAction(ObservableList<RepoHelper> repoHelpers) {
        Main.assertFxThread();
        console.info(repoHelpers);
        EventHandler<ActionEvent> handler = repoDropdownSelector.getOnAction();
        repoDropdownSelector.setOnAction(null);
        repoDropdownSelector.setItems(repoHelpers);
        repoDropdownSelector.setOnAction(handler);
    }
}
