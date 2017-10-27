package elegit.controllers;

import elegit.*;
import elegit.exceptions.*;
import elegit.gui.*;
import elegit.models.*;
import elegit.monitors.ConflictingFileWatcher;
import elegit.monitors.RepositoryMonitor;
import elegit.repofile.MissingRepoFile;
import elegit.repofile.RepoFile;
import elegit.treefx.CommitTreeController;
import elegit.treefx.CommitTreeModel;
import elegit.treefx.CommitTreePanelView;
import elegit.treefx.TreeLayout;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler;
import io.reactivex.schedulers.Schedulers;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.*;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.apache.commons.lang3.SystemUtils;
import org.apache.http.annotation.GuardedBy;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.controlsfx.control.PopOver;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.*;
import org.eclipse.jgit.errors.NoMergeBaseException;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.awt.*;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.prefs.BackingStoreException;

/**
 * The controller for the entire session.
 */
public class SessionController {

    @FXML private Button commitButton;
    @FXML private Button pushButton;
    @FXML private Button fetchButton;
    @FXML private Button addButton;
    @FXML private Button removeButton;
    @FXML private Button checkoutFileButton;
    @FXML private Button mergeButton;
    @FXML private Button addDeleteBranchButton;
    @FXML private Button checkoutButton;
    @FXML private Button tagButton;
    @FXML private Button pushTagsButton;

    @FXML private Node root;

    @FXML private Tab workingTreePanelTab;
    @FXML private Tab indexPanelTab;
    @FXML private Tab allFilesPanelTab;

    @FXML private TabPane filesTabPane;

    @FXML private WorkingTreePanelView workingTreePanelView;
    @FXML private AllFilesPanelView allFilesPanelView;
    @FXML private StagedTreePanelView indexPanelView;
    @FXML private CommitTreePanelView commitTreePanelView;

    @FXML private ImageView remoteImage;
    @FXML private TextField tagNameField;

    @FXML private HBox currentLocalBranchHbox;
    @FXML private HBox currentRemoteTrackingBranchHbox;

    @FXML private Text browserText;
    @FXML private Text needToFetch;
    @FXML private Text branchStatusText;

    @FXML private ContextMenu pushContextMenu;
    @FXML private ContextMenu commitContextMenu;
    @FXML private ContextMenu fetchContextMenu;

    @FXML private Hyperlink legendLink;
    @FXML private StackPane statusTextPane;
    @FXML private AnchorPane anchorRoot;
    @FXML private NotificationController notificationPaneController;

    @FXML private MenuController menuController;
    @FXML private DropdownController dropdownController;

    // Commit Info Box
    @FXML public CommitInfoController commitInfoController;
    @FXML public VBox infoTagBox;

    // JavaFX items, intended only to be used on the FX thread
    private Label currentLocalBranchLabel;
    private Label currentRemoteTrackingLabel;

    private final SessionModel theModel;

    private static final Logger logger = LogManager.getLogger();
    private static final BooleanProperty anythingChecked = new SimpleBooleanProperty(false);

    @GuardedBy("this") private boolean tryCommandAgainWithHTTPAuth;
    @GuardedBy("this") public CommitTreeModel commitTreeModel;
    private final AtomicReference<String> commitInfoNameText = new AtomicReference<>();


    public static final Object globalLock = new Object();


    public SessionController() {
        theModel = SessionModel.getSessionModel();
        tryCommandAgainWithHTTPAuth = false;
    }

    /**
     * Initializes the environment by obtaining the model
     * and putting the views on display.
     *
     * This method is automatically called by JavaFX.
     */
    public synchronized void initialize() {
        Main.assertFxThread();

        // Gives other controllers access to this one
        CommitTreeController.setSessionController(this);
        menuController.setSessionController(this);
        dropdownController.setSessionController(this);
        commitInfoController.setSessionController(this);

        // Creates the commit tree model, and points MVC all looking at each other
        commitTreeModel = CommitTreeModel.getCommitTreeModel();
        commitTreeModel.setView(commitTreePanelView);
        //CommitTreeController.commitTreeModel = this.commitTreeModel;

        this.initializeLayoutParameters();

        this.initButtons();
        this.setButtonIconsAndTooltips();
        this.setButtonsDisabled(true);
        this.initWorkingTreePanelTab();

        // SLOW
        // here now looking
        this.initPanelViews();
        this.updateUIEnabledStatus();
        this.setRecentReposDropdownToCurrentRepo();
        this.refreshRecentReposInDropdown();

        this.initRepositoryMonitor();

        this.initStatusText();

        this.notificationPaneController.bindParentBounds(anchorRoot.heightProperty());

        VBox.setVgrow(filesTabPane, Priority.ALWAYS);

        // if there are conflicting files on startup, watches them for changes
        ConflictingFileWatcher.watchConflictingFiles(theModel.getCurrentRepoHelper());
    }

    @FXML void handleFetchButton() {
        handleFetchButton(false, false);
    }

    private class TryAgainException extends RuntimeException {};

    private interface GitOperation {
        List<Result> doGitOperation(Optional<RepoHelperBuilder.AuthDialogResponse> authResponse);
    }

//    /**
//     * Loads the repository (from its RepoHelper) that was open when the app was
//     * last closed. If this repo has been moved or deleted, it doesn't load anything.
//     *
//     * Uses the Java Preferences API (wrapped in IBM's PrefObj class) to load the repo.
//     */
//    public void loadMostRecentRepoHelper() {
//        Main.assertFxThread();
//        try{
//            String lastOpenedRepoPathString = (String) PrefObj.getObject(
//                    theModel.preferences, theModel.LAST_OPENED_REPO_PATH_KEY
//            );
//            if (lastOpenedRepoPathString != null) {
//                Path path = Paths.get(lastOpenedRepoPathString);
//                try {
//                    ExistingRepoHelper existingRepoHelper =
//                            new ExistingRepoHelper(path, new ElegitUserInfoGUI());
//                    theModel.openRepoFromHelper(existingRepoHelper);
//                    return;
//                } catch (IllegalArgumentException e) {
//                    logger.warn("Recent repo not found in directory it used to be in");
//                    // The most recent repo is no longer in the directory it used to be in,
//                    // so just don't load it.
//                }catch(GitAPIException | MissingRepoException e) {
//                    logger.error("Git error or missing repo exception");
//                    logger.debug(e.getStackTrace());
//                    e.printStackTrace();
//                } catch (CancelledAuthorizationException e) {
//                    // Should never be used, as no authorization is needed for loading local files.
//                }
//            }
//            List<RepoHelper> allRepoHelpers = theModel.getAllRepoHelpers();
//            if (allRepoHelpers.size()>0) {
//                RepoHelper helper = allRepoHelpers.get(0);
//                try {
//                    theModel.openRepoFromHelper(helper);
//                } catch (MissingRepoException e) {
//                    logger.error("Missing repo exception");
//                    e.printStackTrace();
//                }
//            }
//        }catch(IOException | BackingStoreException | ClassNotFoundException e){
//            logger.error("Some sort of error loading most recent repo helper");
//            logger.debug(e.getStackTrace());
//            e.printStackTrace();
//        }
//    }

