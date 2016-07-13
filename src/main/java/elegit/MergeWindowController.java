package elegit;

import elegit.exceptions.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.controlsfx.control.NotificationPane;
import org.controlsfx.control.action.Action;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.errors.*;
import org.eclipse.jgit.errors.NoMergeBaseException;
import org.eclipse.jgit.lib.BranchTrackingStatus;
import org.eclipse.jgit.lib.Repository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by connellyj on 7/11/16.
 *
 * Controller for the merge window
 */
public class MergeWindowController {

    @FXML private Text remoteTrackingBranchName;
    @FXML private Text localBranchName1;
    @FXML private Text localBranchName2;
    @FXML private Text intoLocalBranchText1;
    @FXML private Text intoLocalBranchText2;
    @FXML private CheckBox mergeRemoteTrackingCheckBox;
    @FXML private CheckBox mergeDifLocalBranchCheckBox;
    @FXML private NotificationPane notificationPane;
    @FXML private ComboBox<LocalBranchHelper> branchDropdownSelector = new ComboBox<>();
    @FXML private Button mergeButton;
    @FXML private Text mergeRemoteTrackingText;
    @FXML private Hyperlink trackLink;

    Stage stage;
    SessionModel sessionModel;
    RepoHelper repoHelper;
    BranchModel branchModel;
    boolean disable;
    LocalCommitTreeModel localCommitTreeModel;

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
        branchDropdownSelector.setPromptText("local branches...");

        //init commit tree models
        ArrayList<?> models = CommitTreeController.getCommitTreeModels();
        localCommitTreeModel = (LocalCommitTreeModel) models.get(0);

