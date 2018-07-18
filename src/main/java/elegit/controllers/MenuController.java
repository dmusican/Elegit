package elegit.controllers;

import elegit.Main;
import elegit.gui.GitIgnoreEditor;
import elegit.models.LoggingModel;
import elegit.models.RepoHelper;
import elegit.models.SessionModel;
import elegit.treefx.TreeLayout;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.apache.commons.lang3.SystemUtils;
import net.jcip.annotations.GuardedBy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Created by dmusicant on 4/8/17.
 * Has all the functionality for the menus. Most are from SessionController, but some options needed to be modified,
 * so they are here.
 */
public class MenuController {

    @GuardedBy("this")
    private SessionController sessionController;

    @FXML
    private MenuItem gitIgnoreMenuItem;
    @FXML
    private Menu repoMenu;
    @FXML
    private MenuItem cloneMenuItem;
    @FXML
    private Menu loadSelectedRepoMenuOption;
    @FXML
    private Menu removeRecentReposMenuOption;
    @FXML
    private MenuItem createBranchMenuItem;
    @FXML
    private MenuItem commitNormalMenuItem;
    @FXML
    private MenuItem normalFetchMenuItem;
    @FXML
    private MenuItem pullMenuItem;
    @FXML
    private MenuItem pushMenuItem;
    @FXML
    private MenuItem stashMenuItem1;
    @FXML
    private MenuItem stashMenuItem2;
    @FXML
    private MenuItem reenableWindowResizing;

    // Normally, our MVC-like setup should put the toggle status in the model. However, the FX
    // CheckMenuItem can't be bound to that; it is set here and automatically toggled when the menu is selected.
    // So, this is the primary space the status is being stored; and the model status is bound to this one.
    @FXML
    private CheckMenuItem loggingToggle;
    @FXML
    private CheckMenuItem commitSortToggle;

    private static final Logger logger = LogManager.getLogger();

    public void initialize() {
        initMenuBarShortcuts();
        initializeLoggingToggle();
        LoggingModel.bindLogging(loggingToggle.selectedProperty());
        TreeLayout.bindSorting(commitSortToggle.selectedProperty());

        commitSortToggle.setSelected(true); //default

        // Workaround for this bug:
        // http://bugs.java.com/bugdatabase/view_bug.do?bug_id=8140491
        if (!SystemUtils.IS_OS_LINUX) {
            reenableWindowResizing.setVisible(false);
        }
    }

    public void initializeLoggingToggle() {
        loggingToggle.setSelected(LoggingModel.loggingOn());
    }

