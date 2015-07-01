package main.java.edugit;

import javafx.application.Platform;
import javafx.concurrent.Task;
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
import main.java.edugit.exceptions.*;
import org.controlsfx.control.NotificationPane;
import org.controlsfx.control.action.Action;
import org.eclipse.jgit.api.errors.*;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.prefs.BackingStoreException;

/**
 * The controller for the entire session.
 */
public class SessionController extends Controller {

    public ComboBox<LocalBranchHelper> branchSelector;
    public Text currentRepoLabel;
    public NotificationPane notificationPane;
    public Button selectAllButton;
    public Button deselectAllButton;
    private SessionModel theModel;

    public Button openRepoDirButton;
    public Button gitStatusButton;
    public Button commitButton;
    public Button mergeFromFetchButton;
    public Button pushButton;
    public Button fetchButton;
    public Button branchesButton;

    public ProgressIndicator fetchProgressIndicator;
    public ProgressIndicator pushProgressIndicator;

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
        this.theModel = SessionModel.getSessionModel();

        this.initializeLayoutParameters();
        this.initializeButtonDisableBindings();

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

        workingTreePanelView.setMinSize(Control.USE_PREF_SIZE, 200);
        commitMessageField.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);

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

    private void initializeButtonDisableBindings(){
        commitButton.setDisable(true);
        mergeFromFetchButton.setDisable(true);
        pushButton.setDisable(true);
        fetchButton.setDisable(true);
        this.theModel.currentRepoHelperProperty.addListener((observable, oldValue, newValue) -> {
            commitButton.disableProperty().bind(gitStatusButton.disableProperty()
                    .or(commitMessageField.textProperty().isEmpty())
                    .or(workingTreePanelView.isAnyFileSelectedProperty.not()));

            mergeFromFetchButton.disableProperty().bind(gitStatusButton.disableProperty()
                    .or(newValue.hasRemoteProperty.not()));
//                    .or(newValue.hasUnmergedCommitsProperty.not()));
//
            pushButton.disableProperty().bind(gitStatusButton.disableProperty()
                    .or(newValue.hasRemoteProperty.not()));
//                    .or(newValue.hasUnpushedCommitsProperty.not()));
//
            fetchButton.disableProperty().bind(gitStatusButton.disableProperty()
                    .or(newValue.hasRemoteProperty.not()));
        });
    }

    /**
     * Gets the local branches and populates the branch selector dropdown.
     *
     * @throws NoRepoLoadedException
     * @throws MissingRepoException
     */
    public void updateBranchDropdown() throws NoRepoLoadedException, MissingRepoException{
        RepoHelper currentRepoHelper = this.theModel.getCurrentRepoHelper();
        if(currentRepoHelper==null) throw new NoRepoLoadedException();
        if(!currentRepoHelper.exists()) throw new MissingRepoException();

        this.branchSelector.setVisible(true);

        List<LocalBranchHelper> branches = currentRepoHelper.getLocalBranchesFromManager();
        this.branchSelector.getItems().setAll(branches);

        LocalBranchHelper currentBranch = currentRepoHelper.getCurrentBranch();

        if(currentBranch == null){
            // This block will run when the app first opens and there is no selection in the dropdown.
            // It finds the repoHelper that matches the currently checked-out branch.
            try{
                String branchName = this.theModel.getCurrentRepo().getFullBranch();
                LocalBranchHelper current = new LocalBranchHelper(branchName, this.theModel.getCurrentRepo());
                for(LocalBranchHelper branchHelper : branches){
                    if(branchHelper.getBranchName().equals(current.getBranchName())){
                        currentBranch = current;
                        currentRepoHelper.setCurrentBranch(currentBranch);
                        break;
                    }
                }
            }catch(IOException e){
                this.showGenericErrorNotification();
                e.printStackTrace();
            }
            if(currentBranch != null){
                CommitTreeController.focusCommitInGraph(currentRepoHelper.getCommitByBranchName(currentBranch.refPathString));
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
                this.showInvalidRepoNotification();
            }catch(JGitInternalException e){
                this.showNonEmptyFolderNotification();
            }catch(InvalidRemoteException e){
                this.showInvalidRemoteNotification();
            }catch(TransportException e){
                this.showNotAuthorizedNotification(() -> cloneOption.getOnAction().handle(t));
            }catch(NoRepoSelectedException e){
                // The user pressed cancel on the dialog box. Do nothing!
            }catch(NoOwnerInfoException e){
                this.showNotLoggedInNotification(() -> cloneOption.getOnAction().handle(t));
            }catch(MissingRepoException e){
                this.showMissingRepoNotification();
                updateMenuBarWithRecentRepos();
            }catch(ClassNotFoundException | BackingStoreException e){
                // These should only occur when the recent repo information
                // fails to be loaded or stored, respectively
                // Should be ok to silently fail
            }catch(GitAPIException | IOException e){
                // Somehow, the repository failed to get properly cloned
                // TODO: better error message?
                this.showRepoWasNotLoadedNotification();
            }
        });

        MenuItem existingOption = new MenuItem("Load existing repository");
        existingOption.setOnAction(t -> {
            ExistingRepoHelperBuilder builder = new ExistingRepoHelperBuilder(this.theModel);
            try {
                RepoHelper repoHelper = builder.getRepoHelperFromDialogs();
                this.theModel.openRepoFromHelper(repoHelper);

                this.initPanelViews();
                this.updateUIEnabledStatus();
            } catch (IllegalArgumentException e) {
                this.showInvalidRepoNotification();
            } catch (NoRepoSelectedException e) {
                // The user pressed cancel on the dialog box. Do nothing!
            } catch(NoOwnerInfoException e) {
                this.showNotLoggedInNotification(() -> existingOption.getOnAction().handle(t));
            } catch(BackingStoreException | ClassNotFoundException e) {
                // These should only occur when the recent repo information
                // fails to be loaded or stored, respectively
                // Should be ok to silently fail
            } catch (IOException | GitAPIException e) {
                // Somehow, the repository failed to get properly cloned
                // TODO: better error message?
                this.showRepoWasNotLoadedNotification();
            } catch (MissingRepoException e) {
                this.showMissingRepoNotification();
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

    /**
     * Puts all the model's RepoHelpers into the menubar.
     */
    private void updateMenuBarWithRecentRepos() {
        this.openRecentRepoMenu.getItems().clear();

        List<RepoHelper> repoHelpers = this.theModel.getAllRepoHelpers();
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
                } catch(MissingRepoException e){
                    this.showMissingRepoNotification();
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

            if(this.theModel.getCurrentRepoHelper() == null) throw new NoRepoLoadedException();
            if(!this.theModel.getCurrentRepoHelper().exists()) throw new MissingRepoException();

            Thread th = new Thread(new Task<Void>(){
                @Override
                protected Void call() {
                    try{
                        for(RepoFile checkedFile : workingTreePanelView.getCheckedFilesInDirectory()){
                            checkedFile.updateFileStatusInRepo();
                        }

                        theModel.getCurrentRepoHelper().commit(commitMessage);

                        // Now clear the commit text and a view reload ( or `git status`) to show that something happened
                        commitMessageField.clear();
                        onGitStatusButton();
                    } catch(MissingRepoException | JGitInternalException e){
                        showMissingRepoNotification();
                        setButtonsDisabled(true);
                        updateMenuBarWithRecentRepos();
                    } catch (TransportException e) {
                        showNotAuthorizedNotification(null);
                    } catch (WrongRepositoryStateException e) {
                        System.out.println("Threw a WrongRepositoryStateException");
                        e.printStackTrace();

                        // TODO remove the above debug statements
                        // This should only come up when the user chooses to resolve conflicts in a file.
                        // Do nothing.

                    }catch(GitAPIException e){
                        showGenericErrorNotification();
                        e.printStackTrace();
                    }
                    return null;
                }
            });
            th.setDaemon(true);
            th.setName("Git commit");
            th.start();
        } catch(NoRepoLoadedException e){
            this.showNoRepoLoadedNotification();
            setButtonsDisabled(true);
        }catch(MissingRepoException e){
            this.showMissingRepoNotification();
            setButtonsDisabled(true);
        }
    }

    /**
     * Merges in FETCH_HEAD (after a fetch).
     */
    public void handleMergeFromFetchButton() {
        try{
            if(this.theModel.getCurrentRepoHelper() == null) throw new NoRepoLoadedException();

            Thread th = new Thread(new Task<Void>(){
                @Override
                protected Void call(){
                    try{
                        theModel.getCurrentRepoHelper().mergeFromFetch();
                        onGitStatusButton();
                    }catch(InvalidRemoteException e){
                        showNoRemoteNotification();
                    }catch(TransportException e){
                        showNotAuthorizedNotification(null);
                    }catch(MissingRepoException e){
                        showMissingRepoNotification();
                        setButtonsDisabled(true);
                        updateMenuBarWithRecentRepos();
                    }catch(GitAPIException | IOException e){
                        showGenericErrorNotification();
                        e.printStackTrace();
                    }
                    return null;
                }
            });
            th.setDaemon(true);
            th.setName("Git merge FETCH_HEAD");
            th.start();
        }catch(NoRepoLoadedException e){
            this.showNoRepoLoadedNotification();
            setButtonsDisabled(true);
        }
    }

    /**
     * Performs a `git push`
     */
    public void handlePushButton() {
        try {
            if(this.theModel.getCurrentRepoHelper() == null) throw new NoRepoLoadedException();
            pushButton.setVisible(false);
            pushProgressIndicator.setVisible(true);

            Thread th = new Thread(new Task<Void>(){
                @Override
                protected Void call() {
                    try{
                        theModel.getCurrentRepoHelper().pushAll();
                        onGitStatusButton();
                    }  catch(InvalidRemoteException e){
                        showNoRemoteNotification();
                    } catch (TransportException e) {
                        showNotAuthorizedNotification(null);
                    } catch(MissingRepoException e){
                        showMissingRepoNotification();
                        setButtonsDisabled(true);
                        updateMenuBarWithRecentRepos();
                    } catch(GitAPIException e){
                        showGenericErrorNotification();
                        e.printStackTrace();
                    }finally{
                        pushProgressIndicator.setVisible(false);
                        pushButton.setVisible(true);
                    }
                    return null;
                }
            });
            th.setDaemon(true);
            th.setName("Git push");
            th.start();
        }catch(NoRepoLoadedException e){
            this.showNoRepoLoadedNotification();
            setButtonsDisabled(true);
        }
    }

    /**
     * Performs a `git fetch`
     */
    public void handleFetchButton(){
        try{
            if(this.theModel.getCurrentRepoHelper() == null) throw new NoRepoLoadedException();
            fetchButton.setVisible(false);
            fetchProgressIndicator.setVisible(true);

            Thread th = new Thread(new Task<Void>(){
                @Override
                protected Void call() {
                    try{
                        theModel.getCurrentRepoHelper().fetch();
                        onGitStatusButton();
                    } catch(InvalidRemoteException e){
                        showNoRemoteNotification();
                    } catch (TransportException e) {
                        showNotAuthorizedNotification(null);
                    } catch(MissingRepoException e){
                        showMissingRepoNotification();
                        setButtonsDisabled(true);
                        updateMenuBarWithRecentRepos();
                    } catch(GitAPIException e){
                        showGenericErrorNotification();
                        e.printStackTrace();
                    } finally{
                        fetchProgressIndicator.setVisible(false);
                        fetchButton.setVisible(true);
                    }
                    return null;
                }
            });
            th.setDaemon(true);
            th.setName("Git fetch");
            th.start();
        }catch(NoRepoLoadedException e){
            this.showNoRepoLoadedNotification();
            setButtonsDisabled(true);
        }
    }

    /**
     * Loads the panel views when the "git status" button is clicked.
     */
    public void onGitStatusButton(){
        Thread th = new Thread(new Task<Void>(){
            @Override
            protected Void call(){
                try{
                    workingTreePanelView.drawDirectoryView();
                    localCommitTreeModel.update();
                    remoteCommitTreeModel.update();

                    updateBranchDropdown();
                } catch(MissingRepoException e){
                    showMissingRepoNotification();
                    setButtonsDisabled(true);
                    updateMenuBarWithRecentRepos();
                } catch(NoRepoLoadedException e){
                    showNoRepoLoadedNotification();
                    setButtonsDisabled(true);
                } catch(GitAPIException | IOException e){
                    showGenericErrorNotification();
                    e.printStackTrace();
                }
                return null;
            }
        });
        th.setDaemon(true);
        th.setName("Git status");
        th.start();
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
                if(this.theModel.getCurrentRepoHelper() == null) throw new NoRepoLoadedException();
                if(!this.theModel.getCurrentRepoHelper().exists()) throw new MissingRepoException();

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
            }catch(MissingRepoException e){
                this.showMissingRepoNotification();
                this.setButtonsDisabled(true);
                updateMenuBarWithRecentRepos();
            }catch(NoRepoLoadedException e){
                this.showNoRepoLoadedNotification();
                this.setButtonsDisabled(true);
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
//        commitButton.setDisable(disable);
//        mergeFromFetchButton.setDisable(disable);
//        pushButton.setDisable(disable);
//        fetchButton.setDisable(disable);
        selectAllButton.setDisable(disable);
        deselectAllButton.setDisable(disable);
        remoteCircle.setVisible(!disable);
        commitMessageField.setDisable(disable);
    }

    /**
     * Checks out the branch that is currently selected in the dropdown.
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
            try{
                this.updateBranchDropdown();
            }catch(NoRepoLoadedException e1){
                this.showNoRepoLoadedNotification();
                setButtonsDisabled(true);
            }catch(MissingRepoException e1){
                this.showMissingRepoNotification();
                setButtonsDisabled(true);
                updateMenuBarWithRecentRepos();
            }
        } catch(GitAPIException e){
            this.showGenericErrorNotification();
            e.printStackTrace();
        }
    }

    /**
     * A helper helper method to enable or disable buttons/UI elements
     * depending on whether there is a repo open for the buttons to
     * interact with.
     */
    private void updateUIEnabledStatus() {
        try{
            if(this.theModel.getCurrentRepoHelper() == null && this.theModel.getAllRepoHelpers().size() == 0) {
                // (There's no repo for the buttons to interact with)
                setButtonsDisabled(true);
                this.branchSelector.setVisible(false);
            } else if (this.theModel.getCurrentRepoHelper() == null && this.theModel.getAllRepoHelpers().size() > 0) {
                // (There's no repo for buttons to interact with, but there are repos in the menu bar)
                setButtonsDisabled(true);
                this.branchSelector.setVisible(false);
                this.updateMenuBarWithRecentRepos();
            }else{
                setButtonsDisabled(false);
                this.updateBranchDropdown();
                this.updateMenuBarWithRecentRepos();
                this.updateCurrentRepoLabel();
            }
        }catch(NoRepoLoadedException e){
            this.showNoRepoLoadedNotification();
            setButtonsDisabled(true);
        }catch(MissingRepoException e){
            this.showMissingRepoNotification();
            setButtonsDisabled(true);
            updateMenuBarWithRecentRepos();
        }
    }

    /**
     * Updates the repo label with the current repo's directory name
     */
    private void updateCurrentRepoLabel() {
        String name = this.theModel.getCurrentRepoHelper().toString();
        this.currentRepoLabel.setText(name);
    }

    /**
     * Clears the history stored with the Preferences API.
     *
     * TODO: Come up with better solution?
     *
     * @throws BackingStoreException
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public void clearSavedStuff() throws BackingStoreException, IOException, ClassNotFoundException {
        this.theModel.clearStoredPreferences();
        this.showPrefsClearedNotification();
    }

    /**
     * Creates a new owner and set it as the current default owner.
     */
    public boolean switchUser() {
        // Begin with a nullified RepoOwner:
        RepoOwner newOwner = this.theModel.getDefaultOwner() == null ? new RepoOwner(null, null) : this.theModel.getDefaultOwner();
        boolean switchedLogin = true;

        try {
            newOwner = new RepoOwner();
        } catch (CancelledLoginException e) {
            // User cancelled the login, so we'll leave the owner full of nullness.
            switchedLogin = false;
        }

        RepoHelper currentRepoHelper = theModel.getCurrentRepoHelper();
        if(currentRepoHelper != null){
            currentRepoHelper.setOwner(newOwner);
        }
        this.theModel.setCurrentDefaultOwner(newOwner);
        return switchedLogin;
    }


    public void handleSwitchUserButton(){
        this.switchUser();
    }

    /**
     * Opens the current repo directory (e.g. in Finder or Windows Explorer).
     */
    public void openRepoDirectory(){
        if (Desktop.isDesktopSupported()) {
            try{
                if(this.theModel.getCurrentRepoHelper() == null) throw new NoRepoLoadedException();
                Desktop.getDesktop().open(this.theModel.getCurrentRepoHelper().localPath.toFile());
            }catch(IOException | IllegalArgumentException e){
                this.showFailedToOpenLocalNotification();
            }catch(NoRepoLoadedException e){
                this.showNoRepoLoadedNotification();
                setButtonsDisabled(true);
            }
        }
    }

    /// BEGIN: ERROR NOTIFICATIONS:

    private void showNotLoggedInNotification(Runnable callBack) {
        Platform.runLater(() -> {
            this.notificationPane.setText("You need to log in to do that.");

            Action loginAction = new Action("Enter login info", e -> {
                this.notificationPane.hide();
                if(this.switchUser()){
                    if(callBack != null) callBack.run();
                }
            });

            this.notificationPane.getActions().clear();
            this.notificationPane.getActions().setAll(loginAction);
            this.notificationPane.show();
        });
    }


    private void showNoRepoLoadedNotification() {
        Platform.runLater(() -> {
            this.notificationPane.setText("You need to load a repository before you can perform operations on it!");

            this.notificationPane.getActions().clear();
            this.notificationPane.show();
        });
    }

    private void showInvalidRepoNotification() {
        Platform.runLater(()-> {
            this.notificationPane.setText("Make sure the directory you selected contains an existing Git repository.");

            this.notificationPane.getActions().clear();
            this.notificationPane.show();
        });
    }

    private void showMissingRepoNotification(){
        Platform.runLater(()-> {
            this.notificationPane.setText("That repository no longer exists.");

            this.notificationPane.getActions().clear();
            this.notificationPane.show();
        });
    }

    private void showNoRemoteNotification(){
        Platform.runLater(()-> {
            String name = this.theModel.getCurrentRepoHelper() != null ? this.theModel.getCurrentRepoHelper().toString() : "the current repository";

            this.notificationPane.setText("There is no remote repository associated with " + name);

            this.notificationPane.getActions().clear();
            this.notificationPane.show();
        });
    }

    private void showFailedToOpenLocalNotification(){
        Platform.runLater(()-> {
            String path = this.theModel.getCurrentRepoHelper() != null ? this.theModel.getCurrentRepoHelper().getDirectory().toString() : "the location of the local repository";

            this.notificationPane.setText("Could not open directory at " + path);

            this.notificationPane.getActions().clear();
            this.notificationPane.show();
        });
    }

    private void showNonEmptyFolderNotification() {
        Platform.runLater(()-> {
            this.notificationPane.setText("Make sure the directory you selected is completely empty. The best " +
                                "way to do this is to create a new folder from the directory chooser.");

            this.notificationPane.getActions().clear();
            this.notificationPane.show();
        });
    }

    private void showInvalidRemoteNotification() {
        Platform.runLater(()-> {
            this.notificationPane.setText("Make sure you entered the correct remote URL.");

            this.notificationPane.getActions().clear();
            this.notificationPane.show();
        });
    }

    private void showGenericErrorNotification() {
        Platform.runLater(()-> {
            this.notificationPane.setText("Sorry, there was an error.");

            this.notificationPane.getActions().clear();
            this.notificationPane.show();
        });
    }

    private void showNotAuthorizedNotification(Runnable callback) {
        Platform.runLater(() -> {
            this.notificationPane.setText("The login information you gave does not allow you to modify this repository. Try switching your login and trying again.");

            Action loginAction = new Action("Log in", e -> {
                this.notificationPane.hide();
                if(this.switchUser()){
                    if(callback != null) callback.run();
                }
            });

            this.notificationPane.getActions().clear();
            this.notificationPane.getActions().setAll(loginAction);
            this.notificationPane.show();
        });
    }

    private void showRepoWasNotLoadedNotification() {
        Platform.runLater(()-> {
            this.notificationPane.setText("No repository was loaded.");

            this.notificationPane.getActions().clear();
            this.notificationPane.show();
        });
    }

    private void showPrefsClearedNotification() {
        Platform.runLater(()-> {
            this.notificationPane.setText("Your recent repositories have been cleared. Restart the app for changes to take effect.");

            this.notificationPane.getActions().clear();
            this.notificationPane.show();
        });
    }

    private void showCheckoutConflictsNotification(List<String> conflictingPaths) {
        Platform.runLater(() -> {
            String conflictList = "";
            for(String pathName : conflictingPaths){
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
        });
    }

    // END: ERROR NOTIFICATIONS ^^^

    /**
     * Opens up the current repo helper's Branch Manager window.
     */
    public void showBranchChooser() {
        try{
            if(this.theModel.getCurrentRepoHelper() == null) throw new NoRepoLoadedException();
            this.theModel.getCurrentRepoHelper().getBranchManager().showBranchChooserWindow();
        }catch(IOException e){
            this.showGenericErrorNotification();
            e.printStackTrace();
        }catch(NoRepoLoadedException e){
            this.showNoRepoLoadedNotification();
            setButtonsDisabled(true);
        }
    }

    public void selectCommit(String id){
        CommitHelper commit = this.theModel.getCurrentRepoHelper().getCommit(id);
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

    /**
     * Selects all files in the working tree for a commit.
     *
     */
    public void onSelectAllButton() {
        this.workingTreePanelView.setAllFilesSelected(true);
    }

    /**
     * Deselects all files in the working tree for a commit.
     *
     */
    public void onDeselectAllButton() {
        this.workingTreePanelView.setAllFilesSelected(false);
    }
}
