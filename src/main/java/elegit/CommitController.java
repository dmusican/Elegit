package elegit;

import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Created by Eric on 7/11/2016.
 */
public class CommitController {

    private Stage stage;
    @FXML
    public Button commitButton;

    /**
     * Initialize method automatically called by JavaFX
     *
     * Sets up views and buttons
     */
    public void initialize(){
        initializeLayoutParameters();
    }

    public void handleCommitButton() {
        System.out.println("Commiting!");
    }

    private void initializeLayoutParameters() {
        commitButton.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
    }

    /**
     * Shows the branch manager
     * @param pane NotificationPane
     */
    public void showStage(GridPane pane) {
        stage = new Stage();
        stage.setTitle("Commit");
        stage.setScene(new Scene(pane, 550, 450));
        stage.initModality(Modality.APPLICATION_MODAL);
        //stage.setOnCloseRequest(event -> logger.info("Closed branch manager window"));
        stage.show();
    }
}
