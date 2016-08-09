package elegit;

import de.jensd.fx.glyphs.GlyphsDude;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.api.CheckoutResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Controller class for the checkout files window
 */
public class CheckoutFilesController {

    private Stage stage;
    private SessionController sessionController;
    private RepoHelper repoHelper;
    private CommitHelper commitHelper;

    @FXML private TextField fileField;
    @FXML private VBox filesToCheckout;
    @FXML private NotificationController notificationPaneController;
    @FXML private Label header;

    private List<String> fileNames;

    static final Logger logger = LogManager.getLogger();

    /**
     * Initialize method automatically called by JavaFX
     *
     * Sets up views and buttons
     */
    public void initialize(){
        logger.info("Started up checkout files from commit window");

        SessionModel sessionModel = SessionModel.getSessionModel();
        this.repoHelper = sessionModel.getCurrentRepoHelper();
        this.fileNames = new ArrayList<>();
    }

    /**
     * Shows the checkout manager
     * @param pane the anchor of the stage
     */
    public void showStage(AnchorPane pane) {
        stage = new Stage();
        stage.setTitle("Checkout files");
        stage.setScene(new Scene(pane));
        stage.setOnCloseRequest(event -> {
            logger.info("Closed checkout files from commit window");
        });
        stage.show();
    }

    /**
     * Handler for the commit button. Attempts a commit of the added files,
     * then closes the window and notifies SessionController its done
     */
    public void handleCheckoutButton() {
        try {
            CheckoutResult result = this.repoHelper.checkoutFiles(fileNames, commitHelper.getId());
            switch (result.getStatus()) {
                case CONFLICTS:
                    notificationPaneController.addNotification("Checkout has not completed because of checkout conflicts");
                    break;
                case ERROR:
                    notificationPaneController.addNotification("An error occurred during checkout");
                    break;
                case NONDELETED:
                    notificationPaneController.addNotification("Checkout has completed, but some files could not be deleted.");
                    break;
                case NOT_TRIED:
                    notificationPaneController.addNotification("Something went wrong... try checking out again.");
                    break;
                // The OK case happens when a file is changed in the index or an invalid file
                // was entered, for now just call git status and close
                // TODO: figure out if anything actually changed
                case OK:
                    sessionController.gitStatus();
                    closeWindow();
                    break;
            }
        } catch (Exception e) {
            notificationPaneController.addNotification("Something went wrong.");
        }
    }

    public void handleAddButton() {
        String fileName = fileField.getText();

        // Don't allow adding the same file more than once
        if (fileNames.contains(fileName)) {
            notificationPaneController.addNotification(fileName+" has already been added.");
            return;
        }

        Label line = new Label(fileName);
        line.setWrapText(true);
        line.setId("notification");
        line.setGraphic(GlyphsDude.createIcon(FontAwesomeIcon.TIMES_CIRCLE));
        line.setOnMouseClicked(event -> {
            if (event.getTarget().equals(line.getGraphic()))
                fileNames.remove(fileName);
                filesToCheckout.getChildren().remove(line);
        });
        fileNames.add(fileName);
        filesToCheckout.getChildren().add(0,line);
    }

    public void closeWindow() { this.stage.close(); }

    void setSessionController(SessionController controller) { this.sessionController = controller; }

    void setCommitHelper(CommitHelper commitHelper) {
        this.commitHelper = commitHelper;
        header.setText(header.getText()+"from "+commitHelper.getId().substring(0,8));
    }
}
