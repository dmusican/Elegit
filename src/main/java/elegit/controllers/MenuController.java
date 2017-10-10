package elegit.controllers;

import elegit.GitIgnoreEditor;
import elegit.SessionModel;
import elegit.treefx.TreeLayout;
import io.reactivex.rxjavafx.observables.JavaFxObservable;
import javafx.event.ActionEvent;
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
import org.apache.http.annotation.GuardedBy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created by dmusicant on 4/8/17.
 */
public class MenuController {

    @GuardedBy("this") private SessionController sessionController;

    @FXML private MenuItem gitIgnoreMenuItem;
    @FXML private Menu repoMenu;


    @FXML private MenuItem cloneMenuItem;
    @FXML private MenuItem createBranchMenuItem;
    @FXML private MenuItem commitNormalMenuItem;
    @FXML private MenuItem normalFetchMenuItem;
    @FXML private MenuItem pullMenuItem;
    @FXML private MenuItem pushMenuItem;
    @FXML private MenuItem stashMenuItem1;
    @FXML private MenuItem stashMenuItem2;

    private static final Logger logger = LogManager.getLogger();

    // Hard: I think I want a separate class just for logging to constrain the locking
    @FXML @GuardedBy("this") private CheckMenuItem loggingToggle;
    @FXML private CheckMenuItem commitSortToggle;

    public void initialize() {
        initMenuBarShortcuts();
        commitSortToggle.setSelected(true); //default

    }

    public synchronized void setLoggingToggle(boolean toggle) {
        loggingToggle.setSelected(toggle);
    }

    /**
     * Sets up keyboard shortcuts for menu items
     *
     *  Combinations:
     *  CMD-N   Clone
     *  Shift + CMD-B   Branch
     *  Shift + CMD-C   Commit
     *  Shift + CMD-F   Fetch
     *  Shift + CMD-L   Pull
     *  Shift + CMD-P   Push (current branch)
     *  Shift + CMD-S   Stash (manager)
     */
    private void initMenuBarShortcuts() {
        this.cloneMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.N, KeyCombination.META_DOWN));
        this.createBranchMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.B, KeyCombination.META_DOWN, KeyCombination.SHIFT_DOWN));
        this.commitNormalMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.C, KeyCombination.META_DOWN, KeyCombination.SHIFT_DOWN));
        this.normalFetchMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.F, KeyCombination.META_DOWN, KeyCombination.SHIFT_DOWN));
        this.pullMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.L, KeyCombination.META_DOWN, KeyCombination.SHIFT_DOWN));
        this.pushMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.P, KeyCombination.META_DOWN, KeyCombination.SHIFT_DOWN));
        this.stashMenuItem1.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN));
        this.stashMenuItem2.setAccelerator(new KeyCodeCombination(KeyCode.L, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN));
    }

    public synchronized void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    // "Preferences" Dropdown Menu Items:

    public synchronized void handleLoggingToggle() {
        if (loggingToggle.isSelected()) {
            sessionController.handleLoggingOn();
        } else {
            sessionController.handleLoggingOff();
        }
        assert !loggingToggle.isSelected() == sessionController.getLoggingLevel().equals(org.apache.logging.log4j.Level.toLevel("OFF"));
    }

    public synchronized void handleCommitSortToggle() {
        if (commitSortToggle.isSelected()){
            sessionController.handleCommitSortTopological();
        } else {
            sessionController.handleCommitSortDate();
        }
        assert commitSortToggle.isSelected() == TreeLayout.commitSortTopological ;
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

    public synchronized void handleAbout() {
        try{
            logger.info("About clicked");
            // Create and display the Stage:
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/elegit/fxml/About.fxml"));
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
        }catch(IOException e) {
            sessionController.showGenericErrorNotification();
            e.printStackTrace();
        }
    }


    /**
     * Helper method for disabling the menu bar
     */
    public void updateMenuBarEnabledStatus(boolean disable) {
        repoMenu.setDisable(disable);
        gitIgnoreMenuItem.setDisable(disable);
    }


    // "Edit" Dropdown Menu Item:

    // TODO: Make sure GitIgnoreEditor is threadsafe
    public void handleGitIgnoreMenuItem()
    {
        GitIgnoreEditor.show(SessionModel.getSessionModel().getCurrentRepoHelper(), null);
    }

    // "Repository" Dropdown Menu Items (2 layers):

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

    public synchronized void handleCloneNewRepoOption() {
        sessionController.handleCloneNewRepoOption();
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

}