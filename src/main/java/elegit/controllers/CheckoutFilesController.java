package elegit.controllers;

import de.jensd.fx.glyphs.GlyphsDude;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import elegit.*;
import elegit.models.CommitHelper;
import elegit.models.RepoHelper;
import elegit.models.SessionModel;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import net.jcip.annotations.GuardedBy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.api.CheckoutResult;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Controller class for the checkout files window
 */
public class CheckoutFilesController {

    @FXML private TextField fileField;
    @FXML private VBox filesToCheckout;
    @FXML private NotificationController notificationPaneController;
    @FXML private Label header;
    @FXML private AnchorPane anchorRoot;

    // This is not threadsafe, since it escapes to other portions of the JavaFX view. Only access from FX thread.
    private Stage stage;

    private final RepoHelper repoHelper;

    @GuardedBy("this") private final List<String> fileNames;
    @GuardedBy("this") private CommitHelper commitHelper;

    private static final Logger logger = LogManager.getLogger();

    public static final AtomicReference<SessionController> sessionController = new AtomicReference<>();




    public CheckoutFilesController() {
        repoHelper = SessionModel.getSessionModel().getCurrentRepoHelper();
        this.fileNames = new ArrayList<>();
    }

    /**
     * Initialize method automatically called by JavaFX
     *
     * Sets up views and buttons
     */
    public synchronized void initialize(){
        logger.info("Started up checkout files from commit window");
        this.notificationPaneController.bindParentBounds(anchorRoot.heightProperty());
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
        stage.setOnHiding(e -> {
            notificationPaneController.hideBubbleInstantly();
        });
        stage.show();
        this.notificationPaneController.setAnchor(stage);
    }

    /**
     * Handler for the commit button. Attempts a commit of the added files,
     * then closes the window and notifies SessionController its done
     */
    public synchronized void handleCheckoutButton() {
        try {
            if(fileNames.size() == 0) {
                notificationPaneController.addNotification("You need to add some files first");
                return;
            }
            sessionController.get().updateCommandText("git checkout "+ commitHelper.getName()+ " "+String.join(" ", fileNames));
            // New ArrayList used below so that checkoutFiles cannot modify this list, nor worry about sync errors
            CheckoutResult result = this.repoHelper.checkoutFiles(new ArrayList<>(fileNames), commitHelper.getName());
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
                    sessionController.get().gitStatus();
                    closeWindow();
                    break;
            }
        } catch (Exception e) {
            notificationPaneController.addNotification("Something went wrong.");
        }
    }

    public synchronized void handleAddButton() {
        Main.assertFxThread();
        String fileName = fileField.getText();

        // Don't allow adding the same file more than once
        if (fileNames.contains(fileName)) {
            notificationPaneController.addNotification(fileName+" has already been added.");
            return;
        }

        if(!fileName.equals("")){
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
        }else {
            notificationPaneController.addNotification("You need to type a file name first");
            return;
        }
    }

    public void closeWindow() { this.stage.close(); }

    synchronized void setCommitHelper(CommitHelper commitHelper) {
        this.commitHelper = commitHelper;
        header.setText(header.getText()+commitHelper.getName().substring(0,8));
    }

    public static void setSessionController(SessionController sc) {
        Main.assertFxThread();
        sessionController.set(sc);
    }
}
