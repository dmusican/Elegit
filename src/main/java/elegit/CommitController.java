package elegit;

import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.controlsfx.control.NotificationPane;

/**
 * Created by Eric on 7/11/2016.
 */
public class CommitController {

    private Stage stage;

    public void handleCommitButton() {
        System.out.println("Commiting!");
    }

    /**
     * Shows the branch manager
     * @param pane NotificationPane
     */
    public void showStage(NotificationPane pane) {
        stage = new Stage();
        stage.setTitle("Commit");
        stage.setScene(new Scene(pane, 550, 450));
        stage.initModality(Modality.APPLICATION_MODAL);
        //stage.setOnCloseRequest(event -> logger.info("Closed branch manager window"));
        stage.show();
    }
}
