package elegit.controllers;

import de.jensd.fx.glyphs.GlyphsDude;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import elegit.*;
import elegit.models.*;
import elegit.treefx.CommitTreeController;
import elegit.treefx.CommitTreeModel;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.api.errors.*;

import java.io.IOException;
import java.util.List;

/**
 *
 * A controller for the BranchManager view that holds all a repository's
 * branches (in the form of BranchHelpers) and manages branch creation,
 * deletion, and tracking of remotes.
 *
 */
public class BranchCheckoutController {

    // All FXML variables are not threadsafe. Only access them from within the FX thread.
    @FXML private ListView<RemoteBranchHelper> remoteListView;
    @FXML private ListView<LocalBranchHelper> localListView;
    @FXML private AnchorPane anchorRoot;
    @FXML private Button trackRemoteBranchButton;
    @FXML private Button checkoutLocalBranchButton;
    @FXML private StackPane notificationPane;
    @FXML private NotificationController notificationPaneController;

    // This is not threadsafe, since it escapes to other portions of the JavaFX view. Only access from FX thread.
    private Stage stage;

    private final SessionModel sessionModel;
    private final SessionController sessionController;
    private final RepoHelper repoHelper;
    private final BranchModel branchModel;
    private final CommitTreeModel localCommitTreeModel;


    private static final Logger logger = LogManager.getLogger();

    public BranchCheckoutController() {
        sessionModel = SessionModel.getSessionModel();
        sessionController = SessionController.getSessionController();
        repoHelper = sessionModel.getCurrentRepoHelper();
        branchModel = repoHelper.getBranchModel();
        localCommitTreeModel = CommitTreeController.getCommitTreeModel();
    }

    public void initialize() throws Exception {

        logger.info("Started up branch manager");

        this.remoteListView.setItems(FXCollections.observableArrayList(branchModel.getRemoteBranchesTyped()));
        this.localListView.setItems(FXCollections.observableArrayList(branchModel.getLocalBranchesTyped()));

        // Local list view can select multiple (for merges):
        this.localListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        this.remoteListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        this.setIcons();
        this.updateButtons();

        this.notificationPaneController.bindParentBounds(anchorRoot.heightProperty());
    }

    /**
     * A helper method that sets the icons and colors for buttons
     */
    private void setIcons() {
        Text cloudDownIcon = GlyphsDude.createIcon(FontAwesomeIcon.CLOUD_DOWNLOAD);
        cloudDownIcon.setFill(Color.WHITE);
        this.trackRemoteBranchButton.setGraphic(cloudDownIcon);
    }

    /**
     * Shows the branch manager
     * @param pane AnchorPane root
     */
    void showStage(AnchorPane pane) {
        Main.assertFxThread();
        anchorRoot = pane;
        stage = new Stage();
        stage.setTitle("Branch Checkout");
        stage.setScene(new Scene(anchorRoot, 550, 450));
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setOnCloseRequest(event -> logger.info("Closed branch manager window"));
        stage.setOnHiding(e -> {
            notificationPaneController.hideBubbleInstantly();
        });
        stage.show();
        this.notificationPaneController.setAnchor(stage);
    }

    /**
     * Closes the branch manager
     */
    public synchronized void closeWindow() {
        Main.assertFxThread();
        stage.close();
    }

    /**
     * Handles a mouse click on the remote list view
     */
    public void handleRemoteListViewMouseClick() {
        if (!localListView.getSelectionModel().isEmpty()) {
            localListView.getSelectionModel().clearSelection();
        }
        this.updateButtons();
    }

    /**
     * Handles a mouse click on the local list view
     */
    public void handleLocalListViewMouseClick() {
        if (!remoteListView.getSelectionModel().isEmpty()) {
            remoteListView.getSelectionModel().clearSelection();
        }
        this.updateButtons();
    }

    /**
     * Updates the track remote, merge, and delete local buttons'
     * text and/or disabled/enabled status.
     */
    private void updateButtons() {

        // Update delete button
        if (this.localListView.getSelectionModel().getSelectedIndices().size() > 0) {
            this.checkoutLocalBranchButton.setDisable(false);
            // But keep trackRemoteBranchButton disabled
            this.trackRemoteBranchButton.setDisable(true);
        }

        // Update track button
        if (this.remoteListView.getSelectionModel().getSelectedIndices().size() > 0) {
            this.trackRemoteBranchButton.setDisable(false);
            // But keep the other buttons disabled
            this.checkoutLocalBranchButton.setDisable(true);
        }
    }

