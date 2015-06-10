package edugit;

import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;

import java.io.File;

/**
 * Created by makik on 6/10/15.
 */
public class InitRepoController extends Controller {
    public Tab cloneRepoTab;
    public Tab createRepoTab;
    public Tab findRepoTab;

    public Text cloneRepoLocationText;
    public Button cloneRepoLocationButton;
    public Button cloneRepoChangeLocationButton;
    public Button cloneRepoGoButton;

    public Text createRepoLocationText;
    public Button createRepoLocationButton;
    public Button createRepoChangeLocationButton;
    public TextField createRepoNameTextField;
    public Button createRepoGoButton;

    private File cloneRepoFile;
    private File createRepoFile;


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

    public void handleCloneRepoGoButton(ActionEvent actionEvent){
        try{
            RepoHelper repo = new ClonedRepoHelper(cloneRepoFile.toPath(), SECRET_CONSTANTS.TEST_GITHUB_TOKEN);
            SessionModel.getSessionModel().addRepo(repo);
        }catch(Exception e){
            e.printStackTrace();
            return;
        }
    }

    public void handleCreateRepoGoButton(ActionEvent actionEvent){
        try{
            RepoHelper repo = new NewRepoHelper((new File(createRepoFile, createRepoNameTextField.getText())).toPath(), SECRET_CONSTANTS.TEST_GITHUB_TOKEN);
            SessionModel.getSessionModel().addRepo(repo);
        }catch(Exception e){
            e.printStackTrace();
            return;
        }
    }
}
