package edugit;

import javafx.concurrent.Task;
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

    private ThreadController threadController;

    private File selectedRepo;
    private File selectedFile;

    @FXML private Text actionTargetText;
    @FXML private Label repoPathLabel;
    @FXML private Label fileNameLabel;
    @FXML private TextField commitText;

    @FXML private Button chooseRepoButton;
    @FXML private Button chooseFileButton;
    @FXML private Button commitButton;

    public Controller(){
        this.threadController = new ThreadController(this);
    }

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
        if(threadController.isLoadingUIThreadRunning()){
            chooseRepoButton.setDisable(true);
            chooseFileButton.setDisable(true);
            commitButton.setDisable(true);
        }else {
            chooseRepoButton.setDisable(false);
            chooseFileButton.setDisable(selectedRepo == null);
            commitButton.setDisable(selectedRepo == null || selectedFile == null);
        }
    }

    public void handleSubmitButtonAction(ActionEvent actionEvent) {
        Task task = new Task<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return cloneRepoAndPush();
            }
        };
        Thread th = new Thread(task);
        th.setDaemon(false);
        th.start();
    }

    private boolean cloneRepoAndPush() {
        threadController.startLoadingUIThread(this.actionTargetText);

        this.updateButtonDisables();

        String repoPath = repoPathLabel.getText();
        String filePath = fileNameLabel.getText();
        String commitMessage = commitText.getText();

        RepoModel repo = new RepoModel(new File(repoPath), SECRET_CONSTANTS.TEST_GITHUB_TOKEN, true);

        repo.pushNewFile(new File(filePath), commitMessage);

        repo.closeRepo();

        threadController.endLoadingUIThread();

        this.actionTargetText.setText(filePath + " added");

        this.updateButtonDisables();

        return true;
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
