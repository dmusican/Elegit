package edugit;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.*;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.File;

/**
 * Created by makik on 6/10/15.
 */
public class SessionController extends Controller {

    public MenuBar menuBar;
    private SessionModel theModel;

    public TextArea commitMessageField;
    public WorkingTreePanelView workingTreePanelView;

    public LocalPanelView localPanelView;
    public RemotePanelView remotePanelView;

    /**
     * Initialize the environment by creating the model
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

    private void initializeMenuBar() {
        // TODO: break this out into a separate controller
        Menu openMenu = new Menu("Load a Repository");

        MenuItem cloneOption = new MenuItem("Clone");
        cloneOption.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent t) {
                File cloneRepoDirectory = getPathFromChooser(true, "Choose a Location", null);
                try{
                    RepoHelper repoHelper = new ClonedRepoHelper(cloneRepoDirectory.toPath(), SECRET_CONSTANTS.TEST_GITHUB_TOKEN);
                    SessionModel.getSessionModel().openRepoFromHelper(repoHelper);
                } catch(Exception e){
                    e.printStackTrace();
                }
            }
        });

        MenuItem existingOption = new MenuItem("Load existing repository");
        existingOption.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent t) {
                File existingRepoDirectory = getPathFromChooser(true, "Choose a Location", null);
                try{
                    RepoHelper repoHelper = new ExistingRepoHelper(existingRepoDirectory.toPath(), SECRET_CONSTANTS.TEST_GITHUB_TOKEN);
                    SessionModel.getSessionModel().openRepoFromHelper(repoHelper);
                } catch(Exception e){
                    e.printStackTrace();
                }
            }
        });

        MenuItem newOption = new MenuItem("Start a new repository");
        newOption.setDisable(true);

        openMenu.getItems().addAll(cloneOption, existingOption, newOption);
        menuBar.getMenus().addAll(openMenu);

    }

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

    public void handleReloadButton(ActionEvent actionEvent) throws GitAPIException {
        this.workingTreePanelView.drawDirectoryView();
        this.localPanelView.drawTreeFromCurrentRepo();
        this.remotePanelView.drawTreeFromCurrentRepo();
    }
}
