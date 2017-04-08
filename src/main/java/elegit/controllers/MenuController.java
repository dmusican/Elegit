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
        sessionController.handleLoggingOffMenuItem();
    }

    public void handleLoggingOnMenuItem() {
        sessionController.handleLoggingOnMenuItem();
    }

    public void handleCommitSortTopological() {
        sessionController.handleCommitSortTopological();
    }

    public void handleCommitSortDate() {
        sessionController.handleCommitSortDate();
    }
}