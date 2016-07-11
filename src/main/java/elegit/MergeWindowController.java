package elegit;

import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.controlsfx.control.NotificationPane;

/**
 * Created by connellyj on 7/11/16.
 *
 * Controller for the merge window
 */
public class MergeWindowController {

    @FXML
    private Text remoteTrackingBranchName;
    @FXML
    private Text localBranchName1;
    @FXML
    private Text localBranchName2;
    @FXML
    private Text intoLocalBranchText1;
    @FXML
    private Text intoLocalBranchText2;
    @FXML
    private CheckBox mergeRemoteTrackingCheckBox;
    @FXML
    private CheckBox mergeDifLocalBranchCheckBox;
    @FXML
    private NotificationPane notificationPane;

    Stage stage;
    SessionModel sessionModel;
    RepoHelper repoHelper;

    static final Logger logger = LogManager.getLogger();

    public void initialize() {
        sessionModel = SessionModel.getSessionModel();
        repoHelper = sessionModel.getCurrentRepoHelper();
        localBranchName1.setText("master");
        localBranchName2.setText("master");
        remoteTrackingBranchName.setText("origin/master");
        intoLocalBranchText1.setText("\t\tinto local branch");
        intoLocalBranchText2.setText("\t\tinto local branch");
    }

    public void showStage(NotificationPane pane) {
        stage = new Stage();
        stage.setTitle("Merge");
        stage.setScene(new Scene(pane, 550, 450));
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setOnCloseRequest(event -> logger.info("Closed merge window"));
        stage.show();
    }

    public void closeWindow() {
        stage.close();
    }

    public void handleMergeButton() {

    }
}
