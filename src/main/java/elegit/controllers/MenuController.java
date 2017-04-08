package elegit.controllers;

import elegit.GitIgnoreEditor;
import elegit.SessionModel;
import elegit.exceptions.NoRepoLoadedException;
import elegit.treefx.TreeLayout;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.Text;
import org.apache.logging.log4j.Level;
import org.controlsfx.control.PopOver;

import java.io.IOException;

/**
 * Created by dmusicant on 4/8/17.
 */
public class MenuController {

    private SessionController sessionController;

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public void handleLoggingOffMenuItem() {
        sessionController.changeLogging(Level.OFF);
        PopOver popOver = new PopOver(new Text("Toggled logging off"));
        popOver.setTitle("");
        popOver.show(sessionController.commitTreePanelView);
        popOver.detach();
        popOver.setAutoHide(true);
    }

    public void handleLoggingOnMenuItem() {
        sessionController.changeLogging(Level.INFO);
        PopOver popOver = new PopOver(new Text("Toggled logging on"));
        popOver.show(sessionController.commitTreePanelView);
        popOver.detach();
        popOver.setAutoHide(true);
        sessionController.logger.log(Level.INFO, "Toggled logging on");
    }

    public void handleCommitSortTopological() {
        TreeLayout.commitSortTopological = true;
        try {
            sessionController.commitTreeModel.updateView();
        } catch (Exception e) {
            e.printStackTrace();
            sessionController.showGenericErrorNotification();
        }
    }

    public void handleCommitSortDate() {
        TreeLayout.commitSortTopological = false;
        try {
            sessionController.commitTreeModel.updateView();
        } catch (Exception e) {
            e.printStackTrace();
            sessionController.showGenericErrorNotification();
        }
    }

    /**
     * Opens an editor for the .gitignore
     */
    public void handleGitIgnoreMenuItem() {
        GitIgnoreEditor.show(SessionModel.getSessionModel().getCurrentRepoHelper(), null);
    }


    public void handleNewBranchButton() {
        sessionController.handleCreateOrDeleteBranchButton("create");
    }

    public void handleDeleteLocalBranchButton() {
        sessionController.handleCreateOrDeleteBranchButton("local");
    }

    public void handleDeleteRemoteBranchButton() {
        sessionController.handleCreateOrDeleteBranchButton("remote");
    }


    /**
     * Opens up the current repo helper's Branch Manager window after
     * passing in this SessionController object, so that the
     * BranchCheckoutController can update the main window's views.
     */
    public void showBranchCheckout() {
        try{
            sessionController.logger.info("Branch checkout clicked");
            if(sessionController.theModel.getCurrentRepoHelper() == null) throw new NoRepoLoadedException();

            sessionController.logger.info("Opened branch checkout window");
            // Create and display the Stage:
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/elegit/fxml/BranchCheckout.fxml"));
            fxmlLoader.load();
            BranchCheckoutController branchCheckoutController = fxmlLoader.getController();
            AnchorPane fxmlRoot = fxmlLoader.getRoot();
            branchCheckoutController.showStage(fxmlRoot);
        }catch(IOException e){
            sessionController.showGenericErrorNotification();
            e.printStackTrace();
        }catch(NoRepoLoadedException e){
            sessionController.showNoRepoLoadedNotification();
            sessionController.setButtonsDisabled(true);
        }
    }


}
