package edugit;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.io.File;

public class Controller {

    public final String defaultPath = System.getProperty("user.home")+File.separator+"Desktop"+File.separator+"TestClone";

    private File selectedRepo;
    private File selectedFile;

    @FXML private Text actionTargetText;
    @FXML private Label repoPathLabel;
    @FXML private Label fileNameLabel;
    @FXML private TextField commitText;

    @FXML private Button chooseRepoButton;
    @FXML private Button chooseFileButton;
    @FXML private Button commitButton;

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
                    new FileChooser.ExtensionFilter("All Files", "*.*"),
                    new FileChooser.ExtensionFilter("Text Files", "*.txt"));

            returnFile = fileChooser.showOpenDialog(actionTargetText.getScene().getWindow());
        }
        return returnFile;
    }

    private String getRelativePath(File parent, File file){
        return parent.toURI().relativize(file.toURI()).getPath();
    }

    private void updateButtonDisables() {
        chooseRepoButton.setDisable(false);
        chooseFileButton.setDisable(selectedRepo == null);
        commitButton.setDisable(selectedRepo == null || selectedFile == null);
    }

    public void handleSubmitButtonAction(ActionEvent actionEvent) throws Exception {
        String repoPath = repoPathLabel.getText();
        String filePath = fileNameLabel.getText();
        String commitMessage = commitText.getText();

        ClonedRepoModel repo = new ClonedRepoModel(new File(repoPath), SECRET_CONSTANTS.TEST_GITHUB_TOKEN);

        repo.pushNewFile(new File(filePath), commitMessage);

        repo.closeRepo();

        this.actionTargetText.setText(filePath+" added");
    }

    public void handleChooseRepoLocationButton(ActionEvent actionEvent) {
        File newSelectedRepo = this.getPathFromChooser(true, "Git Repositories");

        if(newSelectedRepo != null){
            this.selectedRepo = newSelectedRepo;
            this.repoPathLabel.setText(selectedRepo.toString());
            this.updateButtonDisables();
        }
    }

    public void handleChooseFileButton(ActionEvent actionEvent) {
        File newSelectedFile = this.getPathFromChooser(false, "Repo Files");

        if(newSelectedFile != null){
            this.selectedFile = newSelectedFile;
            this.fileNameLabel.setText(getRelativePath(selectedRepo, selectedFile));
            this.updateButtonDisables();
        }
    }
}
