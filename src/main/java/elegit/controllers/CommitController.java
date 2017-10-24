package elegit.controllers;

import elegit.exceptions.MissingRepoException;
import elegit.gui.AllFilesPanelView;
import elegit.gui.StagedTreePanelView;
import elegit.models.RepoHelper;
import elegit.models.SessionModel;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.util.concurrent.TimeUnit;

/**
 * Controller class for the commit window
 */
public class CommitController {

    @FXML private Button commitButton;
    @FXML private TextArea commitMessageField;
    // TODO: Make sure FileStructurePanelView, and two subclasses below are threadsafe
    @FXML private StagedTreePanelView stagedFilesPanelView;
    @FXML private AllFilesPanelView allFilesPanelView;

    // This is not threadsafe, since it escapes to other portions of the JavaFX view. Only access from FX thread.
    private Stage stage;

    // TODO: Make sure repoHelper is threadsafe
    private final RepoHelper repoHelper;

    private static final Logger logger = LogManager.getLogger();


    public CommitController() {
        repoHelper = SessionModel.getSessionModel().getCurrentRepoHelper();
    }

    /**
     * Initialize method automatically called by JavaFX
     *
     * Sets up views and buttons
     */
    public void initialize(){
        logger.info("Started up branch manager");

        this.commitMessageField.requestFocus();

        updatePanelViews();

        // Commit button can only be clicked if there is a commit message in the window
        this.commitButton.setDisable(true);
        this.commitMessageField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.equals(""))
                this.commitButton.setDisable(false);
            else
                this.commitButton.setDisable(true);
        });
        initializeLayoutParameters();
    }

    /**
     * Helper method to initialize the layout parameters for the nodes
     */
    private void initializeLayoutParameters() {
        commitButton.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
        commitMessageField.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
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

        Disposable refreshTimer = Observable.interval(3, TimeUnit.SECONDS)
                .observeOn(JavaFxScheduler.platform())
                .subscribe(unused -> updatePanelViews());

        stage.setOnCloseRequest(event -> {
            logger.info("Closed commit window");
            refreshTimer.dispose();
        });
        stage.show();

    }

    public void updatePanelViews() {
        try {
            stagedFilesPanelView.drawDirectoryView();
            allFilesPanelView.drawDirectoryView();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Handler for the commit button. Attempts a commit of the added files,
     * then closes the window and notifies SessionController its done
     */
    public void handleCommitButton() {
        try {
            String messageText = commitMessageField.getText();
            closeWindow();
            BusyWindow.show();
            BusyWindow.setLoadingText("Committing...");
            this.repoHelper.commit(messageText);
        } catch (GitAPIException e) {
            e.printStackTrace();
        } catch (MissingRepoException e) {
            e.printStackTrace();
        } finally {
            BusyWindow.hide();
            // TODO: Need to appropriately register a gitStatus should happen right now
        }
    }

    public void closeWindow() { this.stage.close(); }
}
