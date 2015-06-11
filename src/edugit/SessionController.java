package edugit;

import javafx.event.ActionEvent;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

/**
 * Created by makik on 6/10/15.
 */
public class SessionController extends Controller {

    private SessionModel theModel;

    public TextArea commitMessageField;
    public WorkingTreePanelView workingTreePanelView;

    /**
     * Initialize the environment by creating the model
     * and putting the views on display.
     *
     * This method is automatically called by JavaFX.
     */
    public void initialize() {
        this.theModel = SessionModel.getSessionModel();
        this.workingTreePanelView.setSessionModel(this.theModel);
    }

    public void handleCommitButton(ActionEvent actionEvent){
    }

    public void handleMergeButton(ActionEvent actionEvent){
    }

    public void handlePushButton(ActionEvent actionEvent){
    }

    public void handleFetchButton(ActionEvent actionEvent){

    }
}
