package edugit;

import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.io.File;

public class Controller {

    public final String defaultPath = System.getProperty("user.home")+File.separator+"Desktop"+File.separator+"TestClone";

    private ThreadController threadController;

    @FXML private Tab addFileTab;
    @FXML private Tab createRepoTab;

    private File selectedRepo;
    private File selectedFile;

    @FXML private Text messageText;
    @FXML private Label repoPathLabel;
    @FXML private Label fileNameLabel;
    @FXML private TextField commitText;

    @FXML private Button chooseRepoButton;
    @FXML private Button chooseFileButton;
    @FXML private Button commitButton;

    private File newRepo;

    @FXML private Label createRepoPathLabel;
    @FXML private TextField createRepoNameText;
    @FXML private Text createMessageText;
    @FXML private Button createRepoPathButton;
    @FXML private Button createButton;

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

            returnFile = chooser.showDialog(messageText.getScene().getWindow());
        }else{
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle(title);
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("All Files", "*.*"),
                    new FileChooser.ExtensionFilter("Text Files", "*.txt"));

            returnFile = fileChooser.showOpenDialog(messageText.getScene().getWindow());
        }
        return returnFile;
    }

    private String getRelativePath(File parent, File file){
        return parent.toURI().relativize(file.toURI()).getPath();
    }

    @FXML private void updateButtonDisables() {
        if(addFileTab.isSelected()) {
            if (threadController.isLoadingUIThreadRunning()) {
                chooseRepoButton.setDisable(true);
                chooseFileButton.setDisable(true);
                commitButton.setDisable(true);
            } else {
                chooseRepoButton.setDisable(false);
                chooseFileButton.setDisable(selectedRepo == null);
                commitButton.setDisable(selectedRepo == null || selectedFile == null);
            }
        }else if(createRepoTab.isSelected()){
            if (threadController.isLoadingUIThreadRunning()) {
                createRepoPathButton.setDisable(true);
                createButton.setDisable(true);
            } else {
                createRepoPathButton.setDisable(false);
                createButton.setDisable(newRepo == null || createRepoNameText.getText().equals(""));
            }
        }
    }

    public void handleCommitButtonAction(ActionEvent actionEvent) {
        Task task = new Task<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return addFileAndPush();
            }
        };
        Thread th = new Thread(task);
        th.setDaemon(false);
        th.start();
    }

    public void handleCreateButtonAction(ActionEvent actionEvent) {
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
        threadController.startLoadingUIThread(this.createMessageText);

        this.updateButtonDisables();

        String repoPath = createRepoPathLabel.getText();
        String repoName = createRepoNameText.getText();

        NewRepoModel repo;

        try {
            repo = new NewRepoModel(new File(repoPath, repoName), SECRET_CONSTANTS.TEST_GITHUB_TOKEN);
        }catch (Exception e){
            e.printStackTrace();
            threadController.endLoadingUIThread();
            this.updateButtonDisables();
            return false;
        }

        repo.closeRepo();

        threadController.endLoadingUIThread();

        this.createMessageText.setText(repoName + " created");

        this.updateButtonDisables();

        return true;
    }

    private boolean addFileAndPush(){
        threadController.startLoadingUIThread(this.messageText);

        this.updateButtonDisables();

        String repoPath = repoPathLabel.getText();
        String filePath = fileNameLabel.getText();
        String commitMessage = commitText.getText();

        ClonedRepoModel repo;

        try {
            repo = new ClonedRepoModel(new File(repoPath), SECRET_CONSTANTS.TEST_GITHUB_TOKEN);
        }catch (Exception e){
            e.printStackTrace();
            threadController.endLoadingUIThread();
            this.updateButtonDisables();
            return false;
        }

        repo.pushNewFile(new File(filePath), commitMessage);

        repo.closeRepo();

        threadController.endLoadingUIThread();

        this.messageText.setText(filePath + " added");

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

    public void handleCreateRepoLocationButton(ActionEvent actionEvent) {
        File newRepo = this.getPathFromChooser(true, "Choose a location");

        if(newRepo != null){
            this.newRepo = newRepo;
            this.createRepoPathLabel.setText(newRepo.toString());
            this.updateButtonDisables();
        }
    }
}
