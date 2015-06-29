package main.java.edugit;

import javafx.fxml.FXMLLoader;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.text.Text;
import javafx.stage.Stage;
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

    private Repository repo;
    public ListSelectionView<BranchHelper> listSelectionView;

    public BranchManager(ArrayList<LocalBranchHelper> localBranches, ArrayList<RemoteBranchHelper> remoteBranches, Repository repo) {
        this.repo = repo;

        this.listSelectionView = new ListSelectionView<>();
        this.listSelectionView.setSourceHeader(new Text("Remote Branches"));
        this.listSelectionView.setTargetHeader(new Text("Local Branches"));

        this.listSelectionView.getSourceItems().setAll(remoteBranches);
        this.listSelectionView.getTargetItems().setAll(localBranches);
    }

    public void showBranchChooser() throws IOException {
        Group root = new Group();
        Stage stage = new Stage();
        stage.setTitle("Branch Manager");
        stage.setScene(new Scene(root, 450, 450));
        root.getChildren().setAll(this.listSelectionView);
        stage.show();
    }

    public ArrayList<LocalBranchHelper> getLocalBranches() throws GitAPIException, IOException {
        ArrayList<LocalBranchHelper> localBranches = new ArrayList<>();
        for (BranchHelper branchHelper : this.listSelectionView.getTargetItems()) {
            if (branchHelper.isLocal()) {
                localBranches.add((LocalBranchHelper) branchHelper);
            } else {
                if (branchHelper.trackingBranch != null) {
                    localBranches.add(branchHelper.trackingBranch);
                } else {
                    // So, it's a remote branchHelper
                    Ref trackingBranchRef = new Git(this.repo).checkout().
                            setCreateBranch(true).
                            setName(branchHelper.getBranchName()).
                            setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK).
                            setStartPoint(branchHelper.getRefPathString()).
                            call();
                    LocalBranchHelper trackingBranch = new LocalBranchHelper(trackingBranchRef, this.repo);
                    branchHelper.setTrackingBranch(trackingBranch);
                }
            }
        }
        return localBranches;
    }
}
