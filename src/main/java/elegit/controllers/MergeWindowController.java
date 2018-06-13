package elegit.controllers;

import elegit.*;
import elegit.models.BranchModel;
import elegit.models.LocalBranchHelper;
import elegit.models.RepoHelper;
import elegit.models.SessionModel;
import elegit.monitors.ConflictingFileWatcher;
import elegit.treefx.CellLabel;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;
import net.jcip.annotations.GuardedBy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.*;
import org.eclipse.jgit.lib.BranchTrackingStatus;
import org.eclipse.jgit.lib.Repository;

import java.io.IOException;

/**
 * Controller for the merge window
 */
// TODO: Make sure that every git operation happens sequentially, i.e., no two happen in different threads at the same time.
public class MergeWindowController {

    @FXML private Label remoteTrackingBranchName;
    @FXML private Label localBranchName1;
    @FXML private Label localBranchName2;
    @FXML private AnchorPane anchorRoot;
    @FXML private Button mergeButton;
    @FXML private Text mergeRemoteTrackingText;
    @FXML private HBox remoteBranchBox;
    @FXML private AnchorPane arrowPane;
    @FXML private HBox localBranchBox1;
    @FXML private TabPane mergeTypePane;
    @FXML private Tab localBranchTab;
    @FXML private ComboBox<LocalBranchHelper> branchDropdownSelector;
    @FXML private NotificationController notificationPaneController;

    private static final int REMOTE_PANE=0;
    private static final int LOCAL_PANE=1;

    private Stage stage;

    @GuardedBy("this") private boolean disable;
    @GuardedBy("this") private SessionController sessionController;


    private static final Logger logger = LogManager.getLogger();

    /**
     * initializes the window
     * called when the fxml is loaded
     */
    // TODO: Make sure RepoHelper and BranchModel are threadsafe
    public void initialize() throws IOException {
        //get session model and repo helper and branch model
        RepoHelper repoHelper = SessionModel.getSessionModel().getCurrentRepoHelper();
        BranchModel branchModel = repoHelper.getBranchModel();

        //init branch dropdown selector
        // TODO: Make sure objects for branches are immutable
        branchDropdownSelector.setItems(FXCollections.observableArrayList(branchModel.getLocalBranchesTyped()));
        branchDropdownSelector.setPromptText("...");

        branchDropdownSelector.setCellFactory(new Callback<ListView<LocalBranchHelper>, ListCell<LocalBranchHelper>>() {
            @Override
            public ListCell<LocalBranchHelper> call(ListView<LocalBranchHelper> param) {
                return new ListCell<LocalBranchHelper>() {

                    private final Label branchName; {
                        branchName = new Label();
                        branchName.setStyle("-fx-text-fill: #333333;"+
                                "-fx-font-size: 14px;"+
                                "-fx-font-weight: bold;"+
                                "-fx-background-color: #CCCCCC;"+
                                "-fx-background-radius: 5;"+
                                "-fx-padding: 0 3 0 3");
                    }

                    @Override protected void updateItem(LocalBranchHelper helper, boolean empty) {
                        super.updateItem(helper, empty);

                        if (helper == null || empty) { setGraphic(null); }
                        else {
                            if(helper.getRefName().length() > CellLabel.MAX_CHAR_PER_LABEL){
                                branchName.setTooltip(new Tooltip(helper.getRefName()));
                            }
                            branchName.setText(helper.getAbbrevName());
                            setGraphic(branchName);
                            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                        }
                    }
                };
            }
        });

        this.notificationPaneController.bindParentBounds(anchorRoot.heightProperty());

        initText();
        initMergeButton();
    }

