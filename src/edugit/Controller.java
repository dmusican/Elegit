package edugit;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.io.File;

public class Controller {

    public final String defaultPath = System.getProperty("user.home")+File.separator+"Desktop"+File.separator+"TestClone";

    @FXML private Text actionTarget;
    @FXML private Label repoLocation;
    @FXML private Label fileLocation;
    @FXML private TextField commitText;

    public Controller(){}

    private File getPathFromChooser(boolean isDirectory, String title){
        File path = new File(defaultPath);

        File returnFile;
        if(isDirectory) {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle(title);
            chooser.setInitialDirectory(path.getParentFile());

            returnFile = chooser.showDialog(actionTarget.getScene().getWindow());
        }else{
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle(title);
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Text Files", "*.txt"),
                    new FileChooser.ExtensionFilter("All Files", "*.*"));

            returnFile = fileChooser.showOpenDialog(actionTarget.getScene().getWindow());
        }
        return returnFile;
    }

    public void handleSubmitButtonAction(ActionEvent actionEvent) {
        String fileName = fileLocation.getText();
        String commitMessage = commitText.getText();

        RepoModel repo = new RepoModel(SECRET_CONSTANTS.TEST_GITHUB_TOKEN, new File(this.repoLocation.getText()));

        repo.cloneRepo();
//        repo.findRepo();
        repo.pushNewFile(new File(path, fileName), commitMessage);
        repo.closeRepo();

        this.actionTarget.setText(fileName+" added");
    }

    public void handleChooseRepoLocationButton(ActionEvent actionEvent) {
        File selectedDirectory = this.getPathFromChooser(true, "Git Repositories");

        if(selectedDirectory != null){
            this.repoLocation.setText(selectedDirectory.toString());
        }
    }

    public void handleChooseFileButton(ActionEvent actionEvent) {
        File selectedFile = this.getPathFromChooser(false, "Repo Files");

        if(selectedFile != null){
            this.fileLocation.setText(selectedFile.getName());
        }
    }
}
