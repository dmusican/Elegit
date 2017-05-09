package elegit.controllers;

import elegit.RepoHelper;
import elegit.SessionModel;
import elegit.exceptions.NoFilesToStashException;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.api.errors.GitAPIException;

public class StashSaveController {

    private Stage stage;

    private SessionController sessionController;
    private RepoHelper repoHelper;

    @FXML private NotificationController notificationPaneController;
    @FXML private AnchorPane anchorRoot;
    @FXML private Button saveButton;
    @FXML private TextField stashMessage;
    @FXML private CheckBox includeUntracked;

    static final Logger logger = LogManager.getLogger();

    /**
     * Initialize method automatically called by JavaFX
     *
     * Sets up views and buttons
     */
    public void initialize() {
        logger.info("Started up stash save window");

        SessionModel sessionModel = SessionModel.getSessionModel();
        this.repoHelper = sessionModel.getCurrentRepoHelper();

        stashMessage.setOnAction((event -> {
            stashSave(stashMessage.getText());
        }));

        this.notificationPaneController.bindParentBounds(anchorRoot.heightProperty());
    }

    /**
     * Shows the stash list manager
     * @param pane the anchor of the stage
     */
    public void showStage(AnchorPane pane) {
        stage = new Stage();
        stage.setTitle("Stash Save");
        stage.setScene(new Scene(pane));
        stage.setOnCloseRequest(event -> {
            logger.info("Closed stash save window");
        });
        stage.show();
        this.notificationPaneController.setAnchor(stage);
    }

    /* ********************** BUTTON HANDLERS ********************** */
    public void closeWindow() { this.stage.close(); }

    public void handleSave() {
        if (stashMessage.getText() != null)
            stashSave(stashMessage.getText());
        else
            stashSave();
    }

    public void stashSave() {
        try {
            repoHelper.stashSave(includeUntracked.isSelected());
            sessionController.gitStatus();
            closeWindow();
        } catch (GitAPIException e) {
            notificationPaneController.addNotification("Something went wrong with the save.");
        } catch (NoFilesToStashException e) {
            notificationPaneController.addNotification("No files to stash.");
        }
    }

    public void stashSave(String message) {
        try {
            repoHelper.stashSave(includeUntracked.isSelected(), message,"");
            sessionController.gitStatus();
            closeWindow();
        } catch (GitAPIException e) {
            notificationPaneController.addNotification("Something went wrong with the save.");
        } catch (NoFilesToStashException e) {
            notificationPaneController.addNotification("No files to stash");
        }
    }

    void setSessionController(SessionController controller) { this.sessionController = controller; }

}
