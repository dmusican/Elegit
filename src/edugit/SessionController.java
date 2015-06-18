package edugit;

import javafx.event.ActionEvent;
import javafx.scene.control.*;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.lib.Ref;

import java.io.IOException;
import java.util.*;

/**
 * The controller for the entire session.
 */
public class SessionController extends Controller {

    public ComboBox<RepoHelper> repositorySelector;
    public ComboBox<String> branchSelector;
    public Button loadBranchButton;
    private SessionModel theModel;

    public Button gitStatusButton;
    public Button commitButton;
    public Button mergeFromFetchButton;
    public Button pushButton;
    public Button fetchButton;

    public MenuBar menuBar;
    public TextArea commitMessageField;
    public WorkingTreePanelView workingTreePanelView;
	public CommitTreePanelView localCommitTreePanelView;
    public CommitTreePanelView remoteCommitTreePanelView;

    CommitTreeModel localCommitTreeModel;
    CommitTreeModel remoteCommitTreeModel;

    /**
     * Initializes the environment by obtaining the model
     * and putting the views on display.
     *
     * This method is automatically called by JavaFX.
     */
    public void initialize() {
        this.theModel = SessionModel.getSessionModel();
        this.workingTreePanelView.setSessionModel(this.theModel);
        this.localCommitTreeModel = new LocalCommitTreeModel(this.theModel, this.localCommitTreePanelView);
        this.remoteCommitTreeModel = new RemoteCommitTreeModel(this.theModel, this.remoteCommitTreePanelView);

        // Buttons start out disabled, since no repo is loaded
        this.setButtonsDisabled(true);

        // Branch selector and trigger button starts invisible, since there's no repo and no branches
        this.branchSelector.setVisible(false);
        this.loadBranchButton.setVisible(false);

        this.initializeMenuBar();
        this.updateRepoDropdown();
    }

    private void updateRepoDropdown() {
        // TODO: let users *delete* repos from this list
        ArrayList<RepoHelper> items = this.theModel.getAllRepoHelpers();
        this.repositorySelector.getItems().setAll(items);

        if (items.size() != 0) {
            // Load and display the most recently added value
            this.theModel.openRepoFromHelper(items.get(items.size()-1));
            this.repositorySelector.setValue(items.get(items.size() - 1));
        }
    }

    private void updateBranchDropdown() throws GitAPIException, IOException {
        this.branchSelector.setVisible(true);
        this.loadBranchButton.setVisible(true);

        List<String> branches = this.theModel.getCurrentRepoHelper().getLocalBranchNames();
        this.branchSelector.getItems().setAll(branches);

        if (branches.size() != 0) {
            // Set the dropdown to be selecting the current branch
            String currentBranchName = this.theModel.getCurrentRepoHelper().getCurrentBranchName();
            this.branchSelector.setValue(currentBranchName);
        }
    }

    /**
     * Sets up the MenuBar by adding some options to it (for cloning).
     *
     * Each option offers a different way of loading a repository, and each
     * option instantiates the appropriate RepoHelper class for the chosen
     * loading method.
     *
     * Since each option creates a new repo, this method handles errors.
     */
    private void initializeMenuBar() {
        Menu openMenu = new Menu("Load a Repository");

        MenuItem cloneOption = new MenuItem("Clone");
        cloneOption.setOnAction(t -> {
            try {
                ClonedRepoHelperBuilder builder = new ClonedRepoHelperBuilder(this.theModel);
                RepoHelper repoHelper = builder.getRepoHelperFromDialogs(); // this creates and sets the RepoHelper

                this.theModel.openRepoFromHelper(repoHelper);

                // After loading (cloning) a repo, activate the buttons
                this.setButtonsDisabled(false);

                this.updateRepoDropdown();
                this.updateBranchDropdown();
            } catch (IllegalArgumentException e) {
                ERROR_ALERT_CONSTANTS.invalidRepo().showAndWait();
            } catch (JGitInternalException e) {
                ERROR_ALERT_CONSTANTS.nonemptyFolder().showAndWait();
            } catch (InvalidRemoteException e) {
                ERROR_ALERT_CONSTANTS.invalidRemote().showAndWait();
            } catch (TransportException e) {
                ERROR_ALERT_CONSTANTS.notAuthorized().showAndWait();
                // FIXME: TransportExceptions don't *only* indicate a permissions issue... Figure out what else they do
            } catch (NullPointerException e) {
                ERROR_ALERT_CONSTANTS.notLoggedIn().showAndWait();
                e.printStackTrace();

                // Re-prompt the user to log in:
                this.theModel.getOwner().presentLoginDialogsToSetValues();
            } catch (Exception e) {
                // The generic error is totally unhelpful, so try not to ever reach this catch statement
                ERROR_ALERT_CONSTANTS.genericError().showAndWait();
                e.printStackTrace();
            }
        });

        MenuItem existingOption = new MenuItem("Load existing repository");
        existingOption.setOnAction(t -> {
            ExistingRepoHelperBuilder builder = new ExistingRepoHelperBuilder(this.theModel);
            try {
                RepoHelper repoHelper = builder.getRepoHelperFromDialogs();
                this.theModel.openRepoFromHelper(repoHelper);

                // After loading a repo, activate the buttons
                this.setButtonsDisabled(false);

                this.updateRepoDropdown();
                this.updateBranchDropdown();
            } catch (IllegalArgumentException e) {
                ERROR_ALERT_CONSTANTS.invalidRepo().showAndWait();
            } catch (NullPointerException e) {
                ERROR_ALERT_CONSTANTS.repoWasNotLoaded().showAndWait();
                e.printStackTrace();
            } catch (Exception e) {
                ERROR_ALERT_CONSTANTS.genericError().showAndWait();
                System.out.println("***** FIGURE OUT WHY THIS EXCEPTION IS NEEDED *******");
                e.printStackTrace();
            }
        });

        // TODO: implement New Repository option.
        MenuItem newOption = new MenuItem("Start a new repository");
        newOption.setDisable(true);

        openMenu.getItems().addAll(cloneOption, existingOption, newOption);
        menuBar.getMenus().addAll(openMenu);
    }

