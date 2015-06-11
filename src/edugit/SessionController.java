package edugit;

import javafx.event.ActionEvent;
import javafx.scene.control.TextArea;

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
        String commitMessage = commitMessageField.getText();

        // TODO: delete print statements
        System.out.println(commitMessage);
        System.out.println(this.workingTreePanelView.getCheckedFilesInDirectory());

        this.theModel.currentRepoHelper.addFilePaths(this.workingTreePanelView.getCheckedFilesInDirectory());
        this.theModel.currentRepoHelper.commitFile(commitMessage);
        this.theModel.currentRepoHelper.pushAll();

    }

    public void handleMergeButton(ActionEvent actionEvent){
    }

    public void handlePushButton(ActionEvent actionEvent){
    }

    public void handleFetchButton(ActionEvent actionEvent){

    }

    public void handleReloadButton(ActionEvent actionEvent) {
        this.workingTreePanelView.drawDirectoryView();
    }
}