    /**
     * helper method to initialize some text
     * @throws IOException if there is an error getting branch names
     */
    private synchronized void initText() throws IOException {
        RepoHelper repoHelper = SessionModel.getSessionModel().getCurrentRepoHelper();
        String curBranch = repoHelper.getBranchModel().getCurrentBranch().getRefName();
        BranchTrackingStatus b = BranchTrackingStatus.of(repoHelper.getRepo(), curBranch);
        if(b == null) {
            disable = true;
            mergeRemoteTrackingText.setText("\nThis branch does not have an\n" +
                    "upstream remote branch.\n\n" +
                    "Push to create a remote branch.");
            hideRemoteMerge();

        } else {
            disable = false;
            String curRemoteTrackingBranch = b.getRemoteTrackingBranch();
            curRemoteTrackingBranch = Repository.shortenRefName(curRemoteTrackingBranch);
            localBranchName1.setText(curBranch);
            remoteTrackingBranchName.setText(curRemoteTrackingBranch);
        }
        localBranchName2.setText(curBranch);
    }

    /**
     * Helper method that decides when the merge button will be enabled.
     * If there is a remote tracking branch, always and if not, then only
     * when on the merge local branches tab.
     */
    private synchronized void initMergeButton() {
        if (disable)
            mergeButton.disableProperty().bind(mergeTypePane.getSelectionModel().selectedIndexProperty().lessThan(1));
    }

    /**
     * Helper method to hide the various items in the remote tracking pane
     * if there is no remote-tracking branch for the current branch
     */
    private void hideRemoteMerge() {
        remoteBranchBox.setVisible(false);
        arrowPane.setVisible(false);
        localBranchBox1.setVisible(false);
    }

    /**
     * shows the window
     * @param pane AnchorPane root
     */
    void showStage(AnchorPane pane, boolean localTabOpen) {
        anchorRoot = pane;
        stage = new Stage();
        stage.setTitle("Merge");
        stage.setScene(new Scene(anchorRoot));
        stage.setResizable(false);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setOnCloseRequest(event -> logger.info("Closed merge window"));
        stage.show();
        if(localTabOpen) mergeTypePane.getSelectionModel().select(localBranchTab);
        this.notificationPaneController.setAnchor(stage);
    }

    /**
     * closes the window
     */
    public void closeWindow() {
        stage.close();
    }

    /**
     * Handler for merge button. Will merge selected local branch into the current
     * branch if in the local tab, otherwise it will merge from fetch.
     */
    public void handleMergeButton() throws GitAPIException, IOException {
        try {
            if (mergeTypePane.getSelectionModel().isSelected(LOCAL_PANE)) {
                if (!branchDropdownSelector.getSelectionModel().isEmpty()) localBranchMerge();
                else showSelectBranchNotification();
            }
            if (mergeTypePane.getSelectionModel().isSelected(REMOTE_PANE)) {
                mergeFromFetch();
            }
        } catch (JGitInternalException e) {
            showJGitInternalError(e);
        } catch (GitAPIException | IOException e) {
            showGenericErrorNotification();
        }
    }

    /**
     * merges the remote-tracking branch associated with the current branch into the current local branch
     */
    private synchronized void mergeFromFetch() {
        Main.assertFxThread();
        // Do the merge, and close the window if successful
        sessionController.mergeFromFetchCreateChain(notificationPaneController)
                .subscribe(results -> {
                    boolean success = true;
                    for (SessionController.Result result : results) {
                        if (result.status == SessionController.ResultStatus.MERGE_FAILED ||
                                result.status == SessionController.ResultStatus.EXCEPTION)
                            success = false;
                    }
                    if (success) {
                        stage.close();
                    }
                }, Throwable::printStackTrace);

    }

