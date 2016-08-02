package elegit;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.controlsfx.control.PopOver;
import org.eclipse.jgit.api.errors.*;

import java.io.IOException;
import java.util.List;

/**
 * Controller for the create/delete branch window
 */
public class CreateDeleteBranchWindowController {

    @FXML private AnchorPane anchorRoot;
    @FXML private CheckBox checkoutCheckBox;
    @FXML private TextArea newBranchTextArea;
    @FXML private ComboBox<LocalBranchHelper> localBranchesDropdown;
    @FXML private Button createButton;
    @FXML private Button deleteButton;
    @FXML private StackPane notificationPane;
    @FXML private NotificationController notificationPaneController;

    private Stage stage;
    SessionModel sessionModel;
    RepoHelper repoHelper;
    private BranchModel branchModel;
    private CommitTreeModel localCommitTreeModel;

    static final Logger logger = LogManager.getLogger();

    /**
     * Initialize method called automatically by JavaFX
     */
    public void initialize() {
        sessionModel = SessionModel.getSessionModel();
        repoHelper = sessionModel.getCurrentRepoHelper();
        branchModel = repoHelper.getBranchModel();
        refreshLocalBranchesDropdown();
        localBranchesDropdown.setPromptText("Select a branch...");
        newBranchTextArea.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
        createButton.setDisable(true);
        deleteButton.setDisable(true);

        newBranchTextArea.textProperty().addListener(((observable, oldValue, newValue) -> {
            if(newValue.equals("")) {
                createButton.setDisable(true);
            }else {
                createButton.setDisable(false);
            }
        }));
        localBranchesDropdown.getSelectionModel().selectedIndexProperty().addListener(((observable, oldValue, newValue) -> {
            if((int) newValue == -1) {
                deleteButton.setDisable(true);
            }else {
                deleteButton.setDisable(false);
            }
        }));

        // Get the current commit tree models
        localCommitTreeModel = CommitTreeController.getCommitTreeModel();
    }

    /**
     * helper method to update branch dropdown
     */
    private void refreshLocalBranchesDropdown() {
        localBranchesDropdown.setItems(FXCollections.observableArrayList(branchModel.getLocalBranchesTyped()));
    }

    /**
     * shows the window
     * @param pane AnchorPane root
     */
    void showStage(AnchorPane pane) {
        anchorRoot = pane;
        stage = new Stage();
        stage.setTitle("Create or delete branch");
        stage.setScene(new Scene(anchorRoot));
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setOnCloseRequest(event -> logger.info("Closed create/delete branch window"));
        stage.show();
    }

    /**
     * closes the window
     */
    public void closeWindow() {
        stage.close();
    }

    public void handleCreateBranch() {
        createNewBranch(newBranchTextArea.getText(), checkoutCheckBox.isSelected());
    }

