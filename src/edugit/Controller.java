package edugit;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;

import java.io.File;

public class Controller {

    @FXML private Text actionTarget;
    @FXML private TextField fileNameText;
    @FXML private TextField commitText;

    public void handleSubmitButtonAction(ActionEvent actionEvent) {
        String fileName = fileNameText.getText();
        String commitMessage = commitText.getText();

        File path = new File(System.getProperty("user.home")+File.separator+"Desktop"+File.separator+"TestNonClone");

        RepoModel repo = new RepoModel(path, SECRET_CONSTANTS.TEST_GITHUB_TOKEN, true);
        repo.pushNewFile(new File(path, fileName), commitMessage);
        repo.closeRepo();

        actionTarget.setText(fileName + " added");
    }
}
