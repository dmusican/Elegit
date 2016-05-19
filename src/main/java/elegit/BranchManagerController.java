package main.java.elegit;

import de.jensd.fx.glyphs.GlyphsDude;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.controlsfx.control.NotificationPane;
import org.controlsfx.control.action.Action;
import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeCommand;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.errors.*;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;

import java.io.IOException;
import java.util.List;

/**
 *
 * A controller for the BranchManager view that holds all a repository's
 * branches (in the form of BranchHelpers) and manages branch creation,
 * deletion, and tracking of remotes.
 *
 */
public class BranchManagerController {

    public Text branchOffFromText;
    public TextField newBranchNameField;
    @FXML
    private Button newBranchButton;
    public ListView<RemoteBranchHelper> remoteListView;
    public ListView<LocalBranchHelper> localListView;
    private Repository repo;
    private RepoHelper repoHelper;
    @FXML
    private NotificationPane notificationPane;

    @FXML
    private Button mergeButton;
    @FXML
    private Button deleteLocalBranchesButton;
    @FXML
    private Button trackRemoteBranchButton;
    @FXML
    private Button swapMergeBranchesButton;

    private SessionModel sessionModel;
    private BranchManagerModel branchManagerModel;
    private LocalCommitTreeModel localCommitTreeModel;

    static final Logger logger = LogManager.getLogger();

    public void initialize() throws Exception {
        logger.info("Started up branch manager");
        this.sessionModel = SessionModel.getSessionModel();
        this.repoHelper = this.sessionModel.getCurrentRepoHelper();
        this.repo = this.repoHelper.getRepo();
        this.branchManagerModel = this.repoHelper.getBranchManagerModel();
        for (CommitTreeModel commitTreeModel : CommitTreeController.allCommitTreeModels) {
            if (commitTreeModel.getViewName().equals(LocalCommitTreeModel
                    .LOCAL_TREE_VIEW_NAME)) {
                this.localCommitTreeModel = (LocalCommitTreeModel)commitTreeModel;
            }
        }

        List<LocalBranchHelper> localBranches = this.branchManagerModel.getLocalBranches();
        List<RemoteBranchHelper> remoteBranches = this.branchManagerModel.getRemoteBranches();

        this.remoteListView.setItems(FXCollections.observableArrayList(remoteBranches));
        this.localListView.setItems(FXCollections.observableArrayList(localBranches));

        this.remoteListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        this.remoteListView.setOnMouseClicked(e -> {

            if (!localListView.getSelectionModel().isEmpty()) {
                localListView.getSelectionModel().clearSelection();
            }
            try {
                this.updateButtons();
            } catch (IOException e1) {
                logger.error("Branch manager remote list view mouse click error");
                logger.debug(e1.getStackTrace());
                e1.printStackTrace();
            }
        });

        // Local list view can select multiple (for merges):
        this.localListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        this.localListView.setOnMouseClicked(e -> {
            if (!remoteListView.getSelectionModel().isEmpty()) {
                remoteListView.getSelectionModel().clearSelection();
            }
            try {
                this.updateButtons();
            } catch (IOException e1) {
                logger.error("Branch manager local list view mouse click error");
                logger.debug(e1.getStackTrace());
                e1.printStackTrace();
            }
        });

        Text cloudDownIcon = GlyphsDude.createIcon(FontAwesomeIcon.CLOUD_DOWNLOAD);
        cloudDownIcon.setFill(Color.WHITE);
        this.trackRemoteBranchButton.setGraphic(cloudDownIcon);

        this.trackRemoteBranchButton.setDisable(true);

        Text trashIcon = GlyphsDude.createIcon(FontAwesomeIcon.TRASH);
        trashIcon.setFill(Color.WHITE);
        this.deleteLocalBranchesButton.setGraphic(trashIcon);
        this.deleteLocalBranchesButton.setDisable(true);

        Text arrowsIcon = GlyphsDude.createIcon(FontAwesomeIcon.EXCHANGE);
        arrowsIcon.setFill(Color.WHITE);
        this.swapMergeBranchesButton.setGraphic(arrowsIcon);
        this.swapMergeBranchesButton.setTooltip(new Tooltip("Swap which branch is merging into which."));

        Text branchIcon = GlyphsDude.createIcon(FontAwesomeIcon.CODE_FORK);
        branchIcon.setFill(Color.WHITE);
        this.newBranchButton.setGraphic(branchIcon);

        this.updateButtons();
    }

