package elegit.controllers;

import elegit.treefx.TreeLayout;
import io.reactivex.rxjavafx.observables.JavaFxObservable;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;

/**
 * Created by dmusicant on 4/8/17.
 */
public class MenuController {

    private SessionController sessionController;
    @FXML public CheckMenuItem loggingToggle; // public so can be selected when prefs loaded in SessionController
    @FXML private CheckMenuItem commitSortToggle;
    @FXML MenuItem gitIgnoreMenuItem; // has to be public because of SessionController.updateMenuBarEnabledStatus()
    @FXML Menu repoMenu;
    @FXML private MenuItem cloneMenuItem;
    @FXML private MenuItem createBranchMenuItem;
    @FXML private MenuItem commitNormalMenuItem;
    @FXML private MenuItem normalFetchMenuItem;
    @FXML private MenuItem pullMenuItem;
    @FXML private MenuItem pushMenuItem;
    @FXML private MenuItem stashMenuItem1;
    @FXML private MenuItem stashMenuItem2;

    public void initialize() {
        initMenuBarShortcuts();
        commitSortToggle.setSelected(true); //default

        JavaFxObservable.actionEventsOf(normalFetchMenuItem)
//                .map(ae -> sessionController.handleFetchButton(false, false))
                .subscribe(SessionController.getNormalFetchRequests());

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

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    // "Preferences" Dropdown Menu Items:

    public void handleLoggingToggle() {
        if (loggingToggle.isSelected()) {
            sessionController.handleLoggingOn();
        } else {
            sessionController.handleLoggingOff();
        }
        assert !loggingToggle.isSelected() == sessionController.getLoggingLevel().equals(org.apache.logging.log4j.Level.toLevel("OFF"));
    }

    public void handleCommitSortToggle() {
        if (commitSortToggle.isSelected()){
            sessionController.handleCommitSortTopological();
        } else {
            sessionController.handleCommitSortDate();
        }
        assert commitSortToggle.isSelected() == TreeLayout.commitSortTopological ;
    }

    public void handleAbout() {
        sessionController.handleAbout();
    }

    // "Edit" Dropdown Menu Item:

    public void handleGitIgnoreMenuItem() {
        sessionController.handleGitIgnoreMenuItem();
    }

    // "Repository" Dropdown Menu Items (2 layers):

    public void handleNewBranchButton() {
        sessionController.handleNewBranchButton();
    }

    public void handleDeleteLocalBranchButton() {
        sessionController.handleDeleteLocalBranchButton();
    }

    public void handleDeleteRemoteBranchButton() {
        sessionController.handleDeleteRemoteBranchButton();
    }

    public void showBranchCheckout() {
        sessionController.showBranchCheckout();
    }

    public void handleCloneNewRepoOption() {
        sessionController.handleCloneNewRepoOption();
    }

    public void handleCommitAll() {
        sessionController.handleCommitAll();
    }

    public void handleCommitNormal() {
        sessionController.handleCommitNormal();
    }

    public void handlePruneFetchButton() {
        sessionController.handlePruneFetchButton();
    }

    public void mergeFromFetch() {
        sessionController.mergeFromFetch();
    }

    public void handleBranchMergeButton() {
        sessionController.handleBranchMergeButton();
    }

    public void handlePullButton() {
        sessionController.handlePullButton();
    }

    public void handlePushButton() {
        sessionController.handlePushButton();
    }

    public void handlePushAllButton() {
        sessionController.handlePushAllButton();
    }

    public void handlePushTagsButton() {
        sessionController.handlePushTagsButton();
    }

    public void handleStashSaveButton() {
        sessionController.handleStashSaveButton();
    }

    public void handleStashApplyButton() {
        sessionController.handleStashApplyButton();
    }

    public void handleStashListButton() {
        sessionController.handleStashListButton();
    }

    public void handleStashDropButton() {
        sessionController.handleStashDropButton();
    }

    }