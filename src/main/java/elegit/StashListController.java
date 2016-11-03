package elegit;

import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.WrongRepositoryStateException;

public class StashListController {

    private Stage stage;

    private SessionController sessionController;
    private RepoHelper repoHelper;

    @FXML private NotificationController notificationPaneController;
    @FXML private AnchorPane anchorRoot;

    static final Logger logger = LogManager.getLogger();

    /**
     * Initialize method automatically called by JavaFX
     *
     * Sets up views and buttons
     */
    public void initialize() {
        logger.info("Started up stash list window");

        SessionModel sessionModel = SessionModel.getSessionModel();
        this.repoHelper = sessionModel.getCurrentRepoHelper();

        this.notificationPaneController.bindParentBounds(anchorRoot.heightProperty());
    }

    /**
     * Shows the stash list manager
     * @param pane the anchor of the stage
     */
    public void showStage(AnchorPane pane) {
        stage = new Stage();
        stage.setTitle("Stash List");
        stage.setScene(new Scene(pane));
        stage.setOnCloseRequest(event -> {
            logger.info("Closed stash list window");
        });
        stage.show();
        this.notificationPaneController.setAnchor(stage);
    }

    /* ********************** BUTTON HANDLERS ********************** */
    public void closeWindow() { this.stage.close(); }
    public void handleApply() {
        // TODO: get selected stash
        String stashRef = "";
        // apply the selected stash
        try {
            repoHelper.stashApply(stashRef, false);
        } catch (WrongRepositoryStateException e) {
            notificationPaneController.addNotification("Conflicts occured while trying to apply stash. Commit/stash changes or force apply (right click).");
        } catch (GitAPIException e) {
            notificationPaneController.addNotification("Something went wrong with the apply. Try committing any uncommitted changes.");
        }
    }

    public void handleDrop() {

    }

    public void handleClearStash() {

    }

    public void handlePop() {

    }

    void setSessionController(SessionController controller) { this.sessionController = controller; }

}