    /**
     * merges the selected local branch with the current local branch
     * @throws GitAPIException if there is a merging error
     * @throws IOException if there is an error with the file access of merge
     */
    private void localBranchMerge() throws GitAPIException, IOException {
        logger.info("Merging selected branch with current");
        // Get the branch to merge with
        LocalBranchHelper selectedBranch = this.branchDropdownSelector.getSelectionModel().getSelectedItem();

        // Get the merge result from the branch merge
        MergeResult mergeResult =
                SessionModel.getSessionModel().getCurrentRepoHelper().getBranchModel().mergeWithBranch(selectedBranch);
        sessionController.updateCommandText("git merge "+selectedBranch);

        if (mergeResult.getMergeStatus().equals(MergeResult.MergeStatus.CONFLICTING)){
            this.showConflictsNotification();
            // TODO: Call gitStatus once I've got it better threaded
            //this.sessionController.gitStatus();
            ConflictingFileWatcher.watchConflictingFiles(SessionModel.getSessionModel().getCurrentRepoHelper());

        } else if (mergeResult.getMergeStatus().equals(MergeResult.MergeStatus.ALREADY_UP_TO_DATE)) {
            this.showUpToDateNotification();

        } else if (mergeResult.getMergeStatus().equals(MergeResult.MergeStatus.FAILED)) {
            if (!SessionModel.getSessionModel().modifiedAndStagedFilesAreSame()) {
                this.showChangedFilesNotification();
            } else {
                this.showFailedMergeNotification();
            }
        } else if (mergeResult.getMergeStatus().equals(MergeResult.MergeStatus.MERGED)
                || mergeResult.getMergeStatus().equals(MergeResult.MergeStatus.MERGED_NOT_COMMITTED)) {
            // TODO: Call gitStatus once I've got it better threaded
            sessionController.gitStatus();
            closeWindow();
            return;

        } else if (mergeResult.getMergeStatus().equals(MergeResult.MergeStatus.FAST_FORWARD)) {
            this.showFastForwardMergeNotification();

        } else {
            System.out.println(mergeResult.getMergeStatus());
            // todo: handle all cases (maybe combine some)
        }
        // Tell the rest of the UI to update
        // TODO: Put gitStatus back in once I've threaded it better
        //sessionController.gitStatus();
    }

    /**
     * Setter method for sessionController, needed for merge operations
     * @param sessionController the sessionController that made this window
     */
    synchronized void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    ///******* START ERROR NOTIFICATIONS *******/

    private void showFastForwardMergeNotification() {
        logger.info("Fast forward merge complete notification");
        notificationPaneController.addNotification("Fast-forward merge completed.");
    }

    private void showMergeSuccessNotification() {
        logger.info("Merge completed notification");
        notificationPaneController.addNotification("Merge completed.");
    }

    private void showFailedMergeNotification() {
        logger.warn("Merge failed notification");
        notificationPaneController.addNotification("The merge failed.");
    }

    private void showChangedFilesNotification() {
        logger.warn("Merge failed because of changed files notification");
        notificationPaneController.addNotification("Merge failed. Commit or reset any changed files.");
    }

    private void showUpToDateNotification() {
        logger.warn("No merge necessary notification");
        notificationPaneController.addNotification("No merge necessary. Those two branches are already up-to-date.");
    }

    private void showConflictsNotification() {
        logger.info("Merge conflicts notification");
        notificationPaneController.addNotification("That merge resulted in conflicts. Check the working tree to resolve them.");
    }

    private void showGenericErrorNotification() {
        Platform.runLater(()-> {
            logger.warn("Generic error.");
            notificationPaneController.addNotification("Sorry, there was an error.");
        });
    }

    private void showJGitInternalError(JGitInternalException e) {
        Platform.runLater(()-> {
            if (e.getCause().toString().contains("LockFailedException")) {
                logger.warn("Lock failed warning.");
                notificationPaneController.addNotification("Cannot lock .git/index. If no other git processes are running, manually remove all .lock files.");
            } else {
                logger.warn("Generic jgit internal warning.");
                notificationPaneController.addNotification("Sorry, there was a Git error.");
            }
        });
    }

    private void showRefAlreadyExistsNotification() {
        logger.info("Branch already exists notification");
        notificationPaneController.addNotification("That branch already exists locally.");
    }

    private void showSelectBranchNotification() {
        logger.info("Select a branch first notification");
        notificationPaneController.addNotification("You need to select a branch first");
    }

    ///******* END ERROR NOTIFICATIONS *******/
}
