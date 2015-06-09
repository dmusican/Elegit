package edugit;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;

public class Controller {

    @FXML private Text actionTarget;
    @FXML private TextField inputText;

    public void handleSubmitButtonAction(ActionEvent actionEvent) {
        String fileName = inputText.getText();

        RepoModel repo = new RepoModel(SECRET_CONSTANTS.TEST_GITHUB_TOKEN);
        repo.cloneRepo();
//        repo.findRepo();
        repo.pushNewFile(fileName, fileName);
        repo.closeRepo();
    }
}
