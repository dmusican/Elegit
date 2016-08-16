package elegit;

import elegit.exceptions.*;
import elegit.treefx.CellLabel;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.errors.*;
import org.eclipse.jgit.errors.NoMergeBaseException;
import org.eclipse.jgit.lib.BranchTrackingStatus;
import org.eclipse.jgit.lib.Repository;

import java.io.IOException;
import java.util.List;

/**
 * Controller for the merge window
 */
public class MergeWindowController {

    @FXML private Label remoteTrackingBranchName;
    @FXML private Label localBranchName1;
    @FXML private Label localBranchName2;
    @FXML private AnchorPane anchorRoot;
    @FXML private ComboBox<LocalBranchHelper> branchDropdownSelector;
    @FXML private Button mergeButton;
    @FXML private Text mergeRemoteTrackingText;
    @FXML private StackPane notificationPane;
    @FXML private NotificationController notificationPaneController;

    @FXML private HBox remoteBranchBox;
    @FXML private Text intoText1;
    @FXML private AnchorPane arrowPane;
    @FXML private HBox localBranchBox1;
    @FXML private TabPane mergeTypePane;

    private static final int REMOTE_PANE=0;
    private static final int LOCAL_PANE=1;

    private Stage stage;
    SessionModel sessionModel;
    RepoHelper repoHelper;
    private BranchModel branchModel;
    private boolean disable;
    private CommitTreeModel localCommitTreeModel;

    private SessionController sessionController;

    static final Logger logger = LogManager.getLogger();

