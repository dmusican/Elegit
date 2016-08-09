package elegit;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Scene;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.api.errors.GitAPIException;

/**
 * Controller class for the checkout files window
 */
public class CheckoutFilesController {

    private Stage stage;
    private SessionModel sessionModel;
    private RepoHelper repoHelper;

    public BooleanProperty isClosed;
    private Thread refresher;

    private static SessionController sessionController;

    static final Logger logger = LogManager.getLogger();

    /**
     * Initialize method automatically called by JavaFX
     *
     * Sets up views and buttons
     */
    public void initialize(){
        logger.info("Started up checkout files from commit window");

        isClosed = new SimpleBooleanProperty(false);

        this.sessionModel = SessionModel.getSessionModel();
        this.repoHelper = this.sessionModel.getCurrentRepoHelper();
    }

    /**
     * Shows the branch manager
     * @param pane NotificationPane
     */
    public void showStage(GridPane pane) {
        stage = new Stage();
        stage.setTitle("Checkout files");
        stage.setScene(new Scene(pane, 550, 450));
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setOnCloseRequest(event -> {
            logger.info("Closed checkout files from commit window");
            refresher.interrupt();
        });
        stage.show();
    }

    /**
     * Handler for the commit button. Attempts a commit of the added files,
     * then closes the window and notifies SessionController its done
     */
    public void handleCheckoutButton() {
        try {
            this.repoHelper.checkoutFiles(null);
        } catch (GitAPIException e) {
            e.printStackTrace();
        } finally {
            closeWindow();
            isClosed.setValue(true);
        }
    }

    public void closeWindow() { this.stage.close(); }

    static void setSessionController(SessionController controller) {
        sessionController = controller;
    }
}
