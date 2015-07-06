package main.java.edugit;

import de.jensd.fx.glyphs.GlyphsDude;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.controlsfx.control.NotificationPane;
import org.controlsfx.control.action.Action;
import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeCommand;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.errors.*;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;

import java.awt.*;
import java.io.IOException;
import java.util.List;

/**
 *
 * A class that holds all a repository's branches (in the form of
 * BranchHelpers) and manages branch creation, deletion, and tracking
 * of remotes.
 *
 */
public class BranchManager {

    public ListView<RemoteBranchHelper> remoteListView;
    public ListView<LocalBranchHelper> localListView;
    private Repository repo;
    private RepoHelper repoHelper;
    private NotificationPane notificationPane;

    private TextField newBranchNameField;

    private Button mergeButton;
    private Button deleteLocalBranchesButton;
    private Button trackRemoteBranchButton;
    private Button swapMergeBranchesButton;

    public BranchManager(List<LocalBranchHelper> localBranches, List<RemoteBranchHelper> remoteBranches, RepoHelper repoHelper) throws IOException {
        this.repoHelper = repoHelper;
        this.repo = repoHelper.getRepo();

        this.remoteListView = new ListView<>(FXCollections.observableArrayList(remoteBranches));
        this.localListView = new ListView<>(FXCollections.observableArrayList(localBranches));

        this.remoteListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        this.remoteListView.setOnMouseClicked(e -> {
            try {
                this.updateButtons();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        });

        // Local list view can select multiple (for merges):
        this.localListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        this.localListView.setOnMouseClicked(e -> {
            try {
                this.updateButtons();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        });

        this.newBranchNameField = new TextField();
        this.newBranchNameField.setPromptText("Branch name");

        this.trackRemoteBranchButton = new Button("Track branch locally");
        this.trackRemoteBranchButton.setOnAction(e -> {
            try {
                this.trackSelectedBranchLocally();
            } catch (GitAPIException e1) {
                e1.printStackTrace();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        });
        this.trackRemoteBranchButton.setDisable(true);

        this.deleteLocalBranchesButton = new Button("Delete local branch");
        this.deleteLocalBranchesButton.setOnAction(e -> this.deleteSelectedLocalBranches());
        this.deleteLocalBranchesButton.setDisable(true);

        this.mergeButton = new Button(); // text is set dynamically
        this.mergeButton.setOnAction(e -> {
            try {
                this.mergeSelectedBranchWithCurrent();
            } catch (IOException e1) {
                e1.printStackTrace();
            } catch (GitAPIException e1) {
                e1.printStackTrace();
            }
        });

        Text arrowsIcon = GlyphsDude.createIcon(FontAwesomeIcon.ARROWS_H);
        arrowsIcon.setFill(Color.WHITE);
        this.swapMergeBranchesButton = new Button(null, arrowsIcon);
        this.swapMergeBranchesButton.setOnAction(e -> {
            try {
                this.swapMergeBranches();
            } catch (GitAPIException e1) {
                e1.printStackTrace();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        });

        this.updateButtons();
    }

    /**
     * Creates, populates, and displays the window that lets the user
     * create, delete, or track branches.
     *
     * A GridPane is set up programmatically in this function.
     *
     * @throws IOException
     */
    public void showBranchChooserWindow() throws IOException {
        final int PADDING = 10;
        GridPane root = new GridPane();
        root.setHgap(PADDING);
        root.setVgap(PADDING);
        root.setPadding(new Insets(PADDING));
        root.setAlignment(Pos.TOP_CENTER);

        root.add(new Text("Remote branches:"), 0, 0); // col, row
        root.add(new Text("Local branches:"), 1, 0);
        root.add(this.remoteListView, 0, 1);
        root.add(this.localListView, 1, 1);

        this.updateButtons();

        HBox trackDeleteButtons = new HBox(trackRemoteBranchButton, deleteLocalBranchesButton);
        trackDeleteButtons.setAlignment(Pos.CENTER);
        trackDeleteButtons.setSpacing(PADDING);
        root.add(trackDeleteButtons, 0, 2, 2, 1);

        HBox mergeButtons = new HBox(this.mergeButton, this.swapMergeBranchesButton);
        mergeButtons.setAlignment(Pos.TOP_CENTER);
        mergeButtons.setSpacing(PADDING);
        root.add(mergeButtons, 0, 3, 2, 1);

        root.add(new Text(String.format("Branch off from %s:", this.repo.getBranch())), 0, 4, 2, 1); // colspan = 2

        root.add(this.newBranchNameField, 0, 5);

        Button newBranchButton = new Button("Create branch");
        newBranchButton.setOnAction(e -> {
            try {
                LocalBranchHelper newLocalBranch = this.createNewLocalBranch(this.newBranchNameField.getText());
                this.localListView.getItems().add(newLocalBranch);
                this.newBranchNameField.clear();
            } catch (InvalidRefNameException e1) {
                this.showInvalidBranchNameNotification();
            } catch (RefNotFoundException e1) {
                // When a repo has no commits, you can't create branches because there
                //  are no commits to point to. This error gets raised when git can't find
                //  HEAD.
                this.showNoCommitsYetNotification();
            } catch (GitAPIException e1) {
                this.showGenericGitErrorNotification();
                e1.printStackTrace();
            } catch (IOException e1) {
                this.showGenericErrorNotification();
                e1.printStackTrace();
            }
        });
        root.add(newBranchButton, 1, 5);

        Stage stage = new Stage();
        stage.setTitle("Branch Manager");
        this.notificationPane = new NotificationPane(root);
        this.notificationPane.getStylesheets().add("/main/resources/edugit/css/BaseStyle.css");
        stage.setScene(new Scene(this.notificationPane, 550, 450));
        stage.show();
    }

    /**
     * Updates the track remote, merge, and delete local buttons'
     * text and/or disabled/enabled status.
     *
     * @throws IOException
     */
    private void updateButtons() throws IOException {
        String currentBranchName = this.repo.getBranch();

        // Update delete button
        if (this.localListView.getSelectionModel().getSelectedIndices().size() > 0) {
            this.deleteLocalBranchesButton.setDisable(false);
        }

        // Update track button
        if (this.remoteListView.getSelectionModel().getSelectedIndices().size() > 0) {
            this.trackRemoteBranchButton.setDisable(false);
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
        Ref trackingBranchRef = new Git(this.repo).branchCreate().
                setName(remoteBranchHelper.getBranchName()).
                setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK).
                setStartPoint(remoteBranchHelper.getRefPathString()).
                call();
        LocalBranchHelper trackingBranch = new LocalBranchHelper(trackingBranchRef, this.repoHelper);
        return trackingBranch;
    }

    public List<LocalBranchHelper> getLocalBranches() {
        return this.localListView.getItems();
    }

    /**
     * Tracks the selected branch (in the remoteListView) locally.
     *
     * @throws GitAPIException
     * @throws IOException
     */
    public void trackSelectedBranchLocally() throws GitAPIException, IOException {
        RemoteBranchHelper selectedRemoteBranch = this.remoteListView.getSelectionModel().getSelectedItem();
        try {
            if (selectedRemoteBranch != null) {
                LocalBranchHelper tracker = this.createLocalTrackingBranchForRemote(selectedRemoteBranch);
                this.localListView.getItems().add(tracker);
            }
        } catch (RefAlreadyExistsException e) {
            this.showRefAlreadyExistsNotification();
        }
    }

    /**
     * Deletes the selected branches (in the localListView) through git.
     */
    public void deleteSelectedLocalBranches() {
        Git git = new Git(this.repo);

        for (LocalBranchHelper selectedBranch : this.localListView.getSelectionModel().getSelectedItems()) {
            try {
                if (selectedBranch != null) {
                    // Local delete:
                    git.branchDelete().setBranchNames(selectedBranch.getRefPathString()).call();
                    this.localListView.getItems().remove(selectedBranch);
                }
            } catch (NotMergedException e) {
                this.showNotMergedNotification(selectedBranch);
            } catch (CannotDeleteCurrentBranchException e) {
                this.showCannotDeleteBranchNotification(selectedBranch);
            } catch (GitAPIException e) {
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
        Git git = new Git(this.repo);

        try {
            if (branchToDelete != null) {
                // Local delete:
                git.branchDelete().setForce(true).setBranchNames(branchToDelete.getRefPathString()).call();
                this.localListView.getItems().remove(branchToDelete);
            }
        } catch (CannotDeleteCurrentBranchException e) {
            this.showCannotDeleteBranchNotification(branchToDelete);
        } catch (GitAPIException e) {
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
        } else if (mergeResult.getMergeStatus().equals(MergeResult.MergeStatus.FAST_FORWARD)) {
            this.showFastForwardMergeNotification();
        } else {
            System.out.println(mergeResult.getMergeStatus());
            // todo: handle all cases (maybe combine some)
        }
        git.close();
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
    private void swapMergeBranches() throws GitAPIException, IOException {
        LocalBranchHelper selectedBranch = this.localListView.getSelectionModel().getSelectedItem();
        LocalBranchHelper checkedOutBranch = this.repoHelper.getCurrentBranch();

        selectedBranch.checkoutBranch();
        this.localListView.getSelectionModel().select(checkedOutBranch);
        this.updateButtons();

        this.showBranchSwapNotification(selectedBranch.getBranchName());
    }

    /// BEGIN: ERROR NOTIFICATIONS:

    private void showBranchSwapNotification(String newlyCheckedOutBranchName) {
        this.notificationPane.setText(String.format("%s is now checked out.", newlyCheckedOutBranchName));

        this.notificationPane.getActions().clear();
        this.notificationPane.show();
    }

    private void showFastForwardMergeNotification() {
        this.notificationPane.setText("Fast-forward merge completed (HEAD was updated).");

        this.notificationPane.getActions().clear();
        this.notificationPane.show();
    }

    private void showMergeSuccessNotification() {
        this.notificationPane.setText("Merge completed.");

        this.notificationPane.getActions().clear();
        this.notificationPane.show();
    }

    private void showFailedMergeNotification() {
        this.notificationPane.setText("The merge failed.");

        this.notificationPane.getActions().clear();
        this.notificationPane.show();
    }

    private void showUpToDateNotification() {
        this.notificationPane.setText("No merge necessary. Those two branches are already up-to-date.");

        this.notificationPane.getActions().clear();
        this.notificationPane.show();
    }

    private void showGenericGitErrorNotificationWithBranch(LocalBranchHelper branch) {
        this.notificationPane.setText(String.format("Sorry, there was a git error on branch %s.", branch.getBranchName()));

        this.notificationPane.getActions().clear();
        this.notificationPane.show();
    }

    private void showGenericGitErrorNotification() {
        this.notificationPane.setText("Sorry, there was a git error.");

        this.notificationPane.getActions().clear();
        this.notificationPane.show();
    }

    private void showGenericErrorNotification() {
        this.notificationPane.setText("Sorry, there was an error.");

        this.notificationPane.getActions().clear();
        this.notificationPane.show();
    }

    private void showNotMergedNotification(LocalBranchHelper nonmergedBranch) {
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
        this.notificationPane.setText(String.format("Sorry, %s can't be deleted right now. " +
                "Try checking out a different branch first.", branch.getBranchName()));
        // probably because it's checked out

        this.notificationPane.getActions().clear();
        this.notificationPane.show();
    }

    private void showRefAlreadyExistsNotification() {
        this.notificationPane.setText("Looks like that branch already exists locally!");

        this.notificationPane.getActions().clear();
        this.notificationPane.show();
    }

    private void showInvalidBranchNameNotification() {
        this.notificationPane.setText("That branch name is invalid.");

        this.notificationPane.getActions().clear();
        this.notificationPane.show();
    }

    private void showConflictsNotification() {
        this.notificationPane.setText("That merge resulted in conflicts. Check the working tree to resolve them.");

        this.notificationPane.getActions().clear();
        this.notificationPane.show();
    }

    private void showNoCommitsYetNotification() {
        this.notificationPane.setText("You cannot make a branch since your repo has no commits yet. Make a commit first!");

        this.notificationPane.getActions().clear();
        this.notificationPane.show();
    }

    /// END: ERROR NOTIFICATIONS ^^^
}
