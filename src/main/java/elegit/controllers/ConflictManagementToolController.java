package elegit.controllers;

import de.jensd.fx.glyphs.GlyphIcons;
import de.jensd.fx.glyphs.GlyphsDude;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import elegit.Main;
import elegit.models.ConflictManagementModel;
import elegit.models.RepoHelper;
import elegit.models.SessionModel;
import elegit.models.ConflictLine;
import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import net.jcip.annotations.GuardedBy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Set;
import java.io.File;

/**
 * Created by grenche on 6/18/18.
 * Controller for the conflict management tool.
 */
public class ConflictManagementToolController {
    @GuardedBy("this")
    private SessionController sessionController;
    @FXML
    private AnchorPane anchorRoot;
    @FXML
    private Text leftDocLabel;
    @FXML
    private Text middleDocLabel;
    @FXML
    private Text rightDocLabel;
    @FXML
    private TextArea leftDoc;
    @FXML
    private TextArea middleDoc;
    @FXML
    private TextArea rightDoc;
    @FXML
    private TextArea leftLineNumbers;
    @FXML
    private TextArea middleLineNumbers;
    @FXML
    private TextArea rightLineNumbers;
    @FXML
    private Button rightAccept;
    @FXML
    private Button rightReject;
    @FXML
    private Button leftAccept;
    @FXML
    private Button leftReject;
    @FXML
    private ComboBox<String> conflictingFilesDropdown;
    @FXML
    private Button upToggle;
    @FXML
    private Button downToggle;
    @FXML
    private Button applyChanges;
    @FXML
    private Button abortMerge;
    @FXML
    private NotificationController notificationPaneController;

    private boolean fileSelected = false;

    private static final Logger console = LogManager.getLogger("briefconsolelogger");

    synchronized void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public void initialize() {
        initButtons();
        initDropdown();
        initTextAreas();
        // Disable everything except the dropdown if the user has not specified a file to solve merge conflicts for yet
        if (!fileSelected) {
            setButtonsDisabled(true);
        }
    }

    /**
     * Add graphics and tool tips for all of the buttons in the tool.
     */
    private void initButtons() {
        console.info("Initializing buttons.");
        // Accept change buttons
        initButton(FontAwesomeIcon.CHECK, "checkIcon", rightAccept, "Integrate the highlighted commit.");
        initButton(FontAwesomeIcon.CHECK, "checkIcon", leftAccept, "Integrate the highlighted commit.");

        // Reject change buttons
        initButton(FontAwesomeIcon.TIMES, "xIcon", rightReject, "Ignore the highlighted commit.");
        initButton(FontAwesomeIcon.TIMES, "xIcon", leftReject, "Ignore the highlighted commit.");

        // Toggle change buttons
        initButton(FontAwesomeIcon.ARROW_UP, "arrowIcon", upToggle, "Go to previous change.");
        initButton(FontAwesomeIcon.ARROW_DOWN, "arrowIcon", downToggle, "Go to next change.");

        // Apply and abort buttons
        applyChanges.setTooltip(new Tooltip("Use the \"result\" document with \n the changes you've made."));
        abortMerge.setTooltip(new Tooltip("Ignore all changes made and \n return to previous state."));
    }

    private void initButton(GlyphIcons glyphIcon, String id, Button button, String toolTip) {
        Text icon = GlyphsDude.createIcon(glyphIcon);
        icon.setId(id);
        button.setGraphic(icon);
        button.setTooltip(new Tooltip(toolTip));
    }

    private void initDropdown() {
        try {
            // Get the names of all the conflicting files and put them in the dropdown
            Set<String> conflictingFiles = SessionModel.getSessionModel().getConflictingFiles(null);
            for (String file : conflictingFiles) {
                conflictingFilesDropdown.getItems().add(file);
            }
            conflictingFilesDropdown.setPromptText("None");

        } catch (GitAPIException e) {
            e.printStackTrace();
        }
    }

