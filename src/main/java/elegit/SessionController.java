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
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import javafx.scene.text.Font;
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
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.awt.*;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.BackingStoreException;

/**
 * The controller for the entire session.
 */
public class SessionController {

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
    public Button pushTagsButton;
    public Button pushButton;
    public Button fetchButton;
    public Button addButton;
    public Button removeButton;
    public Button mergeButton;

    public Tab workingTreePanelTab;
    public Tab allFilesPanelTab;
    public WorkingTreePanelView workingTreePanelView;
    public AllFilesPanelView allFilesPanelView;

	public CommitTreePanelView commitTreePanelView;

    public ImageView remoteImage;

    public String commitInfoNameText = "";
    public Button commitInfoNameCopyButton;
    public Button commitInfoGoToButton;
    public TextArea commitInfoMessageText;
    public Text currentLocalBranchText;
    public Text currentRemoteTrackingBranchText;
    public Button addDeleteBranchButton;
    public Button checkoutButton;

    public ScrollPane tagsPane;
    public Label tagsLabel;
    public Button tagButton;
    public TextArea tagNameField;

    public Text browserText;
    public URL remoteURL;

    public DataSubmitter d;

    public CommitTreeModel commitTreeModel;

    public BooleanProperty isWorkingTreeTabSelected;

    private volatile boolean isRecentRepoEventListenerBlocked = false;

    static final Logger logger = LogManager.getLogger(SessionController.class);

    public ContextMenu newRepoOptionsMenu;
    public MenuItem cloneOption;
    public MenuItem existingOption;
    public Text needToFetch;
    public Text commitsAheadText;

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

        // Creates the commit tree model
        this.commitTreeModel = new LocalCommitTreeModel(this.theModel, this.commitTreePanelView);

        // Passes theModel to panel views
        this.workingTreePanelView.setSessionModel(this.theModel);
        this.allFilesPanelView.setSessionModel(this.theModel);

        this.initializeLayoutParameters();

        this.setButtonIconsAndTooltips();
        this.setButtonsDisabled(true);
        this.initWorkingTreePanelTab();
        this.theModel.loadRecentRepoHelpersFromStoredPathStrings();
        this.theModel.loadMostRecentRepoHelper();

        this.initPanelViews();
        this.updateUIEnabledStatus();
        this.setRecentReposDropdownToCurrentRepo();
        this.refreshRecentReposInDropdown();

        this.initRepositoryMonitor();

        this.updateStatusText();

