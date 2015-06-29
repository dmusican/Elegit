package main.java.edugit;

import com.sun.xml.internal.ws.api.pipe.FiberContextSwitchInterceptor;
import javafx.fxml.FXML;
import main.java.edugit.exceptions.CancelledLoginException;
import main.java.edugit.exceptions.NoOwnerInfoException;
import main.java.edugit.exceptions.NoRepoSelectedException;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.scene.control.Button;
import javafx.scene.control.*;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.paint.RadialGradient;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import org.controlsfx.control.ListSelectionView;
import org.controlsfx.control.NotificationPane;
import org.controlsfx.control.action.Action;
import org.eclipse.jgit.api.errors.*;
import org.eclipse.jgit.lib.Config;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.prefs.BackingStoreException;

/**
 * The controller for the entire session.
 */
public class SessionController extends Controller {

    public ComboBox<BranchHelper> branchSelector;
    public Text currentRepoLabel;
    public NotificationPane notificationPane;
    private SessionModel theModel;

    public Button openRepoDirButton;
    public Button gitStatusButton;
    public Button commitButton;
    public Button mergeFromFetchButton;
    public Button pushButton;
    public Button fetchButton;

    public TextArea commitMessageField;
    public WorkingTreePanelView workingTreePanelView;
	public CommitTreePanelView localCommitTreePanelView;
    public CommitTreePanelView remoteCommitTreePanelView;

    public Circle remoteCircle;
    private static final RadialGradient startGradient  = RadialGradient.valueOf("center 50% 50%, radius 50%,  #52B3D9 60%, #3498DB");
    private static final RadialGradient hoverGradient = RadialGradient.valueOf("center 50% 50%, radius 50%,  #81CFE0 60%, #52B3D9");
    private static final RadialGradient clickGradient = RadialGradient.valueOf("center 50% 50%, radius 50%,  #3498DB 60%, #52B3D9");

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

        gitStatusButton.setMaxWidth(Double.MAX_VALUE);

        commitMessageField.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
        workingTreePanelView.setMinSize(Control.USE_PREF_SIZE, 200);