    public void onNewBranchButton() {
        try {
            logger.info("New branch button clicked");
            LocalBranchHelper newLocalBranch = this.createNewLocalBranch(this.newBranchNameField.getText());
            this.localListView.getItems().add(newLocalBranch);
            this.branchManagerModel.setLocalBranches(this.localListView.getItems());
            this.newBranchNameField.clear();
        } catch (InvalidRefNameException e1) {
            logger.warn("Invalid branch name warning");
            this.showInvalidBranchNameNotification();
        } catch (RefNotFoundException e1) {
            // When a repo has no commits, you can't create branches because there
            //  are no commits to point to. This error gets raised when git can't find
            //  HEAD.
            logger.warn("Can't create branch without a commit in the repo warning");
            this.showNoCommitsYetNotification();
        } catch (GitAPIException e1) {
            logger.warn("Git error");
            logger.debug(e1.getStackTrace());
            this.showGenericGitErrorNotification();
            e1.printStackTrace();
        } catch (IOException e1) {
            logger.warn("Unspecified IOException");
            logger.debug(e1.getStackTrace());
            this.showGenericErrorNotification();
            e1.printStackTrace();
        }
    }

    /**
     * Updates the track remote, merge, and delete local buttons'
     * text and/or disabled/enabled status.
     *
     * @throws IOException
     */
    private void updateButtons() throws IOException {
        String currentBranchName = this.repo.getBranch();
        this.branchOffFromText.setText(String.format("Branch off from %s:", currentBranchName));

        // Update delete button
        if (this.localListView.getSelectionModel().getSelectedIndices().size() > 0) {
            this.deleteLocalBranchesButton.setDisable(false);
            // But keep trackRemoteBranchButton disabled
            this.trackRemoteBranchButton.setDisable(true);
        }

        // Update track button
        if (this.remoteListView.getSelectionModel().getSelectedIndices().size() > 0) {
            this.trackRemoteBranchButton.setDisable(false);
            // But keep the other buttons disabled
            this.deleteLocalBranchesButton.setDisable(true);
            this.mergeButton.setDisable(true);
            this.swapMergeBranchesButton.setDisable(true);
        }

        // Update merge button
        if (this.localListView.getSelectionModel().getSelectedIndices().size() == 1) {
            this.mergeButton.setDisable(false);
            this.swapMergeBranchesButton.setDisable(false);
            String selectedBranchName = this.localListView.getSelectionModel().getSelectedItem().getBranchName();
            this.mergeButton.setText(String.format("Merge %s into %s", selectedBranchName, currentBranchName));
        } else {
            this.mergeButton.setText(String.format("Merge selected branch into %s", currentBranchName));
            this.mergeButton.setDisable(true);
            this.swapMergeBranchesButton.setDisable(true);
        }
    }

