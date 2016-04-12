package main.java.elegit;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.eclipse.jgit.lib.Constants;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;

/**
 * A simple editor for .gitignore files
 */
public class GitIgnoreEditor {

    private static Stage window;
    private static RepoHelper repoHelper;
    private static Path gitIgnoreFile;
    private static String addedPath;

    /**
     * Set up the parameters of the window
     * @return the main window
     */
    private static Stage initWindow(){
        Stage window = new Stage();

        Rectangle2D primaryScreenBounds = Screen.getPrimary().getVisualBounds();

        window.setMaxHeight(primaryScreenBounds.getHeight());
        window.setMaxWidth(primaryScreenBounds.getWidth());
        window.setMinHeight(400);
        window.setMinWidth(450);

        window.initModality(Modality.APPLICATION_MODAL);

        window.setScene(new Scene(getRootOfScene()));

        window.setTitle(gitIgnoreFile.toString());

        return window;
    }

    /**
     * Set up the content of the window, including styling and layout
     * @return the top-level node in the scene
     */
    private static Parent getRootOfScene(){
        TextArea textArea = new TextArea();
        textArea.setStyle("-fx-focus-color: transparent; -fx-faint-focus-color: transparent;");

        Button okButton = new Button("Save");
        okButton.setOnAction(event -> handleConfirmation(textArea.getText(), true));
        Button cancelButton = new Button("Cancel");
        cancelButton.setOnAction(event -> handleConfirmation(textArea.getText(), false));

        HBox buttonArea = new HBox(cancelButton, okButton);
        buttonArea.setPadding(new Insets(5, 5, 5, 5));
        buttonArea.setSpacing(5);
        buttonArea.setAlignment(Pos.BOTTOM_RIGHT);

        VBox parent = new VBox(textArea, new Separator(Orientation.HORIZONTAL), buttonArea);
        VBox.setVgrow(textArea, Priority.ALWAYS);
        parent.setStyle("-fx-border-color: #3498DB");

        textArea.setText(getTextFromGitIgnoreFile());

        return parent;
    }

    /**
     * When the user selects one of the buttons, close the window and write the new
     * text to the .gitignore file on disk if they chose the 'save' option
     * @param newText the text to write if saveChanges is true
     * @param saveChanges whether to write to file or not
     */
    private static void handleConfirmation(String newText, boolean saveChanges) {
        if(saveChanges){
            try(BufferedReader br = new BufferedReader(new StringReader(newText));
                BufferedWriter bw = Files.newBufferedWriter(gitIgnoreFile)){
                for(String line = br.readLine(); line != null; line = br.readLine()) {
                    bw.write(line);
                    bw.newLine();
                }
            } catch (IOException ignored) {}
        }

        try{
            showTrackingIgnoredFilesWarning(repoHelper.getTrackedIgnoredFiles());
        }catch (IOException ignored) {}

        window.close();
    }

    /**
     * @return the full text from the .gitignore file, with the added file path added on the end
     */
    private static String getTextFromGitIgnoreFile() {
        String returnText = addedPath.length() > 0 ? "\n" + addedPath + "\n" : "";
        try {
            returnText = new String(Files.readAllBytes(gitIgnoreFile)) + returnText;
        } catch (IOException ignored){}

        return returnText;
    }

    private static void showTrackingIgnoredFilesWarning(Collection<String> trackedIgnoredFiles) {
        if(trackedIgnoredFiles.size() > 0){
            String fileStrings = "";
            for(String s : trackedIgnoredFiles){
                fileStrings += "\n"+s;
            }
            Alert alert = new Alert(Alert.AlertType.WARNING, "The following files are being tracked by Git, " +
                    "but also match an ignore pattern. If you want to ignore these files, remove them from Git.\n"+fileStrings);
            alert.showAndWait();
        }
    }

    /**
     * Show the window with the .gitinogre from the given repo and with the given path appended
     * @param repo the repository to pull the .gitignore from
     * @param pathToAdd the path of a file to append to the .gitignore, if applicable
     */
    public static void show(RepoHelper repo, Path pathToAdd){
        repoHelper = repo;
        gitIgnoreFile = repoHelper.getLocalPath().resolve(Constants.DOT_GIT_IGNORE);
        addedPath = pathToAdd == null ? "" : pathToAdd.toString();

        window = initWindow();
        Platform.runLater(window::show);
    }
}
