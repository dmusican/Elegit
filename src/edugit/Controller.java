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

    private RepoModel repo;

    public Controller(){
        this.repo = new RepoModel(SECRET_CONSTANTS.TEST_GITHUB_TOKEN);
    }

    public void handleSubmitButtonAction(ActionEvent actionEvent) {
        String fileName = fileNameText.getText();
        String commitMessage = commitText.getText();

        this.repo.cloneRepo();
//        repo.findRepo();
        this.repo.pushNewFile(fileName, commitMessage);
        this.repo.closeRepo();

        this.actionTarget.setText(fileName+" added");
    }

    public void handleChooseRepoLocationButton(ActionEvent actionEvent) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Git Repositories");
        File defaultDirectory = this.repo.getLocalPath();
        chooser.setInitialDirectory(defaultDirectory.getParentFile());
        File selectedDirectory = chooser.showDialog(actionTarget.getScene().getWindow());
        if(selectedDirectory != null){
            this.repoLocation.setText(selectedDirectory.toString());
        }
    }
}
