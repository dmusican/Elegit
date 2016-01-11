package elegit;

import de.jensd.fx.glyphs.GlyphsDude;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import elegit.exceptions.*;
import org.controlsfx.control.CheckListView;
import org.controlsfx.control.NotificationPane;
import org.controlsfx.control.PopOver;
import org.controlsfx.control.action.Action;
import org.eclipse.jgit.api.errors.*;
import org.eclipse.jgit.errors.NoMergeBaseException;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;
import java.util.prefs.BackingStoreException;

/**
 * The controller for the entire session.
 */
public class SessionController {

    public ComboBox<LocalBranchHelper> branchDropdownSelector;

    public ComboBox<RepoHelper> repoDropdownSelector;
    public Button loadNewRepoButton;
    public Button removeRecentReposButton;

    private SessionModel theModel;

    public Node root;

    public NotificationPane notificationPane;
    public Button selectAllButton;
    public Button deselectAllButton;
    public Button switchUserButton;

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

    public Tab workingTreePanelTab;
    public Tab allFilesPanelTab;
    public WorkingTreePanelView workingTreePanelView;
    public AllFilesPanelView allFilesPanelView;

	public CommitTreePanelView localCommitTreePanelView;
    public CommitTreePanelView remoteCommitTreePanelView;

    public Circle remoteCircle;

    public Label commitInfoNameText;
    public Label commitInfoAuthorText;
    public Label commitInfoDateText;
    public Button commitInfoNameCopyButton;
    public Button commitInfoGoToButton;
    public TextArea commitInfoMessageText;

    CommitTreeModel localCommitTreeModel;
    CommitTreeModel remoteCommitTreeModel;

    BooleanProperty isWorkingTreeTabSelected;

    private volatile boolean isRecentRepoEventListenerBlocked = false;

    /**
     * Initializes the environment by obtaining the model
     * and putting the views on display.
     *
     * This method is automatically called by JavaFX.
     */
    public void initialize() {
        this.theModel = SessionModel.getSessionModel();

        this.initializeLayoutParameters();

        CommitTreeController.sessionController = this;

        this.workingTreePanelView.setSessionModel(this.theModel);
        this.allFilesPanelView.setSessionModel(this.theModel);

        isWorkingTreeTabSelected = new SimpleBooleanProperty(true);
        isWorkingTreeTabSelected.bind(workingTreePanelTab.selectedProperty());
        workingTreePanelTab.getTabPane().getSelectionModel().select(workingTreePanelTab);

        this.localCommitTreeModel = new LocalCommitTreeModel(this.theModel, this.localCommitTreePanelView);
        this.remoteCommitTreeModel = new RemoteCommitTreeModel(this.theModel, this.remoteCommitTreePanelView);

        // Add FontAwesome icons to buttons:
        Text openExternallyIcon = GlyphsDude.createIcon(FontAwesomeIcon.EXTERNAL_LINK);
        openExternallyIcon.setFill(javafx.scene.paint.Color.WHITE);
        this.openRepoDirButton.setGraphic(openExternallyIcon);
        this.openRepoDirButton.setTooltip(new Tooltip("Open repository directory"));

        Text plusIcon = GlyphsDude.createIcon(FontAwesomeIcon.PLUS);
        plusIcon.setFill(Color.WHITE);
        this.loadNewRepoButton.setGraphic(plusIcon);

        Text minusIcon = GlyphsDude.createIcon(FontAwesomeIcon.MINUS);
        minusIcon.setFill(Color.WHITE);
        this.removeRecentReposButton.setGraphic(minusIcon);
        this.removeRecentReposButton.setTooltip(new Tooltip("Clear shortcuts to recently opened repos"));

        Text userIcon = GlyphsDude.createIcon(FontAwesomeIcon.USER);
        userIcon.setFill(Color.WHITE);
        this.switchUserButton.setGraphic(userIcon);

        Text branchIcon = GlyphsDude.createIcon(FontAwesomeIcon.CODE_FORK);
        branchIcon.setFill(Color.WHITE);
        this.branchesButton.setGraphic(branchIcon);

        Text clipboardIcon = GlyphsDude.createIcon(FontAwesomeIcon.CLIPBOARD);
        clipboardIcon.setFill(Color.WHITE);
        this.commitInfoNameCopyButton.setGraphic(clipboardIcon);

        Text goToIcon = GlyphsDude.createIcon(FontAwesomeIcon.ARROW_CIRCLE_LEFT);
        goToIcon.setFill(Color.WHITE);
        this.commitInfoGoToButton.setGraphic(goToIcon);

        // Set up the "+" button for loading new repos (give it a menu)
        Text downloadIcon = GlyphsDude.createIcon(FontAwesomeIcon.CLOUD_DOWNLOAD);
        MenuItem cloneOption = new MenuItem("Clone repository", downloadIcon);
        cloneOption.setOnAction(t -> {
            if(this.theModel.getDefaultOwner() == null) this.showNotLoggedInNotification(() -> handleLoadRepoMenuItem(new ClonedRepoHelperBuilder(this.theModel)));
            else handleLoadRepoMenuItem(new ClonedRepoHelperBuilder(this.theModel));
        });

        Text folderOpenIcon = GlyphsDude.createIcon(FontAwesomeIcon.FOLDER_OPEN);
        MenuItem existingOption = new MenuItem("Load existing repository", folderOpenIcon);
        existingOption.setOnAction(t -> handleLoadRepoMenuItem(new ExistingRepoHelperBuilder(this.theModel)));
        ContextMenu newRepoOptionsMenu = new ContextMenu(cloneOption, existingOption);

        this.loadNewRepoButton.setOnAction(e -> newRepoOptionsMenu.show(this.loadNewRepoButton, Side.BOTTOM ,0, 0));
        this.loadNewRepoButton.setTooltip(new Tooltip("Load a new repository"));

        // Buttons start out disabled, since no repo is loaded
        this.setButtonsDisabled(true);

        // Branch selector and trigger button starts invisible, since there's no repo and no branches
        this.branchDropdownSelector.setVisible(false);

        this.theModel.loadRecentRepoHelpersFromStoredPathStrings();
        this.theModel.loadMostRecentRepoHelper();

        this.initPanelViews();
        this.updateUIEnabledStatus();
        this.setRecentReposDropdownToCurrentRepo();
        this.refreshRecentReposInDropdown();

        RepositoryMonitor.beginWatchingRemote(theModel);
        RepositoryMonitor.hasFoundNewRemoteChanges.addListener((observable, oldValue, newValue) -> {
            if(newValue) showNewRemoteChangesNotification();
        });
        RepositoryMonitor.beginWatchingLocal(this, theModel);
    }