        // if there are conflicting files on startup, watches them for changes
        try {
            ConflictingFileWatcher.watchConflictingFiles(theModel.getCurrentRepoHelper());
        } catch (GitAPIException | IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Helper method to update the current local branch, remote tracking branch and
     * whether or not there are remote changes to fetch
     */
    private void updateStatusText(){
        boolean update;

        update = RepositoryMonitor.hasFoundNewRemoteChanges.get();
        String fetchText = update ? "New changes to fetch" : "Up to date";
        Color fetchColor = update ? Color.FIREBRICK : Color.FORESTGREEN;
        needToFetch.setText(fetchText);
        needToFetch.setFont(new Font(15));
        needToFetch.setFill(fetchColor);

        String localBranch = this.theModel.getCurrentRepoHelper().getBranchModel().getCurrentBranch().branchName;
        update = localBranch.equals(currentLocalBranchText.getText()) ? false : true;
        if (update) {
            currentLocalBranchText.setText(localBranch);
            currentLocalBranchText.setFont(new Font(15));
            currentLocalBranchText.setFill(Color.DODGERBLUE);
        }

        String remoteBranch="";
        BranchTrackingStatus status=null;
        try {
            remoteBranch = this.theModel.getCurrentRepoHelper().getBranchModel().getCurrentRemoteBranch();
            status = BranchTrackingStatus.of(this.theModel.getCurrentRepo(), localBranch);
        } catch (IOException e) {
            // Startup should catch any chance of this
        }
        if (remoteBranch==null) remoteBranch = "N/A";
        update = remoteBranch.equals(currentRemoteTrackingBranchText.getText()) ? false : true;
        if (update) {
            currentRemoteTrackingBranchText.setText(remoteBranch);
            currentRemoteTrackingBranchText.setFont(new Font(15));
            currentRemoteTrackingBranchText.setFill(Color.DODGERBLUE);
        }

        // Ahead/behind count
        int ahead=0, behind=0;
        String statusText="Up to date.";
        if (status!=null && remoteBranch!=null) {
            behind = status.getBehindCount();
            ahead = status.getAheadCount();
            if (ahead >0) {
                statusText="Ahead "+ahead+" commit";
                if (ahead > 1)
                    statusText+="s";
                if (behind > 0) {
                    statusText += " and behind " + behind + " commit";
                    if (behind > 1)
                        statusText+="s";
                }
                statusText+=".";
            } else if (behind > 0) {
                statusText = "Behind " + behind + " commit";
                if (behind > 1)
                    statusText+="s";
                statusText+=".";
            }
        }
        update = statusText.equals(branchStatusText.getText()) ? false : true;
        Color statusColor = statusText.equals("Up to date.") ? Color.FORESTGREEN : Color.FIREBRICK;
        if (update) {
            branchStatusText.setText(statusText);
            branchStatusText.setFont(new Font(10));
            branchStatusText.setFill(statusColor);
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
            if(newValue) updateStatusText();
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
        addButton.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
        removeButton.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
        addDeleteBranchButton.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
        mergeButton.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
        checkoutButton.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
        pushTagsButton.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
        pushButton.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
        fetchButton.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
        commitInfoNameCopyButton.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
        commitInfoGoToButton.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);

        // Set minimum sizes for other fields and views
        workingTreePanelView.setMinSize(Control.USE_PREF_SIZE, 200);
        allFilesPanelView.setMinSize(Control.USE_PREF_SIZE, 200);
        final int REPO_DROPDOWN_MAX_WIDTH = 147;
        repoDropdownSelector.setMaxWidth(REPO_DROPDOWN_MAX_WIDTH);
        tagNameField.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
        commitInfoMessageText.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
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
        this.addButton.setTooltip(new Tooltip(
                "Stage changes for selected files"
        ));
        this.removeButton.setTooltip(new Tooltip(
                "Delete selected files and remove them from Git"
        ));
        this.fetchButton.setTooltip(new Tooltip(
                "Download files from another repository to remote repository"
        ));
        this.pushButton.setTooltip(new Tooltip(
                "Update remote repository with local changes"
        ));
        this.pushButton.setTooltip(new Tooltip(
                "Revert the changes in the most recent commit"
        ));

        this.loadNewRepoButton.setTooltip(new Tooltip(
                "Load a new repository"
        ));
        this.mergeButton.setTooltip(new Tooltip(
                "Merge two commits together"
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
            Tooltip.install(browserText, URLTooltip);

            browserText.setFill(Color.DARKCYAN);
            browserText.setUnderline(true);
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
     * Adds all files that are selected if they can be added
     */
    public void handleAddButton() {
        try {
            logger.info("Add button clicked");
            if(this.theModel.getCurrentRepoHelper() == null) throw new NoRepoLoadedException();
            if(!this.theModel.getCurrentRepoHelper().exists()) throw new MissingRepoException();

            if(!workingTreePanelView.isAnyFileSelected()) throw new NoFilesSelectedToAddException();

            BusyWindow.show();
            BusyWindow.setLoadingText("Adding...");
            Thread th = new Thread(new Task<Void>(){
                @Override
                protected Void call() {
                    try{
                        ArrayList<Path> filePathsToAdd = new ArrayList<>();
                        // Try to add all files, throw exception if there are ones that can't be added
                        for(RepoFile checkedFile : workingTreePanelView.getCheckedFilesInDirectory()) {
                            if (checkedFile.canAdd())
                                filePathsToAdd.add(checkedFile.getFilePath());
                            else
                                throw new UnableToAddException(checkedFile.filePath.toString());
                        }

                        theModel.getCurrentRepoHelper().addFilePaths(filePathsToAdd);
                        gitStatus();

                    } catch(JGitInternalException e){
                        showGenericErrorNotification();
                        e.printStackTrace();
                    } catch (UnableToAddException e) {
                        showCannotAddFileNotification(e.filename);
                    } catch (GitAPIException | IOException e) {
                        showGenericErrorNotification();
                    } finally {
                        BusyWindow.hide();
                    }
                    return null;
                }
            });
            th.setDaemon(true);
            th.setName("Git add");
            th.start();
        } catch (NoFilesSelectedToAddException e) {
            this.showNoFilesSelectedForAddNotification();
        } catch (NoRepoLoadedException e) {
            this.showNoRepoLoadedNotification();
        } catch (MissingRepoException e) {
            this.showMissingRepoNotification();
        }
    }

    /**
     * Removes all files from staging area that are selected if they can be removed
     */
    public void handleRemoveButton() {
        try {
            logger.info("Remove button clicked");
            if(this.theModel.getCurrentRepoHelper() == null) throw new NoRepoLoadedException();
            if(!this.theModel.getCurrentRepoHelper().exists()) throw new MissingRepoException();

            if(!workingTreePanelView.isAnyFileSelected()) throw new NoFilesSelectedToRemoveException();

            BusyWindow.show();
            BusyWindow.setLoadingText("Removing...");
            Thread th = new Thread(new Task<Void>(){
                @Override
                protected Void call() {
                    try{
                        ArrayList<Path> filePathsToRemove = new ArrayList<>();
                        // Try to remove all files, throw exception if there are ones that can't be added
                        for(RepoFile checkedFile : workingTreePanelView.getCheckedFilesInDirectory()) {
                            if (checkedFile.canRemove())
                                filePathsToRemove.add(checkedFile.getFilePath());
                            else
                                throw new UnableToRemoveException(checkedFile.filePath.toString());
                        }

                        theModel.getCurrentRepoHelper().removeFilePaths(filePathsToRemove);
                        gitStatus();

                    } catch(JGitInternalException e){
                        showGenericErrorNotification();
                        e.printStackTrace();
                    } catch (UnableToRemoveException e) {
                        showCannotRemoveFileNotification(e.filename);
                    } catch (GitAPIException e) {
                        showGenericErrorNotification();
                    } finally {
                        BusyWindow.hide();
                    }
                    return null;
                }
            });
            th.setDaemon(true);
            th.setName("Git rm");
            th.start();
        } catch (NoFilesSelectedToRemoveException e) {
            this.showNoFilesSelectedForRemoveNotification();
        } catch (NoRepoLoadedException e) {
            this.showNoRepoLoadedNotification();
        } catch (MissingRepoException e) {
            this.showMissingRepoNotification();
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

            if(!workingTreePanelView.isAnyFileStaged()) throw new NoFilesStagedForCommitException();

            BusyWindow.show();
            BusyWindow.setLoadingText("Committing...");

            try{
                logger.info("Commit manager clicked");
                if(theModel.getCurrentRepoHelper() == null) throw new NoRepoLoadedException();

                logger.info("Opened commit manager window");
                // Create and display the Stage:
                FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/elegit/fxml/CommitView.fxml"));
                fxmlLoader.load();
                CommitController commitController = fxmlLoader.getController();
                commitController.isClosed.addListener((observable, oldValue, newValue) -> {
                    if (!oldValue.booleanValue()&&newValue.booleanValue())
                        gitStatus();
                });
                GridPane fxmlRoot = fxmlLoader.getRoot();
                commitController.showStage(fxmlRoot);
            }catch(IOException e){
                showGenericErrorNotification();
                e.printStackTrace();
            }catch(NoRepoLoadedException e){
                showNoRepoLoadedNotification();
                setButtonsDisabled(true);
            }catch(Exception e) {
                e.printStackTrace();
            }
            finally {
                BusyWindow.hide();
            }
        } catch(NoRepoLoadedException e){
            this.showNoRepoLoadedNotification();
            setButtonsDisabled(true);
        } catch(MissingRepoException e){
            this.showMissingRepoNotification();
            setButtonsDisabled(true);
            refreshRecentReposInDropdown();
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
                        theModel.getCurrentRepoHelper().tag(tagName, commitInfoNameText);

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
                commitTreeModel.update();
                workingTreePanelView.drawDirectoryView();
                allFilesPanelView.drawDirectoryView();
                updateStatusText();
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
            commitTreeModel.init();
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
    public void setButtonsDisabled(boolean disable) {
        Platform.runLater(() -> {
            openRepoDirButton.setDisable(disable);
            gitStatusButton.setDisable(disable);
            tagButton.setDisable(disable);
            commitButton.setDisable(disable);
            addButton.setDisable(disable);
            removeButton.setDisable(disable);
            pushTagsButton.setDisable(disable);
            pushButton.setDisable(disable);
            fetchButton.setDisable(disable);
            selectAllButton.setDisable(disable);
            deselectAllButton.setDisable(disable);
            remoteImage.setVisible(!disable);
            browserText.setVisible(!disable);
            workingTreePanelTab.setDisable(disable);
            allFilesPanelTab.setDisable(disable);
            removeRecentReposButton.setDisable(disable);
            repoDropdownSelector.setDisable(disable);
            addDeleteBranchButton.setDisable(disable);
            checkoutButton.setDisable(disable);
            mergeButton.setDisable(disable);
        });

        notificationPane.setOnMousePressed(event -> {
            if (disable) showNoRepoLoadedNotification();
            if (notificationPane.isShowing()) notificationPane.hide();
        });
        notificationPane.setShowFromTop(false);
    }

    /**
     * A helper helper method to enable or disable buttons/UI elements
     * depending on whether there is a repo open for the buttons to
     * interact with.
     */
    private void updateUIEnabledStatus() {
        if (this.theModel.getCurrentRepoHelper() == null && this.theModel.getAllRepoHelpers().size() >= 0) {
            // (There's no repo for buttons to interact with, but there are repos in the menu bar)
            setButtonsDisabled(true);
        } else {
            setButtonsDisabled(false);
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


    private void showNoFilesSelectedForAddNotification(){
        Platform.runLater(() -> {
            logger.warn("No files selected for add warning");
            this.notificationPane.setText("You need to select files to add");

            this.notificationPane.getActions().clear();
            this.notificationPane.show();
        });
    }


    private void showNoFilesSelectedForRemoveNotification(){
        Platform.runLater(() -> {
            logger.warn("No files staged for remove warning");
            this.notificationPane.setText("You need select files to remove");

            this.notificationPane.getActions().clear();
            this.notificationPane.show();
        });
    }


    private void showCannotAddFileNotification(String filename) {
        Platform.runLater(() -> {
            logger.warn("Cannot add file notification");
            this.notificationPane.setText("Cannot add "+filename+". It might already be added (staged).");

            this.notificationPane.getActions().clear();
            this.notificationPane.show();
        });
    }

    private void showCannotRemoveFileNotification(String filename) {
        Platform.runLater(() -> {
            logger.warn("Cannot remove file notification");
            this.notificationPane.setText("Cannot remove "+filename+" because it hasn't been staged yet.");

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

    private void showNoCommitsFetchedNotification(){
        Platform.runLater(() -> {
            logger.warn("No commits fetched warning");
            this.notificationPane.setText("No new commits were fetched");

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

    // may be used - method which called these moved to CreateBranchWindowController
    /*private void showInvalidBranchNameNotification() {
        logger.warn("Invalid branch name notification");
        this.notificationPane.setText("That branch name is invalid.");

        this.notificationPane.getActions().clear();
        this.notificationPane.show();
    }

    private void showNoCommitsYetNotification() {
        logger.warn("No commits yet notification");
        this.notificationPane.setText("You cannot make a branch since your repo has no commits yet. Make a commit first!");

        this.notificationPane.getActions().clear();
        this.notificationPane.show();
    }

    private void showGenericGitErrorNotification() {
        logger.warn("Git error notification");
        this.notificationPane.setText("Sorry, there was a git error.");

        this.notificationPane.getActions().clear();
        this.notificationPane.show();
    }*/

    // END: ERROR NOTIFICATIONS ^^^

    /**
     * Opens up the current repo helper's Branch Manager window after
     * passing in this SessionController object, so that the
     * BranchCheckoutController can update the main window's views.
     */
    public void showBranchCheckout() {
        try{
            logger.info("Branch checkout clicked");
            if(this.theModel.getCurrentRepoHelper() == null) throw new NoRepoLoadedException();

            logger.info("Opened branch checkout window");
            // Create and display the Stage:
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/elegit/fxml/BranchCheckout.fxml"));
            fxmlLoader.load();
            BranchCheckoutController branchCheckoutController = fxmlLoader.getController();
            NotificationPane fxmlRoot = fxmlLoader.getRoot();
            branchCheckoutController.showStage(fxmlRoot);
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
            commitInfoNameText = commit.getName();
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
        content.putString(commitInfoNameText);
        clipboard.setContent(content);
    }

    /**
     * Jumps to the selected commit in the tree display
     */
    public void handleGoToCommitButton(){
        logger.info("Go to commit button clicked");
        String id = commitInfoNameText;
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

    /**
     * Pops up a window where the user can create a new branch
     */
    public void handleNewBranchButton() {
        try{
            logger.info("Create/delete branch button clicked");
            if(this.theModel.getCurrentRepoHelper() == null) throw new NoRepoLoadedException();

            logger.info("Opened create/delete branch window");
            // Create and display the Stage:
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/elegit/fxml/CreateDeleteBranchWindow.fxml"));
            fxmlLoader.load();
            CreateDeleteBranchWindowController createDeleteBranchController = fxmlLoader.getController();
            StackPane fxmlRoot = fxmlLoader.getRoot();
            createDeleteBranchController.showStage(fxmlRoot);
        }catch(IOException e){
            this.showGenericErrorNotification();
            e.printStackTrace();
        }catch(NoRepoLoadedException e){
            this.showNoRepoLoadedNotification();
            setButtonsDisabled(true);
        }
    }

    /**
     * shows the merge window
     */
    public void handleGeneralMergeButton() {
        try{
            logger.info("Merge button clicked");
            if(this.theModel.getCurrentRepoHelper() == null) throw new NoRepoLoadedException();

            logger.info("Opened merge window");
            // Create and display the Stage:
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/elegit/fxml/MergeWindow.fxml"));
            fxmlLoader.load();
            MergeWindowController mergeWindowController = fxmlLoader.getController();
            NotificationPane fxmlRoot = fxmlLoader.getRoot();
            mergeWindowController.showStage(fxmlRoot);
        }catch(IOException e){
            this.showGenericErrorNotification();
            e.printStackTrace();
        }catch(NoRepoLoadedException e){
            this.showNoRepoLoadedNotification();
            setButtonsDisabled(true);
        }
    }
}
