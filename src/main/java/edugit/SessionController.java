package main.java.edugit;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import main.java.edugit.exceptions.CancelledLoginException;
import main.java.edugit.exceptions.MissingRepoException;
import main.java.edugit.exceptions.NoOwnerInfoException;
import main.java.edugit.exceptions.NoRepoSelectedException;
import org.controlsfx.control.NotificationPane;
import org.controlsfx.control.action.Action;
import org.eclipse.jgit.api.errors.*;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.BackingStoreException;

/**
 * The controller for the entire session.
 */
public class SessionController extends Controller {

    public ComboBox<LocalBranchHelper> branchSelector;
    public Text currentRepoLabel;
    public NotificationPane notificationPane;
    private SessionModel theModel;

    public Button openRepoDirButton;
    public Button gitStatusButton;
    public Button commitButton;
    public Button mergeFromFetchButton;
    public Button pushButton;
    public Button fetchButton;
    public Button branchesButton;

    public TextArea commitMessageField;
    public WorkingTreePanelView workingTreePanelView;
	public CommitTreePanelView localCommitTreePanelView;
    public CommitTreePanelView remoteCommitTreePanelView;

    public Circle remoteCircle;

    public TextField commitInfoNameText;
    public Label commitInfoAuthorText;
    public Label commitInfoDateText;
    public Button commitInfoNameCopyButton;
    public Button commitInfoGoToButton;
    public TextArea commitInfoMessageText;

    CommitTreeModel localCommitTreeModel;
    CommitTreeModel remoteCommitTreeModel;

    // The menu bar
    public MenuBar menuBar;
    private Menu newRepoMenu;
    private Menu openRecentRepoMenu;

    /**
     * Initializes the environment by obtaining the model
     * and putting the views on display.
     *
     * This method is automatically called by JavaFX.
     */
    public void initialize() throws Exception {
        this.initializeLayoutParameters();

        this.theModel = SessionModel.getSessionModel();

        CommitTreeController.sessionController = this;

        this.workingTreePanelView.setSessionModel(this.theModel);
        this.localCommitTreeModel = new LocalCommitTreeModel(this.theModel, this.localCommitTreePanelView);
        this.remoteCommitTreeModel = new RemoteCommitTreeModel(this.theModel, this.remoteCommitTreePanelView);

        // Buttons start out disabled, since no repo is loaded
        this.setButtonsDisabled(true);

        // Branch selector and trigger button starts invisible, since there's no repo and no branches
        this.branchSelector.setVisible(false);

        this.initializeMenuBar();

        this.theModel.loadRecentRepoHelpersFromStoredPathStrings();
        this.theModel.loadMostRecentRepoHelper();

        this.initPanelViews();
        this.updateUIEnabledStatus();
    }

    private void initializeLayoutParameters(){
        openRepoDirButton.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
        gitStatusButton.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
        commitButton.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
        mergeFromFetchButton.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
        pushButton.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
        fetchButton.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
        branchesButton.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);

        gitStatusButton.setMaxWidth(Double.MAX_VALUE);

        commitMessageField.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
        workingTreePanelView.setMinSize(Control.USE_PREF_SIZE, 200);

