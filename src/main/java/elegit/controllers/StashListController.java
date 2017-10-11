package elegit.controllers;

import elegit.CommitHelper;
import elegit.RepoHelper;
import elegit.SessionModel;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Callback;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.StashApplyFailureException;
import org.eclipse.jgit.api.errors.WrongRepositoryStateException;

import java.io.IOException;

public class StashListController {

    private Stage stage;

    private SessionController sessionController;
    private RepoHelper repoHelper;

    @FXML private NotificationController notificationPaneController;
    @FXML private AnchorPane anchorRoot;
    @FXML private ListView<CommitHelper> stashList;
    @FXML private Button applyButton;
    @FXML private Button dropButton;
    @FXML private Button clearButton;
    @FXML private Button popButton;

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

        this.stashList.setCellFactory(new Callback<ListView<CommitHelper>, ListCell<CommitHelper>>() {
            @Override
            public ListCell<CommitHelper> call(ListView<CommitHelper> param) {
                return new ListCell<CommitHelper>() {
                    @Override protected void updateItem(CommitHelper helper, boolean empty) {
                        super.updateItem(helper, empty);
                        Label label = new Label();
                        if(helper == null || empty) { /* do nothing */ }
                        else {
                            label.setText(this.getIndex()+": "+helper.getMessage(true));
                            label.getStyleClass().clear();
                            label.setFont(new Font(12));
                        }
                        setGraphic(label);
                    }
                };
            }
        });
        refreshList();
        setButtonListeners();
        this.stashList.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

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

    /**
     * Makes all the buttons that require a selection only work when there is a selection
     */
    private void setButtonListeners() {
        this.applyButton.disableProperty().bind(this.stashList.getSelectionModel().selectedIndexProperty().lessThan(0));
        this.dropButton.disableProperty().bind(this.stashList.getSelectionModel().selectedIndexProperty().lessThan(0));
        this.popButton.disableProperty().bind(this.stashList.getSelectionModel().selectedIndexProperty().lessThan(0));
    }

    /**
     * Refreshes the list of stashes in the repo
     */
    private void refreshList() {
        try {
            this.stashList.setItems(FXCollections.observableArrayList(repoHelper.stashList()));
        } catch (GitAPIException e) {
            this.notificationPaneController.addNotification("Something went wrong retrieving the stash(es)");
        } catch (IOException e) {
            this.notificationPaneController.addNotification("Something went wrong.");
        }
    }

    /* ********************** BUTTON HANDLERS ********************** */
    public void closeWindow() { this.stage.close(); }

    /**
     * Handles applying a selected stash to the repository
     */
    public void handleApply() {
        String stashRef = this.stashList.getSelectionModel().getSelectedItem().getId();
        try {
            repoHelper.stashApply(stashRef, false);
            sessionController.gitStatus();
        } catch (WrongRepositoryStateException e) {
            notificationPaneController.addNotification("Conflicts occured while trying to apply stash. Commit/stash changes or force apply (right click).");
        } catch (StashApplyFailureException e) {
            showStashConflictsNotification();
        } catch (GitAPIException e) {
            notificationPaneController.addNotification("Something went wrong with the apply. Try committing any uncommitted changes.");
        }
    }

    /**
     * Drops the selected stash
     */
    public void handleDrop() {
        int index = this.stashList.getSelectionModel().getSelectedIndex();
        try {
            repoHelper.stashDrop(index);
            refreshList();
        } catch (GitAPIException e) {
            notificationPaneController.addNotification("Something went wrong with the drop. Try committing any uncommitted changes.");
        }
    }

    /**
     * Clears all the stashes
     *
     * Should this have a warning message?
     */
    public void handleClearStash() {
        try {
            repoHelper.stashClear();
            refreshList();
        } catch (GitAPIException e) {
            notificationPaneController.addNotification("Something went wrong with the clear. Try committing any uncommitted changes.");
        }
    }

    /**
     * Pops off the selected stash (applies and drops it)
     */
    public void handlePop() {
        String stashRef = this.stashList.getSelectionModel().getSelectedItem().getId();
        int index = this.stashList.getSelectionModel().getSelectedIndex();
        try {
            repoHelper.stashApply(stashRef, false);
            repoHelper.stashDrop(index);
            refreshList();
            sessionController.gitStatus();
        }  catch (StashApplyFailureException e) {
            showStashConflictsNotification();
        } catch (GitAPIException e) {
            notificationPaneController.addNotification("Something went wrong with the pop. Try committing any uncommitted changes.");
        }
    }

    void setSessionController(SessionController controller) { this.sessionController = controller; }


    private void showStashConflictsNotification() {
        Platform.runLater(() -> {
            logger.warn("Stash apply conflicts warning");

            EventHandler<MouseEvent> handler = event -> sessionController.quickStashSave();
            this.notificationPaneController.addNotification("You can't apply that stash because there would be conflicts. " +
                    "Stash your changes or resolve conflicts first.", "stash", handler);
        });
    }


}
