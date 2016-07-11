package elegit;

import de.jensd.fx.glyphs.GlyphsDude;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import elegit.treefx.TreeLayout;
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
import elegit.exceptions.*;
import org.apache.commons.lang3.SystemUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.controlsfx.control.CheckListView;
import org.controlsfx.control.NotificationPane;
import org.controlsfx.control.PopOver;
import org.controlsfx.control.action.Action;
import org.eclipse.jgit.api.errors.*;
import org.eclipse.jgit.dircache.InvalidPathException;
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

    public TextArea commitMessageField;

    public Tab workingTreePanelTab;
    public Tab allFilesPanelTab;
    public WorkingTreePanelView workingTreePanelView;
    public AllFilesPanelView allFilesPanelView;

	public CommitTreePanelView localCommitTreePanelView;
    public CommitTreePanelView remoteCommitTreePanelView;

    public ImageView remoteImage;

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
    public URL remoteURL;

    public DataSubmitter d;

    public CommitTreeModel localCommitTreeModel;
    public CommitTreeModel remoteCommitTreeModel;

    public BooleanProperty isWorkingTreeTabSelected;

    private volatile boolean isRecentRepoEventListenerBlocked = false;

    static final Logger logger = LogManager.getLogger(SessionController.class);

    public ContextMenu newRepoOptionsMenu;
    public MenuItem cloneOption;
    public MenuItem existingOption;

    /**
     * Initializes the environment by obtaining the model
     * and putting the views on display.
     *
     * This method is automatically called by JavaFX.
     */
    public void initialize() {

        // Creates the SessionModel
        this.theModel = SessionModel.getSessionModel();

        // Creates a DataSubmitter for logging
        d = new DataSubmitter();

        // Passes this to CommitTreeController
        CommitTreeController.sessionController = this;

        // Creates the local and remote commit tree models
        this.localCommitTreeModel = new LocalCommitTreeModel(this.theModel, this.localCommitTreePanelView);
        this.remoteCommitTreeModel = new RemoteCommitTreeModel(this.theModel, this.remoteCommitTreePanelView);

        // Passes theModel to panel views
        this.workingTreePanelView.setSessionModel(this.theModel);
        this.allFilesPanelView.setSessionModel(this.theModel);

        this.initializeLayoutParameters();
        this.initWorkingTreePanelTab();
        this.setButtonIconsAndTooltips();
        this.setButtonsDisabled(true);

        this.theModel.loadRecentRepoHelpersFromStoredPathStrings();
        this.theModel.loadMostRecentRepoHelper();

        this.initPanelViews();
        this.updateUIEnabledStatus();
        this.setRecentReposDropdownToCurrentRepo();
        this.refreshRecentReposInDropdown();

        this.initRepositoryMonitor();
        this.handleUnpushedTags();

        // if there are conflicting files on startup, watches them for changes
        try {
            ConflictingFileWatcher.watchConflictingFiles(theModel.getCurrentRepoHelper());
        } catch (GitAPIException | IOException e) {
            e.printStackTrace();
        }
    }

    private void handleUnpushedTags() {
        // ASK ERIC
        //if (this.theModel.getCurrentRepoHelper()!= null && this.theModel.getCurrentRepoHelper().hasTagsWithUnpushedCommits()) {
        //this.showTagPointsToUnpushedCommitNotification();
        //}

        // If some tags point to a commit in the remote tree, then these are unpushed tags,
        // so we add them to the repohelper
        if (remoteCommitTreeModel.getTagsToBePushed() != null) {
            this.theModel.getCurrentRepoHelper().setUnpushedTags(remoteCommitTreeModel.getTagsToBePushed());
        }
    }

    /**
     * Initializes the workingTreePanelTab
     */
    private void initWorkingTreePanelTab() {
        isWorkingTreeTabSelected = new SimpleBooleanProperty(true);
        isWorkingTreeTabSelected.bind(workingTreePanelTab.selectedProperty());
        workingTreePanelTab.getTabPane().getSelectionModel().select(workingTreePanelTab);
    }

    /**
     * Initializes the repository monitor
     */
    private void initRepositoryMonitor() {
        // bind currentRepoProperty with menuBar to update menuBar when repo gets changed.
        RepositoryMonitor.bindMenu(theModel);

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
        // Set minimum/maximum sizes for buttons
        openRepoDirButton.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
        gitStatusButton.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
        commitButton.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
        mergeFromFetchButton.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
        pushTagsButton.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
        pushButton.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
        fetchButton.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
        branchesButton.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
        commitInfoNameCopyButton.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
        commitInfoGoToButton.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
        gitStatusButton.setMaxWidth(Double.MAX_VALUE);

        // Set minimum sizes for other fields and views
        workingTreePanelView.setMinSize(Control.USE_PREF_SIZE, 200);
        allFilesPanelView.setMinSize(Control.USE_PREF_SIZE, 200);
        commitMessageField.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
        tagNameField.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
        branchDropdownSelector.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
        final int REPO_DROPDOWN_MAX_WIDTH = 147;
        repoDropdownSelector.setMaxWidth(REPO_DROPDOWN_MAX_WIDTH);

        commitInfoNameText.maxWidthProperty().bind(commitInfoMessageText.widthProperty()
                .subtract(commitInfoGoToButton.widthProperty())
                .subtract(commitInfoNameCopyButton.widthProperty())
                .subtract(10)); // The gap between each button and this label is 5
    }

    /**
     * Adds graphics and tooltips to the buttons
     */
    private void setButtonIconsAndTooltips() {
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

        Text downloadIcon = GlyphsDude.createIcon(FontAwesomeIcon.CLOUD_DOWNLOAD);
        cloneOption.setGraphic(downloadIcon);

        Text folderOpenIcon = GlyphsDude.createIcon(FontAwesomeIcon.FOLDER_OPEN);
        existingOption.setGraphic(folderOpenIcon);

        this.commitInfoGoToButton.setTooltip(new Tooltip(
                "Go to selected commit"
        ));

        this.commitInfoNameCopyButton.setTooltip(new Tooltip(
                "Copy commit ID"
        ));

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
        // TODO: Update this when revert has more functionality
        this.pushButton.setTooltip(new Tooltip(
                "Revert the changes in the most recent commit"
        ));

        this.loadNewRepoButton.setTooltip(new Tooltip(
                "Load a new repository"
        ));
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
                return;
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
            Tooltip URLTooltip = new Tooltip(URLString);
            Tooltip.install(remoteImage, URLTooltip);
            Tooltip.install(browserText, URLTooltip);
        }
        catch(MissingRepoException e) {
            this.showMissingRepoNotification();
            this.setButtonsDisabled(true);
            this.refreshRecentReposInDropdown();
        }catch(NoRepoLoadedException e) {
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

        currentRepoHelper.getBranchModel().updateAllBranches();
        List<LocalBranchHelper> branches = currentRepoHelper.getBranchModel().getLocalBranchesTyped();

        currentRepoHelper.getBranchModel().refreshCurrentBranch();
        LocalBranchHelper currentBranch = (LocalBranchHelper) currentRepoHelper.getBranchModel().getCurrentBranch();

        Platform.runLater(() -> {
            this.branchDropdownSelector.setVisible(true);
            this.branchDropdownSelector.getItems().setAll(branches);
            if(this.branchDropdownSelector.getValue() == null || !this.branchDropdownSelector.getValue().getBranchName().equals(currentBranch.getBranchName())){
                this.branchDropdownSelector.setValue(currentBranch);
            }
        });
    }

    /**
     * Called when the loadNewRepoButton gets pushed, shows a menu of options
     */
    public void handleLoadNewRepoButton() {
        newRepoOptionsMenu.show(this.loadNewRepoButton, Side.BOTTOM ,0, 0);
    }

    /**
     * Called when the "Load existing repository" option is clicked
     */
    public void handleLoadExistingRepoOption() {
        handleLoadRepoMenuItem(new ExistingRepoHelperBuilder(this.theModel));
    }

    /**
     * Called when the "Clone repository" option is clicked
     */
    public void handleCloneNewRepoOption() {
        handleLoadRepoMenuItem(new ClonedRepoHelperBuilder(this.theModel));
    }

    /**
     * Called when a selection is made from the 'Load New Repository' menu. Creates a new repository
     * using the given builder and updates the UI
     * @param builder the builder to use to create a new repository
     */
    private synchronized void handleLoadRepoMenuItem(RepoHelperBuilder builder){
        try{
            RepoHelper repoHelper = builder.getRepoHelperFromDialogs();
            if(theModel.getCurrentRepoHelper() != null && repoHelper.localPath.equals(theModel.getCurrentRepoHelper().localPath)) {
                showSameRepoLoadedNotification();
                return;
            }
            RepositoryMonitor.pause();
            BusyWindow.show();
            BusyWindow.setLoadingText("Loading the repository...");
            Thread th = new Thread(new Task<Void>(){
                @Override
                protected Void call() {
                    try {
                        TreeLayout.stopMovingCells();

                        refreshRecentReposInDropdown();
                        theModel.openRepoFromHelper(repoHelper);
                        setRecentReposDropdownToCurrentRepo();

                        Platform.runLater(() -> {
                            initPanelViews();
                            updateUIEnabledStatus();
                        });
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
                        RepositoryMonitor.unpause();
                        BusyWindow.hide();
                    }
                    return null;
                }
            });
            th.setDaemon(true);
            th.setName("Loading existing/cloning repository");
            th.start();
        } catch(InvalidPathException e) {
            showRepoWasNotLoadedNotification();
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            showInvalidRepoNotification();
        } catch(JGitInternalException e){
            showNonEmptyFolderNotification(() -> handleLoadRepoMenuItem(builder));
        } catch(InvalidRemoteException e){
            showInvalidRemoteNotification(() -> handleLoadRepoMenuItem(builder));
        } catch(TransportException e){
            showNotAuthorizedNotification(() -> handleLoadRepoMenuItem(builder));
        } catch (NoRepoSelectedException | CancelledAuthorizationException e) {
            // The user pressed cancel on the dialog box, or
            // the user pressed cancel on the authorize dialog box. Do nothing!
        } catch(IOException | GitAPIException e) {
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
        RepositoryMonitor.pause();
        BusyWindow.show();
        BusyWindow.setLoadingText("Opening the repository...");
        Thread th = new Thread(new Task<Void>(){
            @Override
            protected Void call() throws Exception{
                try {
                    theModel.openRepoFromHelper(repoHelper);

                    Platform.runLater(() -> {
                        initPanelViews();
                        updateUIEnabledStatus();
                    });
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
                    RepositoryMonitor.unpause();
                    BusyWindow.hide();
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
     * Performs the updateFileStatusInRepo() method for each file whose
     * checkbox is checked if all files have changes to be added
     */
    public void handleAddButton() {
        try {
            logger.info("Add button clicked");
            if(this.theModel.getCurrentRepoHelper() == null) throw new NoRepoLoadedException();
            if(!this.theModel.getCurrentRepoHelper().exists()) throw new MissingRepoException();

            if(!workingTreePanelView.isAnyFileSelected()) throw new NoFilesStagedForCommitException();

            BusyWindow.show();
            BusyWindow.setLoadingText("Adding...");
            Thread th = new Thread(new Task<Void>(){
                @Override
                protected Void call() {
                    try{
                        for(RepoFile checkedFile : workingTreePanelView.getCheckedFilesInDirectory())
                            checkedFile.updateFileStatusInRepo();

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

                    } catch(GitAPIException e){
                        // Git error, or error presenting the file chooser window
                        showGenericErrorNotification();
                        e.printStackTrace();
                    } catch(Exception e) {
                        showGenericErrorNotification();
                        e.printStackTrace();
                    }finally {
                        BusyWindow.hide();
                    }
                    return null;
                }
            });
            th.setDaemon(true);
            th.setName("Git add");
            th.start();
        } catch(NoRepoLoadedException e){
            this.showNoRepoLoadedNotification();
            setButtonsDisabled(true);
        } catch(MissingRepoException e){
            this.showMissingRepoNotification();
            setButtonsDisabled(true);
            refreshRecentReposInDropdown();
        } catch(NoFilesStagedForCommitException e){
            this.showNoFilesStagedForCommitNotification();
        }
    }

    /**
     * Commits all files that have been staged with the message
     */
    public void handleCommitButton() {
        try {
            logger.info("Commit button clicked");
            if(this.theModel.getCurrentRepoHelper() == null) throw new NoRepoLoadedException();
            if(!this.theModel.getCurrentRepoHelper().exists()) throw new MissingRepoException();

            String commitMessage = commitMessageField.getText();

            if(!workingTreePanelView.isAnyFileSelected()) throw new NoFilesStagedForCommitException();
            if(commitMessage.length() == 0) throw new NoCommitMessageException();

            BusyWindow.show();
            BusyWindow.setLoadingText("Committing...");
            Thread th = new Thread(new Task<Void>(){
                @Override
                protected Void call() {
                    try{
                        boolean canCommit = true;

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

                    } catch(GitAPIException e){
                        // Git error, or error presenting the file chooser window
                        showGenericErrorNotification();
                        e.printStackTrace();
                    } catch(Exception e) {
                        showGenericErrorNotification();
                        e.printStackTrace();
                    }finally {
                        BusyWindow.hide();
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

            BusyWindow.show();
            BusyWindow.setLoadingText("Merging...");
            Thread th = new Thread(new Task<Void>(){
                @Override
                protected Void call() throws GitAPIException, IOException {
                    try{
                        if(!theModel.getCurrentRepoHelper().mergeFromFetch().isSuccessful()){
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
                        ConflictingFileWatcher.watchConflictingFiles(theModel.getCurrentRepoHelper());
                    } catch(MissingRepoException e){
                        showMissingRepoNotification();
                        setButtonsDisabled(true);
                        refreshRecentReposInDropdown();
                    } catch(GitAPIException | IOException e){
                        showGenericErrorNotification();
                        e.printStackTrace();
                    } catch(NoTrackingException e) {
                        showNoRemoteTrackingNotification();
                    }catch (Exception e) {
                        showGenericErrorNotification();
                        e.printStackTrace();
                    }finally {
                        BusyWindow.hide();
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

            if(this.theModel.getCurrentRepoHelper() == null) throw new NoRepoLoadedException();
            if(!this.theModel.getCurrentRepoHelper().hasUnpushedCommits()) throw new NoCommitsToPushException();

            BusyWindow.show();
            BusyWindow.setLoadingText("Pushing...");
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
                        pushButton.setVisible(true);
                        if (pushed && theModel.getCurrentRepoHelper().hasUnpushedTags()) {
                            pushTagsButton.setVisible(true);
                            pushButton.setVisible(false);
                        }
                        BusyWindow.hide();
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
     * Reverts the tree to remove the changes in the most recent commit
     */
    public void handleRevertButton(CommitHelper commit) {
        try {
            logger.info("Revert button clicked");

            if(this.theModel.getCurrentRepoHelper() == null) throw new NoRepoLoadedException();

            BusyWindow.show();
            BusyWindow.setLoadingText("Reverting...");
            Thread th = new Thread(new Task<Void>(){
                @Override
                protected Void call() {
                    try{
                        theModel.getCurrentRepoHelper().revertToCommit(commit);
                        gitStatus();
                    } catch(MultipleParentsNotAllowedException e) {
                        if(commit.getParents().size() > 1) {
                            showCantRevertMultipleParentsNotification();
                        }
                        if (commit.getParents().size() == 0) {
                            showCantRevertZeroParentsNotification();
                        }
                    } catch(InvalidRemoteException e){
                        showNoRemoteNotification();
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
                    }finally {
                        BusyWindow.hide();
                    }
                    return null;
                }
            });
            th.setDaemon(true);
            th.setName("Git revert");
            th.start();
        }catch(NoRepoLoadedException e){
            this.showNoRepoLoadedNotification();
            setButtonsDisabled(true);
        }
    }

    /**
     * Resets the tree to the given commit
     * @param commit CommitHelper
     */
    public void handleResetButton(CommitHelper commit) {
        try {
            logger.info("Reset button clicked");

            if(this.theModel.getCurrentRepoHelper() == null) throw new NoRepoLoadedException();

            BusyWindow.show();
            BusyWindow.setLoadingText("Resetting...");
            Thread th = new Thread(new Task<Void>(){
                @Override
                protected Void call() {
                    try{
                        theModel.getCurrentRepoHelper().resetToCommit(commit);
                        gitStatus();
                    }catch(InvalidRemoteException e){
                        showNoRemoteNotification();
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
                    }finally {
                        BusyWindow.hide();
                    }
                    return null;
                }
            });
            th.setDaemon(true);
            th.setName("Git reset");
            th.start();
        }catch(NoRepoLoadedException e){
            this.showNoRepoLoadedNotification();
            setButtonsDisabled(true);
        }
    }

    /**
     * Handles a click on the "Fetch" button. Calls gitFetch()
     */
    public void handleFetchButton(){
        logger.info("Fetch button clicked");
        RepositoryMonitor.pause();
        gitFetch();
        RepositoryMonitor.unpause();
        submitLog();
    }

    /**
     * Queries the remote for new commits, and updates the local
     * remote as necessary.
     * Equivalent to `git fetch`
     */
    public synchronized void gitFetch(){
        try{

            if(this.theModel.getCurrentRepoHelper() == null) throw new NoRepoLoadedException();

            BusyWindow.show();
            BusyWindow.setLoadingText("Fetching...");
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
                    }finally {
                        BusyWindow.hide();
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
        CommitTreeController.focusCommitInGraph(theModel.getCurrentRepoHelper().getBranchModel().getCurrentBranchHead());
    }

    /**
     * Updates the trees, changed files, and branch information. Equivalent
     * to 'git status'
     */
    public void gitStatus(){
        RepositoryMonitor.pause();

        Platform.runLater(() -> {
            try{
                localCommitTreeModel.update();
                remoteCommitTreeModel.update();
                //if (theModel.getCurrentRepoHelper() != null &&
                        //theModel.getCurrentRepoHelper().updateTags()) {
                    //if (theModel.getCurrentRepoHelper().hasTagsWithUnpushedCommits()) {
                        //showTagPointsToUnpushedCommitNotification();
                    //}
                //}

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
            } catch(Exception e) {
                showGenericErrorNotification();
                e.printStackTrace();
            } finally{
                RepositoryMonitor.unpause();
            }
        });
    }

    /**
     * When the image representing the remote repo is clicked, go to the
     * corresponding remote url
     * @param event the mouse event corresponding to the click
     */
    public void handleRemoteMouseClick(MouseEvent event){
        if(event.getButton() != MouseButton.PRIMARY) return;
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
                // Use desktop if the system isn't linux
                if (!SystemUtils.IS_OS_LINUX) {
                    Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
                    if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE))
                        desktop.browse(new URI(remoteURL));
                }else {
                    Runtime runtime = Runtime.getRuntime();
                    String[] args = {"xdg-open", remoteURL};
                    runtime.exec(args);
                }
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

    /**
     * Initializes each panel of the view
     */
	private synchronized void initPanelViews() {
        try {
            workingTreePanelView.drawDirectoryView();
            allFilesPanelView.drawDirectoryView();
            remoteCommitTreeModel.init();
            localCommitTreeModel.init();
            this.setBrowserURL();
        } catch (GitAPIException | IOException e) {
            showGenericErrorNotification();
        }
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
            remoteImage.setVisible(!disable);
            commitMessageField.setDisable(disable);
            browserText.setVisible(!disable);
            branchesButton.setDisable(disable);
            workingTreePanelTab.setDisable(disable);
            allFilesPanelTab.setDisable(disable);
            branchDropdownSelector.setDisable(disable);
            removeRecentReposButton.setDisable(disable);
            repoDropdownSelector.setDisable(disable);
        });

        notificationPane.setOnMousePressed(event -> {
            if (disable) showNoRepoLoadedNotification();
        });
    }

    /**
     * Gets the selected branch from the dropdown and calls checkout
     */
    public void handleCheckoutDropdown() {
        LocalBranchHelper selectedBranch = this.branchDropdownSelector.getValue();
        checkoutBranch(selectedBranch);
    }

    /**
     * Checks out the selected branch and updates the UI
     * @param selectedBranch the branch to check out
     */
    public void checkoutBranch(LocalBranchHelper selectedBranch) {
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
            if(this.theModel.getCurrentRepoHelper() == null && this.theModel.getAllRepoHelpers().size() >= 0) {
                // (There's no repo for buttons to interact with, but there are repos in the menu bar)
                setButtonsDisabled(true);
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
                // Use Desktop to open the current directory unless it's Linux
                if (!SystemUtils.IS_OS_LINUX) {
                    Desktop.getDesktop().open(this.theModel.getCurrentRepoHelper().localPath.toFile());
                }
                else {
                    Runtime runtime = Runtime.getRuntime();
                    String[] args = {"nautilus",this.theModel.getCurrentRepoHelper().localPath.toFile().toString()};
                    runtime.exec(args);
                }
            }catch(IOException | IllegalArgumentException e){
                this.showFailedToOpenLocalNotification();
                e.printStackTrace();
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
            this.notificationPane.setText("You need to load a repository before you can perform operations on it. Click on the plus sign in the upper left corner!");

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

            this.notificationPane.getActions().setAll(authAction);*/

            this.notificationPane.getActions().clear();
            this.notificationPane.show();
        });
    }

    private void showRepoWasNotLoadedNotification() {
        Platform.runLater(() -> {
            logger.warn("Repo not loaded warning");
            this.notificationPane.setText("Something went wrong, so no repository was loaded.");

            this.notificationPane.getActions().clear();
            this.notificationPane.show();
        });
    }

    private void showCheckoutConflictsNotification(List<String> conflictingPaths) {
        Platform.runLater(() -> {
            this.notificationPane.setText("You can't switch to that branch because there would be a merge conflict. Stash your changes or resolve conflicts first.");

            Action seeConflictsAction = new Action("See conflicts", e -> {
                this.notificationPane.hide();
                PopUpWindows.showCheckoutConflictsAlert(conflictingPaths);
            });

            this.notificationPane.getActions().clear();
            this.notificationPane.getActions().setAll(seeConflictsAction);

            this.notificationPane.show();
        });
    }

    private void showMergeConflictsNotification(List<String> conflictingPaths){
        Platform.runLater(() -> {
            this.notificationPane.setText("Can't complete merge due to conflicts. Resolve the conflicts and commit all files to complete merging");

            Action seeConflictsAction = new Action("See conflicting files", e -> {
                this.notificationPane.hide();
                PopUpWindows.showtMergeConflictsAlert(conflictingPaths);
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

    private void showSameRepoLoadedNotification() {
        Platform.runLater(() -> {
            logger.warn("Same repo loaded");
            this.notificationPane.setText("That repository is already open");

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
            // check if there are still new changes, in case this value changes while waiting in the
            //  JavaFXApplication Thread queue
            if(RepositoryMonitor.hasFoundNewRemoteChanges.getValue()) {
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
            }
        });
    }

    private void showNoFilesStagedForCommitNotification(){
        Platform.runLater(() -> {
            logger.warn("No files staged for commit warning");
            this.notificationPane.setText("You need to add files before commiting");

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

    private void showNoRemoteTrackingNotification() {
        Platform.runLater(() -> {
            logger.warn("No remote tracking for current branch notification.");
            this.notificationPane.setText("There is no remote tracking information for the current branch.");

            this.notificationPane.getActions().clear();
            this.notificationPane.show();
        });
    }

    private void showCantRevertMultipleParentsNotification() {
        Platform.runLater(() -> {
            logger.warn("Tried to revert commit with multiple parents.");
            this.notificationPane.setText("You cannot revert that commit because it has two parents.");

            this.notificationPane.getActions().clear();
            this.notificationPane.show();
        });
    }

    private void showCantRevertZeroParentsNotification() {
        Platform.runLater(() -> {
            logger.warn("Tried to revert commit with zero parents.");
            this.notificationPane.setText("You cannot revert that commit because it has zero parents.");

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

            logger.info("Opened branch manager window");
            // Create and display the Stage:
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/elegit/fxml/BranchManager.fxml"));
            fxmlLoader.load();
            BranchManagerController branchManagerController = fxmlLoader.getController();
            NotificationPane fxmlRoot = fxmlLoader.getRoot();
            branchManagerController.showStage(fxmlRoot);
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
            stage.setOnCloseRequest(event -> logger.info("Closed legend"));
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
                link.setOnAction(event -> {
                    logger.info("Delete tag dialog started.");
                    if (t.presentDeleteDialog()) {
                        try {
                            theModel.getCurrentRepoHelper().deleteTag(t.getName());
                        } catch (MissingRepoException | GitAPIException e) {
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

            commitInfoMessageText.setText(theModel.getCurrentRepoHelper().getCommitDescriptorString(commit, true));
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
        Button removeSelectedButton = new Button("Remove repository shortcuts from Elegit");

        PopOver popover = new PopOver(new VBox(repoCheckListView, removeSelectedButton));
        popover.setTitle("Manage Recent Repositories");

        removeSelectedButton.setOnAction(e -> {
            logger.info("Removed repos");
            List<RepoHelper> checkedItems = repoCheckListView.getCheckModel().getCheckedItems();
            this.theModel.removeRepoHelpers(checkedItems);
            popover.hide();

            if (!this.theModel.getAllRepoHelpers().isEmpty() && !this.theModel.getAllRepoHelpers().contains(theModel.getCurrentRepoHelper())) {
                int newIndex = this.theModel.getAllRepoHelpers().size()-1;
                RepoHelper newCurrentRepo = this.theModel.getAllRepoHelpers()
                        .get(newIndex);

                handleRecentRepoMenuItem(newCurrentRepo);
                repoDropdownSelector.setValue(newCurrentRepo);

                this.refreshRecentReposInDropdown();
            } else if (this.theModel.getAllRepoHelpers().isEmpty()){
                TreeLayout.stopMovingCells();
                theModel.resetSessionModel();
                workingTreePanelView.resetFileStructurePanelView();
                allFilesPanelView.resetFileStructurePanelView();
                initialize();
            }else {
                try {
                    theModel.openRepoFromHelper(theModel.getCurrentRepoHelper());
                } catch (BackingStoreException | IOException | MissingRepoException | ClassNotFoundException e1) {
                    e1.printStackTrace();
                }
            }

            this.refreshRecentReposInDropdown();
        });

        popover.show(this.removeRecentReposButton);
    }

    public void submitLog() {
        try {
            String lastUUID = theModel.getLastUUID();
            theModel.setLastUUID(d.submitData(lastUUID));
        } catch (BackingStoreException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            try { theModel.setLastUUID(""); }
            catch (Exception f) { }
        }
    }
}