    /**
     * Tracks the selected branch (in the remoteListView) locally.
     *
     * @throws GitAPIException if the git tracking goes wrong
     * @throws IOException if writing to local directory fails
     */
    public void trackSelectedBranchLocally() throws GitAPIException, IOException {
        logger.info("Track remote branch locally button clicked");
        RemoteBranchHelper selectedRemoteBranch = this.remoteListView.getSelectionModel().getSelectedItem();
        sessionController.updateCommandText("git checkout --track "+selectedRemoteBranch.getRefName());
        try {
            if (selectedRemoteBranch != null) {
                LocalBranchHelper tracker = this.branchModel.trackRemoteBranch(selectedRemoteBranch);
                this.localListView.getItems().add(tracker);
                CommitTreeController.setBranchHeads(this.localCommitTreeModel, this.repoHelper);
            }
        } catch (RefAlreadyExistsException e) {
            logger.warn("Branch already exists locally warning");
            this.showRefAlreadyExistsNotification();
        }
    }

    /**
     * Checks out the selected local branch
     * @param selectedBranch the local branch to check out
     * @param theSessionModel the session model for resetting branch heads
     * @return true if the checkout successfully happens, false if there is an error
     */
    private boolean checkoutBranch(LocalBranchHelper selectedBranch, SessionModel theSessionModel) {
        if(selectedBranch == null) return false;
        try {
            sessionController.updateCommandText("git checkout "+selectedBranch.getRefName());
            selectedBranch.checkoutBranch();
            CommitTreeController.focusCommitInGraph(selectedBranch.getCommit());
            CommitTreeController.setBranchHeads(CommitTreeController.getCommitTreeModel(), theSessionModel.getCurrentRepoHelper());
            return true;
        } catch (JGitInternalException e){
            showJGitInternalError(e);
        } catch (CheckoutConflictException e){
            showCheckoutConflictsNotification(e.getConflictingPaths());
        } catch (GitAPIException e) {
            e.printStackTrace();
            showGenericErrorNotification();
        }
        return false;
    }

    public void handleCheckoutButton() {
        if (checkoutBranch(localListView.getSelectionModel().getSelectedItem(), sessionModel))
            closeWindow();
    }

    /// BEGIN: ERROR NOTIFICATIONS:

    private void showGenericErrorNotification() {
        logger.warn("Generic error notification");
        notificationPaneController.addNotification("Sorry, there was an error.");
    }

    private void showCannotDeleteBranchNotification(LocalBranchHelper branch) {
        logger.warn("Cannot delete current branch notification");
        notificationPaneController.addNotification(String.format("Sorry, %s can't be deleted right now. " +
                "Try checking out a different branch first.", branch.getRefName()));
    }

    private void showJGitInternalError(JGitInternalException e) {
        Platform.runLater(()-> {
            if (e.getCause().toString().contains("LockFailedException")) {
                logger.warn("Lock failed warning.");
                notificationPaneController.addNotification("Cannot lock .git/index. If no other git processes are running, manually remove all .lock files.");
            } else {
                logger.warn("Generic jgit internal warning.");
                notificationPaneController.addNotification("Sorry, there was a Git error.");
            }
        });
    }

    private void showRefAlreadyExistsNotification() {
        logger.info("Branch already exists notification");
        notificationPaneController.addNotification("Looks like that branch already exists locally!");
    }

    private void showCheckoutConflictsNotification(List<String> conflictingPaths) {
        Platform.runLater(() -> {
            logger.warn("Checkout conflicts warning");
            notificationPaneController.addNotification("You can't switch to that branch because there would be a merge conflict. Stash your changes or resolve conflicts first.");

            /*
            Action seeConflictsAction = new Action("See conflicts", e -> {
                anchorRoot.hide();
                PopUpWindows.showCheckoutConflictsAlert(conflictingPaths);
            });*/
        });
    }

    /// END: ERROR NOTIFICATIONS ^^^
}
