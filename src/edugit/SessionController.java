package edugit;

import com.jcraft.jsch.Session;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.util.Pair;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

/**
 * Created by makik on 6/10/15.
 *
 * TODO: Handle errors here?
 */
public class SessionController extends Controller {

    private SessionModel theModel;

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
            ClonedRepoHelperBuilder builder = new ClonedRepoHelperBuilder(this.theModel);
            builder.presentDialogsToConstructRepoHelper();
        });

        MenuItem existingOption = new MenuItem("Load existing repository");
        existingOption.setOnAction(t -> {
            ExistingRepoHelperBuilder builder = new ExistingRepoHelperBuilder(this.theModel);
            try { // TODO: figure out why this needs a try/catch... and remove it
                builder.presentDialogsToConstructRepoHelper();
            } catch (Exception e) {
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
        String commitMessage = commitMessageField.getText();

        for (RepoFile checkedFile : this.workingTreePanelView.getCheckedFilesInDirectory()) {
            checkedFile.updateFileStatusInRepo();
        }

        this.theModel.currentRepoHelper.commit(commitMessage);
        this.theModel.currentRepoHelper.pushAll();

        // Now clear the commit text and a view reload ( or `git status`) to show that something happened
        commitMessageField.clear();
        this.loadPanelViews();

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
        this.workingTreePanelView.drawDirectoryView();
        this.localCommitTreeModel.update();
        this.remoteCommitTreeModel.update();
    }

//    public void handleCloneToDestinationButton(ActionEvent actionEvent) {
//        File cloneRepoFile = getPathFromChooser(true, "Choose a Location", ((Button)actionEvent.getSource()).getScene().getWindow());
//        try{
//            RepoHelper repoHelper = new ClonedRepoHelper(cloneRepoFile.toPath(), SECRET_CONSTANTS.TEST_GITHUB_TOKEN);
//            this.theModel.openRepoFromHelper(repoHelper);
//        } catch(Exception e){
//            e.printStackTrace();
//        }
//    }
}