        initText();
        initCheckBoxes();
    }

    /**
     * helper method to initialize some text
     * @throws IOException
     */
    public void initText() throws IOException {
        String curBranch = repoHelper.getBranchModel().getCurrentBranch().getBranchName();
        BranchTrackingStatus b = BranchTrackingStatus.of(repoHelper.getRepo(), curBranch);
        if(b == null) {
            disable = true;
            mergeRemoteTrackingText.setText("This branch does not have an upstream remote branch.");
            localBranchName1.setText("");
            remoteTrackingBranchName.setText("");
            intoLocalBranchText1.setText("\t\tPush to create a remote branch");

        } else {
            disable = false;
            String curRemoteTrackingBranch = b.getRemoteTrackingBranch();
            curRemoteTrackingBranch = Repository.shortenRefName(curRemoteTrackingBranch);
            localBranchName1.setText(curBranch);
            remoteTrackingBranchName.setText(curRemoteTrackingBranch);
            intoLocalBranchText1.setText("\t\tinto local branch ");
        }
        localBranchName2.setText(curBranch);
        intoLocalBranchText2.setText("\t\tinto local branch ");

        localBranchName1.setFill(Color.DODGERBLUE);
        localBranchName2.setFill(Color.DODGERBLUE);
        remoteTrackingBranchName.setFill(Color.DODGERBLUE);

        trackLink.setFont(new Font(10));
    }

    /**
     * helper method to initialize the checkboxes
     */
    public void initCheckBoxes() {
        mergeDifLocalBranchCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if(newValue && mergeRemoteTrackingCheckBox.selectedProperty().get()) {
                mergeRemoteTrackingCheckBox.selectedProperty().setValue(false);
            }
        });
        mergeRemoteTrackingCheckBox.disableProperty().setValue(disable);
        mergeRemoteTrackingCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if(newValue && mergeDifLocalBranchCheckBox.selectedProperty().get()) {
                mergeDifLocalBranchCheckBox.selectedProperty().setValue(false);
            }
        });
        mergeButton.disableProperty().bind(mergeDifLocalBranchCheckBox.selectedProperty().or(mergeRemoteTrackingCheckBox.selectedProperty()).not());
    }

    /**
     * shows the window
     * @param pane NotificationPane root
     */
    public void showStage(NotificationPane pane) {
        notificationPane = pane;
        stage = new Stage();
        stage.setTitle("Merge");
        stage.setScene(new Scene(notificationPane, 450, 250));
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setOnCloseRequest(event -> logger.info("Closed merge window"));
        stage.show();
    }

    /**
     * closes the window
     */
    public void closeWindow() {
        stage.close();
    }

    /**
     * handles the merge button
     */
    public void handleMergeButton() throws GitAPIException, IOException {
        if(mergeDifLocalBranchCheckBox.isSelected()) {
            localBranchMerge();
        }
        if(mergeRemoteTrackingCheckBox.isSelected()) {
            mergeFromFetch();
        }
    }

    /**
     * merges the remote-tracking branch associated with the current branch into the current local branch
     */
    private void mergeFromFetch() {
        try{
            logger.info("Merge from fetch button clicked");
            if(sessionModel.getCurrentRepoHelper() == null) throw new NoRepoLoadedException();
            if(!sessionModel.getCurrentRepoHelper().hasUnmergedCommits()) throw new NoCommitsToMergeException();

            BusyWindow.show();
            BusyWindow.setLoadingText("Merging...");
            Thread th = new Thread(new Task<Void>(){
                @Override
                protected Void call() throws GitAPIException, IOException {
                    try{
                        if(!sessionModel.getCurrentRepoHelper().mergeFromFetch().isSuccessful()){
                            showUnsuccessfulMergeNotification();
                        }
                        Main.sessionController.gitStatus();
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
                        ConflictingFileWatcher.watchConflictingFiles(sessionModel.getCurrentRepoHelper());
                    } catch(MissingRepoException e){
                        showMissingRepoNotification();
                        Main.sessionController.setButtonsDisabled(true);
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
            Main.sessionController.setButtonsDisabled(true);
        }catch(NoCommitsToMergeException e){
            this.showNoCommitsToMergeNotification();
        }
    }

    /**
     * merges the selected local branch with the current local branch
     * @throws GitAPIException
     * @throws IOException
     */
    private void localBranchMerge() throws GitAPIException, IOException {
        logger.info("Merging selected branch with current");
        // Get the branch to merge with
        LocalBranchHelper selectedBranch = this.branchDropdownSelector.getSelectionModel().getSelectedItem();

        // Get the merge result from the branch merge
        MergeResult mergeResult= this.branchModel.mergeWithBranch(selectedBranch);

        if (mergeResult.getMergeStatus().equals(MergeResult.MergeStatus.CONFLICTING)){
            this.showConflictsNotification();
            Main.sessionController.gitStatus();
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

    /******* START ERROR NOTIFICATIONS *******/

    private void showFastForwardMergeNotification() {
        logger.info("Fast forward merge complete notification");
        notificationPane.setText("Fast-forward merge completed (HEAD was updated).");

        notificationPane.getActions().clear();
        notificationPane.show();
    }

    private void showMergeSuccessNotification() {
        logger.info("Merge completed notification");
        notificationPane.setText("Merge completed.");

        notificationPane.getActions().clear();
        notificationPane.show();
    }

    private void showFailedMergeNotification() {
        logger.warn("Merge failed notification");
        notificationPane.setText("The merge failed.");

        notificationPane.getActions().clear();
        notificationPane.show();
    }

    private void showUpToDateNotification() {
        logger.warn("No merge necessary notification");
        notificationPane.setText("No merge necessary. Those two branches are already up-to-date.");

        notificationPane.getActions().clear();
        notificationPane.show();
    }

    private void showConflictsNotification() {
        logger.info("Merge conflicts notification");
        notificationPane.setText("That merge resulted in conflicts. Check the working tree to resolve them.");

        notificationPane.getActions().clear();
        notificationPane.show();
    }

    private void showUnsuccessfulMergeNotification(){
        Platform.runLater(() -> {
            logger.warn("Failed merged warning");
            this.notificationPane.setText("Merging failed");

            this.notificationPane.getActions().clear();
            this.notificationPane.show();
        });
    }

    private void showNoRepoLoadedNotification() {
        Platform.runLater(() -> {
            logger.warn("No repo loaded");
            this.notificationPane.setText("You need to load a repository before you can perform operations on it. Click on the plus sign in the upper left corner!");

            this.notificationPane.getActions().clear();
            this.notificationPane.show();
        });
    }

    private void showNoRemoteNotification(){
        Platform.runLater(()-> {
            logger.warn("No remote repo warning");
            String name = sessionModel.getCurrentRepoHelper() != null ? sessionModel.getCurrentRepoHelper().toString() : "the current repository";

            this.notificationPane.setText("There is no remote repository associated with " + name);

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

    private void showNotAuthorizedNotification(Runnable callback) {
        Platform.runLater(() -> {
            logger.warn("Invalid authorization");
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

    private void showMergingWithChangedFilesNotification(){
        Platform.runLater(() -> {
            logger.warn("Can't merge with modified files warning");
            this.notificationPane.setText("Can't merge with modified files present");

            // TODO: I think some sort of help text would be nice here, so they know what to do

            this.notificationPane.getActions().clear();
            this.notificationPane.show();
        });
    }

    private void showMergeConflictsNotification(List<String> conflictingPaths){
        Platform.runLater(() -> {
            this.notificationPane.setText("Can't complete merge due to conflicts. Resolve the conflicts and commit all files to complete merging");

            Action seeConflictsAction = new Action("See conflicting files", e -> {
                this.notificationPane.hide();
                PopUpWindows.showMergeConflictsAlert(conflictingPaths);
            });

            this.notificationPane.getActions().clear();
            this.notificationPane.getActions().setAll(seeConflictsAction);

            this.notificationPane.show();
        });
    }

    private void showMissingRepoNotification(){
        Platform.runLater(()-> {
            logger.warn("Missing repo");
            this.notificationPane.setText("That repository no longer exists.");

            this.notificationPane.getActions().clear();
            this.notificationPane.show();
        });
    }

    private void showGenericErrorNotification() {
        Platform.runLater(()-> {
            logger.warn("Generic error.");
            this.notificationPane.setText("Sorry, there was an error.");

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

    private void showRefAlreadyExistsNotification() {
        logger.info("Branch already exists notification");
        notificationPane.setText("Looks like that branch already exists locally!");

        notificationPane.getActions().clear();
        notificationPane.show();
    }

    /******* END ERROR NOTIFICATIONS *******/
}
