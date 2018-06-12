package elegit.controllers;

import elegit.Main;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;

/**
 * Created by grenche on 6/7/18.
 */

public class CommandLineHistoryController {
    private final SessionController sessionController;

    @FXML private TextArea commandHistory;

    // This is not threadsafe, since it escapes to other portions of the JavaFX view. Only access from FX thread.
    private Stage stage;

    private static final Logger logger = LogManager.getLogger();

    public CommandLineHistoryController() {
        sessionController = SessionController.getSessionController();
    }

    //Currently doesn't update with actual history, but with the elegit log file
    public synchronized void initialize() {
        commandHistory.clear();
        String command;
        //Currently cannot get the file. I'm not sure why it's not showing up in the log folder.
        File transcript = new File(System.getProperty("logFolder") + "/elegit.log");
        try {
            BufferedReader br = new BufferedReader(new FileReader(transcript));
            while ((command = br.readLine()) != null) {
                commandHistory.appendText(command + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void handleExportHistoryOption() {
        FileChooser fileChooser = new FileChooser();
        Stage stage = new Stage();
        fileChooser.showSaveDialog(stage);
        File transcript = new File(System.getProperty("logFolder") + "/elegit.log");
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
    public void showHistory() {
        Main.assertFxThread();
        try {
            logger.info("See history clicked");
            // Create and display the Stage:
            ScrollPane fxmlRoot = FXMLLoader.load(getClass().getResource("/elegit/fxml/pop-ups/CommandLineHistory.fxml"));
            stage = new Stage();
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

    public void closeWindow() { this.stage.close(); }
}
