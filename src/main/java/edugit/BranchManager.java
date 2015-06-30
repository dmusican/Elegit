package main.java.edugit;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.controlsfx.control.CheckListView;
import org.controlsfx.control.ListSelectionView;
import org.controlsfx.control.NotificationPane;
import org.controlsfx.control.action.Action;
import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.*;
import org.eclipse.jgit.lib.BranchTrackingStatus;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.RefSpec;

import java.io.IOException;
import java.rmi.Remote;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by grahamearley on 6/29/15.
 */
public class BranchManager {

    public ListView<RemoteBranchHelper> remoteListView;
    public ListView<LocalBranchHelper> localListView;
    private Repository repo;
    private NotificationPane notificationPane;

    private TextField newBranchNameField;

    public BranchManager(ArrayList<LocalBranchHelper> localBranches, ArrayList<RemoteBranchHelper> remoteBranches, Repository repo) throws IOException {
        this.repo = repo;

        this.remoteListView = new ListView<>(FXCollections.observableArrayList(remoteBranches));
        this.localListView = new ListView<>(FXCollections.observableArrayList(localBranches));

        this.remoteListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        this.localListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        this.newBranchNameField = new TextField();
        this.newBranchNameField.setPromptText("Branch name");
    }

    public void showBranchChooserWindow() throws IOException {
        GridPane root = new GridPane();
        root.setHgap(10);
        root.setVgap(10);
        root.setPadding(new Insets(10));
        root.add(this.remoteListView, 0, 0); // col, row
        root.add(this.localListView, 1, 0);

        Button trackRemoteBranchButton = new Button("Track branch locally");
        trackRemoteBranchButton.setOnAction(e -> {
            try {
                this.trackSelectedBranchLocally();
            } catch (GitAPIException e1) {
                e1.printStackTrace();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        });
        Button deleteLocalBranchButton = new Button("Delete local branch");
        deleteLocalBranchButton.setOnAction(e -> {
            this.deleteSelectedLocalBranch();
        });

        root.add(trackRemoteBranchButton, 0, 1);
        root.add(deleteLocalBranchButton, 1, 1);

        root.add(new Text(String.format("Branch off from %s:", this.repo.getBranch())), 0, 2, 2, 1); // colspan = 2

        root.add(this.newBranchNameField, 0, 3);

        Button newBranchButton = new Button("Create branch");
        newBranchButton.setOnAction(e -> {
            try {
                LocalBranchHelper newLocalBranch = this.createNewLocalBranch(this.newBranchNameField.getText());
                this.localListView.getItems().add(newLocalBranch);
            }catch (InvalidRefNameException e1) {
                this.showInvalidBranchNameNotification();
                e1.printStackTrace();
            } catch (GitAPIException e1) {
                e1.printStackTrace();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        });
        root.add(newBranchButton, 1, 3);

        Stage stage = new Stage();
        stage.setTitle("Branch Manager");
        this.notificationPane = new NotificationPane(root);
        this.notificationPane.getStylesheets().add("/main/resources/edugit/css/BaseStyle.css");
        stage.setScene(new Scene(this.notificationPane, 450, 450));
        stage.show();
    }

    private LocalBranchHelper createLocalTrackingBranchForRemote(RemoteBranchHelper remoteBranchHelper) throws GitAPIException, IOException {
        Ref trackingBranchRef = new Git(this.repo).branchCreate().
                setName(remoteBranchHelper.getBranchName()).
                setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK).
                setStartPoint(remoteBranchHelper.getRefPathString()).
                call();
        LocalBranchHelper trackingBranch = new LocalBranchHelper(trackingBranchRef, this.repo);
        return trackingBranch;
    }

    public List<LocalBranchHelper> getLocalBranches() {
        return this.localListView.getItems();
    }

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

    public void deleteSelectedLocalBranch() {
        LocalBranchHelper selectedBranch = this.localListView.getSelectionModel().getSelectedItem();
        Git git = new Git(this.repo);

        try {
            if (selectedBranch != null) {
                // Local delete:
                git.branchDelete().setBranchNames(selectedBranch.getRefPathString()).call();
                this.localListView.getItems().remove(selectedBranch);
            }
        } catch (NotMergedException e) {
            this.showNotMergedNotification();
            e.printStackTrace();
        } catch (CannotDeleteCurrentBranchException e) {
            this.showCannotDeleteBranchNotification();
            e.printStackTrace();
        } catch (GitAPIException e) {
            this.showGenericGitError();
            e.printStackTrace();
        }

        // TODO: add optional delete from remote, too.
        // see http://stackoverflow.com/questions/11892766/how-to-remove-remote-branch-with-jgit
    }

    private LocalBranchHelper createNewLocalBranch(String branchName) throws GitAPIException, IOException {
        Git git = new Git(this.repo);
        Ref newBranch = git.branchCreate().setName(branchName).call();
        LocalBranchHelper newLocalBranchHelper = new LocalBranchHelper(newBranch, this.repo);

        return newLocalBranchHelper;
    }

    private void forceDeleteSelectedLocalBranch() {
        LocalBranchHelper selectedBranch = this.localListView.getSelectionModel().getSelectedItem();
        Git git = new Git(this.repo);

        try {
            if (selectedBranch != null) {
                // Local delete:
                git.branchDelete().setForce(true).setBranchNames(selectedBranch.getRefPathString()).call();
                this.localListView.getItems().remove(selectedBranch);
            }
        } catch (NotMergedException e) {
            this.showNotMergedNotification();
            e.printStackTrace();
        } catch (CannotDeleteCurrentBranchException e) {
            this.showCannotDeleteBranchNotification();
            e.printStackTrace();
        } catch (GitAPIException e) {
            this.showGenericGitError();
            e.printStackTrace();
        }
    }

    private void showGenericGitError() {
        this.notificationPane.setText("Sorry, there was a git error.");

        this.notificationPane.getActions().clear();
        this.notificationPane.show();
    }

    private void showNotMergedNotification() {
        this.notificationPane.setText("That branch has to be merged before you can do that.");

        Action forceDeleteAction = new Action("Force delete", e -> {
            this.forceDeleteSelectedLocalBranch();
            this.notificationPane.hide();
        });

        this.notificationPane.getActions().clear();
        this.notificationPane.getActions().setAll(forceDeleteAction);
        this.notificationPane.show();
    }

    private void showCannotDeleteBranchNotification() {
        this.notificationPane.setText("Sorry, that branch can't be deleted right now. Try checking out a different branch first.");
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
}
