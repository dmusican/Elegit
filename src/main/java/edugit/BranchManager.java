package main.java.edugit;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.controlsfx.control.CheckListView;
import org.controlsfx.control.ListSelectionView;
import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;
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

    public CheckListView<RemoteBranchHelper> remoteListView;
    public ListView<LocalBranchHelper> localListView;
    private Repository repo;

    public BranchManager(ArrayList<LocalBranchHelper> localBranches, ArrayList<RemoteBranchHelper> remoteBranches, Repository repo) {
        this.repo = repo;

        this.remoteListView = new CheckListView<>(FXCollections.observableArrayList(remoteBranches));
        this.localListView = new ListView<>(FXCollections.observableArrayList(localBranches));

        this.remoteListView.getCheckModel().getCheckedItems().addListener((ListChangeListener.Change<? extends RemoteBranchHelper> c) -> {
            try {
                this.updateLocalBranchesWithCheckedRemotes(this.remoteListView.getCheckModel().getCheckedItems());
            } catch (GitAPIException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });


    }

    private void updateLocalBranchesWithCheckedRemotes(ObservableList<RemoteBranchHelper> checkedBranchHelpers) throws GitAPIException, IOException {
        ObservableList<LocalBranchHelper> locals = FXCollections.observableArrayList();
        for (RemoteBranchHelper remoteBranchHelper : checkedBranchHelpers) {
            if (remoteBranchHelper.getTrackingBranch() != null) {
                locals.add(remoteBranchHelper.getTrackingBranch());
            } else {
                // Create a branch that tracks the remote:
                Ref trackingBranchRef = new Git(this.repo).checkout().
                        setCreateBranch(true).
                        setName(remoteBranchHelper.getBranchName()).
                        setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK).
                        setStartPoint(remoteBranchHelper.getRefPathString()).
                        call();
                LocalBranchHelper trackingBranch = new LocalBranchHelper(trackingBranchRef, this.repo);
                remoteBranchHelper.setTrackingBranch(trackingBranch);
                locals.add(trackingBranch);
            }
        }
        this.localListView.setItems(locals);
    }

    public void showBranchChooser() throws IOException {
//        Parent root = FXMLLoader.load(getClass().getResource("/main/resources/edugit/fxml/BranchManager.fxml"));
        GridPane root = new GridPane();
        Stage stage = new Stage();
        stage.setTitle("Branch Manager");
        stage.setScene(new Scene(root, 450, 450));

        root.add(this.remoteListView, 0, 0); // col, row
        root.add(this.localListView, 1, 0);

        stage.show();
    }
}
