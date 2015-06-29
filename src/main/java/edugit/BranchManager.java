package main.java.edugit;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.controlsfx.control.CheckListView;
import org.controlsfx.control.ListSelectionView;
import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.*;
import org.eclipse.jgit.lib.BranchTrackingStatus;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;

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

    public BranchManager(ArrayList<LocalBranchHelper> localBranches, ArrayList<RemoteBranchHelper> remoteBranches, Repository repo) throws IOException {
        this.repo = repo;

        this.remoteListView = new ListView<>(FXCollections.observableArrayList(remoteBranches));
        this.localListView = new ListView<>(FXCollections.observableArrayList(localBranches));

        this.remoteListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        this.localListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
    }

    private LocalBranchHelper createLocalTrackingBranchForRemote(RemoteBranchHelper remoteBranchHelper) throws GitAPIException, IOException {
        Ref trackingBranchRef = new Git(this.repo).checkout().
                setCreateBranch(true).
                setName(remoteBranchHelper.getBranchName()).
                setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK).
                setStartPoint(remoteBranchHelper.getRefPathString()).
                call();
        LocalBranchHelper trackingBranch = new LocalBranchHelper(trackingBranchRef, this.repo);
        return trackingBranch;
    }

    public void showBranchChooser() throws IOException {
//        Parent root = FXMLLoader.load(getClass().getResource("/main/resources/edugit/fxml/BranchManager.fxml"));
        GridPane root = new GridPane();
        Stage stage = new Stage();
        stage.setTitle("Branch Manager");
        stage.setScene(new Scene(root, 450, 450));

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

        stage.show();
    }

    public List<LocalBranchHelper> getLocalBranches() {
        return this.localListView.getItems();
    }

    public void trackSelectedBranchLocally() throws GitAPIException, IOException {
        RemoteBranchHelper selectedRemoteBranch = this.remoteListView.getSelectionModel().getSelectedItem();
        try {
            LocalBranchHelper tracker = this.createLocalTrackingBranchForRemote(selectedRemoteBranch);
            this.localListView.getItems().add(tracker);
        } catch (RefAlreadyExistsException e) {
            // Do nothing. This just means that the branch already exists locally.
            // We'll rely on this git error because git handles it well.
        }
    }

    public void deleteSelectedLocalBranch() {
    }
}
