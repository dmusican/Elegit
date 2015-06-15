package edugit;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.*;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.File;
import java.io.IOException;

/**
 * Created by makik on 6/10/15.
 */
public class SessionController extends Controller {

    private SessionModel theModel;

    public MenuBar menuBar;
    public TextArea commitMessageField;
    public WorkingTreePanelView workingTreePanelView;
    public LocalPanelView localPanelView;
    public RemotePanelView remotePanelView;

    /**
     * Initializes the environment by obtaining the model
     * and putting the views on display.
     *
     * This method is automatically called by JavaFX.
     */
    public void initialize() {
        this.theModel = SessionModel.getSessionModel();
        this.workingTreePanelView.setSessionModel(this.theModel);
        this.localPanelView.setSessionModel(this.theModel);
        this.remotePanelView.setSessionModel(this.theModel);

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
        // TODO: break this out into a separate controller?
        Menu openMenu = new Menu("Load a Repository");

        MenuItem cloneOption = new MenuItem("Clone");

        // TODO: understand lambda expressions...
        cloneOption.setOnAction(t -> {
            File cloneRepoDirectory = getPathFromChooser(true, "Choose a Location", null);
            try{
                RepoHelper repoHelper = new ClonedRepoHelper(cloneRepoDirectory.toPath(), SECRET_CONSTANTS.TEST_GITHUB_TOKEN);
                SessionModel.getSessionModel().openRepoFromHelper(repoHelper);
            } catch(Exception e){
                e.printStackTrace();
            }
        });

        MenuItem existingOption = new MenuItem("Load existing repository");
        existingOption.setOnAction(t -> {
            File existingRepoDirectory = getPathFromChooser(true, "Choose a Location", null);
            try{
                RepoHelper repoHelper = new ExistingRepoHelper(existingRepoDirectory.toPath(), SECRET_CONSTANTS.TEST_GITHUB_TOKEN);
                SessionModel.getSessionModel().openRepoFromHelper(repoHelper);
            } catch(Exception e){
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
     */
    public void handleCommitButton(ActionEvent actionEvent) throws GitAPIException {
        String commitMessage = commitMessageField.getText();

        for (RepoFile checkedFile : this.workingTreePanelView.getCheckedFilesInDirectory()) {
            checkedFile.updateFileStatusInRepo();
        }

        this.theModel.currentRepoHelper.commit(commitMessage);
        this.theModel.currentRepoHelper.pushAll();

    }

    public void handleMergeButton(ActionEvent actionEvent){
    }

    public void handlePushButton(ActionEvent actionEvent){
    }

    public void handleFetchButton(ActionEvent actionEvent){

    }

    /**
     * Reloads the panel views when the button is clicked.
     *
     * TODO: Implement automatic refresh!
     *
     * @param actionEvent the button click event.
     * @throws GitAPIException if the drawDirectoryView() call fails.
     * @throws IOException if the drawDirectoryView() call fails.
     */
    public void handleReloadButton(ActionEvent actionEvent) throws GitAPIException, IOException {
        this.workingTreePanelView.drawDirectoryView();
        this.localPanelView.drawTreeFromCurrentRepo();
        this.remotePanelView.drawTreeFromCurrentRepo();
    }
}