    /**
     * Sets up the layout parameters for things that cannot be set in FXML
     */
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
        allFilesPanelView.setMinSize(Control.USE_PREF_SIZE, 200);
        commitMessageField.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);

        branchDropdownSelector.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);

        final int REPO_DROPDOWN_MAX_WIDTH = 147;
        repoDropdownSelector.setMaxWidth(REPO_DROPDOWN_MAX_WIDTH);

        remoteCommitTreePanelView.heightProperty().addListener((observable, oldValue, newValue) -> {
            remoteCircle.setCenterY(newValue.doubleValue() / 2.0);
            if(oldValue.doubleValue() == 0){
                remoteCircle.setRadius(newValue.doubleValue() / 4.0);
            }
        });

        commitInfoNameCopyButton.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
        commitInfoGoToButton.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);

        commitInfoNameText.maxWidthProperty().bind(commitInfoMessageText.widthProperty()
                .subtract(commitInfoGoToButton.widthProperty())
                .subtract(commitInfoNameCopyButton.widthProperty())
                .subtract(10)); // The gap between each button and this label is 5

    }

    /**
     * Gets the local branches and populates the branch selector dropdown.
     *
     * @throws NoRepoLoadedException
     * @throws MissingRepoException
     */
    public void updateBranchDropdown() throws NoRepoLoadedException, MissingRepoException, IOException, GitAPIException {
        RepoHelper currentRepoHelper = this.theModel.getCurrentRepoHelper();
        if(currentRepoHelper==null) throw new NoRepoLoadedException();
        if(!currentRepoHelper.exists()) throw new MissingRepoException();

        List<LocalBranchHelper> branches = currentRepoHelper.callGitForLocalBranches();

        currentRepoHelper.refreshCurrentBranch();
        LocalBranchHelper currentBranch = currentRepoHelper.getCurrentBranch();

        Platform.runLater(() -> {
            this.branchDropdownSelector.setVisible(true);
            this.branchDropdownSelector.getItems().setAll(branches);
            if(this.branchDropdownSelector.getValue() == null || !this.branchDropdownSelector.getValue().getBranchName().equals(currentBranch.getBranchName())){
                this.branchDropdownSelector.setValue(currentBranch);
            }
        });
    }

    /**
     * Called when a selection is made from the 'Load New Repository' menu. Creates a new repository
     * using the given builder and updates the UI
     * @param builder the builder to use to create a new repository
     */
    private synchronized void handleLoadRepoMenuItem(RepoHelperBuilder builder){
        try{
            RepoHelper repoHelper = builder.getRepoHelperFromDialogs();
            BusyWindow.show();
            RepositoryMonitor.pause();
            Thread th = new Thread(new Task<Void>(){
                @Override
                protected Void call() {
                    try {
                        refreshRecentReposInDropdown();
                        theModel.openRepoFromHelper(repoHelper);
                        setRecentReposDropdownToCurrentRepo();

                        initPanelViews();
                        updateUIEnabledStatus();
                    } catch(BackingStoreException | ClassNotFoundException e) {
                        // These should only occur when the recent repo information
                        // fails to be loaded or stored, respectively
                        // Should be ok to silently fail
                    } catch (MissingRepoException e) {
                        showMissingRepoNotification();
                        refreshRecentReposInDropdown();
                    } catch (IOException e) {
                        // Somehow, the repository failed to get properly loaded
                        // TODO: better error message?
                        showRepoWasNotLoadedNotification();
                    } finally{
                        BusyWindow.hide();
                        RepositoryMonitor.unpause();
                    }
                    return null;
                }
            });
            th.setDaemon(true);
            th.setName("Loading existing/cloning repository");
            th.start();
        } catch (IllegalArgumentException e) {
            showInvalidRepoNotification();
            e.printStackTrace();
        } catch(NoOwnerInfoException e) {
            showNotLoggedInNotification(() -> handleLoadRepoMenuItem(builder));
        } catch(JGitInternalException e){
            showNonEmptyFolderNotification(() -> handleLoadRepoMenuItem(builder));
        } catch(InvalidRemoteException e){
            showInvalidRemoteNotification(() -> handleLoadRepoMenuItem(builder));
        } catch(TransportException e){
            showNotAuthorizedNotification(() -> handleLoadRepoMenuItem(builder));
        } catch (NoRepoSelectedException e) {
            // The user pressed cancel on the dialog box. Do nothing!
        } catch(IOException | GitAPIException e){
            // Somehow, the repository failed to get properly loaded
            // TODO: better error message?
            showRepoWasNotLoadedNotification();
        }
    }

    /**
     * Gets the current RepoHelper and sets it as the selected value of the dropdown.
     */
    @FXML
    private void setRecentReposDropdownToCurrentRepo() {
        Platform.runLater(() -> {
            isRecentRepoEventListenerBlocked = true;
            RepoHelper currentRepo = this.theModel.getCurrentRepoHelper();
            this.repoDropdownSelector.setValue(currentRepo);
            isRecentRepoEventListenerBlocked = false;
        });
    }

    /**
     * Adds all the model's RepoHelpers to the dropdown
     */
    @FXML
    private void refreshRecentReposInDropdown() {
        List<RepoHelper> repoHelpers = this.theModel.getAllRepoHelpers();
        Platform.runLater(() -> this.repoDropdownSelector.setItems(FXCollections.observableArrayList(repoHelpers)));
    }

    /**
     * Loads the given repository and updates the UI accordingly.
     * @param repoHelper the repository to open
     */
    private synchronized void handleRecentRepoMenuItem(RepoHelper repoHelper){
        if(isRecentRepoEventListenerBlocked) return;
        BusyWindow.show();
        RepositoryMonitor.pause();
        Thread th = new Thread(new Task<Void>(){
            @Override
            protected Void call() throws Exception{
                try {
                    theModel.openRepoFromHelper(repoHelper);

                    initPanelViews();
                    updateUIEnabledStatus();
                } catch (IOException e) {
                    // Somehow, the repository failed to get properly loaded
                    // TODO: better error message?
                    showRepoWasNotLoadedNotification();
                } catch(MissingRepoException e){
                    showMissingRepoNotification();
                    refreshRecentReposInDropdown();
                } catch (BackingStoreException | ClassNotFoundException e) {
                    // These should only occur when the recent repo information
                    // fails to be loaded or stored, respectively
                    // Should be ok to silently fail
                } finally{
                    BusyWindow.hide();
                    RepositoryMonitor.unpause();
                }
                return null;
            }
        });
        th.setDaemon(true);
        th.setName("Open repository from recent list");
        th.start();
    }

    /**
     * A helper method that grabs the currently selected repo in the repo dropdown
     * and loads it using the handleRecentRepoMenuItem(...) method.
     */
    public void loadSelectedRepo() {
        RepoHelper selectedRepoHelper = this.repoDropdownSelector.getValue();
        this.handleRecentRepoMenuItem(selectedRepoHelper);
    }

    /**
     * Perform the updateFileStatusInRepo() method for each file whose
     * checkbox is checked. Then commit with the commit message and push.
     */
    public void handleCommitButton() {
        try {
            if(this.theModel.getCurrentRepoHelper() == null) throw new NoRepoLoadedException();
            if(!this.theModel.getCurrentRepoHelper().exists()) throw new MissingRepoException();

            String commitMessage = commitMessageField.getText();

            if(!workingTreePanelView.isAnyFileSelected()) throw new NoFilesStagedForCommitException();
            if(commitMessage.length() == 0) throw new NoCommitMessageException();

            Thread th = new Thread(new Task<Void>(){
                @Override
                protected Void call() {
                    try{
                        boolean canCommit = true;
                        for(RepoFile checkedFile : workingTreePanelView.getCheckedFilesInDirectory()){
                            canCommit = canCommit && checkedFile.updateFileStatusInRepo();
                        }

                        if(canCommit) {
                            theModel.getCurrentRepoHelper().commit(commitMessage);

                            // Now clear the commit text and a view reload ( or `git status`) to show that something happened
                            commitMessageField.clear();
                            gitStatus();
                        }
                    } catch(JGitInternalException e){
                        showGenericErrorNotification();
                        e.printStackTrace();
                    } catch(MissingRepoException e){
                        showMissingRepoNotification();
                        setButtonsDisabled(true);
                        refreshRecentReposInDropdown();
                    } catch (TransportException e) {
                        showNotAuthorizedNotification(null);
                    } catch (WrongRepositoryStateException e) {
                        showGenericErrorNotification();
                        e.printStackTrace();

                        // TODO remove the above debug statements
                        // This should hopefully not appear any more. Previously occurred when attempting to resolve
                        // conflicts in an external editor
                        // Do nothing.

                    } catch(GitAPIException | IOException e){
                        // Git error, or error presenting the file chooser window
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
        } catch(MissingRepoException e){
            this.showMissingRepoNotification();
            setButtonsDisabled(true);
            refreshRecentReposInDropdown();
        } catch(NoCommitMessageException e){
            this.showNoCommitMessageNotification();
        }catch(NoFilesStagedForCommitException e){
            this.showNoFilesStagedForCommitNotification();
        }
    }

    /**
     * Merges in FETCH_HEAD (after a fetch).
     */
    public void handleMergeFromFetchButton() {
        try{
            if(this.theModel.getCurrentRepoHelper() == null) throw new NoRepoLoadedException();
            if(!this.theModel.getCurrentRepoHelper().hasUnmergedCommits()) throw new NoCommitsToMergeException();

            Thread th = new Thread(new Task<Void>(){
                @Override
                protected Void call(){
                    try{
                        if(!theModel.getCurrentRepoHelper().mergeFromFetch()){
                            showUnsuccessfulMergeNotification();
                        }
                        gitStatus();
                    } catch(InvalidRemoteException e){
                        showNoRemoteNotification();
                    } catch(TransportException e){
                        showNotAuthorizedNotification(null);
                    } catch (NoMergeBaseException | JGitInternalException e) {
                        // Merge conflict
                        System.out.println("*****");
                        e.printStackTrace();
                        // todo: figure out rare NoMergeBaseException.
                        //  Has something to do with pushing conflicts.
                        //  At this point in the stack, it's caught as a JGitInternalException.
                    } catch(CheckoutConflictException e){
                        showMergingWithChangedFilesNotification();
                    } catch(ConflictingFilesException e){
                        showMergeConflictsNotification(e.getConflictingFiles());
                    } catch(MissingRepoException e){
                        showMissingRepoNotification();
                        setButtonsDisabled(true);
                        refreshRecentReposInDropdown();
                    } catch(GitAPIException | IOException e){
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
        }catch(NoCommitsToMergeException e){
            this.showNoCommitsToMergeNotification();
        }
    }

    /**
     * Performs a `git push`
     */
    public void handlePushButton() {
        try {
            if(this.theModel.getCurrentRepoHelper() == null) throw new NoRepoLoadedException();
            if(!this.theModel.getCurrentRepoHelper().hasUnpushedCommits()) throw new NoCommitsToPushException();

            pushButton.setVisible(false);
            pushProgressIndicator.setVisible(true);

            Thread th = new Thread(new Task<Void>(){
                @Override
                protected Void call() {
                    try{
                        RepositoryMonitor.resetFoundNewChanges(false);
                        theModel.getCurrentRepoHelper().pushAll();
                        gitStatus();
                    }  catch(InvalidRemoteException e){
                        showNoRemoteNotification();
                    } catch(PushToAheadRemoteError e) {
                        showPushToAheadRemoteNotification(e.isAllRefsRejected());
                    } catch (TransportException e) {
                        if (e.getMessage().contains("git-receive-pack not found")) {
                            // The error has this message if there is no longer a remote to push to
                            showLostRemoteNotification();
                        } else {
                            showNotAuthorizedNotification(null);
                        }
                    } catch(MissingRepoException e){
                        showMissingRepoNotification();
                        setButtonsDisabled(true);
                        refreshRecentReposInDropdown();
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
        }catch(NoCommitsToPushException e){
            this.showNoCommitsToPushNotification();
        }
    }

    /**
     * Handles a click on the "Fetch" button. Calls gitFetch()
     */
    public void handleFetchButton(){
        gitFetch();
    }

    /**
     * Queries the remote for new commits, and updates the local
     * remote as necessary.
     * Equivalent to `git fetch`
     */
    public synchronized void gitFetch(){
        try{
            if(this.theModel.getCurrentRepoHelper() == null) throw new NoRepoLoadedException();

            fetchButton.setVisible(false);
            fetchProgressIndicator.setVisible(true);

            Thread th = new Thread(new Task<Void>(){
                @Override
                protected Void call() {
                    try{
                        RepositoryMonitor.resetFoundNewChanges(false);
                        if(!theModel.getCurrentRepoHelper().fetch()){
                            showNoCommitsFetchedNotification();
                        }
                        gitStatus();
                    } catch(InvalidRemoteException e){
                        showNoRemoteNotification();
                    } catch (TransportException e) {
                        showNotAuthorizedNotification(null);
                    } catch(MissingRepoException e){
                        showMissingRepoNotification();
                        setButtonsDisabled(true);
                        refreshRecentReposInDropdown();
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
     * Updates the panel views when the "git status" button is clicked.
     * Highlights the current HEAD.
     */
    public void onGitStatusButton(){
        this.gitStatus();
        CommitTreeController.focusCommitInGraph(theModel.getCurrentRepoHelper().getHead());
    }

    /**
     * Updates the trees, changed files, and branch information. Equivalent
     * to 'git status'
     *
     * See initPanelViews for Thread information
     */
    public synchronized void gitStatus(){
        RepositoryMonitor.pause();
        Thread th = new Thread(new Task<Void>(){
            @Override
            protected Void call(){
                try{
                    localCommitTreeModel.update();
                    remoteCommitTreeModel.update();

                    workingTreePanelView.drawDirectoryView();
                    allFilesPanelView.drawDirectoryView();
                    updateBranchDropdown();
                } catch(MissingRepoException e){
                    showMissingRepoNotification();
                    setButtonsDisabled(true);
                    refreshRecentReposInDropdown();
                } catch(NoRepoLoadedException e){
                    showNoRepoLoadedNotification();
                    setButtonsDisabled(true);
                } catch(GitAPIException | IOException e){
                    showGenericErrorNotification();
                    e.printStackTrace();
                }finally{
                    RepositoryMonitor.unpause();
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
                this.refreshRecentReposInDropdown();
            }catch(NoRepoLoadedException e){
                this.showNoRepoLoadedNotification();
                this.setButtonsDisabled(true);
            }
        }
    }

    /**
     * Initializes each panel of the view
     */
	private synchronized void initPanelViews() {
        BusyWindow.show();

        try {
            workingTreePanelView.drawDirectoryView();
            allFilesPanelView.drawDirectoryView();
            localCommitTreeModel.init();
            remoteCommitTreeModel.init();
        } catch (GitAPIException | IOException e) {
            showGenericErrorNotification();
        }

        BusyWindow.hide();
    }

    /**
     * A helper method for enabling/disabling buttons.
     *
     * @param disable a boolean for whether or not to disable the buttons.
     */
    private void setButtonsDisabled(boolean disable) {
        Platform.runLater(() -> {
            openRepoDirButton.setDisable(disable);
            gitStatusButton.setDisable(disable);
            commitButton.setDisable(disable);
            mergeFromFetchButton.setDisable(disable);
            pushButton.setDisable(disable);
            fetchButton.setDisable(disable);
            selectAllButton.setDisable(disable);
            deselectAllButton.setDisable(disable);
            remoteCircle.setVisible(!disable);
            commitMessageField.setDisable(disable);
        });
    }

    /**
     * Checks out the branch that is currently selected in the dropdown.
     */
    public void loadSelectedBranch() {
        LocalBranchHelper selectedBranch = this.branchDropdownSelector.getValue();
        if(selectedBranch == null) return;
        Thread th = new Thread(new Task<Void>(){
            @Override
            protected Void call() {

                try{
                    // This is an edge case for new local repos.
                    //
                    // When a repo is first initialized,the `master` branch is checked-out,
                    //  but it is "unborn" -- it doesn't exist yet in the `refs/heads` folder
                    //  until there are commits.
                    //
                    // (see http://stackoverflow.com/a/21255920/5054197)
                    //
                    // So, check that there are refs in the refs folder (if there aren't, do nothing):
                    String gitDirString = theModel.getCurrentRepo().getDirectory().toString();
                    Path refsHeadsFolder = Paths.get(gitDirString + "/refs/heads");
                    DirectoryStream<Path> pathStream = Files.newDirectoryStream(refsHeadsFolder);
                    Iterator<Path> pathStreamIterator = pathStream.iterator();

                    if (pathStreamIterator.hasNext()){ // => There ARE branch refs in the folder
                        selectedBranch.checkoutBranch();
                        CommitTreeController.focusCommitInGraph(selectedBranch.getHead());
                    }
                }catch(CheckoutConflictException e){
                    showCheckoutConflictsNotification(e.getConflictingPaths());
                    try{
                        updateBranchDropdown();
                    }catch(NoRepoLoadedException e1){
                        showNoRepoLoadedNotification();
                        setButtonsDisabled(true);
                    }catch(MissingRepoException e1){
                        showMissingRepoNotification();
                        setButtonsDisabled(true);
                        refreshRecentReposInDropdown();
                    }catch(GitAPIException | IOException e1){
                        showGenericErrorNotification();
                        e1.printStackTrace();
                    }
                }catch(GitAPIException | IOException e){
                    showGenericErrorNotification();
                    e.printStackTrace();
                }
                return null;
            }
        });
        th.setDaemon(true);
        th.setName("Branch Checkout");
        th.start();
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
                Platform.runLater(() -> this.branchDropdownSelector.setVisible(false));
            } else if (this.theModel.getCurrentRepoHelper() == null && this.theModel.getAllRepoHelpers().size() > 0) {
                // (There's no repo for buttons to interact with, but there are repos in the menu bar)
                setButtonsDisabled(true);
                Platform.runLater(() -> this.branchDropdownSelector.setVisible(false));
            }else{
                setButtonsDisabled(false);
                this.updateBranchDropdown();
                this.updateLoginButtonText();
            }
        }catch(NoRepoLoadedException e){
            this.showNoRepoLoadedNotification();
            setButtonsDisabled(true);
        }catch(MissingRepoException e){
            this.showMissingRepoNotification();
            setButtonsDisabled(true);
            this.refreshRecentReposInDropdown();
        } catch (GitAPIException | IOException e) {
            this.showGenericErrorNotification();
            e.printStackTrace();
        }
    }

    /**
     * Creates a new owner and sets it as the current default owner.
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
        this.updateLoginButtonText();
        return switchedLogin;
    }

    /**
     * Called when the switch user button is clicked. See switchUser
     */
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

    private void showGenericErrorNotification() {
        Platform.runLater(()-> {
            this.notificationPane.setText("Sorry, there was an error.");

            this.notificationPane.getActions().clear();
            this.notificationPane.show();
        });
    }

    private void showNotLoggedInNotification(Runnable callBack) {
        Platform.runLater(() -> {
            this.notificationPane.setText("You need to log in first in order to clone a repository.");

            Action loginAction = new Action("Log in", e -> {
                this.notificationPane.hide();
                if (this.switchUser()) {
                    if (callBack != null) callBack.run();
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
        Platform.runLater(() -> {
            this.notificationPane.setText("Make sure the directory you selected contains an existing (non-bare) Git repository.");

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
            String path = this.theModel.getCurrentRepoHelper() != null ? this.theModel.getCurrentRepoHelper().getLocalPath().toString() : "the location of the local repository";

            this.notificationPane.setText("Could not open directory at " + path);

            this.notificationPane.getActions().clear();
            this.notificationPane.show();
        });
    }

    private void showNonEmptyFolderNotification(Runnable callback) {
        Platform.runLater(()-> {
            this.notificationPane.setText("Make sure a folder with that name doesn't already exist in that location");

            Action okAction = new Action("OK", e -> {
                this.notificationPane.hide();
                if(callback != null) callback.run();
            });

            this.notificationPane.getActions().clear();
            this.notificationPane.getActions().setAll(okAction);
            this.notificationPane.show();
        });
    }

    private void showInvalidRemoteNotification(Runnable callback) {
        Platform.runLater(() -> {
            this.notificationPane.setText("Make sure you entered the correct remote URL.");

            Action okAction = new Action("OK", e -> {
                this.notificationPane.hide();
                if(callback != null) callback.run();
            });

            this.notificationPane.getActions().clear();
            this.notificationPane.getActions().setAll(okAction);
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
        Platform.runLater(() -> {
            this.notificationPane.setText("No repository was loaded.");

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

    private void showMergeConflictsNotification(List<String> conflictingPaths){
        Platform.runLater(() -> {
            String conflictList = "";
            for(String pathName : conflictingPaths){
                conflictList += "\n" + pathName;
            }
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Conflicting files");
            alert.setHeaderText("Can't complete merge");
            alert.setContentText("There were conflicts in the following files: "
                    + conflictList);

            this.notificationPane.setText("Can't complete merge due to conflicts. Resolve the conflicts and commit all files to complete merging");

            Action seeConflictsAction = new Action("See conflicting files", e -> {
                this.notificationPane.hide();
                alert.showAndWait();
            });

            this.notificationPane.getActions().clear();
            this.notificationPane.getActions().setAll(seeConflictsAction);

            this.notificationPane.show();
        });
    }


    private void showPushToAheadRemoteNotification(boolean allRefsRejected){
        Platform.runLater(() -> {
            if(allRefsRejected){
                this.notificationPane.setText("The remote repository is ahead of the local. You need to fetch and then merge (pull) before pushing.");
            }else{
                this.notificationPane.setText("You need to merge in order to push all of your changes.");
            }

            this.notificationPane.getActions().clear();
            this.notificationPane.show();
        });
    }

    private void showLostRemoteNotification() {
        Platform.runLater(() -> {
            this.notificationPane.setText("The push failed because the remote repository couldn't be found.");

            this.notificationPane.getActions().clear();
            this.notificationPane.show();
        });
    }

    private void showUnsuccessfulMergeNotification(){
        Platform.runLater(() -> {
            this.notificationPane.setText("Merging failed");

            this.notificationPane.getActions().clear();
            this.notificationPane.show();
        });
    }

    private void showNewRemoteChangesNotification(){
        Platform.runLater(() -> {
            this.notificationPane.setText("There are new changes in the remote repository.");

            Action fetchAction = new Action("Fetch", e -> {
                this.notificationPane.hide();
                gitFetch();
            });

            Action ignoreAction = new Action("Ignore", e -> {
                this.notificationPane.hide();
                RepositoryMonitor.resetFoundNewChanges(true);
            });

            this.notificationPane.getActions().clear();
            this.notificationPane.getActions().setAll(fetchAction, ignoreAction);

            this.notificationPane.show();
        });
    }

    private void showNoFilesStagedForCommitNotification(){
        Platform.runLater(() -> {
            this.notificationPane.setText("You need to select which files to commit");

            this.notificationPane.getActions().clear();
            this.notificationPane.show();
        });
    }

    private void showNoCommitMessageNotification(){
        Platform.runLater(() -> {
            this.notificationPane.setText("You need to write a commit message in order to commit your changes");

            this.notificationPane.getActions().clear();
            this.notificationPane.show();
        });
    }

    private void showNoCommitsToPushNotification(){
        Platform.runLater(() -> {
            this.notificationPane.setText("There aren't any local commits to push");

            this.notificationPane.getActions().clear();
            this.notificationPane.show();
        });
    }

    private void showNoCommitsToMergeNotification(){
        Platform.runLater(() -> {
            this.notificationPane.setText("There aren't any commits to merge. Try fetching first");

            this.notificationPane.getActions().clear();
            this.notificationPane.show();
        });
    }

    private void showNoCommitsFetchedNotification(){
        Platform.runLater(() -> {
            this.notificationPane.setText("No new commits were fetched");

            this.notificationPane.getActions().clear();
            this.notificationPane.show();
        });
    }

    private void showMergingWithChangedFilesNotification(){
        Platform.runLater(() -> {
            this.notificationPane.setText("Can't merge with modified files present");

            // TODO: I think some sort of help text would be nice here, so they know what to do

            this.notificationPane.getActions().clear();
            this.notificationPane.show();
        });
    }

    // END: ERROR NOTIFICATIONS ^^^

    /**
     * Opens up the current repo helper's Branch Manager window after
     * passing in this SessionController object, so that the
     * BranchManagerController can update the main window's views.
     */
    public void showBranchManager() {
        try{
            if(this.theModel.getCurrentRepoHelper() == null) throw new NoRepoLoadedException();
            this.theModel.getCurrentRepoHelper().showBranchManagerWindow();
        }catch(IOException e){
            this.showGenericErrorNotification();
            e.printStackTrace();
        }catch(NoRepoLoadedException e){
            this.showNoRepoLoadedNotification();
            setButtonsDisabled(true);
        }
    }

    /**
     * Opens up the help page to inform users about what symbols mean
     */
    public void showLegend() {
        try{
            // Create and display the Stage:
            NotificationPane fxmlRoot = FXMLLoader.load(getClass().getResource("/elegit/fxml/Legend.fxml"));

            Stage stage = new Stage();
            stage.setTitle("Legend");
            stage.setScene(new Scene(fxmlRoot, 250, 300));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.show();
        }catch(IOException e) {
            this.showGenericErrorNotification();
            e.printStackTrace();
        }
    }

    /**
     * Displays information about the commit with the given id
     * @param id the selected commit
     */
    public void selectCommit(String id){
        Platform.runLater(() -> {
            CommitHelper commit = this.theModel.getCurrentRepoHelper().getCommit(id);
            commitInfoNameText.setText(commit.getName());
            commitInfoAuthorText.setText(commit.getAuthorName());
            commitInfoDateText.setText(commit.getFormattedWhen());
            commitInfoMessageText.setVisible(true);
            commitInfoNameCopyButton.setVisible(true);
            commitInfoGoToButton.setVisible(true);

            String s = "";
            for (BranchHelper branch : commit.getBranchesAsHead()) {
                if (branch instanceof RemoteBranchHelper) {
                    s = s + "origin/";
                }
                s = s + branch.getBranchName() + "\n";
            }
            if (s.length() > 0) {
                commitInfoMessageText.setText("Head of branches: \n" + s + "\n\n" + commit.getMessage(true));
            } else {
                commitInfoMessageText.setText(commit.getMessage(true));
            }
        });
    }

    /**
     * Stops displaying commit information
     */
    public void clearSelectedCommit(){
        Platform.runLater(() -> {
            commitInfoNameText.setText("");
            commitInfoAuthorText.setText("");
            commitInfoDateText.setText("");
            commitInfoMessageText.setText("");
            commitInfoMessageText.setVisible(false);
            commitInfoNameCopyButton.setVisible(false);
            commitInfoGoToButton.setVisible(false);
        });
    }

    /**
     * Copies the commit hash onto the clipboard
     */
    public void handleCommitNameCopyButton(){
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(commitInfoNameText.getText());
        clipboard.setContent(content);
    }

    /**
     * Jumps to the selected commit in the tree display
     */
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

    public void chooseRecentReposToDelete() {
        List<RepoHelper> repoHelpers = this.theModel.getAllRepoHelpers();
        CheckListView<RepoHelper> repoCheckListView = new CheckListView<>(FXCollections.observableArrayList(repoHelpers));

        // Remove the currently checked out repo:
        RepoHelper currentRepo = this.theModel.getCurrentRepoHelper();
        repoCheckListView.getItems().remove(currentRepo);

        Button removeSelectedButton = new Button("Remove repository shortcuts from Elegit");

        PopOver popover = new PopOver(new VBox(repoCheckListView, removeSelectedButton));
        popover.setTitle("Manage Recent Repositories");

        removeSelectedButton.setOnAction(e -> {
            List<RepoHelper> checkedItems = repoCheckListView.getCheckModel().getCheckedItems();
            this.theModel.removeRepoHelpers(checkedItems);
            popover.hide();
            this.refreshRecentReposInDropdown();
        });

        popover.show(this.removeRecentReposButton);
    }

    private void updateLoginButtonText() {
        Platform.runLater(() -> {
            if(theModel.getCurrentRepoHelper() != null) {
                String loginText = this.theModel.getCurrentRepoHelper().getUsername();
                if (loginText == null) loginText = "Login";
                this.switchUserButton.setText(loginText);
            } else{
                this.switchUserButton.setText("Login");
            }
        });
    }
}