    /**
     * Helper method that creates a new branch, and checks it out sometimes
     * @param branchName String
     * @param checkout boolean
     */
    private void createNewBranch(String branchName, boolean checkout) {
        Thread th = new Thread(new Task<Void>() {
            @Override
            protected Void call() {
                LocalBranchHelper newBranch = null;
                try {
                    logger.info("New branch button clicked");
                    newBranch = branchModel.createNewLocalBranch(branchName);

                    updateUser(" created ");

                } catch (InvalidRefNameException e1) {
                    logger.warn("Invalid branch name warning");
                    showInvalidBranchNameNotification();
                } catch (RefNotFoundException e1) {
                    // When a repo has no commits, you can't create branches because there
                    //  are no commits to point to. This error gets raised when git can't find
                    //  HEAD.
                    logger.warn("Can't create branch without a commit in the repo warning");
                    showNoCommitsYetNotification();
                } catch (GitAPIException e1) {
                    logger.warn("Git error");
                    logger.debug(e1.getStackTrace());
                    showGenericGitErrorNotification();
                    e1.printStackTrace();
                } catch (IOException e1) {
                    logger.warn("Unspecified IOException");
                    logger.debug(e1.getStackTrace());
                    showGenericErrorNotification();
                    e1.printStackTrace();
                }finally {
                    refreshLocalBranchesDropdown();
                }
                if(checkout) {
                    if(newBranch != null) {
                        checkoutBranch(newBranch, sessionModel);
                    }
                }
                return null;
            }
        });
        th.setDaemon(true);
        th.setName("createNewBranch");
        th.start();
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
            selectedBranch.checkoutBranch();
            CommitTreeController.focusCommitInGraph(selectedBranch.getHead());
            CommitTreeController.setBranchHeads(CommitTreeController.getCommitTreeModel(), theSessionModel.getCurrentRepoHelper());
            return true;
        } catch (JGitInternalException e){
            showJGitInternalError(e);
        } catch (CheckoutConflictException e){
            showCheckoutConflictsNotification(e.getConflictingPaths());
        } catch (GitAPIException | IOException e) {
            showGenericErrorNotification();
        }
        return false;
    }

    /**
     * Deletes the selected branch.
     */
    public void handleDeleteBranch() {
        logger.info("Delete branches button clicked");
        LocalBranchHelper selectedBranch = localBranchesDropdown.getSelectionModel().getSelectedItem();

            try {
                if (selectedBranch != null) {
                    // Local delete:
                    this.branchModel.deleteLocalBranch(selectedBranch);
                    refreshLocalBranchesDropdown();

                    // Reset the branch heads
                    CommitTreeController.setBranchHeads(localCommitTreeModel, repoHelper);

                    updateUser(" deleted ");
                }
            } catch (NotMergedException e) {
                logger.warn("Can't delete branch because not merged warning");
                Platform.runLater(() -> {
                    if(PopUpWindows.showForceDeleteBranchAlert()) {
                        forceDeleteBranch(selectedBranch);
                    }
                });
                this.showNotMergedNotification(selectedBranch);
            } catch (CannotDeleteCurrentBranchException e) {
                logger.warn("Can't delete current branch warning");
                this.showCannotDeleteBranchNotification(selectedBranch);
            } catch (GitAPIException e) {
                logger.warn("Git error");
                this.showGenericGitErrorNotificationWithBranch(selectedBranch);
            }finally {
                refreshLocalBranchesDropdown();
            }
        // TODO: add optional delete from remote, too.
        // see http://stackoverflow.com/questions/11892766/how-to-remove-remote-branch-with-jgit
    }

    /**
     * force deletes a branch
     * @param branchToDelete LocalBranchHelper
     */
    private void forceDeleteBranch(LocalBranchHelper branchToDelete) {
        logger.info("Deleting local branch");

        try {
            if (branchToDelete != null) {
                // Local delete:
                branchModel.forceDeleteLocalBranch(branchToDelete);

                // Reset the branch heads
                CommitTreeController.setBranchHeads(localCommitTreeModel, repoHelper);

                updateUser(" deleted ");
            }
        } catch (CannotDeleteCurrentBranchException e) {
            logger.warn("Can't delete current branch warning");
            this.showCannotDeleteBranchNotification(branchToDelete);
        } catch (GitAPIException e) {
            logger.warn("Git error");
            this.showGenericGitErrorNotificationWithBranch(branchToDelete);
            e.printStackTrace();
        }finally {
            refreshLocalBranchesDropdown();
        }
    }

    /**
     * helper method that informs the user their action was successful
     * @param type String
     */
    private void updateUser(String type) {
        Platform.runLater(() -> {
            Text txt = new Text(" Branch" + type);
            PopOver popOver = new PopOver(txt);
            popOver.setTitle("");
            if(type.equals(" created ")) {
                popOver.show(createButton);
                popOver.detach();
                newBranchTextArea.clear();
                checkoutCheckBox.setSelected(false);
            }else {
                popOver.show(deleteButton);
                popOver.detach();
                localBranchesDropdown.getSelectionModel().clearSelection();
            }


        });
    }

    //**************** BEGIN ERROR NOTIFICATIONS***************************

    private void showInvalidBranchNameNotification() {
        Platform.runLater(() -> {
            logger.warn("Invalid branch name notification");
            notificationPaneController.addNotification("That branch name is invalid.");
        });
    }

    private void showNoCommitsYetNotification() {
        Platform.runLater(() -> {
            logger.warn("No commits yet notification");
            notificationPaneController.addNotification("You cannot make a branch since your repo has no commits yet. Make a commit first!");
        });
    }

    private void showGenericGitErrorNotification() {
        Platform.runLater(() -> {
            logger.warn("Git error notification");
            notificationPaneController.addNotification("Sorry, there was a git error.");
        });
    }

    private void showGenericErrorNotification() {
        Platform.runLater(()-> {
            logger.warn("Generic error warning.");
            notificationPaneController.addNotification("Sorry, there was an error.");
        });
    }

    private void showCannotDeleteBranchNotification(LocalBranchHelper branch) {
        Platform.runLater(() -> {
            logger.warn("Cannot delete current branch notification");
            notificationPaneController.addNotification(String.format("Sorry, %s can't be deleted right now. " +
                    "Try checking out a different branch first.", branch.getBranchName()));
        });
    }

    private void showGenericGitErrorNotificationWithBranch(LocalBranchHelper branch) {
        Platform.runLater(() -> {
            logger.warn("Git error on branch notification");
            notificationPaneController.addNotification(String.format("Sorry, there was a git error on branch %s.", branch.getBranchName()));
        });
    }

    private void showNotMergedNotification(LocalBranchHelper nonmergedBranch) {
        logger.warn("Not merged notification");
        notificationPaneController.addNotification("That branch has to be merged before you can do that.");

        /*
        Action forceDeleteAction = new Action("Force delete", e -> {
            this.forceDeleteBranch(nonmergedBranch);
            anchorPane.hide();
        });*/
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


    //**************** END ERROR NOTIFICATIONS***************************
}
