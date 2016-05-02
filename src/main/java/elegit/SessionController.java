package main.java.elegit;

import de.jensd.fx.glyphs.GlyphsDude;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import main.java.elegit.exceptions.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.controlsfx.control.CheckListView;
import org.controlsfx.control.NotificationPane;
import org.controlsfx.control.PopOver;
import org.controlsfx.control.action.Action;
import org.eclipse.jgit.api.errors.*;
import org.eclipse.jgit.errors.NoMergeBaseException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.awt.*;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
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

    public Button openRepoDirButton;
    public Button gitStatusButton;
    public Button commitButton;
    public Button mergeFromFetchButton;
    public Button pushTagsButton;
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

    public ImageView browserImageView;

    public Label commitInfoNameText;
    public Label commitInfoAuthorText;
    public Label commitInfoDateText;
    public Button commitInfoNameCopyButton;
    public Button commitInfoGoToButton;
    public TextArea commitInfoMessageText;

    public ScrollPane tagsPane;
    public Label tagsLabel;
    public Button tagButton;
    public TextArea tagNameField;

    public Text browserText;
    public Text authText;
    public URL remoteURL;

    public DataSubmitter d;

    CommitTreeModel localCommitTreeModel;
    CommitTreeModel remoteCommitTreeModel;

    BooleanProperty isWorkingTreeTabSelected;

    private volatile boolean isRecentRepoEventListenerBlocked = false;

    static final Logger logger = LogManager.getLogger(SessionController.class);

    /**
     * Initializes the environment by obtaining the model
     * and putting the views on display.
     *
     * This method is automatically called by JavaFX.
     */
    public void initialize() {
        this.theModel = SessionModel.getSessionModel();

        d = new DataSubmitter();

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
        this.openRepoDirButton.setGraphic(openExternallyIcon);
        this.openRepoDirButton.setTooltip(new Tooltip("Open repository directory"));

        Text plusIcon = GlyphsDude.createIcon(FontAwesomeIcon.PLUS);
        this.loadNewRepoButton.setGraphic(plusIcon);

        Text minusIcon = GlyphsDude.createIcon(FontAwesomeIcon.MINUS);
        this.removeRecentReposButton.setGraphic(minusIcon);
        this.removeRecentReposButton.setTooltip(new Tooltip("Clear shortcuts to recently opened repos"));

        Text branchIcon = GlyphsDude.createIcon(FontAwesomeIcon.CODE_FORK);
        this.branchesButton.setGraphic(branchIcon);

        Text clipboardIcon = GlyphsDude.createIcon(FontAwesomeIcon.CLIPBOARD);
        this.commitInfoNameCopyButton.setGraphic(clipboardIcon);

        Text goToIcon = GlyphsDude.createIcon(FontAwesomeIcon.ARROW_CIRCLE_LEFT);
        this.commitInfoGoToButton.setGraphic(goToIcon);

        this.commitButton.setTooltip(new Tooltip(
                "Check in selected files to local repository"
        ));
        this.mergeFromFetchButton.setTooltip(new Tooltip(
                "Merge files from remote repository to local repository"
        ));
        this.fetchButton.setTooltip(new Tooltip(
                "Download files from another repository to remote repository"
        ));
        this.pushButton.setTooltip(new Tooltip(
                "Update remote repository with local changes"
        ));

        // Set up the "+" button for loading new repos (give it a menu)
        Text downloadIcon = GlyphsDude.createIcon(FontAwesomeIcon.CLOUD_DOWNLOAD);
        MenuItem cloneOption = new MenuItem("Clone repository", downloadIcon);
        cloneOption.setOnAction(t -> {
            logger.info("Load remote repo button clicked");
            handleLoadRepoMenuItem(new ClonedRepoHelperBuilder(this.theModel));
        });

        Text folderOpenIcon = GlyphsDude.createIcon(FontAwesomeIcon.FOLDER_OPEN);
        MenuItem existingOption = new MenuItem("Load existing repository", folderOpenIcon);
        existingOption.setOnAction(t -> {
            logger.info("Load local repo button clicked");
            handleLoadRepoMenuItem(new ExistingRepoHelperBuilder(this.theModel));
        });
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

        if (this.theModel.getCurrentRepoHelper()!= null && this.theModel.getCurrentRepoHelper().hasTagsWithUnpushedCommits()) {
            this.showTagPointsToUnpushedCommitNotification();
        }
        // If some tags point to a commit in the remote tree, then these are unpushed tags,
        // so we add them to the repohelper
        if (remoteCommitTreeModel.getTagsToBePushed() != null) {
            this.theModel.getCurrentRepoHelper().setUnpushedTags(remoteCommitTreeModel.getTagsToBePushed());
        }
    }

    /**
     * Sets up the layout parameters for things that cannot be set in FXML
     */
    private void initializeLayoutParameters(){
        openRepoDirButton.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
        gitStatusButton.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
        commitButton.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
        mergeFromFetchButton.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
        pushTagsButton.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
        pushButton.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
        fetchButton.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
        branchesButton.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);

        gitStatusButton.setMaxWidth(Double.MAX_VALUE);

        workingTreePanelView.setMinSize(Control.USE_PREF_SIZE, 200);
        allFilesPanelView.setMinSize(Control.USE_PREF_SIZE, 200);
        commitMessageField.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
        tagNameField.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);

        branchDropdownSelector.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);

        final int REPO_DROPDOWN_MAX_WIDTH = 147;
        repoDropdownSelector.setMaxWidth(REPO_DROPDOWN_MAX_WIDTH);


        commitInfoNameCopyButton.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
        commitInfoGoToButton.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);

        commitInfoNameText.maxWidthProperty().bind(commitInfoMessageText.widthProperty()
                .subtract(commitInfoGoToButton.widthProperty())
                .subtract(commitInfoNameCopyButton.widthProperty())
                .subtract(10)); // The gap between each button and this label is 5

    }

    /**
     * Populates the browser image with the remote URL
     */
    public void setBrowserURL() {
        try {
            RepoHelper currentRepoHelper = this.theModel.getCurrentRepoHelper();
            if (currentRepoHelper == null) throw new NoRepoLoadedException();
            if (!currentRepoHelper.exists()) throw new MissingRepoException();
            List<String> remoteURLs = currentRepoHelper.getLinkedRemoteRepoURLs();
            if(remoteURLs.size() == 0){
                this.showNoRemoteNotification();
            }
            String URLString = remoteURLs.get(0);

            if (URLString != null) {
                if(URLString.contains("@")){
                    URLString = "https://"+URLString.replace(":","/").split("@")[1];
                }
                try {
                    remoteURL = new URL(URLString);
                    browserText.setText(remoteURL.getHost());
                } catch (MalformedURLException e) {
                    browserText.setText(URLString);
                }
            }
            authText.setText("Auth: " + currentRepoHelper.protocol.toString());
            Tooltip URLTooltip = new Tooltip(URLString);
            Tooltip.install(browserImageView, URLTooltip);
            Tooltip.install(browserText, URLTooltip);
        }
        catch(MissingRepoException e){
            this.showMissingRepoNotification();
            this.setButtonsDisabled(true);
            this.refreshRecentReposInDropdown();
        }catch(NoRepoLoadedException e){
            this.showNoRepoLoadedNotification();
            this.setButtonsDisabled(true);
        }
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

        List<LocalBranchHelper> branches = currentRepoHelper.getListOfLocalBranches();

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
                    } catch(Exception e) {
                        showGenericErrorNotification();
                        e.printStackTrace();
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
        } catch(CancelledAuthorizationException e) {
            //The user pressed cancel on the authorize dialog box. Do nothing!
        }
    }

    /**
     * Gets the current RepoHelper and sets it as the selected value of the dropdown.
     */
    @FXML
    private void setRecentReposDropdownToCurrentRepo() {
        Platform.runLater(() -> {
            synchronized (this) {
                isRecentRepoEventListenerBlocked = true;
                RepoHelper currentRepo = this.theModel.getCurrentRepoHelper();
                this.repoDropdownSelector.setValue(currentRepo);
                isRecentRepoEventListenerBlocked = false;
            }
        });
    }

    /**
     * Adds all the model's RepoHelpers to the dropdown
     */
    @FXML
    private void refreshRecentReposInDropdown() {
        Platform.runLater(() -> {
            synchronized (this) {
                isRecentRepoEventListenerBlocked = true;
                List<RepoHelper> repoHelpers = this.theModel.getAllRepoHelpers();
                this.repoDropdownSelector.setItems(FXCollections.observableArrayList(repoHelpers));
                isRecentRepoEventListenerBlocked = false;
            }
        });
    }

    /**
     * Loads the given repository and updates the UI accordingly.
     * @param repoHelper the repository to open
     */
    private synchronized void handleRecentRepoMenuItem(RepoHelper repoHelper){
        if(isRecentRepoEventListenerBlocked || repoHelper == null) return;

        logger.info("Switching repos");
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
                } catch(Exception e) {
                    showGenericErrorNotification();
                    e.printStackTrace();
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
        if (theModel.getAllRepoHelpers().size() == 0) return;
        RepoHelper selectedRepoHelper = this.repoDropdownSelector.getValue();
        this.handleRecentRepoMenuItem(selectedRepoHelper);
    }

    /**
     * Perform the updateFileStatusInRepo() method for each file whose
     * checkbox is checked. Then commit with the commit message and push.
     */
    public void handleCommitButton() {
        try {
            logger.info("Commit button clicked");
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
                    } catch(Exception e) {
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
     * Checks things are ready for a tag, then performs a git-tag
     *
     */
    public void handleTagButton() {
        logger.info("Clicked tag button");
        try {
            if(this.theModel.getCurrentRepoHelper() == null) throw new NoRepoLoadedException();
            if(!this.theModel.getCurrentRepoHelper().exists()) throw new MissingRepoException();

            String tagName = tagNameField.getText();
            if (theModel.getCurrentRepoHelper().getTag(tagName) != null) {
                throw new TagNameExistsException();
            }

            if(tagName.length() == 0) throw new NoTagNameException();

            Thread th = new Thread(new Task<Void>(){
                @Override
                protected Void call() {
                    try{
                        theModel.getCurrentRepoHelper().tag(tagName, commitInfoNameText.getText());

                        // Now clear the tag text and a view reload ( or `git status`) to show that something happened
                        tagNameField.clear();
                        gitStatus();
                    }catch(JGitInternalException e){
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

                    } catch(GitAPIException e){
                        // Git error
                        showGenericErrorNotification();
                        e.printStackTrace();
                    } catch(TagNameExistsException e){
                        showTagExistsNotification();
                    }
                    catch(Exception e) {
                        showGenericErrorNotification();
                        e.printStackTrace();
                    }

                    if (!theModel.getCurrentRepoHelper().hasUnpushedCommits()) {
                        pushTagsButton.setVisible(true);
                        pushButton.setVisible(false);
                    }
                    tagNameField.setText("");
                    clearSelectedCommit();
                    selectCommit(theModel.getCurrentRepoHelper().getTag(tagName).getCommitId());

                    return null;
                }
            });
            th.setDaemon(true);
            th.setName("Git tag");
            th.start();
        } catch(NoRepoLoadedException e){
            this.showNoRepoLoadedNotification();
            setButtonsDisabled(true);
        } catch(MissingRepoException e){
            this.showMissingRepoNotification();
            setButtonsDisabled(true);
            refreshRecentReposInDropdown();
        } catch(NoTagNameException e){
            this.showNoTagNameNotification();
        } catch(TagNameExistsException e) {
            this.showTagExistsNotification();
        }
    }

    /**
     * Merges in FETCH_HEAD (after a fetch).
     */
    public void handleMergeFromFetchButton() {
        try{
            logger.info("Merge from fetch button clicked");
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
                    } catch(Exception e) {
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
            logger.info("Push button clicked");

            Thread submit = new Thread(new Task<Void>() {
                @Override
                protected Void call() {
                    d.submitData();
                    return null;
                }
            });
            submit.setDaemon(true);
            submit.setName("Data submit");
            submit.start();

            if(this.theModel.getCurrentRepoHelper() == null) throw new NoRepoLoadedException();
            if(!this.theModel.getCurrentRepoHelper().hasUnpushedCommits()) throw new NoCommitsToPushException();

            Thread th = new Thread(new Task<Void>(){
                @Override
                protected Void call() {
                    boolean pushed = false;
                    try{
                        RepositoryMonitor.resetFoundNewChanges(false);
                        theModel.getCurrentRepoHelper().pushAll();
                        gitStatus();
                        pushed = true;
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
                    } catch(Exception e) {
                        showGenericErrorNotification();
                        e.printStackTrace();
                    } finally{
                        pushProgressIndicator.setVisible(false);
                        pushButton.setVisible(true);
                        if (pushed && theModel.getCurrentRepoHelper().hasUnpushedTags()) {
                            pushTagsButton.setVisible(true);
                            pushButton.setVisible(false);
                        }
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
     * Performs a `git push --tags`
     */
    public void handlePushTagsButton() {
        try {
            logger.info("Push tags button clicked");

            Thread submit = new Thread(new Task<Void>() {
                @Override
                protected Void call() {
                    d.submitData();
                    return null;
                }
            });
            submit.setDaemon(true);
            submit.setName("Data submit");
            submit.start();

            if(this.theModel.getCurrentRepoHelper() == null) throw new NoRepoLoadedException();
            if(!this.theModel.getCurrentRepoHelper().hasUnpushedTags()) throw new NoTagsToPushException();

            Thread th = new Thread(new Task<Void>(){
                @Override
                protected Void call() {
                    boolean tagsPushed = true;
                    try{
                        RepositoryMonitor.resetFoundNewChanges(false);
                        theModel.getCurrentRepoHelper().pushTags();
                        gitStatus();
                    }  catch(InvalidRemoteException e){
                        showNoRemoteNotification();
                        tagsPushed = false;
                    } catch(PushToAheadRemoteError e) {
                        showPushToAheadRemoteNotification(e.isAllRefsRejected());
                        tagsPushed = false;
                    } catch (TransportException e) {
                        if (e.getMessage().contains("git-receive-pack not found")) {
                            // The error has this message if there is no longer a remote to push to
                            showLostRemoteNotification();
                        } else {
                            showNotAuthorizedNotification(null);
                        }
                        tagsPushed = false;
                    } catch(MissingRepoException e){
                        showMissingRepoNotification();
                        setButtonsDisabled(true);
                        refreshRecentReposInDropdown();
                        tagsPushed = false;
                    } catch(GitAPIException e){
                        showGenericErrorNotification();
                        e.printStackTrace();
                        tagsPushed = false;
                    } catch(Exception e) {
                        showGenericErrorNotification();
                        e.printStackTrace();
                        tagsPushed = false;
                    } finally{
                        pushProgressIndicator.setVisible(false);
                    }
                    if (tagsPushed) {
                        pushTagsButton.setVisible(false);
                        pushButton.setVisible(true);
                    }
                    else {
                        pushTagsButton.setVisible(true);
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
        }catch(NoTagsToPushException e){
            this.showNoTagsToPushNotification();
        }
    }

    /**
     * Handles a click on the "Fetch" button. Calls gitFetch()
     */
    public void handleFetchButton(){
        logger.info("Fetch button clicked");
        gitFetch();
    }

    /**
     * Queries the remote for new commits, and updates the local
     * remote as necessary.
     * Equivalent to `git fetch`
     */
    public synchronized void gitFetch(){
        try{
            Thread submit = new Thread(new Task<Void>() {
                @Override
                protected Void call() {
                    d.submitData();
                    return null;
                }
            });
            submit.setDaemon(true);
            submit.setName("Data submit");
            submit.start();

            if(this.theModel.getCurrentRepoHelper() == null) throw new NoRepoLoadedException();

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
                    } catch(Exception e) {
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
        }catch(NoRepoLoadedException e) {
            this.showNoRepoLoadedNotification();
            setButtonsDisabled(true);
        }
    }

    /**
     * Updates the panel views when the "git status" button is clicked.
     * Highlights the current HEAD.
     */
    public void onGitStatusButton(){
        logger.info("Git status button clicked");
        this.gitStatus();
        CommitTreeController.focusCommitInGraph(theModel.getCurrentRepoHelper().getHead());
    }

    /**
     * Updates the trees, changed files, and branch information. Equivalent
     * to 'git status'
     */
    public void gitStatus(){
        RepositoryMonitor.pause();
        Thread th = new Thread(new Task<Void>(){
            @Override
            protected Void call(){
                try{
                    localCommitTreeModel.update();
                    remoteCommitTreeModel.update();
                    if (theModel.getCurrentRepoHelper() != null &&
                            theModel.getCurrentRepoHelper().updateTags()) {
                        if (theModel.getCurrentRepoHelper().hasTagsWithUnpushedCommits()) {
                            showTagPointsToUnpushedCommitNotification();
                        }
                    }

                    workingTreePanelView.drawDirectoryView();
                    allFilesPanelView.drawDirectoryView();
                    updateBranchDropdown();
                } catch(MissingRepoException e){
                    showMissingRepoNotification();
                    setButtonsDisabled(true);
                    refreshRecentReposInDropdown();
                } catch(NoRepoLoadedException e){
                    // TODO: I'm changing the way it handles exception,
                    // assuming that the only time when this exception is
                    // thrown is after user closes the last repo.

                    /*showNoRepoLoadedNotification();
                    setButtonsDisabled(true);*/
                } catch(GitAPIException | IOException e){
                    showGenericErrorNotification();
                    e.printStackTrace();
                } catch(Exception e) {
                    showGenericErrorNotification();
                    e.printStackTrace();
                } finally{
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
     * When the image representing the remote repo is clicked, go to the
     * corresponding remote url
     * @param event the mouse event corresponding to the click
     */
    public void handleRemoteImageViewMouseClick(MouseEvent event){
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
            setBrowserURL();
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
            tagButton.setDisable(disable);
            commitButton.setDisable(disable);
            mergeFromFetchButton.setDisable(disable);
            pushTagsButton.setDisable(disable);
            pushButton.setDisable(disable);
            fetchButton.setDisable(disable);
            selectAllButton.setDisable(disable);
            deselectAllButton.setDisable(disable);
            browserImageView.setVisible(!disable);
            commitMessageField.setDisable(disable);
            browserText.setVisible(!disable);
            authText.setVisible(!disable);
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
     * Opens the current repo directory (e.g. in Finder or Windows Explorer).
     */
    public void openRepoDirectory(){
        if (Desktop.isDesktopSupported()) {
            try{
                logger.info("Opening Repo Directory");
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
            logger.warn("Generic error warning.");
            this.notificationPane.setText("Sorry, there was an error.");

            this.notificationPane.getActions().clear();
            this.notificationPane.show();
        });
    }

    private void showNoRepoLoadedNotification() {
        Platform.runLater(() -> {
            logger.warn("No repo loaded warning.");
            this.notificationPane.setText("You need to load a repository before you can perform operations on it!");

            this.notificationPane.getActions().clear();
            this.notificationPane.show();
        });
    }

    private void showInvalidRepoNotification() {
        Platform.runLater(() -> {
            logger.warn("Invalid repo warning.");
            this.notificationPane.setText("Make sure the directory you selected contains an existing (non-bare) Git repository.");

            this.notificationPane.getActions().clear();
            this.notificationPane.show();
        });
    }

    private void showMissingRepoNotification(){
        Platform.runLater(()-> {
            logger.warn("Missing repo warning");
            this.notificationPane.setText("That repository no longer exists.");

            this.notificationPane.getActions().clear();
            this.notificationPane.show();
        });
    }

    private void showNoRemoteNotification(){
        Platform.runLater(()-> {
            logger.warn("No remote repo warning");
            String name = this.theModel.getCurrentRepoHelper() != null ? this.theModel.getCurrentRepoHelper().toString() : "the current repository";

            this.notificationPane.setText("There is no remote repository associated with " + name);

            this.notificationPane.getActions().clear();
            this.notificationPane.show();
        });
    }

    private void showFailedToOpenLocalNotification(){
        Platform.runLater(()-> {
            logger.warn("Failed to load local repo warning");
            String path = this.theModel.getCurrentRepoHelper() != null ? this.theModel.getCurrentRepoHelper().getLocalPath().toString() : "the location of the local repository";

            this.notificationPane.setText("Could not open directory at " + path);

            this.notificationPane.getActions().clear();
            this.notificationPane.show();
        });
    }

    private void showNonEmptyFolderNotification(Runnable callback) {
        Platform.runLater(()-> {
            logger.warn("Folder alread exists warning");
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
            logger.warn("Invalid remote warning");
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
            logger.warn("Invalid authorization warning");
            this.notificationPane.setText("The authorization information you gave does not allow you to modify this repository. " +
                    "Try reentering your password.");

            /*
            Action authAction = new Action("Authorize", e -> {
                this.notificationPane.hide();
                if(this.switchUser()){
                    if(callback != null) callback.run();
                }
            });

            this.notificationPane.getActions().clear();
            this.notificationPane.getActions().setAll(authAction);*/
            this.notificationPane.show();
        });
    }

    private void showRepoWasNotLoadedNotification() {
        Platform.runLater(() -> {
            logger.warn("Repo not loaded warning");
            this.notificationPane.setText("No repository was loaded.");

            this.notificationPane.getActions().clear();
            this.notificationPane.show();
        });
    }

    private void showCheckoutConflictsNotification(List<String> conflictingPaths) {
        Platform.runLater(() -> {
            logger.warn("Checkout conflicts warning");
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
            logger.warn("Merge conflicts warning");
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
            logger.warn("Remote ahead of local warning");
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
            logger.warn("Remote repo couldn't be found warning");
            this.notificationPane.setText("The push failed because the remote repository couldn't be found.");

            this.notificationPane.getActions().clear();
            this.notificationPane.show();
        });
    }

    private void showUnsuccessfulMergeNotification(){
        Platform.runLater(() -> {
            logger.warn("Failed merged warning");
            this.notificationPane.setText("Merging failed");

            this.notificationPane.getActions().clear();
            this.notificationPane.show();
        });
    }

    private void showNewRemoteChangesNotification(){
        Platform.runLater(() -> {
            logger.info("New remote repo changes");
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
            logger.warn("No files staged for commit warning");
            this.notificationPane.setText("You need to select which files to commit");

            this.notificationPane.getActions().clear();
            this.notificationPane.show();
        });
    }

    private void showNoCommitMessageNotification(){
        Platform.runLater(() -> {
            logger.warn("No commit message warning");
            this.notificationPane.setText("You need to write a commit message in order to commit your changes");

            this.notificationPane.getActions().clear();
            this.notificationPane.show();
        });
    }

    private void showNoTagNameNotification(){
        Platform.runLater(() -> {
            logger.warn("No tag name warning");
            this.notificationPane.setText("You need to write a tag name in order to tag the commit");

            this.notificationPane.getActions().clear();
            this.notificationPane.show();
        });
    }

    private void showNoCommitsToPushNotification(){
        Platform.runLater(() -> {
            logger.warn("No local commits to push warning");
            this.notificationPane.setText("There aren't any local commits to push");

            this.notificationPane.getActions().clear();
            this.notificationPane.show();
        });
    }

    private void showNoTagsToPushNotification(){
        Platform.runLater(() -> {
            logger.warn("No local tags to push warning");
            this.notificationPane.setText("There aren't any local tags to push");

            this.notificationPane.getActions().clear();
            this.notificationPane.show();
        });
    }

    private void showNoCommitsToMergeNotification(){
        Platform.runLater(() -> {
            logger.warn("No commits to merge warning");
            this.notificationPane.setText("There aren't any commits to merge. Try fetching first");

            this.notificationPane.getActions().clear();
            this.notificationPane.show();
        });
    }

    private void showNoCommitsFetchedNotification(){
        Platform.runLater(() -> {
            logger.warn("No commits fetched warning");
            this.notificationPane.setText("No new commits were fetched");

            this.notificationPane.getActions().clear();
            this.notificationPane.show();
        });
    }

    private void showMergingWithChangedFilesNotification(){
        Platform.runLater(() -> {
            logger.warn("Can't merge with modified files warning");
            this.notificationPane.setText("Can't merge with modified files present");

            // TODO: I think some sort of help text would be nice here, so they know what to do

            this.notificationPane.getActions().clear();
            this.notificationPane.show();
        });
    }

    private void showTagExistsNotification() {
        Platform.runLater(()-> {
            logger.warn("Tag already exists warning.");
            this.notificationPane.setText("Sorry that tag already exists.");

            this.notificationPane.getActions().clear();
            this.notificationPane.show();
        });
    }

    private void showTagPointsToUnpushedCommitNotification() {
        Platform.runLater(() -> {
            logger.warn("Tag points to unpushed warning.");
            this.notificationPane.setText("A tag points to an unpushed commit.");

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
            logger.info("Branch manager clicked");
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
     * Called when the change login button is clicked.
     */
    public void handleChangeLoginButton() {
        try {
            logger.info("Username button clicked");

            if(this.theModel.getCurrentRepoHelper() == null) {
                throw new NoRepoLoadedException();
            }
            this.changeLogin();
        } catch (NoRepoLoadedException e) {
            showNoRepoLoadedNotification();
            setButtonsDisabled(true);
        }
    }

    /**
     * Creates a new owner and sets it as the current default owner.
     */
    public boolean changeLogin() {
        SessionModel sessionModel = SessionModel.getSessionModel();
        RepoHelper repoHelper = sessionModel.getCurrentRepoHelper();

        try {
            RepoHelperBuilder.AuthDialogResponse response =
                    RepoHelperBuilder.getAuthCredentialFromDialog();
            repoHelper.setAuthCredentials(new UsernamePasswordCredentialsProvider(response.username,
                                                                                  response.password));
            repoHelper.protocol = AuthMethod.HTTPS;
        } catch (CancelledAuthorizationException e) {
            // take no action
        }


//        boolean switchedUser = true;
//
//        RepoHelper currentRepoHelper = theModel.getCurrentRepoHelper();
//
//        try {
//            currentRepoHelper.presentUsernameDialog();
//        } catch (CancelledUsernameException e) {
//            switchedUser = false;
//        }
//
//        this.updateLoginButtonText();
//        if (switchedUser) {
//            this.theModel.setCurrentDefaultUsername(currentRepoHelper.getUsername());
//        }
//
//        return switchedUser;
        return true;
    }


    /**
     * Opens up the help page to inform users about what symbols mean
     */
    public void showLegend() {
        try{
            logger.info("Legend clicked");
            // Create and display the Stage:
            NotificationPane fxmlRoot = FXMLLoader.load(getClass().getResource("/elegit/fxml/Legend.fxml"));

            Stage stage = new Stage();
            stage.setTitle("Legend");
            stage.setScene(new Scene(fxmlRoot, 250, 300));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
                @Override
                public void handle(WindowEvent event) {
                    logger.info("Closed legend");
                }
            });
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

            GridPane tags = new GridPane();
            tags.setPrefHeight(1);
            int numTags = 0;
            for (TagHelper t:commit.getTags()) {
                Hyperlink link = new Hyperlink();
                link.setText(t.getName());
                link.setOnAction(new EventHandler<javafx.event.ActionEvent>() {
                    @Override
                    public void handle(javafx.event.ActionEvent event) {
                        logger.info("Delete tag dialog started.");
                        if (t.presentDeleteDialog()) {
                            try {
                                theModel.getCurrentRepoHelper().deleteTag(t.getName());
                            } catch (MissingRepoException e) {
                                e.printStackTrace();
                            } catch (GitAPIException e) {
                                e.printStackTrace();
                            }
                            if (!theModel.getCurrentRepoHelper().hasUnpushedTags()) {
                                pushTagsButton.setVisible(false);
                                pushButton.setVisible(true);
                            }
                            gitStatus();
                            clearSelectedCommit();
                            selectCommit(id);
                        }
                    }
                });
                tags.add(link,numTags,0);
                numTags++;
            }
            tagsPane.setContent(tags);
            commitInfoNameText.setText(commit.getName());
            commitInfoAuthorText.setText(commit.getAuthorName());
            commitInfoDateText.setText(commit.getFormattedWhen());
            commitInfoMessageText.setVisible(true);
            commitInfoNameCopyButton.setVisible(true);
            commitInfoGoToButton.setVisible(true);

            if (commit.hasTags()) {
                tagsPane.setVisible(true);
                tagsLabel.setVisible(true);
            }
            tagNameField.setVisible(true);
            tagButton.setVisible(true);

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

            tagsPane.setVisible(false);
            tagsLabel.setVisible(false);
            tagNameField.setText("");
            tagNameField.setVisible(false);
            tagButton.setVisible(false);
        });
    }

    /**
     * Copies the commit hash onto the clipboard
     */
    public void handleCommitNameCopyButton(){
        logger.info("Commit name copied");
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(commitInfoNameText.getText());
        clipboard.setContent(content);
    }

    /**
     * Jumps to the selected commit in the tree display
     */
    public void handleGoToCommitButton(){
        logger.info("Go to commit button clicked");
        String id = commitInfoNameText.getText();
        CommitTreeController.focusCommitInGraph(id);
    }

    /**
     * Selects all files in the working tree for a commit.
     *
     */
    public void onSelectAllButton() {
        logger.info("Selected all files");
        this.workingTreePanelView.setAllFilesSelected(true);
    }

    /**
     * Deselects all files in the working tree for a commit.
     *
     */
    public void onDeselectAllButton() {
        logger.info("Deselected all files");
        this.workingTreePanelView.setAllFilesSelected(false);
    }

    public void chooseRecentReposToDelete() {
        logger.info("Remove repos button clicked");
        List<RepoHelper> repoHelpers = this.theModel.getAllRepoHelpers();
        CheckListView<RepoHelper> repoCheckListView = new CheckListView<>(FXCollections.observableArrayList(repoHelpers));

        // Remove the currently checked out repo:
        /*RepoHelper currentRepo = this.theModel.getCurrentRepoHelper();
        repoCheckListView.getItems().remove(currentRepo);*/

        Button removeSelectedButton = new Button("Remove repository shortcuts from Elegit");

        PopOver popover = new PopOver(new VBox(repoCheckListView, removeSelectedButton));
        popover.setTitle("Manage Recent Repositories");

        removeSelectedButton.setOnAction(e -> {
            logger.info("Removed repos");
            List<RepoHelper> checkedItems = repoCheckListView.getCheckModel().getCheckedItems();
            this.theModel.removeRepoHelpers(checkedItems);
            popover.hide();

            if (!this.theModel.getAllRepoHelpers().isEmpty()) {
                int newIndex = this.theModel.getAllRepoHelpers().size()-1;
                RepoHelper newCurrentRepo = this.theModel.getAllRepoHelpers()
                        .get(newIndex);

                handleRecentRepoMenuItem(newCurrentRepo);
                repoDropdownSelector.setValue(newCurrentRepo);

                this.refreshRecentReposInDropdown();
            } else {
                theModel.resetSessionModel();
                workingTreePanelView.resetFileStructurePanelView();
                allFilesPanelView.resetFileStructurePanelView();
                initialize();
            }
        });

        popover.show(this.removeRecentReposButton);
    }
}