    private void handleFetchButton(boolean prune, boolean pull) {
        GitOperation gitOp = authResponse -> gitFetch(authResponse, prune, pull);

        String displayString;
        if (!pull)
            displayString = "Fetching...";
        else
            displayString = "Pulling...";

        Observable
                .just(1)
                .doOnNext(unused -> showBusyWindowAndPauseRepoMonitor(displayString))

                // Note that the below is a threaded operation, and so we want to make sure that the following
                // operations (hiding the window, etc) depend on it.
                .flatMap(unused -> doAndRepeatGitOperation(gitOp))
                .doOnNext(unused -> hideBusyWindowAndResumeRepoMonitor())
                .subscribe(unused -> {}, Throwable::printStackTrace);
    }

    // Repeat trying to fetch. First time: no authentication window. On repeated attempts,
    // authentication window is shown. Effort ends when authentication window is cancelled.
    private Observable<String> doAndRepeatGitOperation(GitOperation gitOp) {
        Main.assertFxThread();
        AtomicBoolean httpAuth = new AtomicBoolean(false);
        return Observable
                .just(1)
                .map(integer -> authenticateReactive(httpAuth.get()))

                //.observeOn(Schedulers.io())
                .map(gitOp::doGitOperation)

                //.observeOn(JavaFxScheduler.platform())
                .map(results -> {
                    gitOperationShowResults(notificationPaneController, results);
                    if (tryOpAgain(results)) {
                        httpAuth.set(true);
                        throw new TryAgainException();
                    }
                    return "success";
                })
                .retry(throwable -> throwable instanceof TryAgainException)
                .onErrorResumeNext(Observable.just("cancelled"));
    }

    private boolean tryOpAgain(List<Result> results) {
        for (Result result : results) {
            // Exception where it wasn't a transport exception: try again
            if (result.status == ResultStatus.EXCEPTION
                    && !(result.exception instanceof TransportException))
                return true;

            // Exception where it was a transport exception, and we should try again
            if (result.status == ResultStatus.EXCEPTION
                    && determineIfTryAgainReactive((TransportException) result.exception))
                return true;
        }
        return false;
    }
    /**
     * Helper method that passes the main stage to session controller
     * @param stage Stage
     */
    public void setStage(Stage stage) {
        notificationPaneController.setAnchor(stage);
    }