    /**
     * Creates a local branch tracking a remote branch.
     *
     * @param remoteBranchHelper the remote branch to be tracked.
     * @return the LocalBranchHelper of the local branch tracking the given remote branch.
     * @throws GitAPIException
     * @throws IOException
     */
    private LocalBranchHelper createLocalTrackingBranchForRemote(RemoteBranchHelper remoteBranchHelper) throws GitAPIException, IOException {
        String localBranchName=remoteBranchHelper.getBranchName().substring(7);
        Ref trackingBranchRef = new Git(this.repo).branchCreate().
                setName(localBranchName).
                setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK).
                setStartPoint(remoteBranchHelper.getRefPathString()).
                call();
        return new LocalBranchHelper(trackingBranchRef, this.repoHelper);
    }

    /**
     * Tracks the selected branch (in the remoteListView) locally.
     *
     * @throws GitAPIException
     * @throws IOException
     */
    public void trackSelectedBranchLocally() throws GitAPIException, IOException {
        logger.info("Track remote branch locally button clicked");
        RemoteBranchHelper selectedRemoteBranch = this.remoteListView.getSelectionModel().getSelectedItem();
        try {
            if (selectedRemoteBranch != null) {
                LocalBranchHelper tracker = this.createLocalTrackingBranchForRemote(selectedRemoteBranch);
                this.localListView.getItems().add(tracker);
                this.branchManagerModel.setLocalBranches(this.localListView.getItems());
            }
        } catch (RefAlreadyExistsException e) {
            logger.warn("Branch already exists locally warning");
            this.showRefAlreadyExistsNotification();
        }
    }

    /**
     * Deletes the selected branches (in the localListView) through git.
     */
    public void deleteSelectedLocalBranches() {
        logger.info("Delete branches button clicked");
        Git git = new Git(this.repo);

        for (LocalBranchHelper selectedBranch : this.localListView.getSelectionModel().getSelectedItems()) {
            try {
                if (selectedBranch != null) {
                    // Local delete:
                    git.branchDelete().setBranchNames(selectedBranch.getRefPathString()).call();
                    this.localListView.getItems().remove(selectedBranch);
                    this.branchManagerModel.setLocalBranches(this.localListView.getItems());
                }
            } catch (NotMergedException e) {
                logger.warn("Can't delete branch because not merged warning");
                this.showNotMergedNotification(selectedBranch);
            } catch (CannotDeleteCurrentBranchException e) {
                logger.warn("Can't delete current branch warning");
                this.showCannotDeleteBranchNotification(selectedBranch);
            } catch (GitAPIException e) {
                logger.warn("Git error");
                this.showGenericGitErrorNotificationWithBranch(selectedBranch);
            }
        }
        git.close();
        // TODO: add optional delete from remote, too.
        // see http://stackoverflow.com/questions/11892766/how-to-remove-remote-branch-with-jgit
    }

    /**
     * Creates a new local branch using git.
     *
     * @param branchName the name of the new branch.
     * @return the new local branch's LocalBranchHelper.
     * @throws GitAPIException
     * @throws IOException
     */
    private LocalBranchHelper createNewLocalBranch(String branchName) throws GitAPIException, IOException {
        Git git = new Git(this.repo);
        Ref newBranch = git.branchCreate().setName(branchName).call();
        LocalBranchHelper newLocalBranchHelper = new LocalBranchHelper(newBranch, this.repoHelper);

        git.close();
        return newLocalBranchHelper;
    }

    /**
     * Deletes a given local branch through git, forcefully.
     */
    private void forceDeleteLocalBranch(LocalBranchHelper branchToDelete) {
        logger.info("Deleting local branch");
        Git git = new Git(this.repo);

        try {
            if (branchToDelete != null) {
                // Local delete:
                git.branchDelete().setForce(true).setBranchNames(branchToDelete.getRefPathString()).call();
                this.localListView.getItems().remove(branchToDelete);
                this.branchManagerModel.setLocalBranches(this.localListView.getItems());
            }
        } catch (CannotDeleteCurrentBranchException e) {
            logger.warn("Can't delete current branch warning");
            this.showCannotDeleteBranchNotification(branchToDelete);
        } catch (GitAPIException e) {
            logger.warn("Git error");
            this.showGenericGitErrorNotificationWithBranch(branchToDelete);
            e.printStackTrace();
        }
        git.close();
    }

    /**
     * Performs a git merge between the currently checked out branch and
     * the selected local branch.
     *
     * @throws IOException
     * @throws GitAPIException
     */
    public void mergeSelectedBranchWithCurrent() throws IOException, GitAPIException {
        logger.info("Merging selected branch with current");
        Git git = new Git(this.repo);
        LocalBranchHelper selectedBranch = this.localListView.getSelectionModel().getSelectedItem();

        MergeCommand merge = git.merge();
        merge.include(this.repo.resolve(selectedBranch.getRefPathString()));

        MergeResult mergeResult = merge.call();

        if (mergeResult.getMergeStatus().equals(MergeResult.MergeStatus.CONFLICTING)){
            this.showConflictsNotification();

        } else if (mergeResult.getMergeStatus().equals(MergeResult.MergeStatus.ALREADY_UP_TO_DATE)) {
            this.showUpToDateNotification();

        } else if (mergeResult.getMergeStatus().equals(MergeResult.MergeStatus.FAILED)) {
            this.showFailedMergeNotification();

        } else if (mergeResult.getMergeStatus().equals(MergeResult.MergeStatus.MERGED)
                || mergeResult.getMergeStatus().equals(MergeResult.MergeStatus.MERGED_NOT_COMMITTED)) {
            this.showMergeSuccessNotification();
            this.updateBranchesOnMergeSuccess();

        } else if (mergeResult.getMergeStatus().equals(MergeResult.MergeStatus.FAST_FORWARD)) {
            this.showFastForwardMergeNotification();
            this.updateBranchesOnMergeSuccess();

        } else {
            System.out.println(mergeResult.getMergeStatus());
            // todo: handle all cases (maybe combine some)
        }
        git.close();
    }


    private void updateBranchesOnMergeSuccess() throws IOException, GitAPIException {
        sessionModel.getCurrentRepoHelper().getListOfLocalBranches();
        Platform.runLater(() -> {
            try {
                CommitTreeController.update(sessionModel.getCurrentRepoHelper());
                //CommitTreeController.sessionController.gitStatus();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }


    /**
     * Swaps the branches to be merged.
     *
     * For example, this action will change
     * `Merge MASTER into DEVELOP` into `Merge DEVELOP into MASTER.`
     *
     * @throws GitAPIException
     * @throws IOException
     */
    public void swapMergeBranches() throws GitAPIException, IOException {
        LocalBranchHelper selectedBranch = this.localListView.getSelectionModel().getSelectedItem();
        LocalBranchHelper checkedOutBranch = this.repoHelper.getCurrentBranch();

        selectedBranch.checkoutBranch();
        this.localListView.getSelectionModel().select(checkedOutBranch);
        this.updateButtons();

        this.showBranchSwapNotification(selectedBranch.getBranchName());
    }

    /// BEGIN: ERROR NOTIFICATIONS:

    private void showBranchSwapNotification(String newlyCheckedOutBranchName) {
        logger.info("Checked out a branch notification");
        this.notificationPane.setText(String.format("%s is now checked out.", newlyCheckedOutBranchName));

        this.notificationPane.getActions().clear();
        this.notificationPane.show();
    }

    private void showFastForwardMergeNotification() {
        logger.info("Fast forward merge complete notification");
        this.notificationPane.setText("Fast-forward merge completed (HEAD was updated).");

        this.notificationPane.getActions().clear();
        this.notificationPane.show();
    }

    private void showMergeSuccessNotification() {
        logger.info("Merge completed notification");
        this.notificationPane.setText("Merge completed.");

        this.notificationPane.getActions().clear();
        this.notificationPane.show();
    }

    private void showFailedMergeNotification() {
        logger.warn("Merge failed notification");
        this.notificationPane.setText("The merge failed.");

        this.notificationPane.getActions().clear();
        this.notificationPane.show();
    }

    private void showUpToDateNotification() {
        logger.warn("No merge necessary notification");
        this.notificationPane.setText("No merge necessary. Those two branches are already up-to-date.");

        this.notificationPane.getActions().clear();
        this.notificationPane.show();
    }

    private void showGenericGitErrorNotificationWithBranch(LocalBranchHelper branch) {
        logger.warn("Git error on branch notification");
        this.notificationPane.setText(String.format("Sorry, there was a git error on branch %s.", branch.getBranchName()));

        this.notificationPane.getActions().clear();
        this.notificationPane.show();
    }

    private void showGenericGitErrorNotification() {
        logger.warn("Git error notification");
        this.notificationPane.setText("Sorry, there was a git error.");

        this.notificationPane.getActions().clear();
        this.notificationPane.show();
    }

    private void showGenericErrorNotification() {
        logger.warn("Generic error notification");
        this.notificationPane.setText("Sorry, there was an error.");

        this.notificationPane.getActions().clear();
        this.notificationPane.show();
    }

    private void showNotMergedNotification(LocalBranchHelper nonmergedBranch) {
        logger.warn("Not merged notification");
        this.notificationPane.setText("That branch has to be merged before you can do that.");

        Action forceDeleteAction = new Action("Force delete", e -> {
            this.forceDeleteLocalBranch(nonmergedBranch);
            this.notificationPane.hide();
        });

        this.notificationPane.getActions().clear();
        this.notificationPane.getActions().setAll(forceDeleteAction);
        this.notificationPane.show();
    }

    private void showCannotDeleteBranchNotification(LocalBranchHelper branch) {
        logger.warn("Cannot delete current branch notification");
        this.notificationPane.setText(String.format("Sorry, %s can't be deleted right now. " +
                "Try checking out a different branch first.", branch.getBranchName()));
        // probably because it's checked out

        this.notificationPane.getActions().clear();
        this.notificationPane.show();
    }

    private void showRefAlreadyExistsNotification() {
        logger.info("Branch already exists notification");
        this.notificationPane.setText("Looks like that branch already exists locally!");

        this.notificationPane.getActions().clear();
        this.notificationPane.show();
    }

    private void showInvalidBranchNameNotification() {
        logger.warn("Invalid branch name notification");
        this.notificationPane.setText("That branch name is invalid.");

        this.notificationPane.getActions().clear();
        this.notificationPane.show();
    }

    private void showConflictsNotification() {
        logger.info("Merge conflicts notification");
        this.notificationPane.setText("That merge resulted in conflicts. Check the working tree to resolve them.");

        this.notificationPane.getActions().clear();
        this.notificationPane.show();
    }

    private void showNoCommitsYetNotification() {
        logger.warn("No commits yet notification");
        this.notificationPane.setText("You cannot make a branch since your repo has no commits yet. Make a commit first!");

        this.notificationPane.getActions().clear();
        this.notificationPane.show();
    }

    /// END: ERROR NOTIFICATIONS ^^^
}
