package main.java.edugit;

import javafx.fxml.FXMLLoader;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.controlsfx.control.ListSelectionView;

import java.io.IOException;
import java.rmi.Remote;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by grahamearley on 6/29/15.
 */
public class BranchManager {

    public ListSelectionView<BranchHelper> listSelectionView;

    public BranchManager(ArrayList<LocalBranchHelper> localBranches, ArrayList<RemoteBranchHelper> remoteBranches) {
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

}
