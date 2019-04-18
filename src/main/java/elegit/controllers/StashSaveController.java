package elegit.controllers;

import elegit.models.RepoHelper;
import elegit.models.SessionModel;
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

    @FXML private NotificationController notificationPaneController;
    @FXML private AnchorPane anchorRoot;
    @FXML private TextField stashMessage;
    @FXML private CheckBox includeUntracked;

    private Stage stage;
    private final RepoHelper repoHelper;
    private static final Logger logger = LogManager.getLogger();
    private final SessionController sessionController;

    public StashSaveController() {
        SessionModel sessionModel = SessionModel.getSessionModel();
        sessionController=SessionController.getSessionController();
        this.repoHelper = sessionModel.getCurrentRepoHelper();
    }
    /**
     * Initialize method automatically called by JavaFX
     *
     * Sets up views and buttons
     */
    public void initialize() {
        logger.info("Started up stash save window");
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
        stage.setOnHiding(e -> {
            notificationPaneController.hideBubbleInstantly();
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
            sessionController.updateCommandText("git stash push");
            // TODO: Fix this when a better version of gitStatus is done
            //sessionController.gitStatus();
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
            sessionController.updateCommandText("git stash push -message \""+ message+"\"");
            // TODO: Fix this when a better version of gitStatus is done
            //sessionController.gitStatus();
            closeWindow();
        } catch (GitAPIException e) {
            notificationPaneController.addNotification("Something went wrong with the save.");
        } catch (NoFilesToStashException e) {
            notificationPaneController.addNotification("No files to stash");
        }
    }


}
