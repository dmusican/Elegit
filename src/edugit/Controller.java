package edugit;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;

import java.io.File;

public class Controller {

    @FXML private Text actionTarget;
    @FXML private Label repoLocation;
    @FXML private TextField fileNameText;
    @FXML private TextField commitText;

    public Controller(){
    }

    public void handleSubmitButtonAction(ActionEvent actionEvent) {
        String fileName = fileNameText.getText();
        String commitMessage = commitText.getText();

        RepoModel repo = new RepoModel(SECRET_CONSTANTS.TEST_GITHUB_TOKEN, new File(this.repoLocation.getText()));

        repo.cloneRepo();
//        repo.findRepo();
        repo.pushNewFile(fileName, commitMessage);
        repo.closeRepo();

        this.actionTarget.setText(fileName+" added");
    }

    public void handleChooseRepoLocationButton(ActionEvent actionEvent) {

        File path = new File(System.getProperty("user.home")+File.separator+"Desktop"+File.separator+"TestClone");

        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Git Repositories");
        chooser.setInitialDirectory(path.getParentFile());
        File selectedDirectory = chooser.showDialog(actionTarget.getScene().getWindow());
        if(selectedDirectory != null){
            this.repoLocation.setText(selectedDirectory.toString());
        }
    }
}
