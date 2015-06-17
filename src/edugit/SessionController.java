package edugit;

import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.scene.text.Text;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.JGitInternalException;

import java.io.IOException;

/**
 * Created by makik on 6/10/15.
 */
public class SessionController extends Controller {

    private SessionModel theModel;

    public Text repoNameText;

    public Button gitStatusButton;
    public Button commitButton;
    public Button mergeButton;
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

        this.initializeMenuBar();
    }

    /**
     * Sets up the MenuBar by adding some options to it (for cloning).
     *
     * Each option offers a different way of loading a repository, and each
     * option instantiates the appropriate RepoHelper class for the chosen
     * loading method.
     */
    private void initializeMenuBar() {
        Menu openMenu = new Menu("Load a Repository");

        MenuItem cloneOption = new MenuItem("Clone");
        cloneOption.setOnAction(t -> {
            try {
                ClonedRepoHelperBuilder builder = new ClonedRepoHelperBuilder(this.theModel);
                RepoHelper repoHelper = builder.getRepoHelperFromDialogs(); // this creates and sets the RepoHelper

                this.theModel.openRepoFromHelper(repoHelper);

                this.repoNameText.setText(this.theModel.getCurrentRepoHelper().getDirectory().getFileName().toString());

                // After loading (cloning) a repo, activate the buttons
                this.setButtonsDisabled(false);
            } catch (IllegalArgumentException e) {
                ERROR_ALERT_CONSTANTS.invalidRepo().showAndWait();
            } catch (JGitInternalException e) {
                ERROR_ALERT_CONSTANTS.nonemptyFolder().showAndWait();
            } catch (InvalidRemoteException e) {
                ERROR_ALERT_CONSTANTS.invalidRemote().showAndWait();
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
                this.repoNameText.setText(this.theModel.getCurrentRepoHelper().getDirectory().getFileName().toString());

                // After loading a repo, activate the buttons
                this.setButtonsDisabled(false);
            } catch (IllegalArgumentException e) {
                ERROR_ALERT_CONSTANTS.invalidRepo().showAndWait();
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
     */
    public void handleCommitButton(ActionEvent actionEvent) throws GitAPIException, IOException {
        try {
            String commitMessage = commitMessageField.getText();

            for (RepoFile checkedFile : this.workingTreePanelView.getCheckedFilesInDirectory()) {
                checkedFile.updateFileStatusInRepo();
            }

            this.theModel.currentRepoHelper.commit(commitMessage);
            this.theModel.currentRepoHelper.pushAll();

            // Now clear the commit text and a view reload ( or `git status`) to show that something happened
            commitMessageField.clear();
            this.loadPanelViews();
        } catch (NullPointerException e) {
            ERROR_ALERT_CONSTANTS.noRepoLoaded().showAndWait();
        } catch (org.eclipse.jgit.api.errors.TransportException e) {
            ERROR_ALERT_CONSTANTS.notAuthorized().showAndWait();
            // FIXME: TransportExceptions don't *only* indicate a permissions issue... Figure out what else they do
        }

    }

    public void handleMergeButton(ActionEvent actionEvent){
    }

    public void handlePushButton(ActionEvent actionEvent){
    }

    public void handleFetchButton(ActionEvent actionEvent){

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
            this.workingTreePanelView.drawDirectoryView();
            this.localCommitTreeModel.update();
            this.remoteCommitTreeModel.update();
        } catch (NullPointerException e) {
            ERROR_ALERT_CONSTANTS.noRepoLoaded().showAndWait();
        }
    }

    private void setButtonsDisabled(boolean disable) {
        gitStatusButton.setDisable(disable);
        commitButton.setDisable(disable);
        mergeButton.setDisable(disable);
        pushButton.setDisable(disable);
        fetchButton.setDisable(disable);
    }
}