    /**
     * Helper method that creates the labels for the branch names
     */
    private void initStatusText() {
        currentRemoteTrackingLabel = new Label("N/A");
        currentLocalBranchLabel = new Label("N/A");
        initCellLabel(currentLocalBranchLabel, currentLocalBranchHbox);
        initCellLabel(currentRemoteTrackingLabel, currentRemoteTrackingBranchHbox);

        updateStatusText();
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
     * Helper method to update the current local branch, remote tracking branch and
     * whether or not there are remote changes to fetch
     */
    private void updateStatusText(){
        Main.assertFxThread();
        if (this.theModel.getCurrentRepoHelper()==null) return;
        boolean update;

        update = RepositoryMonitor.hasFoundNewRemoteChanges.get();
        String fetchText = update ? "New changes to fetch" : "Up to date";
        Color fetchColor = update ? Color.FIREBRICK : Color.FORESTGREEN;
        needToFetch.setText(fetchText);
        needToFetch.setFont(new Font(15));
        needToFetch.setFill(fetchColor);

        BranchHelper localBranch = this.theModel.getCurrentRepoHelper().getBranchModel().getCurrentBranch();
        update = !localBranch.getAbbrevName().equals(currentLocalBranchLabel.getText());
        if (update) {
            Platform.runLater(() -> {
                currentLocalBranchLabel.setText(localBranch.getAbbrevName());
                currentLocalBranchLabel.setOnMouseClicked((event -> CommitTreeController.focusCommitInGraph(localBranch.getCommit())));
                addToolTip(currentLocalBranchHbox, localBranch.getRefName());
            });
        }

        String remoteBranch = "N/A";
        String remoteBranchFull = "N/A";
        CommitHelper remoteHead = null;
        try {
            remoteBranch = this.theModel.getCurrentRepoHelper().getBranchModel().getCurrentRemoteAbbrevBranch();
            remoteHead = this.theModel.getCurrentRepoHelper().getBranchModel().getCurrentRemoteBranchHead();
            remoteBranchFull = this.theModel.getCurrentRepoHelper().getBranchModel().getCurrentRemoteBranch();
        } catch (IOException e) {
            this.showGenericErrorNotification();
        }
        if (remoteBranch==null) {
            remoteBranch = "N/A";
            remoteBranchFull = "N/A";
        }

        String remoteBranchFinal = remoteBranch;
        String remoteBranchFullFinal = remoteBranchFull;
        update = !remoteBranch.equals(currentRemoteTrackingLabel.getText());
        if (update) {
            CommitHelper finalRemoteHead = remoteHead;
            Platform.runLater(() -> {
                currentRemoteTrackingLabel.setText(remoteBranchFinal);
                if (finalRemoteHead != null)
                    currentRemoteTrackingLabel.setOnMouseClicked((event -> CommitTreeController.focusCommitInGraph(finalRemoteHead)));
                addToolTip(currentRemoteTrackingBranchHbox, remoteBranchFullFinal);
            });
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
        Main.assertFxThread();
        workingTreePanelTab.getTabPane().getSelectionModel().select(workingTreePanelTab);
    }

    /**
     * Initializes the repository monitor
     */
    private void initRepositoryMonitor() {
        RepositoryMonitor.init(this);
        RepositoryMonitor.hasFoundNewRemoteChanges.addListener((observable, oldValue, newValue) -> {
            if(newValue) updateStatusText();
        });
    }

    /**
     * Sets up the layout parameters for things that cannot be set in FXML
     */
    private void initializeLayoutParameters(){
        Main.assertFxThread();

        // Set minimum/maximum sizes for buttons
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
        Main.assertFxThread();
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
        Main.assertFxThread();
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
                    URL remoteURL = new URL(URLString);
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
    public void setButtonsDisabled(boolean disable) {
        Main.assertFxThread();
        dropdownController.setButtonsDisabled(disable);
        tagButton.setDisable(disable);
        commitButton.setDisable(disable);
        pushButton.setDisable(disable);
        fetchButton.setDisable(disable);
        remoteImage.setVisible(!disable);
        browserText.setVisible(!disable);
        workingTreePanelTab.setDisable(disable);
        allFilesPanelTab.setDisable(disable);
        indexPanelTab.setDisable(disable);
        addDeleteBranchButton.setDisable(disable);
        checkoutButton.setDisable(disable);
        mergeButton.setDisable(disable);
        pushTagsButton.setDisable(disable);
        needToFetch.setVisible(!disable);
        currentLocalBranchHbox.setVisible(!disable);
        currentRemoteTrackingBranchHbox.setVisible(!disable);
        statusTextPane.setVisible(!disable);
        menuController.updateMenuBarEnabledStatus(disable);

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
        Main.assertFxThread();
        if (this.theModel.getCurrentRepoHelper() == null && this.theModel.getAllRepoHelpers().size() >= 0) {
            // (There's no repo for buttons to interact with, but there are repos in the menu bar)
            setButtonsDisabled(true);
        } else {
            setButtonsDisabled(false);
        }
    }

    /**
     * Called when the "Load existing repository" option is clicked
     */
    public void handleLoadExistingRepoOption() {
        handleLoadRepoMenuItem(new ExistingRepoHelperBuilder());
    }

    /**
     * Called when the "Clone repository" option is clicked
     */
    void handleCloneNewRepoOption() {
        handleLoadRepoMenuItem(new ClonedRepoHelperBuilder());
    }

    /**
     * Called when a selection is made from the 'Load New Repository' menu. Creates a new repository
     * using the given builder and updates the UI
     * @param builder the builder to use to create a new repository
     */
    private synchronized void handleLoadRepoMenuItem(RepoHelperBuilder builder) {
        Main.assertFxThread();
        try {
            RepoHelper repoHelper = builder.getRepoHelperFromDialogs();
            loadDesignatedRepo(repoHelper);
        } catch (Exception e) {
            showSingleResult(notificationPaneController, new Result(ResultOperation.LOAD, e));
        }
    }

    public void loadDesignatedRepo(RepoHelper repoHelper) {
        GitOperation gitOp = authResponse -> loadRepo(authResponse, repoHelper);

        if (theModel.getCurrentRepoHelper() != null && repoHelper.getLocalPath().equals(theModel.getCurrentRepoHelper().getLocalPath())) {
            showSameRepoLoadedNotification();
            return;
        }
        TreeLayout.stopMovingCells();
        refreshRecentReposInDropdown();

        Observable
                .just(1)
                .doOnNext(unused -> showBusyWindowAndPauseRepoMonitor("Loading repository..."))

                // Note that the below is a threaded operation, and so we want to make sure that the following
                // operations (hiding the window, etc) depend on it.
                .flatMap(unused -> doAndRepeatGitOperation(gitOp))

                //.observeOn(Schedulers.io())
                .doOnNext(result -> System.out.println("result is :" + result))
                .doOnNext((result) -> {
                    if (result.equals("success")) {
                        synchronized (globalLock) {
                            initPanelViews();
                        }
                    }
                })

                //.observeOn(JavaFxScheduler.platform())
                .doOnNext((result) -> {
                    if (result.equals("success")) {
                        setRecentReposDropdownToCurrentRepo();
                        updateUIEnabledStatus();
                    }
                })
                .doOnNext(unused -> hideBusyWindowAndResumeRepoMonitor())
                .subscribe(unused -> {
                }, Throwable::printStackTrace);
    }

    private List<Result> loadRepo (Optional<RepoHelperBuilder.AuthDialogResponse> responseOptional,
                                                RepoHelper repoHelper) {
        synchronized (globalLock) {
            List<Result> results = new ArrayList<>();
            try {
                responseOptional.ifPresent(response ->
                        repoHelper.setOwnerAuth(
                                new UsernamePasswordCredentialsProvider(response.username, response.password))
                );
                theModel.openRepoFromHelper(repoHelper);
            } catch (Exception e) {
                results.add(new Result(ResultStatus.EXCEPTION, ResultOperation.LOAD, e));
            }
            return Collections.unmodifiableList(results);
        }
    }


    /**
     * Gets the current RepoHelper and sets it as the selected value of the dropdown.
     */
    @FXML
    private void setRecentReposDropdownToCurrentRepo() {
        Main.assertFxThread();
        synchronized (this) {
            RepoHelper currentRepo = this.theModel.getCurrentRepoHelper();
            dropdownController.setCurrentRepo(currentRepo);
        }
    }

    /**
     * Adds all the model's RepoHelpers to the dropdown
     */
    @FXML
    private void refreshRecentReposInDropdown() {
        Main.assertFxThread();
        synchronized (this) {
            List<RepoHelper> repoHelpers = this.theModel.getAllRepoHelpers();
            ObservableList<RepoHelper> obsRepoHelpers = FXCollections.observableArrayList(repoHelpers);
            ObservableList<RepoHelper> immutableRepoHelpers = FXCollections.unmodifiableObservableList(obsRepoHelpers);
            dropdownController.setAllRepos(FXCollections.observableArrayList(immutableRepoHelpers));
        }
    }

    /**
     * Loads the given repository and updates the UI accordingly.
     * @param repoHelper the repository to open
     */
    private synchronized void handleRecentRepoMenuItem(RepoHelper repoHelper){
        Main.assertFxThread();
        loadDesignatedRepo(repoHelper);
    }

    public void handleAddButton() {
        Main.assertFxThread();

        logger.info("Add button clicked");
        Observable.just(1)
                .doOnNext(unused -> addPreChecks())
                .doOnNext(unused -> showBusyWindowAndPauseRepoMonitor("Adding..."))

                //.observeOn(Schedulers.io())
                .map(unused -> addOperation())

                //.observeOn(JavaFxScheduler.platform())

                .onErrorResumeNext(this::wrapMergeException)
                .doOnNext(results -> gitOperationShowResults(notificationPaneController, results))
                .doOnNext(unused -> hideBusyWindowAndResumeRepoMonitor())
                .subscribe(unused -> {}, Throwable::printStackTrace);
    }

    private void addPreChecks() throws NoRepoLoadedException, MissingRepoException, NoFilesSelectedToAddException, StagedFileCheckedException {
        if(this.theModel.getCurrentRepoHelper() == null) throw new NoRepoLoadedException();
        if(!this.theModel.getCurrentRepoHelper().exists()) throw new MissingRepoException();

        if(!workingTreePanelView.isAnyFileSelected()) throw new NoFilesSelectedToAddException();
        if(workingTreePanelView.isAnyFileStagedSelected()) throw new StagedFileCheckedException();
    }

    /**
     * Adds all files that are selected if they can be added
     * TODO: Make sure this gets appropriately synchronized
     */
    private List<Result> addOperation() {
        synchronized (globalLock) {
            //Main.assertFxThread();

            ArrayList<Result> results = new ArrayList<>();
            try {
                ArrayList<Path> filePathsToAdd = new ArrayList<>();
                ArrayList<Path> filePathsToRemove = new ArrayList<>();

                // Try to add all files, throw exception if there are ones that can't be added
                if (workingTreePanelView.isSelectAllChecked()) {
                    filePathsToAdd.add(Paths.get("."));
                } else {
                    for (RepoFile checkedFile : workingTreePanelView.getCheckedFilesInDirectory()) {
                        if (checkedFile.canAdd()) {
                            filePathsToAdd.add(checkedFile.getFilePath());
                        } else if (checkedFile instanceof MissingRepoFile) {
                            // JGit does not support adding missing files, instead remove them
                            filePathsToRemove.add(checkedFile.getFilePath());
                        } else {
                            throw new UnableToAddException(checkedFile.getFilePath().toString());
                        }
                    }
                }

                if (filePathsToAdd.size() > 0)
                    theModel.getCurrentRepoHelper().addFilePaths(filePathsToAdd);
                if (filePathsToRemove.size() > 0)
                    theModel.getCurrentRepoHelper().removeFilePaths(filePathsToRemove);
            } catch (Exception e) {
                results.add(new Result(ResultOperation.ADD, e));
            }
            return Collections.unmodifiableList(results);
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

            showBusyWindow("Removing...");
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
                                throw new UnableToRemoveException(checkedFile.getFilePath().toString());
                        }

                        theModel.getCurrentRepoHelper().removeFilePaths(filePathsToRemove);
                        gitStatus();

                    } catch(JGitInternalException e){
                        showJGitInternalError(e);
                    } catch (UnableToRemoveException e) {
                        showCannotRemoveFileNotification(e.getFilename());
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
            logger.info("Checkout files from commit button clicked");
            if(this.theModel.getCurrentRepoHelper() == null) throw new NoRepoLoadedException();

            logger.info("Opened checkout files window");
            // Create and display the Stage:
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/elegit/fxml/CheckoutFiles.fxml"));
            fxmlLoader.load();
            CheckoutFilesController checkoutFilesController = fxmlLoader.getController();
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

        showBusyWindow("Committing all...");

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
        GridPane fxmlRoot = fxmlLoader.getRoot();
        commitController.showStage(fxmlRoot);
    }


    /**
     * Checks things are ready for a tag, then performs a git-tag
     *
     */
    public void handleTagButton() {
        Main.assertFxThread();
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
                    // TODO: Tagname field shouldn't be on separate thread. Check this EVERYWHERE (no FX updates on worker threads.)
                    try {
                        theModel.getCurrentRepoHelper().getTagModel().tag(tagName, commitInfoNameText.get());

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
        GitOperation gitOp = authResponse -> pushBranchOrAllDetails(authResponse, pushType, push);

        Observable
                .just(1)
                .doOnNext(unused -> showBusyWindowAndPauseRepoMonitor("Pushing..."))

                // Note that the below is a threaded operation, and so we want to make sure that the following
                // operations (hiding the window, etc) depend on it.
                .flatMap(unused -> doAndRepeatGitOperation(gitOp))
                .doOnNext(unused -> hideBusyWindowAndResumeRepoMonitor())
                .subscribe(unused -> {}, Throwable::printStackTrace);
    }


    private synchronized void determineIfTryAgain(TransportException e) {
        showTransportExceptionNotification(e);

        // Don't try again with HTTP authentication if SSH prompt for authentication is canceled
        if (!e.getMessage().endsWith("Auth cancel"))
            tryCommandAgainWithHTTPAuth = true;
    }

    private boolean determineIfTryAgainReactive(TransportException e) {
        // Don't try again with HTTP authentication if SSH prompt for authentication is canceled
        return (!e.getMessage().endsWith("Auth cancel"));
    }

    private List<Result> pushBranchOrAllDetails(Optional<RepoHelperBuilder.AuthDialogResponse> responseOptional, PushType pushType,
                                        PushCommand push) {
        synchronized (globalLock) {
            Main.assertNotFxThread();

            List<Result> results = new ArrayList<>();
            try {
                RepositoryMonitor.resetFoundNewChanges();
                RepoHelper helper = theModel.getCurrentRepoHelper();
                responseOptional.ifPresent(response ->
                        helper.setOwnerAuth(
                                new UsernamePasswordCredentialsProvider(response.username, response.password))
                );
                if (pushType == PushType.BRANCH) {
                    helper.pushCurrentBranch(push);
                } else if (pushType == PushType.ALL) {
                    helper.pushAll(push);
                } else {
                    assert false : "PushType enum case not handled";
                }
            } catch (Exception e) {
                results.add(new Result(ResultStatus.EXCEPTION, ResultOperation.PUSH, e));
            }
            return Collections.unmodifiableList(results);
        }
    }

    private synchronized RepoHelperBuilder.AuthDialogResponse askUserForCredentials() throws CancelledAuthorizationException {
        final RepoHelperBuilder.AuthDialogResponse response;
        if (tryCommandAgainWithHTTPAuth) {
            response = RepoHelperBuilder.getAuthCredentialFromDialog();
        } else {
            response = null;
        }
        return response;
    }


    private RepoHelperBuilder.AuthDialogResponse askUserForCredentialsReactive(boolean httpAuthenticate) throws CancelledAuthorizationException {
        final RepoHelperBuilder.AuthDialogResponse response;
        if (httpAuthenticate) {
            response = RepoHelperBuilder.getAuthCredentialFromDialog();
        } else {
            response = null;
        }
        return response;
    }

    /**
     * Performs a `git push --tags`
     */
    public synchronized void handlePushTagsButton() {
        try {
            logger.info("Push tags button clicked");

            final RepoHelperBuilder.AuthDialogResponse credentialResponse = authenticateAndShowBusy("Pushing tags...");
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
            RepositoryMonitor.resetFoundNewChanges();
            RepoHelper helper = theModel.getCurrentRepoHelper();
            if (response != null) {
                helper.setOwnerAuth(
                        new UsernamePasswordCredentialsProvider(response.username, response.password));
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
    public synchronized void deleteBranch(BranchHelper selectedBranch) {
        BranchModel branchModel = theModel.getCurrentRepoHelper().getBranchModel();
        boolean authorizationSucceeded = true;
        try {
            if (selectedBranch != null) {
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

    synchronized void deleteRemoteBranch(BranchHelper selectedBranch, BranchModel branchModel, Consumer<String> updateFn) {
        try {
            final RepoHelperBuilder.AuthDialogResponse credentialResponse = askUserForCredentials();

            showBusyWindow("Deleting remote branch...");

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
                theModel.getCurrentRepoHelper().setOwnerAuth(
                        new UsernamePasswordCredentialsProvider(response.username, response.password));
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
     * Adds a commit reverting the selected commits
     * @param commits the commits to revert
     */
    public void handleRevertMultipleButton(List<CommitHelper> commits) {
        try {
            logger.info("Revert button clicked");

            if(this.theModel.getCurrentRepoHelper() == null) throw new NoRepoLoadedException();

            showBusyWindow("Reverting...");
            Thread th = new Thread(new Task<Void>(){
                @Override
                protected Void call() {
                    try{
                        theModel.getCurrentRepoHelper().revertHelpers(commits);
                        gitStatus();
                    } catch(MultipleParentsNotAllowedException e) {
                        for (CommitHelper commit : commits) {
                            if (commit.numParents() > 1) {
                                showCantRevertMultipleParentsNotification();
                            }
                            if (commit.numParents() == 0) {
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

            showBusyWindow("Reverting...");
            Thread th = new Thread(new Task<Void>(){
                @Override
                protected Void call() {
                    try{
                        theModel.getCurrentRepoHelper().revert(commit);
                        gitStatus();
                    } catch(MultipleParentsNotAllowedException e) {
                        if(commit.numParents() > 1) {
                            showCantRevertMultipleParentsNotification();
                        }
                        if (commit.numParents() == 0) {
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

            showBusyWindow("Resetting...");
            Thread th = new Thread(new Task<Void>(){
                @Override
                protected Void call() {
                    try{
                        theModel.getCurrentRepoHelper().reset(commit.getName(), type);
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
            //stashSaveController.setSessionController(this);
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
    void handleStashApplyButton() {
        // TODO: make it clearer which stash this applies
        logger.info("Stash apply button clicked");
        try {
            CommitHelper topStash = theModel.getCurrentRepoHelper().stashList().get(0);
            this.theModel.getCurrentRepoHelper().stashApply(topStash.getName(), false);
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
    void handleStashListButton() {
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

    private void showBusyWindowAndPauseRepoMonitor(String displayMessage) {
      pauseRepoMonitor("Fetch button clicked");
      showBusyWindow(displayMessage);
    }

    private void hideBusyWindowAndResumeRepoMonitor() {
        Main.assertFxThread();
        gitStatus();
        BusyWindow.hide();
        RepositoryMonitor.unpause();
        LoggingModel.submitLog();
    }

    private void pauseRepoMonitor(String fetch_button_clicked) {
        logger.info(fetch_button_clicked);
        RepositoryMonitor.pause();
    }

    /**
     * Handles a click on Fetch -p
     */
    public void handlePruneFetchButton() {
        handleFetchButton(true, false);
    }


    /**
     * Peforms a git pull
     */
    public void handlePullButton() {
        handleFetchButton(false, true);
    }

    enum ResultStatus {OK, NOCOMMITS, EXCEPTION, MERGE_FAILED};
    enum ResultOperation {FETCH, MERGE, ADD, LOAD, PUSH};

    public static class Result {
        public final ResultStatus status;
        public final ResultOperation operation;
        public final Throwable exception;

        public Result(ResultStatus status, ResultOperation operation) {
            this.status = status;
            this.operation = operation;
            this.exception = new RuntimeException();
        }

        public Result(ResultStatus status, ResultOperation operation, Throwable exception) {
            this.status = status;
            this.operation = operation;
            this.exception = exception;
        }

        public Result(ResultOperation operation, Throwable exception) {
            this.status = ResultStatus.EXCEPTION;
            this.operation = operation;
            this.exception = exception;
        }
    }
    /**
     * Queries the remote for new commits, and updates the local
     * remote as necessary. Result objects that it returns indicates what should be done back on the GUI thread
     * when it all comes back.
     * Equivalent to `git fetch`
     */
    private List<Result> gitFetch(Optional<RepoHelperBuilder.AuthDialogResponse> responseOptional, boolean prune, boolean pull) {
        synchronized(globalLock) {
            Main.assertNotFxThread();

            List<Result> results = new ArrayList<>();
            try {
                RepositoryMonitor.resetFoundNewChanges();
                RepoHelper helper = theModel.getCurrentRepoHelper();
                responseOptional.ifPresent(response ->
                        helper.setOwnerAuth(
                                new UsernamePasswordCredentialsProvider(response.username, response.password))
                );
                if (!helper.fetch(prune)) {
                    results.add(new Result(ResultStatus.NOCOMMITS, ResultOperation.FETCH));
                }
                if (pull) {
                    mergeOperation(helper);
                }
            } catch (Exception e) {
                results.add(new Result(ResultStatus.EXCEPTION, ResultOperation.FETCH, e));
            }

            return Collections.unmodifiableList(results);
        }
    }

    private void gitOperationShowResults(NotificationController nc, List<Result> results) {
        Main.assertFxThread();

        for (Result result : results) {
            showSingleResult(nc, result);
        }

        // Possibly overkill in some cases, but this ensures that after updating the interface based on some results,
        // that everything is as up to date as possible.
        gitStatus();
    }

    private void showSingleResult(NotificationController nc, Result result) {
        if (result.status == ResultStatus.NOCOMMITS)
            showNotification(nc, "No commits fetched warning", "No new commits were fetched.");

        else if (result.status == ResultStatus.MERGE_FAILED)
            showNotification(nc, "Failed merged warning", "Merging failed.");

        else if (result.status == ResultStatus.EXCEPTION) {

            if (result.exception instanceof InvalidRemoteException) {
                String name = this.theModel.getCurrentRepoHelper() != null ?
                        this.theModel.getCurrentRepoHelper().toString() :
                        "the current repository";
                showNotification(nc, "No remote repo warning",
                        "There is no remote repository associated with " + name);

            } else if (result.exception instanceof MissingRepoException) {
                showNotification(nc, "Missing repo warning", "That repository no longer exists.");
                if (result.operation != ResultOperation.LOAD) {
                    setButtonsDisabled(true);
                }
                refreshRecentReposInDropdown();

            } else if (result.exception instanceof TransportException) {
                // TODO: need to enhance with text from  showTransportExceptionNotification(e);
                showNotification(nc, "Transport Exception", "Transport Exception.");

            } else if (result.exception instanceof CheckoutConflictException) {
                showNotification(nc, "Can't merge with modified files warning",
                        "Can't merge with modified files present, please add/commit before merging.");

            } else if (result.exception instanceof NoTrackingException) {
                showNotification(nc, "No remote tracking for current branch notification.",
                        "There is no remote tracking information for the current branch.");

            } else if (result.exception instanceof NoRepoLoadedException) {
                showNotification(nc, "No repo loaded warning.",
                        "You need to load a repository before you can perform operations on it. " +
                                "Click on the plus sign in the upper left corner!");
                setButtonsDisabled(true);
                refreshRecentReposInDropdown();

            } else if (result.exception instanceof NoCommitsToMergeException) {
                showNotification(nc, "No commits to merge warning",
                        "There aren't any commits to merge. Try fetching first");

            } else if (result.exception instanceof ConflictingFilesException) {
                showNotification(nc, "Merge conflict warning",
                        "Can't complete merge due to conflicts. " +
                                "Resolve the conflicts and commit all files to complete merging");
                PopUpWindows.showMergeConflictsAlert(
                        ((ConflictingFilesException)result.exception).getConflictingFiles());
                ConflictingFileWatcher.watchConflictingFiles(theModel.getCurrentRepoHelper());


            } else if (result.exception instanceof NoMergeBaseException) {

                // Rare exception, not understood yet. Figure this out. "Has something to do with pushing
                // conflicts. At this point in the stack, it's caught as a JGitInternalException." (jconnelly)
                String stackTrace = Arrays.toString(result.exception.getStackTrace());
                showNotification(nc, "Rare merge exception: " + stackTrace,
                        "Rare merge error: " + stackTrace);

            } else if (result.exception instanceof JGitInternalException) {
                if (result.exception.getCause().toString().contains("LockFailedException")) {
                    showNotification(nc, "Lock failed warning.",
                            result.exception.getCause().getMessage() +
                                    ". If no other git processes are running, manually remove all .lock files.");
                } else {
                    String stackTrace = Arrays.toString(result.exception.getStackTrace());
                    showNotification(nc, "Generic jgit internal warning.",
                            "Git internal error: " + stackTrace);
                }

            } else if (result.exception instanceof UnableToAddException) {
                showNotification(nc, "Cannot add file notification",
                        "Cannot add " + ((UnableToAddException) result.exception).getFilename() +
                                ". It might already be added (staged).");

            } else if (result.exception instanceof NoFilesSelectedToAddException) {
                showNotification(nc, "No files selected for add warning",
                        "You need to select files to add");

            } else if (result.exception instanceof StagedFileCheckedException) {
                showNotification(nc, "Staged files selected for commit warning",
                        "You can't add staged files!");

            } else if (result.exception instanceof BackingStoreException |
                    result.exception instanceof ClassNotFoundException) {
                // These should only occur when the recent repo information
                // fails to be loaded or stored, respectively
                // Should be ok to silently fail
                logger.warn("Shouldn't matter (BSE or CNFE): " + result.exception.getStackTrace());

            } else if (result.exception instanceof NoRepoSelectedException |
                    result.exception instanceof CancelledAuthorizationException) {
                // The user pressed cancel on the dialog box, or
                // the user pressed cancel on the authorize dialog box. Do nothing!
                logger.warn("Shouldn't matter (NRLE or CAE): " + result.exception.getStackTrace());

            } else if (result.exception instanceof IOException && result.operation == ResultOperation.LOAD) {
                showNotification(nc, "Repo not loaded warning",
                        "Something went wrong, so no repository was loaded.");

            } else if (result.exception instanceof InvalidPathException &&
                    result.operation == ResultOperation.LOAD) {
                showNotification(nc, "Repo not loaded warning",
                        "Something went wrong, so no repository was loaded.");

            } else if (result.exception instanceof IllegalArgumentException) {
                showNotification(nc,"Invalid repo warning.",
                        "Make sure the directory you selected contains an existing (non-bare) Git repository.");

            } else {

                String stackTrace = Arrays.toString(result.exception.getStackTrace());
                showNotification(nc, "Unhandled error warning: " + stackTrace,
                        "An error occurred when fetching: " + stackTrace);
            }
        }
    }



    private void showNotification(NotificationController nc, String loggerText, String userText) {
        Main.assertFxThread();
        logger.warn(loggerText);
        nc.addNotification(userText);
    }


    private RepoHelperBuilder.AuthDialogResponse authenticateAndShowBusy(String message)
            throws NoRepoLoadedException, CancelledAuthorizationException {
        if (this.theModel.getCurrentRepoHelper() == null) throw new NoRepoLoadedException();

        final RepoHelperBuilder.AuthDialogResponse response = askUserForCredentials();

        showBusyWindow(message);
        return response;
    }

    private Optional<RepoHelperBuilder.AuthDialogResponse> authenticateReactive(boolean httpAuthenticate)
            throws NoRepoLoadedException, CancelledAuthorizationException {

        final RepoHelperBuilder.AuthDialogResponse response = askUserForCredentialsReactive(httpAuthenticate);


        if (response != null) {
            return Optional.of(response);
        } else
            return Optional.empty();
    }

    private void showBusyWindow(String message) {
        BusyWindow.show();
        BusyWindow.setLoadingText(message);
    }

    /**
     * Does a merge from fetch
     */
    public void mergeFromFetch() {
        mergeFromFetchCreateChain(notificationPaneController)
            .subscribe(unused -> {}, Throwable::printStackTrace);

    }


    /**
     * merges the remote-tracking branch associated with the current branch into the current local branch
     */
    public Observable<List<Result>> mergeFromFetchCreateChain(NotificationController nc) {
        Main.assertFxThread();
        logger.info("Merge from fetch button clicked");

        return Observable.just(theModel.getCurrentRepoHelper())
                .doOnNext(this::mergePreChecks) // skips to onErrorResumeNext when these fail
                .doOnNext(unused -> showBusyWindowAndPauseRepoMonitor("Merging..."))

                .observeOn(Schedulers.io())
                .map(this::mergeOperation)

                .observeOn(JavaFxScheduler.platform())

                .onErrorResumeNext(this::wrapMergeException)
                .doOnNext(results -> gitOperationShowResults(nc, results))
                .doOnNext(unused -> hideBusyWindowAndResumeRepoMonitor());

        //  notice there is no subscribe here; the caller to this method should use it
    }

    private ObservableSource<? extends List<Result>> wrapMergeException(Throwable e) {
        ArrayList<Result> results = new ArrayList<>();
        results.add(new Result(ResultOperation.MERGE, e));
        return Observable.just(results);
    }

    private void mergePreChecks(RepoHelper repoHelper) throws NoRepoLoadedException, IOException, NoCommitsToMergeException {
        Main.assertFxThread();
        if (repoHelper == null) throw new NoRepoLoadedException();
        if (repoHelper.getBehindCount() < 1) throw new NoCommitsToMergeException();
    }

    private List<Result> mergeOperation(RepoHelper repoHelper) {
        synchronized (globalLock) {
            Main.assertNotFxThread();

            ArrayList<Result> results = new ArrayList<>();
            try {
                if (!repoHelper.mergeFromFetch().isSuccessful()) {
                    results.add(new Result(ResultStatus.MERGE_FAILED, ResultOperation.MERGE));
                }
            } catch (Exception e) {
                results.add(new Result(ResultOperation.MERGE, e));
            }
            return results;
        }
    }



    // why are the commitSort methods so slow?
    public synchronized void handleCommitSortToggle() {
        try {
            commitTreeModel.updateView();
        } catch (Exception e) {
            e.printStackTrace();
            showGenericErrorNotification();
        }
    }



    void handleNewBranchButton() {
        handleCreateOrDeleteBranchButton("create");
    }

    void handleDeleteLocalBranchButton() {
        handleCreateOrDeleteBranchButton("local");
    }

    void handleDeleteRemoteBranchButton() {
        handleCreateOrDeleteBranchButton("remote");
    }


    @FXML private void handleCreateOrDeleteBranchButton() {
        handleCreateOrDeleteBranchButton("create");
    }


    /**
     * Pops up a window where the user can create a new branch
     */
    private void handleCreateOrDeleteBranchButton(String tab) {
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
    void handleCommitNameCopyButton(){
        logger.info("Commit name copied");
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(commitInfoNameText.get());
        clipboard.setContent(content);
    }

    /**
     * Jumps to the selected commit in the tree display
     */
    void handleGoToCommitButton(){
        logger.info("Go to commit button clicked");
        String id = commitInfoNameText.get();
        CommitTreeController.focusCommitInGraph(id);
    }

    public void handleMergeFromFetchButton(){
        handleGeneralMergeButton(false);
    }

    void handleBranchMergeButton() {
        handleGeneralMergeButton(true);
    }

    /**
     * shows the merge window
     */
    private void handleGeneralMergeButton(boolean localTabOpen) {
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
     * Updates the trees, changed files, and branch information. Equivalent
     * to 'git status'
     */
    // TODO: Add error checking below from RepositoryMonitor (it doesn't need to do this)
    //                 if(!pauseLocalMonitor && SessionModel.getSessionModel().getCurrentRepoHelper() != null &&
    //                    SessionModel.getSessionModel().getCurrentRepoHelper().exists()){
        public synchronized void gitStatus() {
            //Main.assertFxThread();

            Observable.just(1)
                    .doOnNext(o -> System.out.println("starting git status"))
                    //.doOnNext(u -> BusyWindow.show())
                    //.doOnNext(u -> BusyWindow.setLoadingText("Git status..."))


                    //.observeOn(Schedulers.io())
                    .doOnNext(u -> { synchronized (globalLock) {
                        RepositoryMonitor.pause();
                        System.out.println("It's paused");
                        // If the layout is still going, don't run
//                        if (commitTreePanelView.isLayoutThreadRunning) {
//                            RepositoryMonitor.unpause();
//                            return;
//                        }
                        try {
                            theModel.getCurrentRepoHelper().getBranchModel().updateAllBranches();
                            commitTreeModel.update();
                            workingTreePanelView.drawDirectoryView();
                            allFilesPanelView.drawDirectoryView();
                            System.out.println("I'm halfway through");
                            indexPanelView.drawDirectoryView();
                            this.theModel.getCurrentRepoHelper().getTagModel().updateTags();
                            updateStatusText();

                        } catch (Exception e) {
                            showGenericErrorNotification();
                            e.printStackTrace();
                        } finally {
                            RepositoryMonitor.unpause();

                        }

                    }})

                    .doOnNext(o -> System.out.println("ending git status"))
                    .observeOn(JavaFxScheduler.platform())
                    //.doOnNext(u -> BusyWindow.hide())
                    .subscribe(u -> {}, Throwable::printStackTrace);
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
    void openRepoDirectory(){
        if (Desktop.isDesktopSupported()) {
            try{
                logger.info("Opening Repo Directory");
                if(this.theModel.getCurrentRepoHelper() == null) throw new NoRepoLoadedException();
                // Use Desktop to open the current directory unless it's Linux
                if (!SystemUtils.IS_OS_LINUX) {
                    Desktop.getDesktop().open(this.theModel.getCurrentRepoHelper().getLocalPath().toFile());
                }
                else {
                    Runtime runtime = Runtime.getRuntime();
                    String[] args = {"nautilus",this.theModel.getCurrentRepoHelper().getLocalPath().toFile().toString()};
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
     * removes selected repo shortcuts
     * @param checkedItems list of selected repos
     */
    void handleRemoveReposButton(List<RepoHelper> checkedItems) {
        logger.info("Removed repos");
        this.theModel.removeRepoHelpers(checkedItems);

        // If there are repos that aren't the current one, and the current repo is being removed, load a different repo
        if (!this.theModel.getAllRepoHelpers().isEmpty() && !this.theModel.getAllRepoHelpers().contains(theModel.getCurrentRepoHelper())) {
            int newIndex = this.theModel.getAllRepoHelpers().size()-1;
            RepoHelper newCurrentRepo = this.theModel.getAllRepoHelpers()
                    .get(newIndex);

            loadDesignatedRepo(newCurrentRepo);
            dropdownController.setCurrentRepo(newCurrentRepo);

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
            commitInfoNameText.set(commit.getName());

            commitInfoController.setCommitInfoMessageText(theModel.getCurrentRepoHelper().getCommitDescriptorString(commit, true));

            tagNameField.setVisible(true);
            tagButton.setVisible(true);
            infoTagBox.toFront();
        });
    }

    /**
     * Stops displaying commit information
     */
    public void clearSelectedCommit(){
        Platform.runLater(() -> {
            commitInfoController.clearCommit();
            commitTreePanelView.toFront();

            tagNameField.setText("");
            tagNameField.setVisible(false);
            tagButton.setVisible(false);
            pushTagsButton.setVisible(false);
            infoTagBox.toBack();
        });
    }


    /// ******************************************************************************
    /// ********                 BEGIN: ERROR NOTIFICATIONS:                  ********
    /// ******************************************************************************

    void showGenericErrorNotification() {
        Platform.runLater(()-> {
            System.out.println("I am dumping");
            Thread.dumpStack();
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

    public void showNoRepoLoadedNotification() {
        Platform.runLater(() -> {
            logger.warn("No repo loaded warning.");
            notificationPaneController.addNotification("You need to load a repository before you can perform operations on it. Click on the plus sign in the upper left corner!");
        });
    }

    private void showMissingRepoNotification(){
        Platform.runLater(()-> {
            logger.warn("Missing repo warning");
            notificationPaneController.addNotification("That repository no longer exists.");
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

    private void showPushToAheadRemoteNotification(boolean allRefsRejected){
        Platform.runLater(() -> {
            logger.warn("Remote ahead of local warning");
            if(allRefsRejected) this.notificationPaneController.addNotification("The remote repository is ahead of the local. You need to fetch and then merge (pull) before pushing.");
            else this.notificationPaneController.addNotification("You need to fetch/merge in order to push all of your changes.");
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

            EventHandler<MouseEvent> handler = event -> quickStashSave();
            this.notificationPaneController.addNotification("You can't apply that stash because there would be conflicts. " +
                    "Stash your changes or resolve conflicts first.", "stash", handler);
        });
    }

    private void showCheckoutConflictsNotification(List<String> conflictingPaths) {
        Platform.runLater(() -> {
            logger.warn("Checkout conflicts warning");

            EventHandler<MouseEvent> handler = event -> quickStashSave();
            this.notificationPaneController.addNotification("You can't switch to that branch because there would be a merge conflict. " +
                    "Stash your changes or resolve conflicts first.", "stash", handler);
        });
    }

    private void showRefAlreadyExistsNotification() {
        logger.info("Branch already exists notification");
        notificationPaneController.addNotification("Looks like that branch already exists locally!");
    }

    private void showNoFilesToStashNotification() {
        Platform.runLater(() -> {
            logger.warn("No files to stash warning");
            notificationPaneController.addNotification("There are no files to stash.");
        });
    }

    // END: ERROR NOTIFICATIONS ^^^

    /**
     * Initialization method that loads the level of logging from preferences
     * This will show a popup window if there is no preference
     * @throws BackingStoreException
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public void loadLogging() {
        Main.assertFxThread();
        Level storedLevel = LoggingModel.getLoggingLevel();
        if (storedLevel == null) {
            storedLevel = PopUpWindows.getLoggingPermissions() ? Level.INFO : Level.OFF;
        }
        LoggingModel.changeLogging(storedLevel);
        menuController.initializeLoggingToggle();
        logger.info("Starting up.");
    }

    public static BooleanProperty anythingCheckedProperty() {
        return anythingChecked;
    }

    public synchronized void setTryCommandAgainWithHTTPAuth(boolean value) {
        tryCommandAgainWithHTTPAuth = value;
    }

    public synchronized CommitTreeModel getCommitTreeModel() {
        return commitTreeModel;
    }
}
