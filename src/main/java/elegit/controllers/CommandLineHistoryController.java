package elegit.controllers;

import elegit.Main;
import elegit.exceptions.ExceptionAdapter;
import elegit.models.SessionModel;
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
import java.nio.file.Path;

/**
 * Created by grenche on 6/7/18.
 * Allows the user to see and export their "terminal command history"
 */

public class CommandLineHistoryController {
    private final SessionController sessionController;

    @FXML
    private TextArea commandHistory;

    // This is not threadsafe, since it escapes to other portions of the JavaFX view. Only access from FX thread.
    private Stage stage;

    private static final Logger logger = LogManager.getLogger();

    public CommandLineHistoryController() {
        sessionController = SessionController.getSessionController();
    }

    // Currently doesn't update with actual history, but with the elegit.log file
    public synchronized void initialize() {
        commandHistory.clear();
        String command;
        // Currently cannot get the file. I'm not sure why it's not showing up in the log folder.
        File transcript = new File(System.getProperty("logFolder") + "/transcript.log");

        try {
            BufferedReader br = new BufferedReader(new FileReader(transcript));
            while ((command = br.readLine()) != null) {
                commandHistory.appendText(command + "\n");
            }
        } catch (IOException e) {
            throw new ExceptionAdapter(e);
        }
    }

    /**
     * Opens up a terminal like window and displays command line history
     */
    public void showHistory() {
        Main.assertFxThread();
        if (commandHistory.getText().equals("")) { // Don't show the popup if they haven't made any commands yet
            sessionController.showNoCommandLineHistoryNotification();
        } else {
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
    }

    public void closeWindow() {
        this.stage.close();
    }

    /**
     * Allows the user to save the Elegit actions they've done as terminal commands in a txt file
     */
    public synchronized void handleExportHistoryOption() {
        Main.assertFxThread();
        if (commandHistory.getText().equals("")) { // Don't let them save a file if they haven't made any commands yet
            sessionController.showNoCommandLineHistoryNotification();
        } else {
            FileChooser fileChooser = new FileChooser();
            // Allow the user to export the commands to a .txt file.
            FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("TXT files (*.txt)", "*.txt");
            fileChooser.getExtensionFilters().add(extFilter);
            Stage stage = new Stage();

            // Open up the save window
            File file = fileChooser.showSaveDialog(stage);

            if (file != null) {
                writeToFile(commandHistory.getText(), file);
            }
        }
    }

    /**
     * Helper method for writing the command history to the file to be saved
     */
    private void writeToFile(String commands, File file) {
        try {
            // Write the commands to this new file
            FileWriter fileWriter = new FileWriter(file);
            fileWriter.write(commands);
            fileWriter.close();
        } catch (IOException e) {
            sessionController.showGenericErrorNotification(e);
            e.printStackTrace();
        }
    }
}
