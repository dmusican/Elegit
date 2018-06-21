package elegit.controllers;

import com.sun.javafx.binding.BidirectionalBinding;
import de.jensd.fx.glyphs.GlyphIcons;
import de.jensd.fx.glyphs.GlyphsDude;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import elegit.Main;
import elegit.exceptions.ExceptionAdapter;
import elegit.models.ConflictManagementModel;
import elegit.models.SessionModel;
import elegit.models.ConflictLine;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import net.jcip.annotations.GuardedBy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.StyledTextArea;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.file.Path;
import java.util.*;
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
    private CodeArea leftDoc;
    @FXML
    private CodeArea middleDoc;
    @FXML
    private CodeArea rightDoc;
    @FXML
    private Button rightAccept;
    @FXML
    private Button rightReject;
    @FXML
    private Button rightUndo;
    @FXML
    private Button leftAccept;
    @FXML
    private Button leftReject;
    @FXML
    private Button leftUndo;
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
    @FXML
    private Stage stage;

    private boolean fileSelected = false;

    private static final Logger console = LogManager.getLogger("briefconsolelogger");

    private HashMap<String, CodeArea> files = new HashMap<>();

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

        // Undo change buttons
        initButton(FontAwesomeIcon.UNDO, "undoIcon", rightUndo, "Undo previous choice.");
        initButton(FontAwesomeIcon.UNDO, "undoIcon", leftUndo, "Undo previous choice.");

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
        // Add line numbers to each CodeArea
        addLineNumbers(rightDoc);
        addLineNumbers(middleDoc);
        addLineNumbers(leftDoc);

        // Bind xvalues
        bindHorizontalScroll(rightDoc, middleDoc);
        bindHorizontalScroll(middleDoc, leftDoc);
        bindHorizontalScroll(leftDoc, rightDoc);


        // Bind yvalues
        bindVerticalScroll(rightDoc, middleDoc);
        bindVerticalScroll(middleDoc, leftDoc);
        bindVerticalScroll(leftDoc, rightDoc);
    }

    private void addLineNumbers(CodeArea doc) {
        doc.setParagraphGraphicFactory(LineNumberFactory.get(doc));
    }

    private void bindHorizontalScroll(CodeArea doc1, CodeArea doc2) {
        doc1.estimatedScrollXProperty().bindBidirectional(doc2.estimatedScrollXProperty());
    }

    private void bindVerticalScroll(CodeArea doc1, CodeArea doc2) {
        doc1.estimatedScrollYProperty().bindBidirectional(doc2.estimatedScrollYProperty());
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
        stage = new Stage();
        stage.setTitle("Conflict Management Tool");
        stage.setScene(new Scene(anchorRoot));
        stage.setResizable(false);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setOnCloseRequest(event -> console.info("Closed conflict management tool"));
        stage.show();
        notificationPaneController.setAnchor(stage);
    }

    //this code should be saved for when we implement saving changes made when switching between conflicting files
    /*@FXML
    private void handleApplyAllChanges(){
        //add in a check to see if conflicts remain
        files.put(conflictingFilesDropdown.getPromptText(), middleDoc);
        try {
            BufferedWriter writer;
            for (String fileName : conflictingFilesDropdown.getItems()) {
                Path directory = (new File(SessionModel.getSessionModel().getCurrentRepoHelper().getRepo().getDirectory()
                        .getParent())).toPath();
                String filePathWithoutFileName = directory.toString();
                writer = new BufferedWriter(new FileWriter(filePathWithoutFileName + File.separator + fileName));
                CodeArea value = files.get(fileName);
                if(value!=null){
                    writer.write(value.getText());
                }
                writer.flush();
                writer.close();
            }
            stage.close();
        }
        catch (IOException e) {
            throw new ExceptionAdapter(e);
        }
    }*/

    @FXML
    private void handleAbort(){
        stage.close();
    }

    @FXML
    private void handleApplyChanges(){
        //add in a check to see if conflicts remain
        try {
            Path directory = (new File(SessionModel.getSessionModel().getCurrentRepoHelper().getRepo().getDirectory()
                    .getParent())).toPath();
            String filePathWithoutFileName = directory.toString();
            String fileName = conflictingFilesDropdown.getPromptText();
            BufferedWriter writer = new BufferedWriter(new FileWriter(filePathWithoutFileName + File.separator + fileName));
            writer.write(middleDoc.getText());
            writer.flush();
            writer.close();
            stage.close();
        } catch (IOException e){
            throw new ExceptionAdapter(e);
        }
    }

    @FXML
    private void handleToggleUp() {
    }

    @FXML
    private void handleToggleDown() {
    }

    @FXML
    private void handleAcceptChange() {
    }

    @FXML
    private void handleRejectChange() {
    }

    @FXML
    private synchronized void handleUndoChange() {
    }

    @FXML
    private void setFileToEdit() {
        Main.assertFxThread();
        setDropdownValueToFileName();

        // TODO: save the state of each file

        clearTextAreas();
        updateTextAreasWithNewFile();
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

    private void setLabels(ConflictManagementModel conflictManagementModel) {
        leftDocLabel.setText(conflictManagementModel.getBaseBranch());
        middleDocLabel.setText("Result");
        rightDocLabel.setText(conflictManagementModel.getMergedBranch());
    }

    public void setFile(String filePathWithoutFileName, String fileName) {
        fileSelected = true;
        conflictingFilesDropdown.setPromptText(fileName);
        ConflictManagementModel conflictManagementModel = new ConflictManagementModel();
        ArrayList<ArrayList> results = conflictManagementModel.parseConflicts(filePathWithoutFileName +
                File.separator + fileName);

        setLabels(conflictManagementModel);

        setLines(results.get(0), leftDoc);
        CodeArea middle = setLines(results.get(1), middleDoc);
        setLines(results.get(2), rightDoc);
        files.put(fileName, middle);

        // Allow the user to click buttons
        setButtonsDisabled(false);
    }

    private CodeArea setLines(ArrayList lines, CodeArea doc) {
        for (int i = 0; i < lines.size(); i++) {
            ConflictLine conflict = (ConflictLine) lines.get(i);
            ArrayList<String> conflictLines = conflict.getLines();
            for (String line : conflictLines) {
                int startIndex = doc.getCaretPosition();
                // update the document
                doc.appendText(line + "\n");
                int endIndex = doc.getLength();
                setCSSSelector(conflict, doc, startIndex, endIndex);
            }
        }
        return doc;
    }

    private void setCSSSelector(ConflictLine conflictLine, CodeArea doc, int startIndex, int endIndex) {
        if (conflictLine.isConflicting()) {
            doc.setStyle(startIndex, endIndex, Collections.singleton("conflict"));
        }
    }

    private void showAllConflictsHandledNotification() {
        console.info("All conflicts were handled.");
        notificationPaneController.addNotification("All conflicts were handled. Click apply to use them.");
    }

    // TODO: add a button or something that allows them to continue
    private void showNotAllConflictHandledNotification() {
        console.info("Apply clicked before finishing merge.");
        notificationPaneController.addNotification("Not all conflicts have been handled. Are you sure you want to continue?");
    }
}