        branchSelector.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);

        remoteCommitTreePanelView.heightProperty().addListener((observable, oldValue, newValue) -> {
            remoteCircle.setCenterY(newValue.doubleValue() / 2.0);
            if(oldValue.doubleValue() == 0){
                remoteCircle.setRadius(newValue.doubleValue() / 4.0);
            }
        });

        commitInfoNameCopyButton.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
        commitInfoGoToButton.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
    }

    @FXML
    private void updateBranchDropdown() {
        this.branchSelector.setVisible(true);

        List<LocalBranchHelper> branches = this.theModel.getCurrentRepoHelper().getLocalBranchesFromManager();
        this.branchSelector.getItems().setAll(branches);

        LocalBranchHelper currentBranch = this.theModel.getCurrentRepoHelper().getCurrentBranch();

        if(currentBranch == null){
            // This block will run when the app first opens and there is no selection in the dropdown.
            // It finds the repoHelper that matches the currently checked-out branch.
            try{
                String branchName = this.theModel.getCurrentRepo().getFullBranch();
                LocalBranchHelper current = new LocalBranchHelper(branchName, this.theModel.getCurrentRepo());
                for(BranchHelper branchHelper : branches){
                    if(branchHelper.getBranchName().equals(current.getBranchName())){
                        currentBranch = current;
                        this.theModel.getCurrentRepoHelper().setCurrentBranch(currentBranch);
                        break;
                    }
                }
            }catch(IOException e){
                this.showGenericErrorNotification();
                e.printStackTrace();
            }
            if(currentBranch != null){
                CommitTreeController.focusCommitInGraph(this.theModel.currentRepoHelper.getCommitByBranchName(currentBranch.refPathString));
            }
        }

        this.branchSelector.setValue(currentBranch);
    }

    /**
     * Sets up the MenuBar by adding some options to it (for cloning).
     *
     * Each option offers a different way of loading a repository, and each
     * option instantiates the appropriate RepoHelper class for the chosen
     * loading method.
     *
     * Since each option creates a new repo, this method handles errors.
     *
     * TODO: split this method up or something. it's getting too big?
     */
    private void initializeMenuBar() {
        this.newRepoMenu = new Menu("Load new Repository");

        MenuItem cloneOption = new MenuItem("Clone");
        cloneOption.setOnAction(t -> {
            try{
                ClonedRepoHelperBuilder builder = new ClonedRepoHelperBuilder(this.theModel);
                RepoHelper repoHelper = builder.getRepoHelperFromDialogs(); // this creates and sets the RepoHelper

                this.theModel.openRepoFromHelper(repoHelper);

                this.initPanelViews();
                this.updateUIEnabledStatus();
            }catch(IllegalArgumentException e){
                e.printStackTrace();
                this.showInvalidRepoNotification();
            }catch(JGitInternalException e){
                e.printStackTrace();
                this.showNonEmptyFolderNotification();
            }catch(InvalidRemoteException e){
                e.printStackTrace();
                this.showInvalidRemoteNotification();
            }catch(TransportException e){
                e.printStackTrace();
                this.showNotAuthorizedNotification();
            }catch(NoRepoSelectedException e){
                // The user pressed cancel on the dialog box. Do nothing!
            }catch(NoOwnerInfoException e){
                e.printStackTrace();
                this.showNotLoggedInNotification();
            }catch(MissingRepoException e){
                showMissingRecentRepoNotification();
                updateMenuBarWithRecentRepos();
            }catch(NullPointerException e){
                this.showGenericErrorNotification();
                e.printStackTrace();

                // This block used to catch the NoOwnerInfo case,
                // but now that has its own Exception. Not sure when
                // a NullPointer would be thrown, so the dialog isn't
                // very helpful. Todo: investigate.


            }catch(ClassNotFoundException | BackingStoreException e){
                // These should only occur when the recent repo information
                // fails to be loaded or stored, respectively
                // Should be ok to silently fail
            }catch(GitAPIException | IOException e){
                // Somehow, the repository failed to get properly cloned
                // TODO: better error message?
                this.showGenericErrorNotification();
                e.printStackTrace();
            }
        });

        MenuItem existingOption = new MenuItem("Load existing repository");
        existingOption.setOnAction(t -> {
            ExistingRepoHelperBuilder builder = new ExistingRepoHelperBuilder(this.theModel);
            try{
                RepoHelper repoHelper = builder.getRepoHelperFromDialogs();
                this.theModel.openRepoFromHelper(repoHelper);

                this.initPanelViews();
                this.updateUIEnabledStatus();
            }catch(IllegalArgumentException e){
                this.showInvalidRepoNotification();
            } catch(NoRepoSelectedException e){
                // The user pressed cancel on the dialog box. Do nothing!
            }catch(NoOwnerInfoException e){
                this.showNotLoggedInNotification();
                e.printStackTrace();
            }catch(NullPointerException e){
                // TODO: figure out when nullpointer is thrown (if at all?)
                this.showRepoWasNotLoadedNotification();
                e.printStackTrace();
            }catch(BackingStoreException | ClassNotFoundException e){
                // These should only occur when the recent repo information
                // fails to be loaded or stored, respectively
                // Should be ok to silently fail
            }catch(IOException | GitAPIException e){
                // Somehow, the repository failed to get properly cloned
                // TODO: better error message?
                e.printStackTrace();
            }catch(MissingRepoException e){
                showMissingRecentRepoNotification();
                updateMenuBarWithRecentRepos();
            }
        });

        // TODO: implement New Repository option.
        MenuItem newOption = new MenuItem("Start a new repository");
        newOption.setDisable(true);

        this.newRepoMenu.getItems().addAll(cloneOption, existingOption, newOption);

        // Initialize it with no repos to choose from. This gets updated when there are repos present.
        this.openRecentRepoMenu = new Menu("Open recent repository");
        MenuItem noOptionsAvailable = new MenuItem("No recent repositories");
        noOptionsAvailable.setDisable(true);
        this.openRecentRepoMenu.getItems().add(noOptionsAvailable);

        this.menuBar.getMenus().addAll(newRepoMenu, openRecentRepoMenu);

        if (this.theModel.getAllRepoHelpers().size() != 0) {
            // If there are repos from previous sessions, put them in the menu bar
            this.updateMenuBarWithRecentRepos();
        }

    }

    private void updateMenuBarWithRecentRepos() {
        this.openRecentRepoMenu.getItems().clear();

        ArrayList<RepoHelper> repoHelpers = this.theModel.getAllRepoHelpers();
        for (RepoHelper repoHelper : repoHelpers) {
            MenuItem recentRepoHelperMenuItem = new MenuItem(repoHelper.toString());
            recentRepoHelperMenuItem.setOnAction(t -> {
                try {
                    this.theModel.openRepoFromHelper(repoHelper);

                    this.initPanelViews();
                    this.updateUIEnabledStatus();
                } catch (BackingStoreException | ClassNotFoundException | IOException e) {
                    this.showGenericErrorNotification();
                    e.printStackTrace();
                }catch(MissingRepoException e){
                    showMissingRecentRepoNotification();
                    updateMenuBarWithRecentRepos();
                }
            });
            openRecentRepoMenu.getItems().add(recentRepoHelperMenuItem);
        }

        this.menuBar.getMenus().clear();
        this.menuBar.getMenus().addAll(this.newRepoMenu, this.openRecentRepoMenu);
    }

    /**
     * Perform the updateFileStatusInRepo() method for each file whose
     * checkbox is checked. Then commit with the commit message and push.
     */
    public void handleCommitButton() {
        try {
            String commitMessage = commitMessageField.getText();

            for (RepoFile checkedFile : this.workingTreePanelView.getCheckedFilesInDirectory()) {
                checkedFile.updateFileStatusInRepo();
            }

            this.theModel.currentRepoHelper.commit(commitMessage);

            // Now clear the commit text and a view reload ( or `git status`) to show that something happened
            commitMessageField.clear();
            this.onGitStatusButton();
        } catch (NullPointerException e) {
            this.showNoRepoLoadedNotification();
        } catch (TransportException e) {
            this.showNotAuthorizedNotification();
        } catch (WrongRepositoryStateException e) {
            System.out.println("Threw a WrongRepositoryStateException");
            e.printStackTrace();

            // TODO remove the above debug statements
            // This should only come up when the user chooses to resolve conflicts in a file.
            // Do nothing.

        }catch(GitAPIException e){
            this.showGenericErrorNotification();
            e.printStackTrace();
        }

    }

    public void handleMergeFromFetchButton() {
        try {
            this.theModel.currentRepoHelper.mergeFromFetch();
            // Refresh panel views
            this.onGitStatusButton();
        } catch (NullPointerException e) {
            this.showNoRepoLoadedNotification();
        } catch (TransportException e) {
            this.showNotAuthorizedNotification();
        }catch(GitAPIException | IOException e){
            this.showGenericErrorNotification();
            e.printStackTrace();
        }

    }

    public void handlePushButton() {
        try {
            this.theModel.currentRepoHelper.pushAll();

            // Refresh panel views
            this.onGitStatusButton();
        } catch (NullPointerException e) {
            this.showNoRepoLoadedNotification();
        } catch (TransportException e) {
            this.showNotAuthorizedNotification();
        }catch(GitAPIException e){
            this.showGenericErrorNotification();
            e.printStackTrace();
        }
    }

    public void handleFetchButton() {
        try {
            this.theModel.currentRepoHelper.fetch();
            // Refresh panel views
            this.onGitStatusButton();
        } catch (NullPointerException e) {
            this.showNoRepoLoadedNotification();
        } catch (TransportException e) {
            this.showNotAuthorizedNotification();
        }catch(GitAPIException e){
            this.showGenericErrorNotification();
            e.printStackTrace();
        }
    }

    /**
     * Loads the panel views when the "git status" button is clicked.
     */
    public void onGitStatusButton(){
        try {
            this.workingTreePanelView.drawDirectoryView();
            this.localCommitTreeModel.update();
            this.remoteCommitTreeModel.update();

            this.updateBranchDropdown();
        } catch (NullPointerException e) {
            this.showNoRepoLoadedNotification();
            this.updateUIEnabledStatus();
        }catch(GitAPIException | IOException e){
            this.showGenericErrorNotification();
            e.printStackTrace();
        }
    }

    /**
     * When the circle representing the remote repo is clicked, go to the
     * corresponding remote url
     * @param event the mouse event corresponding to the click
     */
    public void handleRemoteCircleMouseClick(MouseEvent event){
        if(event.getButton() != MouseButton.PRIMARY) return;
        Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
        if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
            try {
                List<String> remoteURLs = this.theModel.getCurrentRepoHelper().getLinkedRemoteRepoURLs();

                if(remoteURLs.size() == 0){
                    this.showNoRemoteNotification();
                }

                for (String remoteURL : remoteURLs) {
                    if(remoteURL.contains("@")){
                        remoteURL = "https://"+remoteURL.replace(":","/").split("@")[1];
                    }
                    desktop.browse(new URI(remoteURL));
                }
            }catch(URISyntaxException | IOException e){
                this.showGenericErrorNotification();
            }
        }
    }

    /**
     * Initializes each panel of the view
     */
	private void initPanelViews() {
        try{
            this.workingTreePanelView.drawDirectoryView();
            this.localCommitTreeModel.init();
            this.remoteCommitTreeModel.init();
        }catch(GitAPIException e){
            this.showGenericErrorNotification();
            e.printStackTrace();
        }
    }

    /**
     * A helper method for enabling/disabling buttons.
     *
     * @param disable a boolean for whether or not to disable the buttons.
     */
    private void setButtonsDisabled(boolean disable) {
        openRepoDirButton.setDisable(disable);
        gitStatusButton.setDisable(disable);
        commitButton.setDisable(disable);
        mergeFromFetchButton.setDisable(disable);
        pushButton.setDisable(disable);
        fetchButton.setDisable(disable);
        remoteCircle.setVisible(!disable);
    }

    /**
     *
     */
    public void loadSelectedBranch() {
        LocalBranchHelper selectedBranch = this.branchSelector.getValue();
        if(selectedBranch == null) return;
        try {
            selectedBranch.checkoutBranch();
            RepoHelper repoHelper = this.theModel.getCurrentRepoHelper();
            CommitTreeController.focusCommitInGraph(repoHelper.getCommitByBranchName(selectedBranch.refPathString));

            this.theModel.getCurrentRepoHelper().setCurrentBranch(selectedBranch);
        } catch (CheckoutConflictException e) {
            this.showCheckoutConflictsNotification(e.getConflictingPaths());
            this.updateBranchDropdown();
        }catch(GitAPIException e){
            e.printStackTrace();
        }
    }

    /**
     * A helper helper method to enable or disable buttons/UI elements
     * depending on whether there is a repo open for the buttons to
     * interact with.
     */
    private void updateUIEnabledStatus() {
        RepoHelper currentRepoHelper = this.theModel.getCurrentRepoHelper();

        if ((currentRepoHelper == null || !currentRepoHelper.exists()) && this.theModel.getAllRepoHelpers().size() == 0) {
            // (There's no repo for the buttons to interact with)
            setButtonsDisabled(true);
            this.branchSelector.setVisible(false);
        } else if ((currentRepoHelper == null || !currentRepoHelper.exists()) && this.theModel.getAllRepoHelpers().size() > 0) {
            // (There's no repo for buttons to interact with, but there are repos in the menu bar)
            setButtonsDisabled(true);
            this.branchSelector.setVisible(false);
            this.updateMenuBarWithRecentRepos();
        } else {
            setButtonsDisabled(false);
            this.updateBranchDropdown();
            this.updateMenuBarWithRecentRepos();
            this.updateCurrentRepoLabel();
        }
    }

    private void updateCurrentRepoLabel() {
        String name = this.theModel.getCurrentRepoHelper().toString();
        this.currentRepoLabel.setText(name);
    }

    /// THIS IS JUST A DEBUG METHOD FOR A DEBUG BUTTON. TEMPORARY!
    // todo: set up more permanent data clearing functionality
    public void clearSavedStuff() throws BackingStoreException, IOException, ClassNotFoundException {
        this.theModel.clearStoredPreferences();
    }

    public void switchUser() {
        // Begin with a nullified RepoOwner:
        RepoOwner newOwner = new RepoOwner(null, null);

        try {
            newOwner = new RepoOwner();
        } catch (CancelledLoginException e) {
            // User cancelled the login, so we'll leave the owner full of nullness.
        }

        if (theModel.getCurrentRepoHelper() != null) {
            // The currentRepoHelper could be null, say,
            // on the first run of the program.
            this.theModel.getCurrentRepoHelper().setOwner(newOwner);
        }
        this.theModel.setCurrentDefaultOwner(newOwner);
    }

    public void openRepoDirectory(){
        if (Desktop.isDesktopSupported()) {
            try{
                Desktop.getDesktop().open(this.theModel.currentRepoHelper.localPath.toFile());
            }catch(IOException e){
                this.showFailedToOpenLocalNotification();
            }
        }
    }

    private void showNotLoggedInNotification() {
        this.notificationPane.setText("You need to log in to do that.");

        Action loginAction = new Action("Enter login info", e -> {
            this.notificationPane.hide();
            this.switchUser();
        });

        this.notificationPane.getActions().clear();
        this.notificationPane.getActions().setAll(loginAction);
        this.notificationPane.show();
    }


    private void showNoRepoLoadedNotification() {
        this.notificationPane.setText("You need to load a repository before you can perform operations on it!");

        this.notificationPane.getActions().clear();
        this.notificationPane.show();
    }

    private void showInvalidRepoNotification() {
        this.notificationPane.setText("Make sure the directory you selected contains an existing Git repository.");

        this.notificationPane.getActions().clear();
        this.notificationPane.show();
    }

    private void showMissingRecentRepoNotification(){
        this.notificationPane.setText("That repository no longer exists.");

        this.notificationPane.getActions().clear();
        this.notificationPane.show();
    }

    private void showNoRemoteNotification(){
        this.notificationPane.setText("There is no remote repository associated with " + this.theModel.getCurrentRepoHelper());

        this.notificationPane.getActions().clear();
        this.notificationPane.show();
    }

    private void showFailedToOpenLocalNotification(){
        this.notificationPane.setText("Oops! Failed to open the local repository directory.");

        this.notificationPane.getActions().clear();
        this.notificationPane.show();
    }

    private void showNonEmptyFolderNotification() {
        this.notificationPane.setText("Make sure the directory you selected is completely empty. The best" +
                            "way to do this is to create a new folder from the directory chooser.");

        this.notificationPane.getActions().clear();
        this.notificationPane.show();
    }

    private void showInvalidRemoteNotification() {
        this.notificationPane.setText("Make sure you entered the correct remote URL.");

        this.notificationPane.getActions().clear();
        this.notificationPane.show();
    }

    private void showGenericErrorNotification() {
        this.notificationPane.setText("Sorry, there was an error.");

        this.notificationPane.getActions().clear();
        this.notificationPane.show();
    }

    private void showNotAuthorizedNotification() {
        this.notificationPane.setText("The login information you gave does not allow you to modify this repository. Try switching your login and trying again.");

        Action loginAction = new Action("Log in", e -> {
            this.notificationPane.hide();
            this.switchUser();
        });

        this.notificationPane.getActions().clear();
        this.notificationPane.getActions().setAll(loginAction);
        this.notificationPane.show();
    }

    private void showRepoWasNotLoadedNotification() {
        this.notificationPane.setText("No repository was loaded.");

        this.notificationPane.getActions().clear();
        this.notificationPane.show();
    }

    private void showCheckoutConflictsNotification(List<String> conflictingPaths) {
        String conflictList = "";
        for (String pathName : conflictingPaths) {
            conflictList += "\n" + pathName;
        }

        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Conflicting files");
        alert.setHeaderText("Can't checkout that branch");
        alert.setContentText("You can't switch to that branch because of the following conflicting files between that branch and your current branch: "
                + conflictList);

        this.notificationPane.setText("You can't switch to that branch because there would be a merge conflict. Stash your changes or resolve conflicts first.");

        Action seeConflictsAction = new Action("See conflicts", e -> {
            this.notificationPane.hide();
            alert.showAndWait();
        });

        this.notificationPane.getActions().clear();
        this.notificationPane.getActions().setAll(seeConflictsAction);

        this.notificationPane.show();
    }

    public void showBranchChooser() throws IOException{
        this.theModel.getCurrentRepoHelper().getBranchManager().showBranchChooserWindow();
    }

    public void selectCommit(String id){
        CommitHelper commit = this.theModel.currentRepoHelper.getCommit(id);
        commitInfoNameText.setText(commit.getName());
        commitInfoAuthorText.setText(commit.getAuthorName());
        commitInfoDateText.setText(commit.getFormattedWhen());
        commitInfoMessageText.setText(commit.getMessage(true));
        commitInfoNameCopyButton.setDisable(false);
        commitInfoGoToButton.setDisable(false);
    }

    public void clearSelectedCommit(){
        commitInfoNameText.clear();
        commitInfoAuthorText.setText("");
        commitInfoDateText.setText("");
        commitInfoMessageText.clear();
        commitInfoNameCopyButton.setDisable(true);
        commitInfoGoToButton.setDisable(true);
    }

    public void handleCommitNameCopyButton(){
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(commitInfoNameText.getText());
        clipboard.setContent(content);
    }

    public void handleGoToCommitButton(){
        String id = commitInfoNameText.getText();
        CommitTreeController.focusCommitInGraph(id);
    }
}
