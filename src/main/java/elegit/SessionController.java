package elegit;

import de.jensd.fx.glyphs.GlyphsDude;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import elegit.exceptions.*;
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
import javafx.scene.control.Button;
import javafx.scene.control.*;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.apache.commons.lang3.SystemUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.controlsfx.control.CheckListView;
import org.controlsfx.control.PopOver;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.*;
import org.eclipse.jgit.dircache.InvalidPathException;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RemoteRefUpdate;
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
    public Button openRepoDirButton;
    public Button gitStatusButton;
    public Button commitButton;
    public Button pushButton;
    public Button fetchButton;
    public Button addButton;
    public Button removeButton;
    public Button checkoutFileButton;
    public Button mergeButton;
    public Button commitInfoNameCopyButton;
    public Button commitInfoGoToButton;
    public Button addDeleteBranchButton;
    public Button checkoutButton;
    public Button tagButton;
    public Button changeLoginButton;

    private SessionModel theModel;

    public Node root;

    public Tab workingTreePanelTab;
    public Tab allFilesPanelTab;

    public WorkingTreePanelView workingTreePanelView;
    public AllFilesPanelView allFilesPanelView;

	public CommitTreePanelView commitTreePanelView;

    CommitTreeModel commitTreeModel;

    public ImageView remoteImage;

    private String commitInfoNameText = "";

    public TextArea commitInfoMessageText;
    public TextArea tagNameField;

    public Text currentLocalBranchText;
    public Text currentRemoteTrackingBranchText;
    public Text browserText;
    public Text needToFetch;
    public Text branchStatusText;

    public URL remoteURL;

    private DataSubmitter d;

    private BooleanProperty isWorkingTreeTabSelected;
    static SimpleBooleanProperty anythingChecked;

    private volatile boolean isRecentRepoEventListenerBlocked = false;

    static final Logger logger = LogManager.getLogger(SessionController.class);

    public ContextMenu newRepoOptionsMenu;
    public ContextMenu pushMenu;

    public MenuItem cloneOption;
    public MenuItem existingOption;

    public Hyperlink legendLink;

    @FXML private AnchorPane anchorRoot;

    // Notification pane
    @FXML private StackPane notificationPane;
    @FXML private NotificationController notificationPaneController;

    boolean authenticateOnNextCommand;

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

        this.notificationPaneController.bindParentBounds(anchorRoot.heightProperty());

        // if there are conflicting files on startup, watches them for changes
        try {
            ConflictingFileWatcher.watchConflictingFiles(theModel.getCurrentRepoHelper());
        } catch (GitAPIException | IOException e) {
            e.printStackTrace();
        }

        authenticateOnNextCommand = false;


    }

    /**
     * Helper method to update the current local branch, remote tracking branch and
     * whether or not there are remote changes to fetch
     */
    private void updateStatusText(){
        if (this.theModel.getCurrentRepoHelper()==null) return;
        boolean update;

        update = RepositoryMonitor.hasFoundNewRemoteChanges.get();
        String fetchText = update ? "New changes to fetch" : "Up to date";
        Color fetchColor = update ? Color.FIREBRICK : Color.FORESTGREEN;
        needToFetch.setText(fetchText);
        needToFetch.setFont(new Font(15));
        needToFetch.setFill(fetchColor);

        String localBranch = this.theModel.getCurrentRepoHelper().getBranchModel().getCurrentBranch().getAbbrevName();
        update = !localBranch.equals(currentLocalBranchText.getText());
        if (update) {
            currentLocalBranchText.setText(localBranch);
            currentLocalBranchText.setFont(new Font(15));
            currentLocalBranchText.setFill(Color.DODGERBLUE);
        }

        String remoteBranch = "N/A";
        try {
            remoteBranch = this.theModel.getCurrentRepoHelper().getBranchModel().getCurrentRemoteAbbrevBranch();
        } catch (IOException e) {
            this.showGenericErrorNotification();
        }
        if (remoteBranch==null) remoteBranch = "N/A";

        update = !remoteBranch.equals(currentRemoteTrackingBranchText.getText());
        if (update) {
            currentRemoteTrackingBranchText.setText(remoteBranch);
            currentRemoteTrackingBranchText.setFont(new Font(15));
            currentRemoteTrackingBranchText.setFill(Color.DODGERBLUE);
        }

        // Ahead/behind count
        int ahead=0, behind=0;
        try {
            ahead = this.theModel.getCurrentRepoHelper().getAheadCount();
            behind = this.theModel.getCurrentRepoHelper().getBehindCount();
        } catch (IOException e) {
            this.showGenericErrorNotification();
        }
        String statusText="Up to date.";
        if (ahead >0) {
            statusText=currentLocalBranchText.getText() + " ahead of " + currentRemoteTrackingBranchText.getText() + " by " + ahead + " commit";
            if (ahead > 1)
                statusText+="s";
            if (behind > 0) {
                statusText += "\nand behind by " + behind + " commit";
                if (behind > 1)
                    statusText+="s";
            }
            statusText+=".";
        } else if (behind > 0) {
            statusText = currentLocalBranchText.getText() + " behind " + currentRemoteTrackingBranchText.getText() + " by " + behind + " commit";
            if (behind > 1)
                statusText+="s";
            statusText+=".";
        }
        update = !statusText.equals(branchStatusText.getText());
        Color statusColor = statusText.equals("Up to date.") ? Color.FORESTGREEN : Color.FIREBRICK;
        if (update) {
            branchStatusText.setText(statusText);
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

        RepositoryMonitor.startWatching(theModel, this);
        RepositoryMonitor.hasFoundNewRemoteChanges.addListener((observable, oldValue, newValue) -> {
            if(newValue) updateStatusText();
        });
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
        checkoutFileButton.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
        removeButton.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
        addDeleteBranchButton.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
        mergeButton.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
        checkoutButton.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
        pushButton.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
        fetchButton.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
        commitInfoNameCopyButton.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
        commitInfoGoToButton.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
        changeLoginButton.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);

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
        anythingChecked = new SimpleBooleanProperty(false);
        checkoutFileButton.disableProperty().bind(anythingChecked.not());
        addButton.disableProperty().bind(anythingChecked.not());
        removeButton.disableProperty().bind(anythingChecked.not());

        legendLink.setFont(new Font(12));

        pushButton.setOnMouseClicked(event -> {
            if(event.getButton() == MouseButton.SECONDARY){
                if(pushMenu != null){
                    pushMenu.show(pushButton, event.getScreenX(), event.getScreenY());
                }
            }
            event.consume();
        });

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
        this.checkoutFileButton.setTooltip(new Tooltip(
                "Checkout files from HEAD (discard all changes)"
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
     * Populates the browser image with the remote URL
     */
    private void setBrowserURL() {
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
     * A helper method for enabling/disabling buttons.
     *
     * @param disable a boolean for whether or not to disable the buttons.
     */
    void setButtonsDisabled(boolean disable) {
        Platform.runLater(() -> {
            openRepoDirButton.setDisable(disable);
            gitStatusButton.setDisable(disable);
            tagButton.setDisable(disable);
            commitButton.setDisable(disable);
            pushButton.setDisable(disable);
            fetchButton.setDisable(disable);
            remoteImage.setVisible(!disable);
            browserText.setVisible(!disable);
            workingTreePanelTab.setDisable(disable);
            allFilesPanelTab.setDisable(disable);
            removeRecentReposButton.setDisable(disable);
            repoDropdownSelector.setDisable(disable);
            addDeleteBranchButton.setDisable(disable);
            checkoutButton.setDisable(disable);
            mergeButton.setDisable(disable);
            needToFetch.setVisible(!disable);
            currentLocalBranchText.setVisible(!disable);
            currentRemoteTrackingBranchText.setVisible(!disable);
            branchStatusText.setVisible(!disable);
        });

        root.setOnMouseClicked(event -> {
            if (disable) showNoRepoLoadedNotification();
            if (this.notificationPaneController.isListPaneVisible()) this.notificationPaneController.toggleNotificationList();
        });
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
            showNotAuthorizedNotification();
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

                    }catch (JGitInternalException e){
                        showJGitInternalError(e);
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
                        showJGitInternalError(e);
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

    /**Basic handler for the checkout button. Just checks out the given file
     * from the index of HEAD
     *
     * @param filePath the path of the file to checkout from the index
     */
    void handleCheckoutButton(Path filePath) {
        try {
            logger.info("Checkout file button clicked");
            if(this.theModel.getCurrentRepoHelper() == null) throw new NoRepoLoadedException();
            if(!this.theModel.getCurrentRepoHelper().exists()) throw new MissingRepoException();
            theModel.getCurrentRepoHelper().checkoutFile(filePath);
        } catch (NoRepoLoadedException e) {
            showNoRepoLoadedNotification();
        } catch (MissingRepoException e) {
            showMissingRepoNotification();
        } catch (GitAPIException e) {
            showGenericErrorNotification();
        }
    }

    /**
     * Handler for the checkout button
     */
    public void handleCheckoutButton() {
        try {
            logger.info("Checkout button clicked");
            if(this.theModel.getCurrentRepoHelper() == null) throw new NoRepoLoadedException();
            if(!this.theModel.getCurrentRepoHelper().exists()) throw new MissingRepoException();

            if(!workingTreePanelView.isAnyFileSelected()) throw new NoFilesSelectedToAddException();

            BusyWindow.show();
            BusyWindow.setLoadingText("Checking out...");
            Thread th = new Thread(new Task<Void>(){
                @Override
                protected Void call() {
                    try{
                        ArrayList<Path> filePathsToCheckout = new ArrayList<>();
                        // Try to add all files, throw exception if there are ones that can't be added
                        for(RepoFile checkedFile : workingTreePanelView.getCheckedFilesInDirectory()) {
                            filePathsToCheckout.add(checkedFile.getFilePath());
                        }
                        theModel.getCurrentRepoHelper().checkoutFiles(filePathsToCheckout);
                        gitStatus();

                    }catch (JGitInternalException e){
                        showJGitInternalError(e);
                    } catch (GitAPIException e) {
                        showGenericErrorNotification();
                    } finally {
                        BusyWindow.hide();
                    }
                    return null;
                }
            });
            th.setDaemon(true);
            th.setName("Git checkout");
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
     * Shows the checkout files dialogue for a given commit
     *
     * @param commitHelper the commit to checkout files from
     */
    public void handleCheckoutFilesButton(CommitHelper commitHelper) {
        try{
            logger.info("Checkout files from commit button clicked");
            if(this.theModel.getCurrentRepoHelper() == null) throw new NoRepoLoadedException();

            logger.info("Opened checkout files window");
            // Create and display the Stage:
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/elegit/fxml/CheckoutFiles.fxml"));
            fxmlLoader.load();
            CheckoutFilesController checkoutFilesController = fxmlLoader.getController();
            checkoutFilesController.setSessionController(this);
            checkoutFilesController.setCommitHelper(commitHelper);
            AnchorPane fxmlRoot = fxmlLoader.getRoot();
            checkoutFilesController.showStage(fxmlRoot);
        }catch(IOException e){
            this.showGenericErrorNotification();
            e.printStackTrace();
        }catch(NoRepoLoadedException e){
            this.showNoRepoLoadedNotification();
            setButtonsDisabled(true);
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
                    if (!oldValue && newValue)
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
            if (theModel.getCurrentRepoHelper().getTagModel().getTag(tagName) != null) {
                throw new TagNameExistsException();
            }

            if(tagName.length() == 0) throw new NoTagNameException();

            Thread th = new Thread(new Task<Void>(){
                @Override
                protected Void call() {
                    try{
                        theModel.getCurrentRepoHelper().getTagModel().tag(tagName, commitInfoNameText);

                        // Now clear the tag text and a view reload ( or `git status`) to show that something happened
                        tagNameField.clear();
                        gitStatus();
                    }catch(JGitInternalException e){
                        showJGitInternalError(e);
                    } catch(MissingRepoException e){
                        showMissingRepoNotification();
                        setButtonsDisabled(true);
                        refreshRecentReposInDropdown();
                    } catch (TransportException e) {
                        showNotAuthorizedNotification();
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
                    tagNameField.setText("");
                    clearSelectedCommit();
                    selectCommit(theModel.getCurrentRepoHelper().getTagModel().getTag(tagName).getCommitId());

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

    enum PushType {BRANCH, ALL};

    public void handlePushButton() {
        pushBranchOrAll(PushType.BRANCH);
    }

    public void handlePushAllButton() {
        pushBranchOrAll(PushType.ALL);
    }

    /**
     * Performs a `git push` on either current branch or all branches, depending on enum parameter.
     */
    public void pushBranchOrAll(PushType pushType) {
        try {
            logger.info("Push button clicked");

            if(this.theModel.getCurrentRepoHelper() == null) throw new NoRepoLoadedException();
            if(!this.theModel.getCurrentRepoHelper().canPush()) throw new NoCommitsToPushException();

            final RepoHelperBuilder.AuthDialogResponse response;
            if (authenticateOnNextCommand) {
                response = RepoHelperBuilder.getAuthCredentialFromDialog();
            } else {
                response = null;
            }

            BusyWindow.show();
            BusyWindow.setLoadingText("Pushing...");
            Thread th = new Thread(new Task<Void>(){
                @Override
                protected Void call() {
                    boolean pushed = false;
                    boolean authorizationSucceeded = true;
                    try{
                        RepositoryMonitor.resetFoundNewChanges(false);
                        RepoHelper helper = theModel.getCurrentRepoHelper();
                        if (response != null) {
                            helper.ownerAuth =
                                    new UsernamePasswordCredentialsProvider(response.username, response.password);
                        }
                        if (pushType == PushType.BRANCH) {
                            helper.pushCurrentBranch();
                        } else if (pushType == PushType.ALL) {
                            helper.pushAll();
                        } else {
                            assert false : "PushType enum case not handled";
                        }
                        gitStatus();
                    }  catch(InvalidRemoteException e){
                        showNoRemoteNotification();
                    }
                    catch(PushToAheadRemoteError e) {
                        showPushToAheadRemoteNotification(e.isAllRefsRejected());
                    }
                    catch (TransportException e) {
                        if (e.getMessage().contains("git-receive-pack not found")) {
                            // The error has this message if there is no longer a remote to push to
                            showLostRemoteNotification();
                        } else {
                            showNotAuthorizedNotification();
                            authorizationSucceeded = false;
                        }
                    } catch(MissingRepoException e){
                        showMissingRepoNotification();
                        setButtonsDisabled(true);
                        refreshRecentReposInDropdown();
                    } catch(Exception e) {
                        showGenericErrorNotification();
                        e.printStackTrace();
                    } finally{
                        BusyWindow.hide();
                        if (authorizationSucceeded) {
                            authenticateOnNextCommand = false;
                        } else {
                            authenticateOnNextCommand = true;
                            Platform.runLater(() -> {
                                pushBranchOrAll(pushType);
                            });
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
        }catch(IOException e) {
            this.showGenericErrorNotification();
        } catch (CancelledAuthorizationException e) {
            this.showCommandCancelledNotification();
        }
    }


    /**
     * Performs a `git push --tags`
     */
    public void handlePushTagsButton() {
        try {
            logger.info("Push tags button clicked");

            if(this.theModel.getCurrentRepoHelper() == null) throw new NoRepoLoadedException();

            Thread th = new Thread(new Task<Void>(){
                @Override
                protected Void call() {
                    boolean tagsPushed = true;
                    Iterable<PushResult> results = null;
                    try{
                        RepositoryMonitor.resetFoundNewChanges(false);
                        results = theModel.getCurrentRepoHelper().pushTags();
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
                            showNotAuthorizedNotification();
                        }
                        tagsPushed = false;
                    } catch(MissingRepoException e){
                        showMissingRepoNotification();
                        setButtonsDisabled(true);
                        refreshRecentReposInDropdown();
                        tagsPushed = false;
                    } catch(Exception e) {
                        showGenericErrorNotification();
                        e.printStackTrace();
                        tagsPushed = false;
                    } finally {
                        boolean upToDate = true;
                        if (tagsPushed) {
                            if (results == null) upToDate = false;
                            else
                                for (PushResult result : results)
                                    for (RemoteRefUpdate update : result.getRemoteUpdates())
                                        if (update.getStatus() == RemoteRefUpdate.Status.OK)
                                            upToDate=false;
                        }
                        if (upToDate) showTagsUpToDateNotification();
                        else showTagsUpdatedNotification();
                    }
                    return null;
                }
            });
            th.setDaemon(true);
            th.setName("Git push --tags");
            th.start();
        }catch(NoRepoLoadedException e){
            this.showNoRepoLoadedNotification();
            setButtonsDisabled(true);
        }
    }


    /**
     * Checks out the selected local branch
     * @param selectedBranch the branch to check out
     * @return true if the checkout successfully happens, false if there is an error
     */
    public boolean checkoutBranch(BranchHelper selectedBranch) {
        if(selectedBranch == null) return false;
        // Track the branch if it is a remote branch that we're not yet tracking
        if (selectedBranch instanceof RemoteBranchHelper) {
            try {
                theModel.getCurrentRepoHelper().getBranchModel().trackRemoteBranch((RemoteBranchHelper) selectedBranch);
            } catch (RefAlreadyExistsException e) {
                showRefAlreadyExistsNotification();
            } catch (Exception e) {
                showGenericErrorNotification();
            }
        }
        try {
            selectedBranch.checkoutBranch();

            // If the checkout worked, update the branch heads and focus on that commit
            CommitTreeController.setBranchHeads(CommitTreeController.getCommitTreeModel(), theModel.getCurrentRepoHelper());
            CommitTreeController.focusCommitInGraph(theModel.getCurrentRepoHelper().getBranchModel().getCurrentBranchHead());
            gitStatus();
            return true;
        } catch (JGitInternalException e){
            showJGitInternalError(e);
        } catch (CheckoutConflictException e){
            showCheckoutConflictsNotification(e.getConflictingPaths());
        } catch (GitAPIException | IOException e) {
            showGenericErrorNotification();
        }
        return false;
    }

    /**
     * Deletes the selected branch
     *
     * @param selectedBranch the branch selected to delete
     */
    public void deleteBranch(BranchHelper selectedBranch) {
        BranchModel branchModel = theModel.getCurrentRepoHelper().getBranchModel();
        try {
            if (selectedBranch != null) {
                RemoteRefUpdate.Status deleteStatus;

                if (selectedBranch instanceof LocalBranchHelper) {
                    branchModel.deleteLocalBranch((LocalBranchHelper) selectedBranch);
                    updateUser(selectedBranch.getBranchName() + " deleted.");
                }else {
                    deleteStatus = branchModel.deleteRemoteBranch((RemoteBranchHelper) selectedBranch);
                    String updateMessage = selectedBranch.getBranchName();
                    // There are a number of possible cases, see JGit's documentation on RemoteRefUpdate.Status
                    // for the full list.
                    switch (deleteStatus) {
                        case OK:
                            updateMessage += " deleted.";
                            break;
                        case NON_EXISTING:
                            updateMessage += " no longer\nexists on the server.";
                        default:
                            updateMessage += " deletion\nfailed.";
                    }
                    updateUser(updateMessage);
                }
            }
        } catch (NotMergedException e) {
            logger.warn("Can't delete branch because not merged warning");
            Platform.runLater(() -> {
                if(PopUpWindows.showForceDeleteBranchAlert() && selectedBranch instanceof LocalBranchHelper) {
                    // If we need to force delete, then it must be a local branch
                    forceDeleteBranch((LocalBranchHelper) selectedBranch);
                }
            });
            this.showNotMergedNotification(selectedBranch);
        } catch (CannotDeleteCurrentBranchException e) {
            logger.warn("Can't delete current branch warning");
            this.showCannotDeleteBranchNotification(selectedBranch);
        } catch (TransportException e) {
            this.showNotAuthorizedNotification();
        } catch (IOException | GitAPIException e) {
            logger.warn("IO error");
            this.showGenericErrorNotification();
        } finally {
            gitStatus();
        }
    }

    /**
     * force deletes a branch
     * @param branchToDelete LocalBranchHelper
     */
    private void forceDeleteBranch(LocalBranchHelper branchToDelete) {
        BranchModel branchModel = theModel.getCurrentRepoHelper().getBranchModel();
        logger.info("Deleting local branch");

        try {
            if (branchToDelete != null) {
                // Local delete:
                branchModel.forceDeleteLocalBranch(branchToDelete);

                // Reset the branch heads
                CommitTreeController.setBranchHeads(commitTreeModel, theModel.getCurrentRepoHelper());

                updateUser(" deleted.");
            }
        } catch (CannotDeleteCurrentBranchException e) {
            this.showCannotDeleteBranchNotification(branchToDelete);
        } catch (GitAPIException e) {
            this.showGenericErrorNotification();
        }finally {
            gitStatus();
        }
    }

    /**
     * Adds a commit reverting the selected commits
     * @param commits the commits to revert
     */
    void handleRevertMultipleButton(List<CommitHelper> commits) {
        try {
            logger.info("Revert button clicked");

            if(this.theModel.getCurrentRepoHelper() == null) throw new NoRepoLoadedException();

            BusyWindow.show();
            BusyWindow.setLoadingText("Reverting...");
            Thread th = new Thread(new Task<Void>(){
                @Override
                protected Void call() {
                    try{
                        theModel.getCurrentRepoHelper().revertHelpers(commits);
                        gitStatus();
                    } catch(MultipleParentsNotAllowedException e) {
                        for (CommitHelper commit : commits) {
                            if (commit.getParents().size() > 1) {
                                showCantRevertMultipleParentsNotification();
                            }
                            if (commit.getParents().size() == 0) {
                                showCantRevertZeroParentsNotification();
                            }
                        }
                    } catch(InvalidRemoteException e){
                        showNoRemoteNotification();
                    } catch (TransportException e) {
                        if (e.getMessage().contains("git-receive-pack not found")) {
                            // The error has this message if there is no longer a remote to push to
                            showLostRemoteNotification();
                        } else {
                            showNotAuthorizedNotification();
                        }
                    } catch(MissingRepoException e){
                        showMissingRepoNotification();
                        setButtonsDisabled(true);
                        refreshRecentReposInDropdown();
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
     * Reverts the tree to remove the changes in the most recent commit
     * @param commit: the commit to revert
     */
    void handleRevertButton(CommitHelper commit) {
        try {
            logger.info("Revert button clicked");

            if(this.theModel.getCurrentRepoHelper() == null) throw new NoRepoLoadedException();

            BusyWindow.show();
            BusyWindow.setLoadingText("Reverting...");
            Thread th = new Thread(new Task<Void>(){
                @Override
                protected Void call() {
                    try{
                        theModel.getCurrentRepoHelper().revert(commit);
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
                            showNotAuthorizedNotification();
                        }
                    } catch(MissingRepoException e){
                        showMissingRepoNotification();
                        setButtonsDisabled(true);
                        refreshRecentReposInDropdown();
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
     * Resets the tree to a given commit with default settings
     *
     * @param commit the commit to reset to
     */
    void handleResetButton(CommitHelper commit) {
        handleAdvancedResetButton(commit, ResetCommand.ResetType.MIXED);
    }

    /**
     * Resets the tree to the given commit, given a specific type
     * @param commit CommitHelper
     * @param type the type of reset to perform
     */
    void handleAdvancedResetButton(CommitHelper commit, ResetCommand.ResetType type) {
        try {
            logger.info("Reset button clicked");

            if(this.theModel.getCurrentRepoHelper() == null) throw new NoRepoLoadedException();

            BusyWindow.show();
            BusyWindow.setLoadingText("Resetting...");
            Thread th = new Thread(new Task<Void>(){
                @Override
                protected Void call() {
                    try{
                        theModel.getCurrentRepoHelper().reset(commit.getId(), type);
                        gitStatus();
                    }catch(InvalidRemoteException e){
                        showNoRemoteNotification();
                    } catch (TransportException e) {
                        if (e.getMessage().contains("git-receive-pack not found")) {
                            // The error has this message if there is no longer a remote to push to
                            showLostRemoteNotification();
                        } else {
                            showNotAuthorizedNotification();
                        }
                    } catch(MissingRepoException e){
                        showMissingRepoNotification();
                        setButtonsDisabled(true);
                        refreshRecentReposInDropdown();
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
    private synchronized void gitFetch(){
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
                        showNotAuthorizedNotification();
                    } catch(MissingRepoException e){
                        showMissingRepoNotification();
                        setButtonsDisabled(true);
                        refreshRecentReposInDropdown();
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
            createDeleteBranchController.setSessionController(this);
            AnchorPane fxmlRoot = fxmlLoader.getRoot();
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
            mergeWindowController.setSessionController(this);
            AnchorPane fxmlRoot = fxmlLoader.getRoot();
            mergeWindowController.showStage(fxmlRoot);
        }catch(IOException e){
            this.showGenericErrorNotification();
            e.printStackTrace();
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
        logger.info("Git status button clicked");
        this.gitStatus();
        CommitTreeController.focusCommitInGraph(theModel.getCurrentRepoHelper().getBranchModel().getCurrentBranchHead());
    }

    /**
     * Updates the trees, changed files, and branch information. Equivalent
     * to 'git status'
     */
    void gitStatus(){
        RepositoryMonitor.pause();

        Platform.runLater(() -> {
            // If the layout is still going, don't run
            if (commitTreePanelView.isLayoutThreadRunning) {
                RepositoryMonitor.unpause();
                return;
            }
            try{
                theModel.getCurrentRepoHelper().getBranchModel().updateAllBranches();
                commitTreeModel.update();
                workingTreePanelView.drawDirectoryView();
                allFilesPanelView.drawDirectoryView();
                this.theModel.getCurrentRepoHelper().getTagModel().updateTags();
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
     * Helper method that tells the user a local branch was created
     * @param type String
     */
    private void updateUser(String type) {
        Platform.runLater(() -> {
            Text txt = new Text(" Branch" + type);
            PopOver popOver = new PopOver(txt);
            popOver.setTitle("");
            popOver.show(commitTreePanelView);
            popOver.detach();
            popOver.setAutoHide(true);
        });
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

    /**
     * Shows a popover with all repos in a checklist
     */
    public void chooseRecentReposToDelete() {
        logger.info("Remove repos button clicked");

        // creates a CheckListView with all the repos in it
        List<RepoHelper> repoHelpers = this.theModel.getAllRepoHelpers();
        CheckListView<RepoHelper> repoCheckListView = new CheckListView<>(FXCollections.observableArrayList(repoHelpers));

        // creates a popover with the list and a button used to remove repo shortcuts
        Button removeSelectedButton = new Button("Remove repository shortcuts from Elegit");
        PopOver popover = new PopOver(new VBox(repoCheckListView, removeSelectedButton));
        popover.setTitle("Manage Recent Repositories");

        // shows the popover
        popover.show(this.removeRecentReposButton);

        removeSelectedButton.setOnAction(e -> {
            this.handleRemoveReposButton(repoCheckListView.getCheckModel().getCheckedItems());
            popover.hide();
        });
    }

    /**
     * removes selected repo shortcuts
     * @param checkedItems list of selected repos
     */
    private void handleRemoveReposButton(List<RepoHelper> checkedItems) {
        logger.info("Removed repos");
        this.theModel.removeRepoHelpers(checkedItems);

        // If there are repos that aren't the current one, and the current repo is being removed, load a different repo
        if (!this.theModel.getAllRepoHelpers().isEmpty() && !this.theModel.getAllRepoHelpers().contains(theModel.getCurrentRepoHelper())) {
            int newIndex = this.theModel.getAllRepoHelpers().size()-1;
            RepoHelper newCurrentRepo = this.theModel.getAllRepoHelpers()
                    .get(newIndex);

            handleRecentRepoMenuItem(newCurrentRepo);
            repoDropdownSelector.setValue(newCurrentRepo);

            this.refreshRecentReposInDropdown();

            // If there are no repos, reset everything
        } else if (this.theModel.getAllRepoHelpers().isEmpty()){
            TreeLayout.stopMovingCells();
            theModel.resetSessionModel();
            workingTreePanelView.resetFileStructurePanelView();
            allFilesPanelView.resetFileStructurePanelView();
            initialize();

            // The repos have been removed, this line just keeps the current repo loaded
        }else {
            try {
                theModel.openRepoFromHelper(theModel.getCurrentRepoHelper());
            } catch (BackingStoreException | IOException | MissingRepoException | ClassNotFoundException e1) {
                e1.printStackTrace();
            }
        }

        this.refreshRecentReposInDropdown();
    }

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
            AnchorPane fxmlRoot = fxmlLoader.getRoot();
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
    private boolean changeLogin() {
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
            GridPane fxmlRoot = FXMLLoader.load(getClass().getResource("/elegit/fxml/Legend.fxml"));

            Stage stage = new Stage();
            stage.setTitle("Legend");
            stage.setScene(new Scene(fxmlRoot));
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
    void selectCommit(String id){
        Platform.runLater(() -> {
            CommitHelper commit = this.theModel.getCurrentRepoHelper().getCommit(id);

            commitInfoNameText = commit.getName();
            commitInfoMessageText.setVisible(true);
            commitInfoNameCopyButton.setVisible(true);
            commitInfoGoToButton.setVisible(true);
            tagNameField.setVisible(true);
            tagButton.setVisible(true);

            commitInfoMessageText.setText(theModel.getCurrentRepoHelper().getCommitDescriptorString(commit, true));
        });
    }

    /**
     * Stops displaying commit information
     */
    void clearSelectedCommit(){
        Platform.runLater(() -> {
            commitInfoMessageText.setText("");
            commitInfoMessageText.setVisible(false);
            commitInfoNameCopyButton.setVisible(false);
            commitInfoGoToButton.setVisible(false);

            tagNameField.setText("");
            tagNameField.setVisible(false);
            tagButton.setVisible(false);
        });
    }

    /// ******************************************************************************
    /// ********                 BEGIN: ERROR NOTIFICATIONS:                  ********
    /// ******************************************************************************

    private void showGenericErrorNotification() {
        Platform.runLater(()-> {
            logger.warn("Generic error warning.");
            this.notificationPaneController.addNotification("Sorry, there was an error.");
        });
    }

    private void showJGitInternalError(JGitInternalException e) {
        Platform.runLater(()-> {
            if (e.getCause().toString().contains("LockFailedException")) {
                logger.warn("Lock failed warning.");
                this.notificationPaneController.addNotification(e.getCause().getMessage()+". If no other git processes are running, manually remove all .lock files.");
            } else {
                logger.warn("Generic jgit internal warning.");
                this.notificationPaneController.addNotification("Sorry, there was a Git error.");
            }
        });
    }

    private void showNoRepoLoadedNotification() {
        Platform.runLater(() -> {
            logger.warn("No repo loaded warning.");
            this.notificationPaneController.addNotification("You need to load a repository before you can perform operations on it. Click on the plus sign in the upper left corner!");
        });
    }

    private void showInvalidRepoNotification() {
        Platform.runLater(() -> {
            logger.warn("Invalid repo warning.");
            this.notificationPaneController.addNotification("Make sure the directory you selected contains an existing (non-bare) Git repository.");
        });
    }

    private void showMissingRepoNotification(){
        Platform.runLater(()-> {
            logger.warn("Missing repo warning");
            this.notificationPaneController.addNotification("That repository no longer exists.");
        });
    }

    private void showNoRemoteNotification(){
        Platform.runLater(()-> {
            logger.warn("No remote repo warning");
            String name = this.theModel.getCurrentRepoHelper() != null ? this.theModel.getCurrentRepoHelper().toString() : "the current repository";

            this.notificationPaneController.addNotification("There is no remote repository associated with " + name);
        });
    }

    private void showFailedToOpenLocalNotification(){
        Platform.runLater(()-> {
            logger.warn("Failed to load local repo warning");
            String path = this.theModel.getCurrentRepoHelper() != null ? this.theModel.getCurrentRepoHelper().getLocalPath().toString() : "the location of the local repository";

            this.notificationPaneController.addNotification("Could not open directory at " + path);
        });
    }

    private void showNonEmptyFolderNotification(Runnable callback) {
        Platform.runLater(()-> {
            logger.warn("Folder alread exists warning");
            this.notificationPaneController.addNotification("Make sure a folder with that name doesn't already exist in that location");
        });
    }

    private void showInvalidRemoteNotification(Runnable callback) {
        Platform.runLater(() -> {
            logger.warn("Invalid remote warning");
            this.notificationPaneController.addNotification("Make sure you entered the correct remote URL.");
        });
    }

    private void showNotAuthorizedNotification() {
        Platform.runLater(() -> {
            logger.warn("Invalid authorization warning");
            this.notificationPaneController.addNotification("The authorization information you gave does not allow you to modify this repository. " +
                    "Try reentering your password.");
        });
    }

    private void showRepoWasNotLoadedNotification() {
        Platform.runLater(() -> {
            logger.warn("Repo not loaded warning");
            this.notificationPaneController.addNotification("Something went wrong, so no repository was loaded.");
        });
    }

    private void showPushToAheadRemoteNotification(boolean allRefsRejected){
        Platform.runLater(() -> {
            logger.warn("Remote ahead of local warning");
            if(allRefsRejected) this.notificationPaneController.addNotification("The remote repository is ahead of the local. You need to fetch and then merge (pull) before pushing.");
            else this.notificationPaneController.addNotification("You need to fetch/merge in order to push all of your changes.");
        });
    }

    private void showLostRemoteNotification() {
        Platform.runLater(() -> {
            logger.warn("Remote repo couldn't be found warning");
            this.notificationPaneController.addNotification("The push failed because the remote repository couldn't be found.");
        });
    }

    private void showSameRepoLoadedNotification() {
        Platform.runLater(() -> {
            logger.warn("Same repo loaded");
            this.notificationPaneController.addNotification("That repository is already open");
        });
    }

    private void showNoFilesStagedForCommitNotification(){
        Platform.runLater(() -> {
            logger.warn("No files staged for commit warning");
            this.notificationPaneController.addNotification("You need to add files before commiting");
        });
    }


    private void showNoFilesSelectedForAddNotification(){
        Platform.runLater(() -> {
            logger.warn("No files selected for add warning");
            this.notificationPaneController.addNotification("You need to select files to add");
        });
    }


    private void showNoFilesSelectedForRemoveNotification(){
        Platform.runLater(() -> {
            logger.warn("No files staged for remove warning");
            this.notificationPaneController.addNotification("You need select files to remove");
        });
    }


    private void showCannotAddFileNotification(String filename) {
        Platform.runLater(() -> {
            logger.warn("Cannot add file notification");
            this.notificationPaneController.addNotification("Cannot add "+filename+". It might already be added (staged).");
        });
    }

    private void showCannotRemoveFileNotification(String filename) {
        Platform.runLater(() -> {
            logger.warn("Cannot remove file notification");
            this.notificationPaneController.addNotification("Cannot remove "+filename+" because it hasn't been staged yet.");
        });
    }

    private void showNoTagNameNotification(){
        Platform.runLater(() -> {
            logger.warn("No tag name warning");
            this.notificationPaneController.addNotification("You need to write a tag name in order to tag the commit");
        });
    }

    private void showNoCommitsToPushNotification(){
        Platform.runLater(() -> {
            logger.warn("No local commits to push warning");
            this.notificationPaneController.addNotification("There aren't any local commits to push");
        });
    }

    private void showTagsUpToDateNotification(){
        Platform.runLater(() -> {
            logger.warn("Tags up to date notification");
            this.notificationPaneController.addNotification("Tags are up to date with the remote");
        });
    }

    private void showTagsUpdatedNotification(){
        Platform.runLater(() -> {
            logger.warn("Tags updated notification");
            this.notificationPaneController.addNotification("Tags were updated");
        });
    }

    private void showNoCommitsFetchedNotification(){
        Platform.runLater(() -> {
            logger.warn("No commits fetched warning");
            this.notificationPaneController.addNotification("No new commits were fetched");
        });
    }

    private void showTagExistsNotification() {
        Platform.runLater(()-> {
            logger.warn("Tag already exists warning.");
            this.notificationPaneController.addNotification("Sorry that tag already exists in this repository.");
        });
    }

    private void showCantRevertMultipleParentsNotification() {
        Platform.runLater(() -> {
            logger.warn("Tried to revert commit with multiple parents.");
            this.notificationPaneController.addNotification("You cannot revert that commit because it has more than one parent.");
        });
    }

    private void showCantRevertZeroParentsNotification() {
        Platform.runLater(() -> {
            logger.warn("Tried to revert commit with zero parents.");
            this.notificationPaneController.addNotification("You cannot revert that commit because it has zero parents.");
        });
    }

    private void showCommandCancelledNotification() {
        Platform.runLater(() -> {
            logger.warn("Command cancelled.");
            this.notificationPaneController.addNotification("Command cancelled.");
        });
    }

    private void showCannotDeleteBranchNotification(BranchHelper branch) {
        Platform.runLater(() -> {
            logger.warn("Cannot delete current branch notification");
            notificationPaneController.addNotification(String.format("Sorry, %s can't be deleted right now. " +
                    "Try checking out a different branch first.", branch.getBranchName()));
        });
    }

    private void showNotMergedNotification(BranchHelper nonmergedBranch) {
        logger.warn("Not merged notification");
        notificationPaneController.addNotification("That branch has to be merged before you can do that.");
    }

    private void showCheckoutConflictsNotification(List<String> conflictingPaths) {
        Platform.runLater(() -> {
            logger.warn("Checkout conflicts warning");
            notificationPaneController.addNotification("You can't switch to that branch because there would be a merge conflict. Stash your changes or resolve conflicts first.");

            /*
            Action seeConflictsAction = new Action("See conflicts", e -> {
                anchorRoot.hide();
                PopUpWindows.showCheckoutConflictsAlert(conflictingPaths);
            });*/
        });
    }

    private void showRefAlreadyExistsNotification() {
        logger.info("Branch already exists notification");
        notificationPaneController.addNotification("Looks like that branch already exists locally!");
    }

    // END: ERROR NOTIFICATIONS ^^^

    private void submitLog() {
        try {
            String lastUUID = theModel.getLastUUID();
            theModel.setLastUUID(d.submitData(lastUUID));
        } catch (BackingStoreException | ClassNotFoundException | IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            try { theModel.setLastUUID(""); }
            catch (Exception f) { // This shouldn't happen
            }
        }
    }
}
