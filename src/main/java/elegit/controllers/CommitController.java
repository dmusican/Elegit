package elegit.controllers;

import elegit.*;
import elegit.exceptions.MissingRepoException;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Task;
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

    private Stage stage;
    private SessionModel sessionModel;
    private RepoHelper repoHelper;
    @FXML
    public Button commitButton;
    @FXML
    public TextArea commitMessageField;
    @FXML
    public StagedTreePanelView stagedFilesPanelView;
    @FXML
    public AllFilesPanelView allFilesPanelView;

    public BooleanProperty isClosed;
    private Thread refresher;
    private Disposable refreshTimer;

    static final Logger logger = LogManager.getLogger();

    static SessionController sessionController;

    /**
     * Initialize method automatically called by JavaFX
     *
     * Sets up views and buttons
     */
    public void initialize(){
        logger.info("Started up branch manager");

        isClosed = new SimpleBooleanProperty(false);

        this.sessionModel = SessionModel.getSessionModel();
        this.repoHelper = this.sessionModel.getCurrentRepoHelper();
        this.commitMessageField.requestFocus();

        this.stagedFilesPanelView.setSessionModel(this.sessionModel);
        this.allFilesPanelView.setSessionModel(this.sessionModel);

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
        stage.setOnCloseRequest(event -> {
            logger.info("Closed commit window");
            //refresher.interrupt();
            refreshTimer.dispose();
        });
        stage.show();

        refreshTimer = Observable.interval(3, TimeUnit.SECONDS)
                .observeOn(JavaFxScheduler.platform())
                .subscribe(unused -> updatePanelViews());
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
            isClosed.setValue(true);
        }
    }

    public void closeWindow() { this.stage.close(); }
}
