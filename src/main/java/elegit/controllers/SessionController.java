package elegit.controllers;

import elegit.*;
import elegit.exceptions.*;
import elegit.treefx.TreeLayout;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.event.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.*;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.apache.commons.lang3.SystemUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.controlsfx.control.CheckListView;
import org.controlsfx.control.PopOver;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.*;
import org.eclipse.jgit.dircache.InvalidPathException;
import org.eclipse.jgit.errors.NoMergeBaseException;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * The controller for the entire session.
 */
public class SessionController {

    public Button commitButton;
    public Button pushButton;
    public Button fetchButton;
    public Button addButton;
    public Button removeButton;
    public Button checkoutFileButton;
    public Button mergeButton;
    public Button addDeleteBranchButton;
    public Button checkoutButton;
    public Button tagButton;
    public Button pushTagsButton;

    private SessionModel theModel;

    public Node root;

    public Tab workingTreePanelTab;
    public Tab indexPanelTab;
    public Tab allFilesPanelTab;

    public TabPane filesTabPane;
    public TabPane indexTabPane;

    public WorkingTreePanelView workingTreePanelView;
    public AllFilesPanelView allFilesPanelView;
    public StagedTreePanelView indexPanelView;

    public CommitTreePanelView commitTreePanelView;

    public CommitTreeModel commitTreeModel;

    public ImageView remoteImage;

    private String commitInfoNameText = "";

    public TextField tagNameField;

    public HBox currentLocalBranchHbox;
    public HBox currentRemoteTrackingBranchHbox;

    private Label currentLocalBranchLabel;
    private Label currentRemoteTrackingLabel;

    public Text browserText;
    public Text needToFetch;
    public Text branchStatusText;
//    public Text updatingText;

    public URL remoteURL;

    private DataSubmitter d;

    private BooleanProperty isWorkingTreeTabSelected;
    public static SimpleBooleanProperty anythingChecked;

    private volatile boolean isRecentRepoEventListenerBlocked = false;

    static final Logger logger = LogManager.getLogger(SessionController.class);

    public ContextMenu pushContextMenu;
    public ContextMenu commitContextMenu;
    public ContextMenu fetchContextMenu;

    public Hyperlink legendLink;

    public StackPane statusTextPane;

    private Stage mainStage;

    @FXML private AnchorPane anchorRoot;

    // Notification pane
    @FXML private StackPane notificationPane;
    @FXML private NotificationController notificationPaneController;

    // Menu Bar
    @FXML private MenuController menuController;
    @FXML private DropdownController dropdownController;
//    @FXML public CheckMenuItem loggingToggle;
//    @FXML public CheckMenuItem commitSortToggle;
//    @FXML private MenuItem gitIgnoreMenuItem;
//    @FXML private Menu repoMenu;
//    @FXML private MenuItem cloneMenuItem;
//    @FXML private MenuItem createBranchMenuItem;
//    @FXML private MenuItem commitNormalMenuItem;
//    @FXML private MenuItem normalFetchMenuItem;
//    @FXML private MenuItem pullMenuItem;
//    @FXML private MenuItem pushMenuItem;
//    @FXML private MenuItem stashMenuItem1;
//    @FXML private MenuItem stashMenuItem2;

    // Commit Info Box
    @FXML public CommitInfoController commitInfoController;


    boolean tryCommandAgainWithHTTPAuth;
    private boolean isGitStatusDone;
    private boolean isTimerDone;

    Preferences preferences;
    private static final String LOGGING_LEVEL_KEY="LOGGING_LEVEL";

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

        // Gives other controllers acccess to this one
        CommitTreeController.sessionController = this;
        CommitController.sessionController = this;
        menuController.setSessionController(this);
        dropdownController.setSessionController(this);
        commitInfoController.setSessionController(this);


        // Creates the commit tree model
        this.commitTreeModel = new CommitTreeModel(this.theModel, this.commitTreePanelView);
        CommitTreeController.commitTreeModel = this.commitTreeModel;

        // Passes theModel to panel views
        this.workingTreePanelView.setSessionModel(this.theModel);
        this.allFilesPanelView.setSessionModel(this.theModel);
        this.indexPanelView.setSessionModel(this.theModel);

        this.initializeLayoutParameters();

        this.initButtons();
        this.setButtonIconsAndTooltips();
        this.setButtonsDisabled(true);
        this.initWorkingTreePanelTab();
        // SLOW
        this.theModel.loadRecentRepoHelpersFromStoredPathStrings();
        this.theModel.loadMostRecentRepoHelper();

        // SLOW
        this.initPanelViews();
        this.updateUIEnabledStatus();
        this.setRecentReposDropdownToCurrentRepo();
        this.refreshRecentReposInDropdown();

        this.initRepositoryMonitor();

        this.initBranchLabels();

        //this.initMenuBarShortcuts();

        this.notificationPaneController.bindParentBounds(anchorRoot.heightProperty());

        VBox.setVgrow(filesTabPane, Priority.ALWAYS);

        // if there are conflicting files on startup, watches them for changes
        try {
            ConflictingFileWatcher.watchConflictingFiles(theModel.getCurrentRepoHelper());
        } catch (GitAPIException | IOException e) {
            e.printStackTrace();
        }

        tryCommandAgainWithHTTPAuth = false;

