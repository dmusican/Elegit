package edugit;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;

public class Controller {

    @FXML private Text actionTarget;
    @FXML private TextField inputText;

    public void handleSubmitButtonAction(ActionEvent actionEvent) {
        actionTarget.setText(inputText.getText());
    }
}
