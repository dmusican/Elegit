package elegit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import net.jcip.annotations.GuardedBy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;

public class CommandLineHistoryController {

    @GuardedBy("this")
    private SessionController sessionController;

    @FXML
    private TextArea commandHistory;

    private static final Logger logger = LogManager.getLogger();

    public synchronized void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    //Currently doesn't update with actual history
    public synchronized void handleSeeHistoryOption() {
        //commandHistory.clear();
        String command;
        //Currently cannot get the file. I'm not sure why it's not showing up in the log folder.
        File transcript = new File(System.getProperty("logFolder") + "/transcript.log");
        try {
            BufferedReader br = new BufferedReader(new FileReader(transcript));
            while ((command = br.readLine()) != null) {
                commandHistory.appendText(command);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        showHistory();
    }

    public synchronized void handleExportHistoryOption() {
        FileChooser fileChooser = new FileChooser();
        Stage stage = new Stage();
        fileChooser.showSaveDialog(stage);
        File transcript = new File(System.getProperty("logFolder") + "/transcript.log");
        //Currently cannot get the file. I'm not sure why it's not showing up in the log folder.
        saveFile(commandHistory.getText(), transcript);
    }

    private static void saveFile(String content, File file) {
        try {
            FileWriter fileWriter = new FileWriter(file);
            fileWriter.write(content);
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Opens up a terminal like window and displays history
     */
    private void showHistory() {
        try {
            logger.info("See history clicked");
            // Create and display the Stage:
            ScrollPane fxmlRoot = FXMLLoader.load(getClass().getResource("/elegit/fxml/pop-ups/CommandLineHistory.fxml"));

            Stage stage = new Stage();
            stage.setTitle("Recent Elegit actions as commands");
            stage.setScene(new Scene(fxmlRoot));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setOnCloseRequest(event -> logger.info("Closed history"));
            stage.show();
        } catch (IOException e) {
            sessionController.showGenericErrorNotification(e);
            e.printStackTrace();
        }
    }
}
