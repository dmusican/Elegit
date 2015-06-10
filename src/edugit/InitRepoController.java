package edugit;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;

import java.io.File;

/**
 * Created by makik on 6/10/15.
 *
 * The controller that handles the creation of a session with a repository either cloned, created, or located.
 * This controller works with the InitRepoView and adds repositories to the SessionModel
 */
public class InitRepoController extends Controller {

    @FXML private Tab cloneRepoTab;
    @FXML private Tab createRepoTab;
    @FXML private Tab findRepoTab;

    @FXML private Text cloneRepoLocationText;
    @FXML private Button cloneRepoLocationButton;
    @FXML private Button cloneRepoChangeLocationButton;
    @FXML private Button cloneRepoGoButton;
    @FXML private ProgressIndicator cloneRepoProgressIndicator;

    @FXML private Text createRepoLocationText;
    @FXML private Button createRepoLocationButton;
    @FXML private Button createRepoChangeLocationButton;
    @FXML private TextField createRepoNameTextField;
    @FXML private Button createRepoGoButton;


    @FXML private Text findRepoLocationText;
    @FXML private Button findRepoLocationButton;
    @FXML private Button findRepoChangeLocationButton;
    @FXML private Button findRepoGoButton;

    private File cloneRepoFile;
    private File createRepoFile;
    private File findRepoFile;

    /**
    * The following methods handle the buttons for selecting a file or directory location. They each
    * present the user with a chooser, then store the result and update the UI appropriately
    */

    public void handleCloneRepoLocationButton(ActionEvent actionEvent){
        cloneRepoFile = getPathFromChooser(true, "Choose a Location", ((Button)actionEvent.getSource()).getScene().getWindow());

        if(cloneRepoFile != null){
            cloneRepoLocationText.setText(cloneRepoFile.toString());

            cloneRepoLocationButton.setVisible(false);
            cloneRepoLocationText.setVisible(true);
            cloneRepoChangeLocationButton.setVisible(true);
        }
    }

    public void handleCreateRepoLocationButton(ActionEvent actionEvent){
        createRepoFile = getPathFromChooser(true, "Choose a Location", ((Button)actionEvent.getSource()).getScene().getWindow());

        if(createRepoFile != null){
            createRepoLocationText.setText(createRepoFile.toString());

            createRepoLocationButton.setVisible(false);
            createRepoLocationText.setVisible(true);
            createRepoChangeLocationButton.setVisible(true);
        }
    }

    public void handleFindRepoLocationButton(ActionEvent actionEvent){
        findRepoFile = getPathFromChooser(true, "Choose a Location", ((Button)actionEvent.getSource()).getScene().getWindow());

        if(findRepoFile != null){
            findRepoLocationText.setText(findRepoFile.toString());

            findRepoLocationButton.setVisible(false);
            findRepoLocationText.setVisible(true);
            findRepoChangeLocationButton.setVisible(true);
        }
    }

    /**
     * Clones a repository into the selected file location (stored in this.cloneRepoFile) and then
     * adds it to the SessionModel.
     *
     * This method spawns two threads, one for the actual cloning and one for updating the UI
     * with progress reports.
     *
     * Currently (as of 6/10/15) uses the test repository every time.
     *
     * @param actionEvent ignored
     */
    public void handleCloneRepoGoButton(ActionEvent actionEvent){

        Runnable r = () -> {
            cloneRepoGoButton.setVisible(false);
            cloneRepoProgressIndicator.setVisible(true);
            ThreadHelper.startProgressThread(cloneRepoProgressIndicator);

            try{
                RepoHelper repo = new ClonedRepoHelper(cloneRepoFile.toPath(), SECRET_CONSTANTS.TEST_GITHUB_TOKEN);
                SessionModel.getSessionModel().addRepo(repo);
            }catch(Exception e){
                e.printStackTrace();
            }

            ThreadHelper.endProgressThread();
            cloneRepoProgressIndicator.setVisible(false);
            cloneRepoGoButton.setVisible(true);
        };

        ThreadHelper.startThread(r);
    }

    /**
     * Creates a directory at the given location (stored in this.createRepoFile) with the given
     * name and initializes a git repository there, then adds it to the SessionModel.
     *
     * @param actionEvent ignored
     */
    public void handleCreateRepoGoButton(ActionEvent actionEvent){
        try{
            RepoHelper repo = new NewRepoHelper((new File(createRepoFile, createRepoNameTextField.getText())).toPath(), SECRET_CONSTANTS.TEST_GITHUB_TOKEN);
            SessionModel.getSessionModel().addRepo(repo);
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    /**
     * Adds the repository at the given location (stored in this.findRepoFile) to the
     * SessionModel.
     *
     * @param actionEvent ignored
     */
    public void handleFindRepoGoButton(ActionEvent actionEvent){
        try{
            RepoHelper repo = new ExistingRepoHelper(findRepoFile.toPath(), SECRET_CONSTANTS.TEST_GITHUB_TOKEN);
            SessionModel.getSessionModel().addRepo(repo);
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    /**
     * Unused -> updates which buttons are enabled based on how much information the user has input
     */
    @FXML private void updateButtonDisables() {
//        if(cloneRepoTab.isSelected()) {
//            if (threadController.isLoadingUIThreadRunning()) {
//                chooseRepoButton.setDisable(true);
//                chooseFileButton.setDisable(true);
//                commitButton.setDisable(true);
//            } else {
//                chooseRepoButton.setDisable(false);
//                chooseFileButton.setDisable(selectedRepo == null);
//                commitButton.setDisable(selectedRepo == null || selectedFile == null);
//            }
//        }else if(createRepoTab.isSelected()){
//            if (threadController.isLoadingUIThreadRunning()) {
//                createRepoPathButton.setDisable(true);
//                createButton.setDisable(true);
//            } else {
//                createRepoPathButton.setDisable(false);
//                createButton.setDisable(newRepo == null || createRepoNameText.getText().equals(""));
//            }
//        }
    }
}