        this.preferences = Preferences.userNodeForPackage(this.getClass());
    }

    /**
     * Helper method that passes the main stage to session controller
     * @param stage Stage
     */
    public void setStage(Stage stage) {
        this.mainStage = stage;
        notificationPaneController.setAnchor(mainStage);
    }

    /**
     * Helper method that creates the labels for the branch names
     */
    private void initBranchLabels() {
//        updatingText.setVisible(false);
//        branchStatusText.visibleProperty().bind(updatingText.visibleProperty().not());

        currentLocalBranchLabel = new Label("N/A");
        currentLocalBranchLabel.setOnMouseClicked(event -> focusCommitLocalBranch());
        currentRemoteTrackingLabel = new Label("N/A");
        currentRemoteTrackingLabel.setOnMouseClicked(event -> focusCommitRemoteBranch());

        initCellLabel(currentLocalBranchLabel, currentLocalBranchHbox);
        initCellLabel(currentRemoteTrackingLabel, currentRemoteTrackingBranchHbox);

        // updateStatusText(); needed?
        // updateNewChangesToFetch(); needed?
        updateBranchLabels();

    }

    /**
     * Helper method that sets style for cell labels
     * @param label the label that contains the branch name
     * @param hbox  the hbox that contains the label
     */
    private void initCellLabel(Label label, HBox hbox){
        hbox.getStyleClass().clear();
        hbox.getStyleClass().add("cell-label-box");
        label.getStyleClass().clear();
        label.getStyleClass().add("cell-label");
        label.setId("current");
        hbox.setId("current");
        hbox.getChildren().add(label);
    }

    /**
     * Helper method for adding tooltips to nodes
     * @param n the node to attach a tooltip to
     * @param text the text for the tooltip
     */
    private void addToolTip(Node n, String text) {
        Tooltip tooltip = new Tooltip(text);
        tooltip.setWrapText(true);
        tooltip.setMaxWidth(350);
        Tooltip.install(n, tooltip);
    }

    /**
     * Helper method to update whether or not there are remote changes to fetch
     */
    private void updateNewChangesToFetch() {
        if (this.theModel.getCurrentRepoHelper() == null) return;
        boolean update;

        update = RepositoryMonitor.hasFoundNewRemoteChanges.get();
        String fetchText = update ? "New changes to fetch" : "Up to date";
        Color fetchColor = update ? Color.FIREBRICK : Color.FORESTGREEN;
        needToFetch.setText(fetchText);
        needToFetch.setFont(new Font(15));
        needToFetch.setFill(fetchColor);
    }

    /**
     * Helper method to update the text of local and remote branch labels
     */
    private void updateBranchLabels() {

        BranchHelper localBranch = theModel.getCurrentBranch();
        boolean update = !localBranch.getAbbrevName().equals(currentLocalBranchLabel.getText());

        if (update) {
            Platform.runLater(() -> {
                currentLocalBranchLabel.setText(localBranch.getAbbrevName());
                addToolTip(currentLocalBranchHbox, localBranch.getRefName());
                CommitTreeController.setBranchHeads(commitTreeModel, theModel.getCurrentRepoHelper());
            });
        }

        String remoteBranch = "N/A";
        String remoteBranchFull = "N/A";
        try {
            remoteBranch = this.theModel.getCurrentRepoHelper().getBranchModel().getCurrentRemoteAbbrevBranch();
            remoteBranchFull = this.theModel.getCurrentRepoHelper().getBranchModel().getCurrentRemoteBranch();
        } catch (IOException e) {
            this.showGenericErrorNotification();
        }
        if (remoteBranch == null) {
            remoteBranch = "N/A";
            remoteBranchFull = "N/A";
        }

        String remoteBranchFinal = remoteBranch;
        String remoteBranchFullFinal = remoteBranchFull;

        update = !remoteBranch.equals(currentRemoteTrackingLabel.getText());
        if (update) {
            Platform.runLater(() -> {
                currentRemoteTrackingLabel.setText(remoteBranchFinal);
                addToolTip(currentRemoteTrackingBranchHbox, remoteBranchFullFinal);
                CommitTreeController.setBranchHeads(commitTreeModel, theModel.getCurrentRepoHelper());
            });
        }
    }

    /**
     * Helper method to update the ahead/behind status text below the local repository icon
     */
    private void updateStatusText(){
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
            statusText= currentLocalBranchLabel.getText() + " ahead of " + currentRemoteTrackingLabel.getText() + " by " + ahead + " commit";
            if (ahead > 1)
                statusText+="s";
            if (behind > 0) {
                statusText += "\nand behind by " + behind + " commit";
                if (behind > 1)
                    statusText+="s";
            }
            statusText+=".";
        } else if (behind > 0) {
            statusText = currentLocalBranchLabel.getText() + " behind " + currentRemoteTrackingLabel.getText() + " by " + behind + " commit";
            if (behind > 1)
                statusText+="s";
            statusText+=".";
        }
        boolean update = !statusText.equals(branchStatusText.getText());
        Color statusColor = statusText.equals("Up to date.") ? Color.FORESTGREEN : Color.FIREBRICK;
        if (update) {
            branchStatusText.setText(statusText);
            branchStatusText.setFill(statusColor);
        }
    }

    /**
     * Private method to emphasize the commit in the commit tree
     * that corresponds to the current local branch
     */
    //todo: transition away from using this except in detatched head state, probably requires
    //todo: combining each into 1 method and revamping code elsewhere
    private void focusCommitLocalBranch() {
        CommitHelper HEAD = theModel.getCurrentRepoHelper().getBranchModel().getCurrentBranchHead();
        CommitTreeController.focusCommitInGraph(HEAD);
    }

    private void focusLocalBranchLabelInGraph() {
        BranchHelper branch = theModel.getCurrentRepoHelper().getBranchModel().getCurrentBranch();
        CommitHelper commit = theModel.getCurrentRepoHelper().getBranchModel().getCurrentBranchHead();
        CommitTreeController.focusBranchLabelInGraph(commit, branch);
    }

    private void focusRemoteBranchLabelInGraph() {
        BranchHelper remoteBranch;
    }

    /**
     * Private method to emphasize the commit in the commit tree
     * that corresponds to the current remote-tracking branch
     */
    //todo: transition away from using this at all - no remote tracking branch in detatched head state
    private void focusCommitRemoteBranch() {
        try {
            CommitHelper commit = theModel.getCurrentRepoHelper().getBranchModel().getCurrentRemoteBranchHead();
            if (commit == null) return;
            CommitTreeController.focusCommitInGraph(commit);
        } catch (IOException e){
            this.showGenericErrorNotification();
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
        RepositoryMonitor.startWatching(theModel, this);
        RepositoryMonitor.hasFoundNewRemoteChanges.addListener((observable, oldValue, newValue) -> {
            if(newValue) updateNewChangesToFetch();
        });
    }

    /**
     * Sets up the layout parameters for things that cannot be set in FXML
     */
    private void initializeLayoutParameters(){
        // Set minimum/maximum sizes for buttons
        //openRepoDirButton.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
        commitButton.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
        addButton.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
        checkoutFileButton.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
        removeButton.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
        addDeleteBranchButton.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
        mergeButton.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
        checkoutButton.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
        pushButton.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
        pushTagsButton.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
        fetchButton.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);

        // Set minimum sizes for other fields and views
        workingTreePanelView.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
        allFilesPanelView.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
        indexPanelView.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
        tagNameField.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
    }

    /**
     * Adds context menus and properties to buttons
     */
    private void initButtons() {
        anythingChecked = new SimpleBooleanProperty(false);
        checkoutFileButton.disableProperty().bind(anythingChecked.not());
        addButton.disableProperty().bind(anythingChecked.not());
        removeButton.disableProperty().bind(anythingChecked.not());

        legendLink.setFont(new Font(12));

        pushButton.setOnMouseClicked(event -> {
            if(event.getButton() == MouseButton.SECONDARY){
                if(pushContextMenu != null){
                    pushContextMenu.show(pushButton, event.getScreenX(), event.getScreenY());
                }
            }
            event.consume();
        });

        commitButton.setOnMouseClicked(event -> {
            if(event.getButton() == MouseButton.SECONDARY){
                if(commitContextMenu != null){
                    commitContextMenu.show(commitButton, event.getScreenX(), event.getScreenY());
                }
            }
            event.consume();
        });

        fetchButton.setOnMouseClicked(event -> {
            if(event.getButton() == MouseButton.SECONDARY){
                if(fetchContextMenu != null){
                    fetchContextMenu.show(fetchButton, event.getScreenX(), event.getScreenY());
                }
            }
            event.consume();
        });

        tagNameField.setOnKeyTyped(event -> {
            if (event.getCharacter().equals("\r")) handleTagButton();
        });
    }

    /**
     * Adds graphics and tooltips to the buttons
     */
    private void setButtonIconsAndTooltips() {
        this.commitButton.setTooltip(new Tooltip(
                "Check in selected files to local repository"
        ));
        this.addButton.setTooltip(new Tooltip(
                "Stage changes for selected files"
        ));
        this.checkoutFileButton.setTooltip(new Tooltip(
                "Checkout files from the index (discard all unstaged changes)"
        ));
        this.removeButton.setTooltip(new Tooltip(
                "Delete selected files and remove them from Git"
        ));
        this.fetchButton.setTooltip(new Tooltip(
                "Download files from another repository to remote repository"
        ));
        this.pushButton.setTooltip(new Tooltip(
                "Update remote repository with local changes,\nright click for advanced options"
        ));

        dropdownController.loadNewRepoButton.setTooltip(new Tooltip(
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
            indexPanelView.drawDirectoryView();
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
            dropdownController.openRepoDirButton.setDisable(disable);
            tagButton.setDisable(disable);
            commitButton.setDisable(disable);
            pushButton.setDisable(disable);
            fetchButton.setDisable(disable);
            remoteImage.setVisible(!disable);
            browserText.setVisible(!disable);
            workingTreePanelTab.setDisable(disable);
            allFilesPanelTab.setDisable(disable);
            indexPanelTab.setDisable(disable);
            dropdownController.removeRecentReposButton.setDisable(disable);
            dropdownController.repoDropdownSelector.setDisable(disable);
            addDeleteBranchButton.setDisable(disable);
            checkoutButton.setDisable(disable);
            mergeButton.setDisable(disable);
            pushTagsButton.setDisable(disable);
            needToFetch.setVisible(!disable);
            currentLocalBranchHbox.setVisible(!disable);
            currentRemoteTrackingBranchHbox.setVisible(!disable);
            statusTextPane.setVisible(!disable);
            updateMenuBarEnabledStatus(disable);
        });

        root.setOnMouseClicked(event -> {
            if (disable) showNoRepoLoadedNotification();
            if (this.notificationPaneController.isListPaneVisible()) this.notificationPaneController.toggleNotificationList();
        });
    }


    /**
     * Helper method for disabling the menu bar
     */
    private void updateMenuBarEnabledStatus(boolean disable) {
        menuController.repoMenu.setDisable(disable);
        menuController.gitIgnoreMenuItem.setDisable(disable);
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
        dropdownController.newRepoOptionsMenu.show(dropdownController.loadNewRepoButton, Side.BOTTOM ,0, 0);
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
            showTransportExceptionNotification(e);
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
                dropdownController.repoDropdownSelector.setValue(currentRepo);
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
                dropdownController.repoDropdownSelector.setItems(FXCollections.observableArrayList(repoHelpers));
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

        this.notificationPaneController.clearAllNotifications();
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
        RepoHelper selectedRepoHelper = dropdownController.repoDropdownSelector.getValue();
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
            if(workingTreePanelView.isAnyFileStagedSelected()) throw new StagedFileCheckedException();

            BusyWindow.show();
            BusyWindow.setLoadingText("Adding...");
            Thread th = new Thread(new Task<Void>(){
                @Override
                protected Void call() {
                    try{
                        ArrayList<Path> filePathsToAdd = new ArrayList<>();
                        // Try to add all files, throw exception if there are ones that can't be added
                        if (workingTreePanelView.isSelectAllChecked()) {
                            filePathsToAdd.add(Paths.get("."));
                        }
                        else {
                            for (RepoFile checkedFile : workingTreePanelView.getCheckedFilesInDirectory()) {
                                if (checkedFile.canAdd())
                                    filePathsToAdd.add(checkedFile.getFilePath());
                                else
                                    throw new UnableToAddException(checkedFile.filePath.toString());
                            }
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
        } catch (StagedFileCheckedException e) {
            this.showStagedFilesSelectedNotification();
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

    /**
     * Basic handler for the checkout button. Just checks out the given file
     * from the index
     *
     * @param filePath the path of the file to checkout from the index
     */
    public void handleCheckoutButton(Path filePath) {
        try {
            logger.info("Checkout file button clicked");
            if (! PopUpWindows.showCheckoutAlert()) throw new CancelledDialogueException();
            if(this.theModel.getCurrentRepoHelper() == null) throw new NoRepoLoadedException();
            if(!this.theModel.getCurrentRepoHelper().exists()) throw new MissingRepoException();
            theModel.getCurrentRepoHelper().checkoutFile(filePath);
        } catch (NoRepoLoadedException e) {
            showNoRepoLoadedNotification();
        } catch (MissingRepoException e) {
            showMissingRepoNotification();
        } catch (GitAPIException e) {
            showGenericErrorNotification();
        } catch (CancelledDialogueException e) {
            // Do nothing if the dialogue was cancelled.
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
            if (! PopUpWindows.showCheckoutAlert()) throw new CancelledDialogueException();
            ArrayList<Path> filePathsToCheckout = new ArrayList<>();
            // Try to add all files, throw exception if there are ones that can't be added
            for(RepoFile checkedFile : workingTreePanelView.getCheckedFilesInDirectory()) {
                filePathsToCheckout.add(checkedFile.getFilePath());
            }
            theModel.getCurrentRepoHelper().checkoutFiles(filePathsToCheckout);
            gitStatus();
        } catch (NoFilesSelectedToAddException e) {
            this.showNoFilesSelectedForAddNotification();
        } catch (NoRepoLoadedException e) {
            this.showNoRepoLoadedNotification();
        } catch (MissingRepoException e) {
            this.showMissingRepoNotification();
        } catch (GitAPIException e) {
            this.showGenericErrorNotification();
        } catch (CancelledDialogueException e) {
            // Do nothing
        }
    }


    /**
     * Shows the checkout files dialogue for a given commit
     *
     * @param commitHelper the commit to checkout files from
     */
    public void handleCheckoutFilesButton(CommitHelper commitHelper) {
        try{
            System.out.println("Loading checkoutfiles pane");
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

    private enum CommitType{NORMAL, ALL}

    public void handleCommitAll() {
        handleCommitButton(CommitType.ALL);
    }

    public void handleCommitNormal() {
        handleCommitButton(CommitType.NORMAL);
    }

    /**
     * Commits all files that have been staged with the message
     */
    public void handleCommitButton(CommitType type) {
        try {
            logger.info("Commit button clicked");
            if(this.theModel.getCurrentRepoHelper() == null) throw new NoRepoLoadedException();
            if(!this.theModel.getCurrentRepoHelper().exists()) throw new MissingRepoException();

            if(!workingTreePanelView.isAnyFileStaged() && type.equals(CommitType.NORMAL)) throw new NoFilesStagedForCommitException();

            if(type.equals(CommitType.NORMAL)) {
                commitNormal();
            }else {
                commitAll();
            }
        } catch(NoRepoLoadedException e){
            this.showNoRepoLoadedNotification();
            setButtonsDisabled(true);
        } catch(MissingRepoException e){
            this.showMissingRepoNotification();
            setButtonsDisabled(true);
            refreshRecentReposInDropdown();
        } catch(NoFilesStagedForCommitException e){
            this.showNoFilesStagedForCommitNotification();
        } catch(IOException e){
            showGenericErrorNotification();
            e.printStackTrace();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private void commitAll() {
        String message = PopUpWindows.getCommitMessage();
        if(message.equals("cancel")) return;

        BusyWindow.show();
        BusyWindow.setLoadingText("Committing all...");

        Thread th = new Thread(new Task<Void>() {
            @Override
            protected Void call() {
                try {
                    theModel.getCurrentRepoHelper().commitAll(message);
                    gitStatus();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    BusyWindow.hide();
                }
                return null;
            }
        });
        th.setDaemon(true);
        th.setName("Git commit all");
        th.start();
    }

    private void commitNormal() throws IOException {
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
                    try {
                        theModel.getCurrentRepoHelper().getTagModel().tag(tagName, commitInfoNameText);

                        // Now clear the tag text and a view reload ( or `git status`) to show that something happened
                        tagNameField.clear();
                        gitStatus();
                    } catch (JGitInternalException e) {
                        showJGitInternalError(e);
                    } catch (MissingRepoException e) {
                        showMissingRepoNotification();
                        setButtonsDisabled(true);
                        refreshRecentReposInDropdown();
                    } catch (InvalidTagNameException e) {
                        showInvalidTagNameNotification(tagName);
                    }catch (TransportException e) {
                        showTransportExceptionNotification(e);
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

    public enum PushType {BRANCH, ALL}

    public void handlePushButton() {
        pushBranchOrAllSetup(PushType.BRANCH);
    }

    public void handlePushAllButton() {
        pushBranchOrAllSetup(PushType.ALL);
    }

    // Set up the push command. Involves querying the user to see if remote branches should be made.
    // This query is done once.
    private void pushBranchOrAllSetup(PushType pushType)  {

        try {

            logger.info("Push button clicked");

            if (this.theModel.getCurrentRepoHelper() == null) throw new NoRepoLoadedException();
            if (pushType == PushType.BRANCH &&
                !this.theModel.getCurrentRepoHelper().canPush()) throw new NoCommitsToPushException();

            RepoHelper helper = theModel.getCurrentRepoHelper();
            final PushCommand push;
            if (pushType == PushType.BRANCH) {
                push = helper.prepareToPushCurrentBranch(false);
            } else if (pushType == PushType.ALL) {
                push = helper.prepareToPushAll();
            } else {
                push = null;
                assert false : "PushType enum case not handled";
            }

            pushBranchOrAll(pushType, push);

        } catch (NoRepoLoadedException e) {
            this.showNoRepoLoadedNotification();
            setButtonsDisabled(true);
        } catch (NoCommitsToPushException e) {
            this.showNoCommitsToPushNotification();
        } catch (IOException e) {
            this.showGenericErrorNotification();
        } catch (PushToAheadRemoteError pushToAheadRemoteError) {
            pushToAheadRemoteError.printStackTrace();
        } catch (MissingRepoException e) {
            showMissingRepoNotification();
            setButtonsDisabled(true);
            refreshRecentReposInDropdown();
        } catch (GitAPIException e) {
            showGenericErrorNotification();
            e.printStackTrace();
        }

    }

    /**
     * Performs a `git push` on either current branch or all branches, depending on enum parameter.
     * This is recursively re-called if authentication fails.
     */
    public void pushBranchOrAll(PushType pushType, PushCommand push) {
        try {
            final RepoHelperBuilder.AuthDialogResponse credentialResponse = askUserForCredentials();

            BusyWindow.show();
            BusyWindow.setLoadingText("Pushing...");
            Thread th = new Thread(new Task<Void>(){
                @Override
                protected Void call() {
                    tryCommandAgainWithHTTPAuth = false;
                    try {
                        pushBranchOrAllDetails(credentialResponse, pushType, push);
                    } catch (TransportException e) {
                        determineIfTryAgain(e);
                    } finally {
                        BusyWindow.hide();
                    }

                    if (tryCommandAgainWithHTTPAuth) {
                        Platform.runLater(() -> {
                            pushBranchOrAll(pushType, push);
                        });
                    }

                    return null;
                }
            });
            th.setDaemon(true);
            th.setName("Git push");
            th.start();
        } catch (CancelledAuthorizationException e) {
            this.showCommandCancelledNotification();
        }
    }

    private void determineIfTryAgain(TransportException e) {
        showTransportExceptionNotification(e);

        // Don't try again with HTTP authentication if SSH prompt for authentication is canceled
        if (!e.getMessage().endsWith("Auth cancel"))
            tryCommandAgainWithHTTPAuth = true;
    }

    private void pushBranchOrAllDetails(RepoHelperBuilder.AuthDialogResponse response, PushType pushType,
                                        PushCommand push) throws
            TransportException {
        try{
            RepositoryMonitor.resetFoundNewChanges(false);
            RepoHelper helper = theModel.getCurrentRepoHelper();
            if (response != null) {
                helper.ownerAuth =
                        new UsernamePasswordCredentialsProvider(response.username, response.password);
            }
            if (pushType == PushType.BRANCH) {
                helper.pushCurrentBranch(push);
            } else if (pushType == PushType.ALL) {
                helper.pushAll(push);
            } else {
                assert false : "PushType enum case not handled";
            }
            gitStatus();
        } catch (InvalidRemoteException e) {
            showNoRemoteNotification();
        } catch (PushToAheadRemoteError e) {
            showPushToAheadRemoteNotification(e.isAllRefsRejected());
        } catch (TransportException e) {
            throw e;
        } catch(Exception e) {
            showGenericErrorNotification();
            e.printStackTrace();
        }
    }

    private RepoHelperBuilder.AuthDialogResponse askUserForCredentials() throws CancelledAuthorizationException {
        final RepoHelperBuilder.AuthDialogResponse response;
        if (tryCommandAgainWithHTTPAuth) {
            response = RepoHelperBuilder.getAuthCredentialFromDialog();
        } else {
            response = null;
        }
        return response;
    }


    /**
     * Performs a `git push --tags`
     */
    public void handlePushTagsButton() {
        try {
            logger.info("Push tags button clicked");

            if(this.theModel.getCurrentRepoHelper() == null) throw new NoRepoLoadedException();

            final RepoHelperBuilder.AuthDialogResponse credentialResponse = askUserForCredentials();

            BusyWindow.show();
            BusyWindow.setLoadingText("Pushing tags...");
            Thread th = new Thread(new Task<Void>(){
                @Override
                protected Void call() {
                    tryCommandAgainWithHTTPAuth = false;
                    try {
                        handlePushTagsButtonDetails(credentialResponse);
                    } catch (TransportException e) {
                        determineIfTryAgain(e);
                    } finally {
                        BusyWindow.hide();
                    }

                    if (tryCommandAgainWithHTTPAuth) {
                        Platform.runLater(() -> {
                            handlePushTagsButton();
                        });
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
        } catch (CancelledAuthorizationException e) {
            this.showCommandCancelledNotification();
        }

    }

    private void handlePushTagsButtonDetails(RepoHelperBuilder.AuthDialogResponse response) throws TransportException {
        Iterable<PushResult> results;
        try{
            RepositoryMonitor.resetFoundNewChanges(false);
            RepoHelper helper = theModel.getCurrentRepoHelper();
            if (response != null) {
                helper.ownerAuth =
                        new UsernamePasswordCredentialsProvider(response.username, response.password);
            }
            results = helper.pushTags();
            gitStatus();

            boolean upToDate = true;

            if (results == null)
                upToDate = false;
            else
                for (PushResult result : results)
                    for (RemoteRefUpdate update : result.getRemoteUpdates())
                        if (update.getStatus() == RemoteRefUpdate.Status.OK)
                            upToDate=false;

            if (upToDate)
                showTagsUpToDateNotification();
            else
                showTagsUpdatedNotification();

        } catch(InvalidRemoteException e){
            showNoRemoteNotification();
        } catch(PushToAheadRemoteError e) {
            showPushToAheadRemoteNotification(e.isAllRefsRejected());
        } catch(MissingRepoException e) {
            showMissingRepoNotification();
            setButtonsDisabled(true);
            refreshRecentReposInDropdown();
        } catch (TransportException e) {
            throw e;

        } catch(Exception e) {
            showGenericErrorNotification();
            e.printStackTrace();
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
        boolean authorizationSucceeded = true;
        try {
            if (selectedBranch != null) {
                RemoteRefUpdate.Status deleteStatus;

                if (selectedBranch instanceof LocalBranchHelper) {
                    branchModel.deleteLocalBranch((LocalBranchHelper) selectedBranch);
                    updateUser(selectedBranch.getRefName() + " deleted.");
                }else {
                    deleteRemoteBranch(selectedBranch, branchModel,
                                       (String message) -> updateUser(message));
                }
            }
        } catch (NotMergedException e) {
            logger.warn("Can't delete branch because not merged warning");
            /*Platform.runLater(() -> {
                if(PopUpWindows.showForceDeleteBranchAlert() && selectedBranch instanceof LocalBranchHelper) {
                    // If we need to force delete, then it must be a local branch
                    forceDeleteBranch((LocalBranchHelper) selectedBranch);
                }
            });*/
            this.showNotMergedNotification(selectedBranch);
        } catch (CannotDeleteCurrentBranchException e) {
            logger.warn("Can't delete current branch warning");
            this.showCannotDeleteBranchNotification(selectedBranch);
        } catch (TransportException e) {
            this.showTransportExceptionNotification(e);
            authorizationSucceeded = false;
        } catch (GitAPIException e) {
            logger.warn("IO error");
            this.showGenericErrorNotification();
        } finally {
            gitStatus();
            if (authorizationSucceeded) {
                tryCommandAgainWithHTTPAuth = false;
            } else {
                tryCommandAgainWithHTTPAuth = true;
                deleteBranch(selectedBranch);
            }
        }
    }

    void deleteRemoteBranch(BranchHelper selectedBranch, BranchModel branchModel, Consumer<String> updateFn) {
        try {
            final RepoHelperBuilder.AuthDialogResponse credentialResponse = askUserForCredentials();

            BusyWindow.show();
            BusyWindow.setLoadingText("Deleting remote branch...");

            Thread th = new Thread(new Task<Void>() {
                @Override
                protected Void call() {
                    tryCommandAgainWithHTTPAuth = false;
                    try {
                        deleteRemoteBranchDetails(credentialResponse, selectedBranch, branchModel, updateFn);
                    } catch (TransportException e) {
                        determineIfTryAgain(e);
                    } finally {
                        BusyWindow.hide();
                    }

                    if (tryCommandAgainWithHTTPAuth) {
                        Platform.runLater(() -> {
                            deleteRemoteBranch(selectedBranch, branchModel, updateFn);
                        });
                    }

                    return null;
                }
            });
            th.setDaemon(true);
            th.setName("Git delete remote branch");
            th.start();

        } catch (CancelledAuthorizationException e) {
            this.showCommandCancelledNotification();
        }
    }

    private void deleteRemoteBranchDetails(RepoHelperBuilder.AuthDialogResponse response, BranchHelper selectedBranch,
                                           BranchModel branchModel, Consumer<String> updateFn) throws TransportException {

        try {
            if (response != null) {
                selectedBranch.repoHelper.ownerAuth =
                        new UsernamePasswordCredentialsProvider(response.username, response.password);
            }
            RemoteRefUpdate.Status deleteStatus = branchModel.deleteRemoteBranch((RemoteBranchHelper) selectedBranch);
            String updateMessage = selectedBranch.getRefName();
            // There are a number of possible cases, see JGit's documentation on RemoteRefUpdate.Status
            // for the full list.
            switch (deleteStatus) {
                case OK:
                    updateMessage += " deleted.";
                    break;
                case NON_EXISTING:
                    updateMessage += " no longer\nexists on the server.\nFetch -p to remove " + updateMessage;
                default:
                    updateMessage += " deletion\nfailed.";
            }
            updateFn.accept(updateMessage);
        } catch (TransportException e) {
            throw e;
        } catch (GitAPIException | IOException e) {
            logger.warn("IO error");
            this.showGenericErrorNotification();
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
    public void handleRevertMultipleButton(List<CommitHelper> commits) {
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
                        showTransportExceptionNotification(e);
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
                        showTransportExceptionNotification(e);
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
    public void handleResetButton(CommitHelper commit) {
        handleAdvancedResetButton(commit, ResetCommand.ResetType.MIXED);
    }

    /**
     * Resets the tree to the given commit, given a specific type
     * @param commit CommitHelper
     * @param type the type of reset to perform
     */
    public void handleAdvancedResetButton(CommitHelper commit, ResetCommand.ResetType type) {
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
                        showTransportExceptionNotification(e);
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
     * Brings up a window that allows the user to stash changes with options
     */
    public void handleStashSaveButton() {
        try {
            logger.info("Stash save button clicked");

            if (this.theModel.getCurrentRepoHelper() == null) throw new NoRepoLoadedException();

            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/elegit/fxml/StashSave.fxml"));
            fxmlLoader.load();
            StashSaveController stashSaveController = fxmlLoader.getController();
            stashSaveController.setSessionController(this);
            AnchorPane fxmlRoot = fxmlLoader.getRoot();
            stashSaveController.showStage(fxmlRoot);
        } catch (IOException e) {
            this.showGenericErrorNotification();
            e.printStackTrace();
        } catch (NoRepoLoadedException e) {
            this.showNoRepoLoadedNotification();
            setButtonsDisabled(true);
        }
    }

    public void quickStashSave() {
        try {
            logger.info("Quick stash save button clicked");

            if (this.theModel.getCurrentRepoHelper() == null) throw new NoRepoLoadedException();

            this.theModel.getCurrentRepoHelper().stashSave(false);
            gitStatus();
        } catch (GitAPIException e) {
            this.showGenericErrorNotification();
            e.printStackTrace();
        } catch (NoFilesToStashException e) {
            this.showNoFilesToStashNotification();
        } catch (NoRepoLoadedException e) {
            this.setButtonsDisabled(true);
        }
    }

    /**
     * Applies the most recent stash
     */
    public void handleStashApplyButton() {
        // TODO: make it clearer which stash this applies
        logger.info("Stash apply button clicked");
        try {
            CommitHelper topStash = theModel.getCurrentRepoHelper().stashList().get(0);
            this.theModel.getCurrentRepoHelper().stashApply(topStash.getId(), false);
            gitStatus();
        } catch (StashApplyFailureException e) {
            showStashConflictsNotification();
        } catch (GitAPIException e) {
            showGenericErrorNotification();
        } catch (IOException e) {
            showGenericErrorNotification();
        }
    }

    /**
     * Shows the stash window
     */
    public void handleStashListButton() {
        try {
            logger.info("Stash list button clicked");

            if (this.theModel.getCurrentRepoHelper() == null) throw new NoRepoLoadedException();

            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/elegit/fxml/StashList.fxml"));
            fxmlLoader.load();
            StashListController stashListController = fxmlLoader.getController();
            stashListController.setSessionController(this);
            AnchorPane fxmlRoot = fxmlLoader.getRoot();
            stashListController.showStage(fxmlRoot);
        } catch (IOException e) {
            this.showGenericErrorNotification();
            e.printStackTrace();
        } catch (NoRepoLoadedException e) {
            this.showNoRepoLoadedNotification();
            setButtonsDisabled(true);
        }
    }

    /**
     * Drops the most recent stash
     */
    public void handleStashDropButton() {
        logger.info("Stash drop button clicked");
        try {
            // TODO: implement droping something besides 0
            this.theModel.getCurrentRepoHelper().stashDrop(0);
        } catch (GitAPIException e) {
            showGenericErrorNotification();
        }
    }

    /**
     * Calls git fetch
     * @param prune boolean should prune
     */
    public void handleFetchButton(boolean prune, boolean pull) {
        logger.info("Fetch button clicked");
        RepositoryMonitor.pause();
        gitFetch(prune, pull);
        RepositoryMonitor.unpause();
        submitLog();
    }

    /**
     * Handles a click on Fetch -p
     */
    public void handlePruneFetchButton() {
        handleFetchButton(true, false);
    }

    /**
     * Handles a click on the "Fetch" button. Calls gitFetch()
     */
    public void handleNormalFetchButton(){
        handleFetchButton(false, false);
    }

    /**
     * Peforms a git pull
     */
    public void handlePullButton() {
        handleFetchButton(false, true);
    }

    /**
     * Queries the remote for new commits, and updates the local
     * remote as necessary.
     * Equivalent to `git fetch`
     */
    private synchronized void gitFetch(boolean prune, boolean pull){
        try{

            if(this.theModel.getCurrentRepoHelper() == null) throw new NoRepoLoadedException();

            final RepoHelperBuilder.AuthDialogResponse response = askUserForCredentials();

            BusyWindow.show();
            BusyWindow.setLoadingText("Fetching...");
            Thread th = new Thread(new Task<Void>(){
                @Override
                protected Void call() {
                    tryCommandAgainWithHTTPAuth = false;
                    try{
                        RepositoryMonitor.resetFoundNewChanges(false);
                        RepoHelper helper = theModel.getCurrentRepoHelper();
                        if (response != null) {
                            helper.ownerAuth =
                                    new UsernamePasswordCredentialsProvider(response.username, response.password);
                        }
                        if(!helper.fetch(prune)){
                            showNoCommitsFetchedNotification();
                        } if (pull) {
                            mergeFromFetch();
                        }
                        gitStatus();
                    } catch(InvalidRemoteException e){
                        showNoRemoteNotification();
                    } catch (TransportException e) {
                        determineIfTryAgain(e);
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

                    if (tryCommandAgainWithHTTPAuth)
                        Platform.runLater(() -> {
                            gitFetch(prune, pull);
                        });
                    return null;
                }
            });
            th.setDaemon(true);
            th.setName("Git fetch");
            th.start();
        }catch(NoRepoLoadedException e) {
            this.showNoRepoLoadedNotification();
            setButtonsDisabled(true);
        } catch (CancelledAuthorizationException e) {
            this.showCommandCancelledNotification();
        }
    }

    /**
     * Does a merge from fetch
     */
    public void mergeFromFetch() {
        mergeFromFetch(notificationPaneController, null);
    }

    /**
     * merges the remote-tracking branch associated with the current branch into the current local branch
     */
    public void mergeFromFetch(NotificationController notificationController, Stage stageToClose) {
        try{
            logger.info("Merge from fetch button clicked");
            if(theModel.getCurrentRepoHelper() == null) throw new NoRepoLoadedException();
            if(theModel.getCurrentRepoHelper().getBehindCount()<1) throw new NoCommitsToMergeException();

            BusyWindow.show();
            BusyWindow.setLoadingText("Merging...");
            Thread th = new Thread(new Task<Void>(){
                @Override
                protected Void call() throws GitAPIException, IOException {
                    try{
                        if(!theModel.getCurrentRepoHelper().mergeFromFetch().isSuccessful()){
                            showUnsuccessfulMergeNotification(notificationController);
                        } else {
                            if(stageToClose != null) Platform.runLater(stageToClose::close);
                        }
                        gitStatus();
                    } catch(InvalidRemoteException e){
                        showNoRemoteNotification(notificationController);
                    } catch(TransportException e){
                        showTransportExceptionNotification(notificationController, e);
                    } catch (NoMergeBaseException | JGitInternalException e) {
                        // Merge conflict
                        e.printStackTrace();
                        // todo: figure out rare NoMergeBaseException.
                        //  Has something to do with pushing conflicts.
                        //  At this point in the stack, it's caught as a JGitInternalException.
                    } catch(CheckoutConflictException e){
                        showMergingWithChangedFilesNotification(notificationController);
                    } catch(ConflictingFilesException e){
                        showMergeConflictsNotification(notificationController);
                        Platform.runLater(() -> PopUpWindows.showMergeConflictsAlert(e.getConflictingFiles()));
                        ConflictingFileWatcher.watchConflictingFiles(theModel.getCurrentRepoHelper());
                    } catch(MissingRepoException e){
                        showMissingRepoNotification(notificationController);
                        setButtonsDisabled(true);
                    } catch(GitAPIException | IOException e){
                        showGenericErrorNotification(notificationController);
                        e.printStackTrace();
                    } catch(NoTrackingException e) {
                        showNoRemoteTrackingNotification(notificationController);
                    }catch (Exception e) {
                        showGenericErrorNotification(notificationController);
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
            this.showNoRepoLoadedNotification(notificationController);
            this.setButtonsDisabled(true);
        }catch(NoCommitsToMergeException e){
            this.showNoCommitsToMergeNotification(notificationController);
        }catch(IOException e) {
            this.showGenericErrorNotification(notificationController);
        }
    }


    public void handleLoggingOff() {
        changeLogging(Level.OFF);
    }

    public void handleLoggingOn() {
        changeLogging(Level.INFO);
        logger.log(Level.INFO, "Toggled logging on");
    }

    // why are the commitSort methods so slow?
    public void handleCommitSortTopological() {
        TreeLayout.commitSortTopological = true;
        try {
            commitTreeModel.updateView();
        } catch (Exception e) {
            e.printStackTrace();
            showGenericErrorNotification();
        }
    }

    public void handleCommitSortDate() {
        TreeLayout.commitSortTopological = false;
        try {
            commitTreeModel.updateView();
        } catch (Exception e) {
            e.printStackTrace();
            showGenericErrorNotification();
        }
    }

    public void handleAbout() {
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
            this.showGenericErrorNotification();
            e.printStackTrace();
        }
    }

    String getVersion() {
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


    /**
     * Opens an editor for the .gitignore
     */
    public void handleGitIgnoreMenuItem() {
        GitIgnoreEditor.show(SessionModel.getSessionModel().getCurrentRepoHelper(), null);
    }


    public void handleNewBranchButton() {
        handleCreateOrDeleteBranchButton("create");
    }

    public void handleDeleteLocalBranchButton() {
        handleCreateOrDeleteBranchButton("local");
    }

    public void handleDeleteRemoteBranchButton() {
        handleCreateOrDeleteBranchButton("remote");
    }


    public void handleCreateOrDeleteBranchButton() {
        handleCreateOrDeleteBranchButton("create");
    }


    /**
     * Pops up a window where the user can create a new branch
     */
    public void handleCreateOrDeleteBranchButton(String tab) {
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
            createDeleteBranchController.showStage(fxmlRoot, tab);
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

    public void handleMergeFromFetchButton(){
        handleGeneralMergeButton(false);
    }

    public void handleBranchMergeButton() {
        handleGeneralMergeButton(true);
    }

    /**
     * shows the merge window
     */
    public void handleGeneralMergeButton(boolean localTabOpen) {
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
            mergeWindowController.showStage(fxmlRoot, localTabOpen);
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
//    public void onRefreshButton(){
//        logger.info("Git status button clicked");
//        showUpdatingText(true);
//        this.gitStatus();
//        showUpdatingText(false);
//        CommitTreeController.focusCommitInGraph(theModel.getCurrentRepoHelper().getBranchModel().getCurrentBranchHead());
//    }

    /**
     * Replaces branch status text with "updating" for 0.75 seconds OR the duration of gitStatus()
     */
//    private void showUpdatingText(boolean setVisible) {
//        if(setVisible){
//            isGitStatusDone = false;
//            isTimerDone = false;
//            updatingText.setVisible(true);
//
//            Timer timer = new Timer(true);
//            timer.schedule(new TimerTask() {
//                @Override
//                public void run() {
//                    if(isGitStatusDone){
//                        updatingText.setVisible(false);
//                    }
//                    isTimerDone = true;
//                }
//            }, 750);
//        }else {
//            isGitStatusDone = true;
//            if(isTimerDone) {
//                updatingText.setVisible(false);
//            }
//        }
//    }

    /**
     * Updates the trees, changed files, and branch information. Equivalent
     * to 'git status'
     */
    public void gitStatus(){
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
                indexPanelView.drawDirectoryView();
                this.theModel.getCurrentRepoHelper().getTagModel().updateTags();
                updateStatusText();
                updateBranchLabels();
                updateNewChangesToFetch();
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
            Text txt = new Text(" Branch " + type);
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
        popover.show(dropdownController.removeRecentReposButton);

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
            dropdownController.repoDropdownSelector.setValue(newCurrentRepo);

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
    public void selectCommit(String id){
        Platform.runLater(() -> {
            CommitHelper commit = this.theModel.getCurrentRepoHelper().getCommit(id);
            commitInfoNameText = commit.getName();

            commitInfoController.setCommitInfoMessageText(theModel.getCurrentRepoHelper().getCommitDescriptorString(commit, true));

            tagNameField.setVisible(true);
            tagButton.setVisible(true);
        });
    }

    /**
     * Stops displaying commit information
     */
    public void clearSelectedCommit(){
        Platform.runLater(() -> {
            commitInfoController.clearCommit();

            tagNameField.setText("");
            tagNameField.setVisible(false);
            tagButton.setVisible(false);
            pushTagsButton.setVisible(false);
        });
    }

    /// ******************************************************************************
    /// ********                 BEGIN: ERROR NOTIFICATIONS:                  ********
    /// ******************************************************************************

    private void showGenericErrorNotification(NotificationController nc) {
        Platform.runLater(()-> {
            logger.warn("Generic error warning.");
            nc.addNotification("Sorry, there was an error.");
        });
    }

    void showGenericErrorNotification() {
        Platform.runLater(()-> {
            logger.warn("Generic error warning.");
            notificationPaneController.addNotification("Sorry, there was an error.");
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

    private void showNoRepoLoadedNotification(NotificationController nc) {
        Platform.runLater(() -> {
            logger.warn("No repo loaded warning.");
            nc.addNotification("You need to load a repository before you can perform operations on it. Click on the plus sign in the upper left corner!");
        });
    }

    private void showNoRepoLoadedNotification() {
        Platform.runLater(() -> {
            logger.warn("No repo loaded warning.");
            notificationPaneController.addNotification("You need to load a repository before you can perform operations on it. Click on the plus sign in the upper left corner!");
        });
    }

    private void showInvalidRepoNotification() {
        Platform.runLater(() -> {
            logger.warn("Invalid repo warning.");
            this.notificationPaneController.addNotification("Make sure the directory you selected contains an existing (non-bare) Git repository.");
        });
    }

    private void showMissingRepoNotification(NotificationController nc){
        Platform.runLater(()-> {
            logger.warn("Missing repo warning");
            nc.addNotification("That repository no longer exists.");
        });
    }

    private void showMissingRepoNotification(){
        Platform.runLater(()-> {
            logger.warn("Missing repo warning");
            notificationPaneController.addNotification("That repository no longer exists.");
        });
    }

    private void showNoRemoteNotification(NotificationController nc){
        Platform.runLater(()-> {
            logger.warn("No remote repo warning");
            String name = this.theModel.getCurrentRepoHelper() != null ? this.theModel.getCurrentRepoHelper().toString() : "the current repository";

            nc.addNotification("There is no remote repository associated with " + name);
        });
    }

    private void showNoRemoteNotification(){
        Platform.runLater(()-> {
            logger.warn("No remote repo warning");
            String name = this.theModel.getCurrentRepoHelper() != null ? this.theModel.getCurrentRepoHelper().toString() : "the current repository";

            notificationPaneController.addNotification("There is no remote repository associated with " + name);
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

    private void showInvalidTagNameNotification(String tagName) {
        Platform.runLater(() -> {
            logger.warn("Invalid tag name exception");
            this.notificationPaneController.addNotification("The tag name '"+tagName+"' is invalid.\nRemove any of .~^:?*[]{}@ and try again.");
        });
    }

    private void showTransportExceptionNotification(TransportException e) {
        Platform.runLater(() -> {
            showTransportExceptionNotification(notificationPaneController, e);
        });

    }

    private void showTransportExceptionNotification(NotificationController nc, TransportException e) {
        Platform.runLater(() -> {

            if (e.getMessage().endsWith("git-receive-pack not permitted")) {
                logger.warn("Invalid authorization for repo warning");
                notificationPaneController.addNotification("Your login is correct, but you do not" +
                                                           " have permission to do this " +
                                                           "operation to this repository.");
            } else if (e.getMessage().endsWith("git-receive-pack not found")) {
                logger.warn("Remote repo couldn't be found warning");
                this.notificationPaneController.addNotification("The push failed because the remote repository couldn't be found.");
            } else if (e.getMessage().endsWith("not authorized")) {
                logger.warn("Invalid authorization warning");
                nc.addNotification("The username/password combination you have provided is incorrect. " +
                                   "Try reentering your password.");
            } else {
                logger.warn("Transport exception");
                nc.addNotification("Error in connecting: " + e.getMessage());
            }

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

    private void showStagedFilesSelectedNotification(){
        Platform.runLater(() -> {
            logger.warn("Staged files selected for commit warning");
            this.notificationPaneController.addNotification("You can't add staged files!");
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
                    "Try checking out a different branch first.", branch.getRefName()));
        });
    }

    private void showNotMergedNotification(BranchHelper nonmergedBranch) {
        logger.warn("Not merged notification");
        notificationPaneController.addNotification("That branch has to be merged before you can do that.");
    }

    private void showStashConflictsNotification() {
        Platform.runLater(() -> {
            logger.warn("Stash apply conflicts warning");

            EventHandler handler = event -> quickStashSave();
            this.notificationPaneController.addNotification("You can't apply that stash because there would be conflicts. " +
                    "Stash your changes or resolve conflicts first.", "stash", handler);
        });
    }

    private void showCheckoutConflictsNotification(List<String> conflictingPaths) {
        Platform.runLater(() -> {
            logger.warn("Checkout conflicts warning");

            EventHandler handler = event -> quickStashSave();
            this.notificationPaneController.addNotification("You can't switch to that branch because there would be a merge conflict. " +
                    "Stash your changes or resolve conflicts first.", "stash", handler);
        });
    }

    private void showRefAlreadyExistsNotification() {
        logger.info("Branch already exists notification");
        notificationPaneController.addNotification("Looks like that branch already exists locally!");
    }

    private void showUnsuccessfulMergeNotification(NotificationController nc){
        Platform.runLater(() -> {
            logger.warn("Failed merged warning");
            nc.addNotification("Merging failed");
        });
    }

    private void showMergingWithChangedFilesNotification(NotificationController nc){
        Platform.runLater(() -> {
            logger.warn("Can't merge with modified files warning");
            nc.addNotification("Can't merge with modified files present, please add/commit before merging.");
        });
    }

    private void showMergeConflictsNotification(NotificationController nc){
        Platform.runLater(() -> {
            logger.warn("Merge conflict warning");
            nc.addNotification("Can't complete merge due to conflicts. Resolve the conflicts and commit all files to complete merging");
        });
    }

    private void showNoRemoteTrackingNotification(NotificationController nc) {
        Platform.runLater(() -> {
            logger.warn("No remote tracking for current branch notification.");
            nc.addNotification("There is no remote tracking information for the current branch.");
        });
    }

    private void showNoCommitsToMergeNotification(NotificationController nc){
        Platform.runLater(() -> {
            logger.warn("No commits to merge warning");
            nc.addNotification("There aren't any commits to merge. Try fetching first");
        });
    }

    private void showNoFilesToStashNotification() {
        Platform.runLater(() -> {
            logger.warn("No files to stash warning");
            notificationPaneController.addNotification("There are no files to stash.");
        });
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

    /**
     * Initialization method that loads the level of logging from preferences
     * This will show a popup window if there is no preference
     * @throws BackingStoreException
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public void loadLogging() {
        Platform.runLater(() -> {
            Level storedLevel = getLoggingLevel();
            if (storedLevel == null) {
                storedLevel = PopUpWindows.getLoggingPermissions() ? Level.INFO : Level.OFF;
            }
            changeLogging(storedLevel);
            menuController.loggingToggle.setSelected(storedLevel.equals(org.apache.logging.log4j.Level.INFO));
            logger.info("Starting up.");
        });
    }

    /**
     * Helper method to change whether or not this session is logging, also
     * stores this in preferences
     * @param level the level to set the logging to
     */
    void changeLogging(Level level) {
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Configuration config = ctx.getConfiguration();
        LoggerConfig loggerConfig = config.getLoggerConfig(LogManager.ROOT_LOGGER_NAME);
        loggerConfig.setLevel(level);
        ctx.updateLoggers();

        setLoggingLevelPref(level);
    }

    Level getLoggingLevel() {
        try {
            return (Level) PrefObj.getObject(this.preferences, LOGGING_LEVEL_KEY);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    void setLoggingLevelPref(Level level) {
        try {
            PrefObj.putObject(this.preferences, LOGGING_LEVEL_KEY, level);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