    /**
     * Perform the updateFileStatusInRepo() method for each file whose
     * checkbox is checked. Then commit with the commit message and push.
     *
     * TODO: Separate push into different button. Work this in with the local tree view.
     *
     * @param actionEvent the button click event.
     * @throws GitAPIException if the updateFileStatusInRepo() call fails.
     * @throws IOException if the loadPanelViews() fails.
     */ //todo: since there's a try/catch, should this method signature not throw exceptions?
    public void handleCommitButton(ActionEvent actionEvent) throws GitAPIException, IOException {
        try {
            String commitMessage = commitMessageField.getText();

            for (RepoFile checkedFile : this.workingTreePanelView.getCheckedFilesInDirectory()) {
                checkedFile.updateFileStatusInRepo();
            }

            this.theModel.currentRepoHelper.commit(commitMessage);

            // Now clear the commit text and a view reload ( or `git status`) to show that something happened
            commitMessageField.clear();
            this.loadPanelViews();
        } catch (NullPointerException e) {
            ERROR_ALERT_CONSTANTS.noRepoLoaded().showAndWait();
        } catch (TransportException e) {
            ERROR_ALERT_CONSTANTS.notAuthorized().showAndWait();
            // FIXME: TransportExceptions don't *only* indicate a permissions issue... Figure out what else they do
        }

    }

    public void handleMergeFromFetchButton(ActionEvent actionEvent) throws IOException, GitAPIException {
        try {
            this.theModel.currentRepoHelper.mergeFromFetch();
            // Refresh panel views
            this.loadPanelViews();
        } catch (NullPointerException e) {
            ERROR_ALERT_CONSTANTS.noRepoLoaded().showAndWait();
        } catch (TransportException e) {
            ERROR_ALERT_CONSTANTS.notAuthorized().showAndWait();
            // FIXME: TransportExceptions don't *only* indicate a permissions issue... Figure out what else they do
        }

    }

    public void handlePushButton(ActionEvent actionEvent) throws GitAPIException, IOException {
        try {
            this.theModel.currentRepoHelper.pushAll();

            // Refresh panel views
            this.loadPanelViews();
        } catch (NullPointerException e) {
            ERROR_ALERT_CONSTANTS.noRepoLoaded().showAndWait();
        } catch (TransportException e) {
            ERROR_ALERT_CONSTANTS.notAuthorized().showAndWait();
            // FIXME: TransportExceptions don't *only* indicate a permissions issue... Figure out what else they do
        }
    }

    public void handleFetchButton(ActionEvent actionEvent) throws GitAPIException, IOException {
        try {
            this.theModel.currentRepoHelper.fetch();
            // Refresh panel views
            this.loadPanelViews();
        } catch (NullPointerException e) {
            ERROR_ALERT_CONSTANTS.noRepoLoaded().showAndWait();
        } catch (TransportException e) {
            ERROR_ALERT_CONSTANTS.notAuthorized().showAndWait();
            e.printStackTrace();
            // FIXME: TransportExceptions don't *only* indicate a permissions issue... Figure out what else they do
        }
    }

    /**
     * Loads the panel views when the "git status" button is clicked.
     *
     * TODO: Implement automatic refresh!
     *
     * @throws GitAPIException if the drawDirectoryView() call fails.
     * @throws IOException if the drawDirectoryView() call fails.
     */
    public void loadPanelViews() throws GitAPIException, IOException{
        try {
            RepoHelper selectedRepoHelper = this.repositorySelector.getValue();
            this.theModel.openRepoFromHelper(selectedRepoHelper);

            this.workingTreePanelView.drawDirectoryView();
            this.localCommitTreeModel.update();
            this.remoteCommitTreeModel.update();

            this.updateBranchDropdown();
        } catch (NullPointerException e) {
            ERROR_ALERT_CONSTANTS.noRepoLoaded().showAndWait();
        }
    }

    private void setButtonsDisabled(boolean disable) {
        gitStatusButton.setDisable(disable);
        commitButton.setDisable(disable);
        mergeFromFetchButton.setDisable(disable);
        pushButton.setDisable(disable);
        fetchButton.setDisable(disable);
    }

    /**
     *
     * @param actionEvent
     * @throws GitAPIException
     * @throws IOException from updateBranchDropdown()
     */
    public void loadSelectedBranch(ActionEvent actionEvent) throws GitAPIException, IOException {
        String branchName = this.branchSelector.getValue();
        this.theModel.getCurrentRepoHelper().checkoutBranch(branchName);
        this.updateBranchDropdown();
    }
}