    /**
     * initializes the window
     * called when the fxml is loaded
     */
    public void initialize() throws IOException {
        //get session model and repo helper and branch model
        sessionModel = SessionModel.getSessionModel();
        repoHelper = sessionModel.getCurrentRepoHelper();
        branchModel = repoHelper.getBranchModel();

        //init branch dropdown selector
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
                            if(helper.getBranchName().length() > CellLabel.MAX_CHAR_PER_LABEL){
                                branchName.setTooltip(new Tooltip(helper.getBranchName()));
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

        //init commit tree models
        localCommitTreeModel = CommitTreeController.getCommitTreeModel();

        initText();
        initMergeButton();
    }

    /**
     * helper method to initialize some text
     * @throws IOException if there is an error getting branch names
     */
    private void initText() throws IOException {
        String curBranch = repoHelper.getBranchModel().getCurrentBranch().getBranchName();
        BranchTrackingStatus b = BranchTrackingStatus.of(repoHelper.getRepo(), curBranch);
        if(b == null) {
            disable = true;
            mergeRemoteTrackingText.setText("This branch does not have an\n" +
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
    private void initMergeButton() {
        if (disable)
            mergeButton.disableProperty().bind(mergeTypePane.getSelectionModel().selectedIndexProperty().lessThan(1));
    }

    /**
     * Helper method to hide the various items in the remote tracking pane
     * if there is no remote-tracking branch for the current branch
     */
    private void hideRemoteMerge() {
        remoteBranchBox.setVisible(false);
        intoText1.setVisible(false);
        arrowPane.setVisible(false);
        localBranchBox1.setVisible(false);
    }

    /**
     * shows the window
     * @param pane AnchorPane root
     */
    void showStage(AnchorPane pane) {
        anchorRoot = pane;
        stage = new Stage();
        stage.setTitle("Merge");
        stage.setScene(new Scene(anchorRoot));
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setOnCloseRequest(event -> logger.info("Closed merge window"));
        stage.show();
    }

    /**
     * closes the window
     */
    public void closeWindow() {
        if (Platform.isFxApplicationThread()) stage.close();
        else {
            Platform.runLater(() -> stage.close());
        }
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
                closeWindow();
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
    private void mergeFromFetch() {
        try{
            logger.info("Merge from fetch button clicked");
            if(sessionModel.getCurrentRepoHelper() == null) throw new NoRepoLoadedException();
            if(sessionModel.getCurrentRepoHelper().getBehindCount()<1) throw new NoCommitsToMergeException();

            BusyWindow.show();
            BusyWindow.setLoadingText("Merging...");
            Thread th = new Thread(new Task<Void>(){
                @Override
                protected Void call() throws GitAPIException, IOException {
                    try{
                        if(!sessionModel.getCurrentRepoHelper().mergeFromFetch().isSuccessful()){
                            showUnsuccessfulMergeNotification();
                        } else {
                            closeWindow();
                        }
                        sessionController.gitStatus();
                    } catch(InvalidRemoteException e){
                        showNoRemoteNotification();
                    } catch(TransportException e){
                        showNotAuthorizedNotification(null);
                    } catch (NoMergeBaseException | JGitInternalException e) {
                        // Merge conflict
                        e.printStackTrace();
                        // todo: figure out rare NoMergeBaseException.
                        //  Has something to do with pushing conflicts.
                        //  At this point in the stack, it's caught as a JGitInternalException.
                    } catch(CheckoutConflictException e){
                        showMergingWithChangedFilesNotification();
                    } catch(ConflictingFilesException e){
                        showMergeConflictsNotification(e.getConflictingFiles());
                        Platform.runLater(() -> PopUpWindows.showMergeConflictsAlert(e.getConflictingFiles()));
                        ConflictingFileWatcher.watchConflictingFiles(sessionModel.getCurrentRepoHelper());
                    } catch(MissingRepoException e){
                        showMissingRepoNotification();
                        sessionController.setButtonsDisabled(true);
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
            this.sessionController.setButtonsDisabled(true);
        }catch(NoCommitsToMergeException e){
            this.showNoCommitsToMergeNotification();
        }catch(IOException e) {
            this.showGenericErrorNotification();
        }
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
        MergeResult mergeResult= this.branchModel.mergeWithBranch(selectedBranch);

        if (mergeResult.getMergeStatus().equals(MergeResult.MergeStatus.CONFLICTING)){
            this.showConflictsNotification();
            this.sessionController.gitStatus();
            ConflictingFileWatcher.watchConflictingFiles(sessionModel.getCurrentRepoHelper());

        } else if (mergeResult.getMergeStatus().equals(MergeResult.MergeStatus.ALREADY_UP_TO_DATE)) {
            this.showUpToDateNotification();

        } else if (mergeResult.getMergeStatus().equals(MergeResult.MergeStatus.FAILED)) {
            this.showFailedMergeNotification();

        } else if (mergeResult.getMergeStatus().equals(MergeResult.MergeStatus.MERGED)
                || mergeResult.getMergeStatus().equals(MergeResult.MergeStatus.MERGED_NOT_COMMITTED)) {
            this.showMergeSuccessNotification();

        } else if (mergeResult.getMergeStatus().equals(MergeResult.MergeStatus.FAST_FORWARD)) {
            this.showFastForwardMergeNotification();

        } else {
            System.out.println(mergeResult.getMergeStatus());
            // todo: handle all cases (maybe combine some)
        }
        // Tell the rest of the UI to update
        sessionController.gitStatus();
    }

    public void handleTrackDifBranch() {
        RemoteBranchHelper toTrack = PopUpWindows.showTrackDifRemoteBranchDialog(FXCollections.observableArrayList(branchModel.getRemoteBranchesTyped()));
        logger.info("Track remote branch locally (in merge window) button clicked");
        try {
            if (toTrack != null) {
                LocalBranchHelper tracker = this.branchModel.trackRemoteBranch(toTrack);
                this.branchDropdownSelector.getItems().add(tracker);
                CommitTreeController.setBranchHeads(localCommitTreeModel, repoHelper);
            }
        } catch (RefAlreadyExistsException e) {
            logger.warn("Branch already exists locally warning");
            this.showRefAlreadyExistsNotification();
        } catch (GitAPIException | IOException e) {
            showGenericErrorNotification();
            e.printStackTrace();
        }
    }

    /**
     * Setter method for sessionController, needed for merge operations
     * @param sessionController the sessionController that made this window
     */
    void setSessionController(SessionController sessionController) {
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

    private void showUpToDateNotification() {
        logger.warn("No merge necessary notification");
        notificationPaneController.addNotification("No merge necessary. Those two branches are already up-to-date.");
    }

    private void showConflictsNotification() {
        logger.info("Merge conflicts notification");
        notificationPaneController.addNotification("That merge resulted in conflicts. Check the working tree to resolve them.");
    }

    private void showUnsuccessfulMergeNotification(){
        Platform.runLater(() -> {
            logger.warn("Failed merged warning");
            notificationPaneController.addNotification("Merging failed");
        });
    }

    private void showNoRepoLoadedNotification() {
        Platform.runLater(() -> {
            logger.warn("No repo loaded");
            notificationPaneController.addNotification("You need to load a repository before you can perform operations on it. Click on the plus sign in the upper left corner!");
        });
    }

    private void showNoRemoteNotification(){
        Platform.runLater(()-> {
            logger.warn("No remote repo warning");
            String name = sessionModel.getCurrentRepoHelper() != null ? sessionModel.getCurrentRepoHelper().toString() : "the current repository";
            notificationPaneController.addNotification("There is no remote repository associated with " + name);
        });
    }

    private void showNoCommitsToMergeNotification(){
        Platform.runLater(() -> {
            logger.warn("No commits to merge warning");
            notificationPaneController.addNotification("There aren't any commits to merge. Try fetching first");
        });
    }

    private void showNotAuthorizedNotification(Runnable callback) {
        Platform.runLater(() -> {
            logger.warn("Invalid authorization");
            notificationPaneController.addNotification("The authorization information you gave does not allow you to modify this repository. " +
                    "Try reentering your password.");
        });
    }

    private void showMergingWithChangedFilesNotification(){
        Platform.runLater(() -> {
            logger.warn("Can't merge with modified files warning");
            notificationPaneController.addNotification("Can't merge with modified files present, please add/commit before merging.");
        });
    }

    private void showMergeConflictsNotification(List<String> conflictingPaths){
        Platform.runLater(() -> {
            logger.warn("Merge conflict warning");
            notificationPaneController.addNotification("Can't complete merge due to conflicts. Resolve the conflicts and commit all files to complete merging");
        });
    }

    private void showMissingRepoNotification(){
        Platform.runLater(()-> {
            logger.warn("Missing repo notification");
            notificationPaneController.addNotification("That repository no longer exists.");
        });
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

    private void showNoRemoteTrackingNotification() {
        Platform.runLater(() -> {
            logger.warn("No remote tracking for current branch notification.");
            notificationPaneController.addNotification("There is no remote tracking information for the current branch.");
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