    private void initTextAreas() {
        // Bind hvalues
        rightDoc.scrollLeftProperty().bindBidirectional(leftDoc.scrollLeftProperty());
        leftDoc.scrollLeftProperty().bindBidirectional(middleDoc.scrollLeftProperty());
        middleDoc.scrollLeftProperty().bindBidirectional(rightDoc.scrollLeftProperty());

        // Bind vvalues
        rightDoc.scrollTopProperty().bindBidirectional(leftDoc.scrollTopProperty());
        middleDoc.scrollTopProperty().bindBidirectional(rightDoc.scrollTopProperty());
        leftDoc.scrollTopProperty().bindBidirectional(middleDoc.scrollTopProperty());

        // Bind line numbers to their TextArea
        rightDoc.scrollTopProperty().bindBidirectional(rightLineNumbers.scrollTopProperty());
        leftDoc.scrollTopProperty().bindBidirectional(leftLineNumbers.scrollTopProperty());
        middleDoc.scrollTopProperty().bindBidirectional(middleLineNumbers.scrollTopProperty());
    }

    private void setButtonsDisabled(boolean disabled) {
        rightAccept.setDisable(disabled);
        rightReject.setDisable(disabled);
        leftAccept.setDisable(disabled);
        leftReject.setDisable(disabled);
        upToggle.setDisable(disabled);
        downToggle.setDisable(disabled);
        applyChanges.setDisable(disabled);
    }

    /**
     * Shows the tool.
     */
    void showStage(AnchorPane pane) {
        anchorRoot = pane;
        Stage stage = new Stage();
        stage.setTitle("Conflict Management Tool");
        stage.setScene(new Scene(anchorRoot));
        stage.setResizable(false);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setOnCloseRequest(event -> console.info("Closed conflict management tool"));
        stage.show();
        notificationPaneController.setAnchor(stage);
    }

    @FXML
    private void setFileToEdit() {
        Main.assertFxThread();
        setDropdownValueToFileName();

        // TODO: save the state of each file

        clearTextAreas();
        updateTextAreasWithNewFile();
        setLabels();
        setButtonsDisabled(false);
    }

    private void setDropdownValueToFileName() {
        // Show file in dropdown
        EventHandler<ActionEvent> handler = conflictingFilesDropdown.getOnAction();
        conflictingFilesDropdown.setOnAction(handler);
    }

    private void clearTextAreas() {
        // Clear the documents before loading new ones
        leftDoc.clear();
        middleDoc.clear();
        rightDoc.clear();
    }

    private void updateTextAreasWithNewFile() {
        // Get the path of the selected file and the name of the file
        Path directory = (new File(SessionModel.getSessionModel().getCurrentRepoHelper().getRepo().getDirectory()
                .getParent())).toPath();
        String filePathWithoutFileName = directory.toString();
        String fileName = conflictingFilesDropdown.getValue();

        // Show files in TextAreas
        setFile(filePathWithoutFileName, fileName);
    }

    private void setLabels() {
        leftDocLabel.setText("Left");
        middleDocLabel.setText("Result");
        rightDocLabel.setText("Right");
    }

    public void setFile(String filePathWithoutFileName, String fileName) {
        fileSelected = true;
        conflictingFilesDropdown.setPromptText(fileName);

        ArrayList<ArrayList> results = ConflictManagementModel.parseConflicts(filePathWithoutFileName +
                File.separator + fileName);

        setLines(results.get(0), leftDoc, leftLineNumbers);
        setLines(results.get(1), middleDoc, middleLineNumbers);
        setLines(results.get(2), rightDoc, rightLineNumbers);

        // Allow the user to click buttons
        setButtonsDisabled(false);
    }

    private void setLines(ArrayList lines, TextArea doc, TextArea lineNumbers) {
        for (int i = 0; i < lines.size(); i++) {
            ConflictLine conflict = (ConflictLine) lines.get(i);
            String line = conflict.getLine();
            // update the document
            doc.appendText(line + "\n");
            // update the line number
            lineNumbers.appendText((i + 1) + "\n");
        }
    }

    private void showAllConflictsHandledNotification() {
        console.info("All conflicts where handled.");
        notificationPaneController.addNotification("All conflicts were handled. Click apply to use them.");
    }

    // TODO: add a button or something that allows them to continue
    private void showNotAllConflictHandledNotification() {
        console.info("Apply clicked before finishing merge.");
        notificationPaneController.addNotification("Not all conflicts have been handled. Are you sure you want to continue?");
    }
}