        branchSelector.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);

        remoteCommitTreePanelView.heightProperty().addListener((observable, oldValue, newValue) -> {
            remoteCircle.setCenterY(newValue.doubleValue() / 2.0);
            if (oldValue.doubleValue() == 0) {
                remoteCircle.setRadius(newValue.doubleValue() / 5.0);
            }
        });
        remoteCircle.setFill(startGradient);
    }

    @FXML
    private void updateBranchDropdown() throws GitAPIException, IOException {
        this.branchSelector.setVisible(true);

        List<LocalBranchHelper> branches = this.theModel.getCurrentRepoHelper().getLocalBranchesFromManager();
        this.branchSelector.getItems().setAll(branches);

        BranchHelper currentBranch = this.theModel.getCurrentRepoHelper().getCurrentBranch();

        if (currentBranch == null) {
            // This block will run when the app first opens and there is no selection in the dropdown.
            // It finds the repoHelper that matches the currently checked-out branch.
            String branchName = this.theModel.getCurrentRepo().getFullBranch();
            LocalBranchHelper current = new LocalBranchHelper(branchName, this.theModel.getCurrentRepo());
            for (BranchHelper branchHelper : branches) {
                if (branchHelper.getBranchName().equals(current.getBranchName())) {
                    currentBranch = current;
                    this.theModel.getCurrentRepoHelper().setCurrentBranch(currentBranch);
                    break;
                }
            }
            if(currentBranch != null){
                CommitTreeController.focusCommit(this.theModel.currentRepoHelper.getCommitByBranchName(currentBranch.refPathString));
            }
        }

        this.branchSelector.setValue(currentBranch);
        // TODO: do a commit-focus on the initial load, too!
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
    private void initializeMenuBar() throws GitAPIException, IOException {
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
            } catch (NoRepoSelectedException e) {

                // The user pressed cancel on the dialog box. Do nothing!

            }catch(NoOwnerInfoException e){
                e.printStackTrace();
                this.showNotLoggedInNotification();
            }catch(NullPointerException e){
                this.showGenericErrorNotification();
                e.printStackTrace();

                // This block used to catch the NoOwnerInfo case,
                // but now that has its own Exception. Not sure when
                // a NullPointer would be thrown, so the dialog isn't
                // very helpful. Todo: investigate.


            } catch(Exception e){
                // The generic error is totally unhelpful, so try not to ever reach this catch statement
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
                e.printStackTrace();
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
            }catch(Exception e){
                this.showGenericErrorNotification();
                System.out.println("***** FIGURE OUT WHY THIS EXCEPTION IS NEEDED *******");
                e.printStackTrace();
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
                } catch (BackingStoreException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                } catch(GitAPIException e){
                    e.printStackTrace();
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
     *
     * @param actionEvent the button click event.
     * @throws GitAPIException if the updateFileStatusInRepo() call fails.
     * @throws IOException if the onGitStatusButton() fails.
     */ //todo: since there's a try/catch, should this method signature not throw exceptions?
    public void handleCommitButton(ActionEvent actionEvent) throws GitAPIException, IOException {
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

        }

    }

    public void handleMergeFromFetchButton(ActionEvent actionEvent) throws IOException, GitAPIException {
        try {
            this.theModel.currentRepoHelper.mergeFromFetch();
            // Refresh panel views
            this.onGitStatusButton();
        } catch (NullPointerException e) {
            this.showNoRepoLoadedNotification();
        } catch (TransportException e) {
            this.showNotAuthorizedNotification();
        }

    }

    public void handlePushButton(ActionEvent actionEvent) throws GitAPIException, IOException {
        try {
            this.theModel.currentRepoHelper.pushAll();

            // Refresh panel views
            this.onGitStatusButton();
        } catch (NullPointerException e) {
            this.showNoRepoLoadedNotification();
        } catch (TransportException e) {
            this.showNotAuthorizedNotification();
        }
    }

    public void handleFetchButton(ActionEvent actionEvent) throws GitAPIException, IOException {
        try {
            this.theModel.currentRepoHelper.fetch();
            // Refresh panel views
            this.onGitStatusButton();
        } catch (NullPointerException e) {
            this.showNoRepoLoadedNotification();
        } catch (TransportException e) {
            this.showNotAuthorizedNotification();
        }
    }

    /**
     * Loads the panel views when the "git status" button is clicked.
     *
     * @throws GitAPIException if the drawDirectoryView() call fails.
     * @throws IOException if the drawDirectoryView() call fails.
     */
    public void onGitStatusButton() throws GitAPIException, IOException{
        try {
            this.workingTreePanelView.drawDirectoryView();
            this.localCommitTreeModel.update();
            this.remoteCommitTreeModel.update();

            this.updateBranchDropdown();
        } catch (NullPointerException e) {
            this.showNoRepoLoadedNotification();
        }
    }

    /**
     * When the circle representing the remote repo is clicked, go to the
     * corresponding remote url
     * @param event
     */
    public void handleRemoteCircleMouseClick(Event event){
        Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
        if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
            try {
                Config storedConfig = this.theModel.getCurrentRepo().getConfig();
                Set<String> remotes = storedConfig.getSubsections("remote");

                for (String remoteName : remotes) {
                    String url = storedConfig.getString("remote", remoteName, "url");
                    if(url.contains("@")){
                        url = "https://"+url.replace(":","/").split("@")[1];
                    }
                    desktop.browse(new URI(url));
                }
            } catch (Exception e) {
                // TODO: real error message
                e.printStackTrace();
                System.out.println("Couldn't open the remote repo");
            } finally{
                remoteCircle.setFill(startGradient);
            }
        }
    }

    public void handleRemoteCircleMouseEnter(Event event){
        remoteCircle.setFill(hoverGradient);
    }

    public void handleRemoteCircleMouseExit(Event event){
        remoteCircle.setFill(startGradient);
    }

    public void handleRemoteCircleMousePressed(Event event){
        remoteCircle.setFill(clickGradient);
    }

    /**
     * Initializes each panel of the view
     * @throws GitAPIException
     * @throws IOException
     */
	private void initPanelViews() throws GitAPIException, IOException{
        this.workingTreePanelView.drawDirectoryView();
        this.localCommitTreeModel.init();
        this.remoteCommitTreeModel.init();
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
     * @param actionEvent
     * @throws GitAPIException
     * @throws IOException from updateBranchDropdown()
     */
    public void loadSelectedBranch(ActionEvent actionEvent) throws GitAPIException, IOException {
        BranchHelper selectedBranch = this.branchSelector.getValue();
        if(selectedBranch == null) return;
        try {
            selectedBranch.checkoutBranch();
            RepoHelper repoHelper = this.theModel.getCurrentRepoHelper();
            CommitTreeController.focusCommit(repoHelper.getCommitByBranchName(selectedBranch.refPathString));

            this.theModel.getCurrentRepoHelper().setCurrentBranch(selectedBranch);
        } catch (CheckoutConflictException e) {
            this.showCheckoutConflictsNotification(e.getConflictingPaths());
            this.updateBranchDropdown();
        }
    }

    /**
     * A helper helper method to enable or disable buttons/UI elements
     * depending on whether there is a repo open for the buttons to
     * interact with.
     */
    private void updateUIEnabledStatus() throws GitAPIException, IOException {
        RepoHelper currentRepoHelper = this.theModel.getCurrentRepoHelper();

        if (currentRepoHelper == null && this.theModel.getAllRepoHelpers().size() == 0) {
            // (There's no repo for the buttons to interact with)
            setButtonsDisabled(true);
            this.branchSelector.setVisible(false);
        } else if (currentRepoHelper == null && this.theModel.getAllRepoHelpers().size() > 0) {
            // (There's no repo for buttons to interact with, but there are repos in the menu bar)
            setButtonsDisabled(true);
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
    public void clearSavedStuff(ActionEvent actionEvent) throws BackingStoreException, IOException, ClassNotFoundException {
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

    public void openRepoDirectory(ActionEvent actionEvent){
        if (Desktop.isDesktopSupported()) {
            try{
                Desktop.getDesktop().open(this.theModel.currentRepoHelper.localPath.toFile());
            }catch(IOException e){
                e.printStackTrace();
                // TODO: real error message
                System.out.println("Couldn't open the local repo. Real error message here eventually");
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

    public void showBranchChooser(ActionEvent actionEvent) throws IOException {
        this.theModel.getCurrentRepoHelper().getBranchManager().showBranchChooser();
    }
}