    /**
     * Sets up keyboard shortcuts for menu items
     * <p>
     * Combinations:
     * CMD-N   Clone
     * Shift + CMD-B   Branch
     * Shift + CMD-C   Commit
     * Shift + CMD-F   Fetch
     * Shift + CMD-L   Pull
     * Shift + CMD-P   Push (current branch)
     * Shift + CMD-S   Stash (manager)
     */
    private void initMenuBarShortcuts() {
        cloneMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.N, KeyCombination.META_DOWN));
        createBranchMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.B, KeyCombination.META_DOWN, KeyCombination.SHIFT_DOWN));
        commitNormalMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.C, KeyCombination.META_DOWN, KeyCombination.SHIFT_DOWN));
        normalFetchMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.F, KeyCombination.META_DOWN, KeyCombination.SHIFT_DOWN));
        pullMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.L, KeyCombination.META_DOWN, KeyCombination.SHIFT_DOWN));
        pushMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.P, KeyCombination.META_DOWN, KeyCombination.SHIFT_DOWN));
        stashMenuItem1.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN));
        stashMenuItem2.setAccelerator(new KeyCodeCombination(KeyCode.L, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN));
    }

    public synchronized void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    /**
     * Helper method for disabling the menu bar
     */
    public void updateMenuBarEnabledStatus(boolean disable) {
        repoMenu.setDisable(disable);
        gitIgnoreMenuItem.setDisable(disable);
    }

    //----- "File" Dropdown Menu Items: -----

    public synchronized void handleCloneNewRepoOption() {
        sessionController.handleCloneNewRepoOption();
    }

    public synchronized void handleLoadExistingRepoOption() { sessionController.handleLoadExistingRepoOption(); }

    /**
     * Very similar to the method in DropdownController. Adds a menu item for each repository, so the user can open
     * or close them from the menus as well as from the dropdown section
     */
    public void setAllReposWithoutInvokingAction(ObservableList<RepoHelper> repoHelpers) {
        Main.assertFxThread();
        // This way there are not duplicates of each repo created every time they are refreshed
        loadSelectedRepoMenuOption.getItems().clear();
        removeRecentReposMenuOption.getItems().clear();

        for (RepoHelper repoHelper : repoHelpers) {
            MenuItem loadMenuItem = new MenuItem(repoHelper.toString());
            loadMenuItem.setOnAction(e -> loadSelectedRepo(repoHelper));
            loadSelectedRepoMenuOption.getItems().add(loadMenuItem);

            MenuItem removeMenuItem = new MenuItem(repoHelper.toString());
            removeMenuItem.setOnAction(e -> chooseRecentRepoToDelete(repoHelper));
            removeRecentReposMenuOption.getItems().add(removeMenuItem);
        }
    }

    private synchronized void loadSelectedRepo(RepoHelper repoHelper) {
        sessionController.loadDesignatedRepo(repoHelper);
    }

    /**
     * Similar to the method in DropdownController, but only allows the user to remove one repo at a time and doesn't
     * open a pop up window, but a normal sub menu with the loaded repositories (refer to
     * setReposWithoutInvokingAction() for that part specifically).
     */
    private synchronized void chooseRecentRepoToDelete(RepoHelper repoHelper) {
        // Have to turn it into a list, so the SessionController method can be called and no repeated code is necessary
        List<RepoHelper> listOfOneRepo = new ArrayList<>(1);
        listOfOneRepo.add(repoHelper);
        sessionController.handleRemoveReposButton(listOfOneRepo);
    }

    //----- "Edit" Dropdown Menu Items: -----

    // TODO: copy

    // TODO: paste

    // TODO: Make sure GitIgnoreEditor is threadsafe
    public void handleGitIgnoreMenuItem() {
        //for macs
        sessionController.updateCommandText("touch .gitignore");
        //for PCs
        //sessionController.updateCommandText("C:\\> \"c:\\program files (x86)\\git\\bin\\touch.exe\" .gitignore");
        GitIgnoreEditor.show(SessionModel.getSessionModel().getCurrentRepoHelper(), null);
    }

    //----- "View" Dropdown Menu Items: -----

    public synchronized void handleCommitSortToggle() {
        sessionController.handleCommitSortToggle();
    }

    public void handleReenableWindowResizing() {
        Main.hidePrimaryStage();
        Main.showPrimaryStage();
    }

    //----- "Repository" Dropdown Menu Items (2 layers): -----


    public synchronized void handleNewBranchButton() {
        sessionController.handleNewBranchButton();
    }

    public synchronized void handleDeleteLocalBranchButton() {
        sessionController.handleDeleteLocalBranchButton();
    }

    public synchronized void handleDeleteRemoteBranchButton() {
        sessionController.handleDeleteRemoteBranchButton();
    }

    public synchronized void showBranchCheckout() {
        sessionController.showBranchCheckout();
    }

    public synchronized void handleCommitAll() {
        sessionController.handleCommitAll();
    }

    public synchronized void handleCommitNormal() {
        sessionController.handleCommitNormal();
    }

    public synchronized void handleFetchButton() {
        sessionController.handleFetchButton();
    }

    public synchronized void handlePruneFetchButton() {
        sessionController.handlePruneFetchButton();
    }

    public synchronized void mergeFromFetch() {
        sessionController.mergeFromFetch();
    }

    public synchronized void handleBranchMergeButton() {
        sessionController.handleBranchMergeButton();
    }

    public synchronized void handlePullButton() {
        sessionController.handlePullButton();
    }

    public synchronized void handlePushButton() {
        sessionController.handlePushButton();
    }

    public synchronized void handlePushAllButton() {
        sessionController.handlePushAllButton();
    }

    public synchronized void handlePushTagsButton() {
        sessionController.handlePushTagsButton();
    }

    public synchronized void handleStashSaveButton() {
        sessionController.handleStashSaveButton();
    }

    public synchronized void handleStashApplyButton() {
        sessionController.handleStashApplyButton();
    }

    public synchronized void handleStashListButton() {
        sessionController.handleStashListButton();
    }

    public synchronized void handleStashDropButton() {
        sessionController.handleStashDropButton();
    }

    public synchronized void handleTranscriptClearMenuItem() {
        sessionController.handleTranscriptClearItem();
    }

    public synchronized void handleTranscriptViewMenuItem() {
        sessionController.handleTranscriptViewMenuItem();
    }

    //----- "Preferences" Dropdown Menu Items: -----

    public synchronized void handleLoggingToggle() {
        LoggingModel.toggleLogging();
    }

    //----- "Help" Dropdown Menu Items: -----

    public synchronized void handleAbout() {
        try {
            logger.info("About clicked");
            // Create and display the Stage:
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/elegit/fxml/pop-ups/About.fxml"));
            GridPane fxmlRoot = fxmlLoader.load();
            AboutController aboutController = fxmlLoader.getController();
            aboutController.setVersion(getVersion());

            Stage stage = new Stage();
            javafx.scene.image.Image img = new javafx.scene.image.Image(getClass().getResourceAsStream("/elegit/images/elegit_icon.png"));
            stage.getIcons().add(img);
            stage.setTitle("About");
            stage.setScene(new Scene(fxmlRoot));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setOnCloseRequest(event -> logger.info("Closed about"));
            stage.show();
        } catch (IOException e) {
            sessionController.showGenericErrorNotification(e);
            e.printStackTrace();
        }
    }

    private String getVersion() {
        String path = "/version.prop";
        InputStream stream = getClass().getResourceAsStream(path);
        if (stream == null)
            return "UNKNOWN";
        Properties props = new Properties();
        try {
            props.load(stream);
            stream.close();
            return (String) props.get("version");
        } catch (IOException e) {
            return "UNKNOWN";
        }
    }
}
