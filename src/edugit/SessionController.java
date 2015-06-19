package edugit;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.text.Text;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.api.errors.TransportException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * The controller for the entire session.
 */
public class SessionController extends Controller {

    public ComboBox<String> branchSelector;
    public Text currentRepoLabel;
    private SessionModel theModel;

    public Button gitStatusButton;
    public Button commitButton;
    public Button mergeFromFetchButton;
    public Button pushButton;
    public Button fetchButton;

    public TextArea commitMessageField;
    public WorkingTreePanelView workingTreePanelView;
	public CommitTreePanelView localCommitTreePanelView;
    public CommitTreePanelView remoteCommitTreePanelView;

    CommitTreeModel localCommitTreeModel;
    CommitTreeModel remoteCommitTreeModel;

    // The menu bar
    public MenuBar menuBar;
    private Menu newRepoMenu;
    private Menu openRecentRepoMenu;

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

        this.initializeMenuBar();
    }

    private void updateBranchDropdown() throws GitAPIException, IOException {
        this.branchSelector.setVisible(true);

        List<String> branches = this.theModel.getCurrentRepoHelper().getLocalBranchNames();
        this.branchSelector.getItems().setAll(branches);

        // TODO: Unify branch name display:
        //      getCurrentBranchName() gives just the name,
        //      but the list is populated with "ref/head/master" etc.
        //  make a BranchHelper?
        String currentBranchName = this.theModel.getCurrentRepoHelper().getCurrentBranchName();
        this.branchSelector.setValue(currentBranchName);
    }

    /**
     * Sets up the MenuBar by adding some options to it (for cloning).
     *
     * Each option offers a different way of loading a repository, and each
     * option instantiates the appropriate RepoHelper class for the chosen
     * loading method.
     *
     * Since each option creates a new repo, this method handles errors.
     *
     * TODO: split this method up or something. it's getting too big?
     */
    private void initializeMenuBar() {
        this.newRepoMenu = new Menu("Load new Repository");

        MenuItem cloneOption = new MenuItem("Clone");
        cloneOption.setOnAction(t -> {
            try {
                ClonedRepoHelperBuilder builder = new ClonedRepoHelperBuilder(this.theModel);
                RepoHelper repoHelper = builder.getRepoHelperFromDialogs(); // this creates and sets the RepoHelper

                this.theModel.openRepoFromHelper(repoHelper);

                // After loading (cloning) a repo, activate the buttons and update stuff
                this.setButtonsDisabled(false);

                this.initPanelViews();
                this.updateBranchDropdown();
                this.updateMenuBarWithRecentRepos();
                this.updateCurrentRepoLabel();
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

                // After loading a repo, activate the buttons and update stuff
                this.setButtonsDisabled(false);

                this.initPanelViews();
                this.updateBranchDropdown();
                this.updateMenuBarWithRecentRepos();
                this.updateCurrentRepoLabel();
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

        this.newRepoMenu.getItems().addAll(cloneOption, existingOption, newOption);

        this.openRecentRepoMenu = new Menu("Open recent repository");
        MenuItem noOptionsAvailable = new MenuItem("No recent repositories");
        noOptionsAvailable.setDisable(true);
        this.openRecentRepoMenu.getItems().add(noOptionsAvailable);

        this.menuBar.getMenus().addAll(newRepoMenu, openRecentRepoMenu);
    }

    private void updateMenuBarWithRecentRepos() throws GitAPIException, IOException {
        this.openRecentRepoMenu.getItems().clear();

        ArrayList<RepoHelper> repoHelpers = this.theModel.getAllRepoHelpers();
        for (RepoHelper repoHelper : repoHelpers) {
            MenuItem recentRepoHelperMenuItem = new MenuItem(repoHelper.toString());
            recentRepoHelperMenuItem.setOnAction(t -> {
                this.theModel.openRepoFromHelper(repoHelper);

                // Updates
                this.setButtonsDisabled(false);
                try {
                    this.updateBranchDropdown();
                } catch (GitAPIException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                this.updateCurrentRepoLabel();
            });
            openRecentRepoMenu.getItems().add(recentRepoHelperMenuItem);
        }

        this.menuBar.getMenus().clear();
        this.menuBar.getMenus().addAll(this.newRepoMenu, this.openRecentRepoMenu);
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
            this.reloadAllViews();
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
            this.reloadAllViews();
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
            this.reloadAllViews();
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
            this.reloadAllViews();
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
    public void reloadAllViews() throws GitAPIException, IOException{
        try {
            this.workingTreePanelView.drawDirectoryView();
            this.localCommitTreeModel.update();
            this.remoteCommitTreeModel.update();

            this.updateBranchDropdown();
        } catch (NullPointerException e) {
            ERROR_ALERT_CONSTANTS.noRepoLoaded().showAndWait();
        }
    }

    private void initPanelViews() throws GitAPIException, IOException{
        this.workingTreePanelView.drawDirectoryView();
        this.localCommitTreeModel.init();
        this.remoteCommitTreeModel.init();
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
    }

    private void updateCurrentRepoLabel() {
        String name = this.theModel.getCurrentRepoHelper().toString();
        this.currentRepoLabel.setText(name);
    }
}
