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

    @FXML private Text actionTargetText;
    @FXML private Label repoPathLabel;
    @FXML private Label fileNameLabel;
    @FXML private TextField commitText;

    public Controller(){}

    private File getPathFromChooser(boolean isDirectory, String title){
        File path = new File(defaultPath);

        File returnFile;
        if(isDirectory) {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle(title);
            chooser.setInitialDirectory(path.getParentFile());

            returnFile = chooser.showDialog(actionTargetText.getScene().getWindow());
        }else{
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle(title);
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Text Files", "*.txt"),
                    new FileChooser.ExtensionFilter("All Files", "*.*"));

            returnFile = fileChooser.showOpenDialog(actionTargetText.getScene().getWindow());
        }
        return returnFile;
    }

    public void handleSubmitButtonAction(ActionEvent actionEvent) {
        String repoPath = repoPathLabel.getText();
        String filePath = fileNameLabel.getText();
        String commitMessage = commitText.getText();

        RepoModel repo = new RepoModel(new File(repoPath), SECRET_CONSTANTS.TEST_GITHUB_TOKEN, true);

        repo.pushNewFile(new File(filePath), commitMessage);

        repo.closeRepo();

        this.actionTargetText.setText(filePath+" added");
    }

    public void handleChooseRepoLocationButton(ActionEvent actionEvent) {
        File selectedDirectory = this.getPathFromChooser(true, "Git Repositories");

        if(selectedDirectory != null){
            this.repoPathLabel.setText(selectedDirectory.toString());
        }
    }

    public void handleChooseFileButton(ActionEvent actionEvent) {
        File selectedFile = this.getPathFromChooser(false, "Repo Files");

        if(selectedFile != null){
            this.fileNameLabel.setText(selectedFile.toString());
        }
    }
}
