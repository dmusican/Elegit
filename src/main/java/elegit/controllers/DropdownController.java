package elegit.controllers;

import de.jensd.fx.glyphs.GlyphsDude;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import elegit.Main;
import elegit.models.RepoHelper;
import elegit.models.SessionModel;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import javafx.application.Platform;
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
        System.out.println("yep");

        // creates a CheckListView with all the repos in it
        List<RepoHelper> repoHelpers = SessionModel.getSessionModel().getAllRepoHelpers();
        CheckListView<RepoHelper> repoCheckListView = new CheckListView<>(FXCollections.observableArrayList(repoHelpers));

        // creates a popover with the list and a button used to remove repo shortcuts
        Button removeSelectedButton = new Button("Remove repository shortcuts from Elegit");
        PopOver popover = new PopOver(new VBox(repoCheckListView, removeSelectedButton));
        popover.setTitle("Manage Recent Repositories");

        // shows the popover
        popover.show(removeRecentReposButton);

        removeSelectedButton.setOnAction(e -> {
            sessionController.handleRemoveReposButton(repoCheckListView.getCheckModel().getCheckedItems());
            popover.hide();
        });
    }

    public void setCurrentRepoWithoutInvokingAction(RepoHelper repoHelper) {
        EventHandler<ActionEvent> handler = repoDropdownSelector.getOnAction();
        repoDropdownSelector.setOnAction(null);
        repoDropdownSelector.setValue(repoHelper);
        repoDropdownSelector.setOnAction(handler);
    }

    public RepoHelper getCurrentRepo() {
        return repoDropdownSelector.getValue();
    }

    public void setAllRepos(ObservableList<RepoHelper> repoHelpers) {
        repoDropdownSelector.setItems(repoHelpers);
    }
}
