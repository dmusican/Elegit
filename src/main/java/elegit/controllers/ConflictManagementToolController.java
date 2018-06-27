package elegit.controllers;

import de.jensd.fx.glyphs.GlyphIcons;
import de.jensd.fx.glyphs.GlyphsDude;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import elegit.Main;
import elegit.exceptions.ExceptionAdapter;
import elegit.gui.ConflictLinePointer;
import elegit.models.ConflictManagementModel;
import elegit.models.SessionModel;
import elegit.models.ConflictLine;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import net.jcip.annotations.GuardedBy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.io.File;
import java.util.function.IntFunction;

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

    private ArrayList<ConflictLine> leftAllConflictLines;

    private ArrayList<ConflictLine> middleAllConflictLines;

    private ArrayList<ConflictLine> rightAllConflictLines;

    private ArrayList<ConflictLine> leftConflictLines = new ArrayList<>();

    private ArrayList<ConflictLine> middleConflictLines = new ArrayList<>();

    private ArrayList<ConflictLine> rightConflictLines = new ArrayList<>();

    private ArrayList<Integer> leftConflictingLineNumbers = new ArrayList<>();

    private ArrayList<Integer> middleConflictingLineNumbers = new ArrayList<>();

    private ArrayList<Integer> rightConflictingLineNumbers = new ArrayList<>();

    private boolean fileSelected = false;

    private int conflictsLeftToHandle;

    private HashMap<String, CodeArea> files = new HashMap<>();

    private HashMap<String, String> mergeResult;

    private static final Logger logger = LogManager.getLogger();

    private static final Logger console = LogManager.getLogger("briefconsolelogger");

    synchronized void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public void initialize() {
        mergeResult = SessionModel.getSessionModel().getMergeResult();
        initButtons();
        initDropdown();
        initCodeAreas();
        // Disable everything except the dropdown if the user has not specified a file to solve merge conflicts for yet
        if (!fileSelected) {
            setButtonsDisabled(true);
        }
    }

    /**
     * Add graphics and tool tips for all of the buttons in the tool.
     */
    private void initButtons() {
        // Accept change buttons
        initButton(FontAwesomeIcon.CHECK, "checkIcon", leftAccept, "Integrate the highlighted commit.");
        initButton(FontAwesomeIcon.CHECK, "checkIcon", rightAccept, "Integrate the highlighted commit.");

        // Reject change buttons
        initButton(FontAwesomeIcon.TIMES, "xIcon", leftReject, "Ignore the highlighted commit.");
        initButton(FontAwesomeIcon.TIMES, "xIcon", rightReject, "Ignore the highlighted commit.");

        // Undo change buttons
        initButton(FontAwesomeIcon.UNDO, "undoIcon", leftUndo, "Undo previous choice.");
        initButton(FontAwesomeIcon.UNDO, "undoIcon", rightUndo, "Undo previous choice.");

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

    private void initCodeAreas() {
        // Add line numbers and pointers to each CodeArea
        addLineNumbersAndPointers(leftDoc);
        addLineNumbersAndPointers(middleDoc);
        addLineNumbersAndPointers(rightDoc);

        // Bind xvalues
        bindHorizontalScroll(leftDoc, rightDoc);
        bindHorizontalScroll(middleDoc, leftDoc);
        bindHorizontalScroll(rightDoc, middleDoc);


        // Bind yvalues
        bindVerticalScroll(leftDoc, rightDoc);
        bindVerticalScroll(middleDoc, leftDoc);
        bindVerticalScroll(rightDoc, middleDoc);
    }

    private void addLineNumbersAndPointers(CodeArea doc) {
        IntFunction<Node> numberFactory = LineNumberFactory.get(doc);
        IntFunction<Node> conflictLinePointer = new ConflictLinePointer(doc.currentParagraphProperty());
        IntFunction<Node> graphicFactory = line -> {
            HBox hbox = new HBox(
                    numberFactory.apply(line),
                    conflictLinePointer.apply(line));
            hbox.setAlignment(Pos.CENTER_LEFT);
            return hbox;
        };
        doc.setParagraphGraphicFactory(graphicFactory);
    }

    private void bindHorizontalScroll(CodeArea doc1, CodeArea doc2) {
//        doc1.estimatedScrollXProperty().values().feedTo(doc2.estimatedScrollXProperty());
//        doc2.estimatedScrollXProperty().values().feedTo(doc1.estimatedScrollXProperty());
//        doc1.estimatedScrollXProperty().bindBidirectional(doc2.estimatedScrollXProperty());
    }

    private void bindVerticalScroll(CodeArea doc1, CodeArea doc2) {
//        doc1.estimatedScrollYProperty().values().feedTo(doc2.estimatedScrollYProperty());
//        doc2.estimatedScrollYProperty().values().feedTo(doc1.estimatedScrollYProperty());
//        doc1.estimatedScrollYProperty().bindBidirectional(doc2.estimatedScrollYProperty());
    }

    private void setButtonsDisabled(boolean disabled) {
        leftAccept.setDisable(disabled);
        leftReject.setDisable(disabled);
        leftUndo.setDisable(disabled);
        rightAccept.setDisable(disabled);
        rightReject.setDisable(disabled);
        rightUndo.setDisable(disabled);
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
        stage.setOnCloseRequest(event -> logger.info("Closed conflict management tool"));
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
    private void handleAbort() {
        logger.info("Conflict management session aborted.");
        stage.close();
    }

    @FXML
    private void handleApplyChanges() {
        logger.info("Changes made with the conflict management tool applied.");
        if (conflictsLeftToHandle != 0) {
            showNotAllConflictHandledNotification();
            // TODO: allow them to override somehow
            return;
        }

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
        } catch (IOException e) {
            throw new ExceptionAdapter(e);
        }
    }

    @FXML
    private void handleToggleUp() {
        logger.info("Toggled up.");
        setModifyingButtonsDisabled(false);

        int currentLine = middleDoc.getCurrentParagraph();
        if (currentLine <= middleConflictingLineNumbers.get(0)) { // Go to the last conflict if at or above the first one
            moveDocCaretsToLastConflict();

        } else if (middleConflictingLineNumbers.size() == 1) { // Go to the only conflict
            moveDocCaretsToFirstConflict();

        } else { // Go to the previous conflict
            findConflictToGoTo(currentLine, true);
        }
    }

    @FXML
    private void handleToggleDown() {
        logger.info("Toggle down.");
        setModifyingButtonsDisabled(false);

        int currentLine = middleDoc.getCurrentParagraph();
        if (currentLine >= middleConflictingLineNumbers.get(leftConflictingLineNumbers.size() - 1)) { // Go to the first conflict if at or above the last one
            moveDocCaretsToFirstConflict();

        } else if (middleConflictingLineNumbers.size() == 1) { // Go to the only conflict
            moveDocCaretsToFirstConflict();

        } else { // Go to the previous conflict
            findConflictToGoTo(currentLine, false);
        }
    }

    private void moveDocCaretsToLastConflict() {
        moveDocCarets(leftDoc, leftConflictingLineNumbers.get(leftConflictingLineNumbers.size() - 1));
        moveDocCarets(middleDoc, middleConflictingLineNumbers.get(middleConflictingLineNumbers.size() - 1));
        moveDocCarets(rightDoc, rightConflictingLineNumbers.get(rightConflictingLineNumbers.size() - 1));
    }

    private void moveDocCaretsToFirstConflict() {
        moveDocCarets(leftDoc, leftConflictingLineNumbers.get(0));
        moveDocCarets(middleDoc, middleConflictingLineNumbers.get(0));
        moveDocCarets(rightDoc, rightConflictingLineNumbers.get(0));
    }

    private void findConflictToGoTo(int currentLine, boolean up) {
        for (int i = 0; i < middleConflictingLineNumbers.size(); i++) {
            if (up) { // Toggling up
                if (currentLine <= middleConflictingLineNumbers.get(i)) {
                    moveDocCaretsGivenDirection(i, -1);
                    return;
                }
            } else { // Toggling down
                if (currentLine == middleConflictingLineNumbers.get(i)) {
                    moveDocCaretsGivenDirection(i, 1);
                    return;
                } else if (currentLine < middleConflictingLineNumbers.get(i)) {
                    moveDocCaretsGivenDirection(i, 0);
                    return;
                }
            }
        }
    }

    private void moveDocCaretsGivenDirection(int i, int direction) {
        moveDocCarets(leftDoc, leftConflictingLineNumbers.get(i + direction));
        moveDocCarets(middleDoc, middleConflictingLineNumbers.get(i + direction));
        moveDocCarets(rightDoc, rightConflictingLineNumbers.get(i + direction));
    }

    private void moveDocCarets(CodeArea doc, int lineNumber) {
        doc.moveTo(lineNumber, 0);
        doc.requestFollowCaret();
    }

    @FXML
    private void handleAcceptLeftChange() {
        logger.info("Change on left document accepted.");
        handleChange(leftDoc, leftConflictingLineNumbers, leftConflictLines, true);
    }

    @FXML
    private void handleAcceptRightChange() {
        logger.info("Change on right document accepted.");
        handleChange(rightDoc, rightConflictingLineNumbers, rightConflictLines, true);
    }

    @FXML
    private void handleRejectLeftChange() {
        logger.info("Change on left document rejected.");
        handleChange(leftDoc, leftConflictingLineNumbers, leftConflictLines, false);
    }

    @FXML
    private void handleRejectRightChange() {
        logger.info("Change on right document rejected.");
        handleChange(rightDoc, rightConflictingLineNumbers, rightConflictLines, false);
    }

    private void handleChange(CodeArea doc, ArrayList<Integer> conflictingLineNumbers,
                              ArrayList<ConflictLine> conflictLines, boolean accepting) {
        int currentLine = doc.getCurrentParagraph();

        for (int conflictLineIndex = 0; conflictLineIndex < conflictingLineNumbers.size(); conflictLineIndex++) {
            int lineNumber = conflictingLineNumbers.get(conflictLineIndex);

            if (lineNumber == currentLine && !conflictLines.get(conflictLineIndex).isHandled()) {
                if (accepting) {
                    updateMiddleDoc(conflictLines, conflictLineIndex);
                }
                updateSideDocCSS(doc, conflictingLineNumbers.get(conflictLineIndex), conflictLines.get(conflictLineIndex).getLines().size(), "handled-conflict");
                updateConflictLineStatus(conflictLines, conflictLineIndex, true, false);

                updateAndCheckConflictsLeftToHandle();
                return;

            } else if (lineNumber == currentLine) { // Already handled this conflict
                showAcceptOrRejectWarning(accepting);
                return;
            }
        }
        showAcceptOrRejectWarning(accepting); // Not a conflict
    }

    private void updateMiddleDoc(ArrayList<ConflictLine> conflictLines, int conflictLineIndex) {
        for (String line : conflictLines.get(conflictLineIndex).getLines()) {
            updateCurrentLine(line);
            middleConflictLines.get(conflictLineIndex).addLine(line);
        }

        updateMiddleConflictingLineNumbers(conflictLines.get(conflictLineIndex).getLines().size(), conflictLineIndex);
        updateMiddleDocCurrentLine(conflictLineIndex);
    }

    private void updateCurrentLine(String line) {
        int startIndex = middleDoc.getCaretPosition();
        middleDoc.insertText(middleDoc.getCurrentParagraph(), 0, line + "\n");
        int endIndex = middleDoc.getCaretPosition();
        setCSSSelector(middleDoc, startIndex, endIndex, "handled-conflict");
    }

    private void updateMiddleConflictingLineNumbers(int numLines, int conflictLineIndex) {
        for (int i = conflictLineIndex + 1; i < middleConflictingLineNumbers.size(); i++) {
            middleConflictingLineNumbers.set(i, middleConflictingLineNumbers.get(i) + numLines);
        }
    }

    private void updateMiddleDocCurrentLine(int conflictLineIndex) {
        middleDoc.moveTo(middleConflictingLineNumbers.get(conflictLineIndex), 0);
    }

    private void updateSideDocCSS(CodeArea doc, int startOfConflict, int numLines, String selector) {
        int startIndex = doc.getCaretPosition();
        doc.moveTo(startOfConflict + numLines, 0);
        int endIndex = doc.getCaretPosition();
        setCSSSelector(doc, startIndex, endIndex, selector);
        doc.moveTo(startOfConflict, 0);
    }

    private void updateConflictLineStatus(ArrayList<ConflictLine> conflictLines, int conflictLineIndex, boolean handled, boolean conflicting) {
        middleConflictLines.get(conflictLineIndex).setHandledStatus(handled);
        middleConflictLines.get(conflictLineIndex).setConflictStatus(conflicting);
        conflictLines.get(conflictLineIndex).setHandledStatus(handled);
        conflictLines.get(conflictLineIndex).setConflictStatus(conflicting);
    }

    private void updateAndCheckConflictsLeftToHandle() {
        conflictsLeftToHandle--;
        if (conflictsLeftToHandle == 0) {
            showAllConflictsHandledNotification();
        }
    }

    private void showAcceptOrRejectWarning(boolean accepting) {
        if (accepting) {
            showAttemptingToAcceptANonConflictNotification();
        } else {
            showAttemptingToRejectANonConflictNotification();
        }
    }

    @FXML
    private void handleUndoLeftChange() {
        logger.info("Modification undone from left document.");
        handleUndoChange(leftDoc, leftConflictLines, leftConflictingLineNumbers);
    }

    @FXML
    private void handleUndoRightChange() {
        logger.info("Modification undone from right document.");
        handleUndoChange(rightDoc, rightConflictLines, rightConflictingLineNumbers);
    }

    private void handleUndoChange(CodeArea doc, ArrayList<ConflictLine> conflictLines, ArrayList<Integer> conflictingLineNumbers) {
        int currentLine = doc.getCurrentParagraph();

        // Find the conflictLineIndex
        for (int conflictLineIndex = 0; conflictLineIndex < conflictingLineNumbers.size(); conflictLineIndex++) {
            int lineNumber = conflictingLineNumbers.get(conflictLineIndex);

            if (lineNumber == currentLine && conflictLines.get(conflictLineIndex).isHandled()) { // Found the index and line number and it has been handled
                // Find the actual string in the ConflictLine for the middleDoc
                for (int i = 0; i < middleConflictLines.get(conflictLineIndex).getLines().size(); i++) {
                    String line = middleConflictLines.get(conflictLineIndex).getLines().get(i);

                    if (line.equals(conflictLines.get(conflictLineIndex).getLines().get(0))) { // Found the first line in the middle ConflictLine
                        // Remove the text from the conflict line
                        for (int j = 0; j < conflictLines.get(conflictLineIndex).getLines().size(); j++) {
                            middleConflictLines.get(conflictLineIndex).getLines().remove(i);
                        }
                        // Remove the text from the CodeArea
                        removeChangeFromMiddleDoc(conflictLines, conflictLineIndex, i);

                        // Update everything else
                        updateMiddleConflictingLineNumbers(-(conflictLines.get(conflictLineIndex).getLines().size()), conflictLineIndex);
                        updateMiddleDocCurrentLine(conflictLineIndex);
                        updateConflictLineStatus(conflictLines, conflictLineIndex, false, true);
                        updateSideDocCSS(doc, conflictingLineNumbers.get(conflictLineIndex), conflictLines.get(conflictLineIndex).getLines().size(), "conflict");
                        return;
                    }
                }

                // If it gets here, the modification must have been reject because the text is not in the ConflictLine
                // Only update the css and ConflictLine status
                updateSideDocCSS(doc, conflictingLineNumbers.get(conflictLineIndex), conflictLines.get(conflictLineIndex).getLines().size(), "conflict");
                updateConflictLineStatus(conflictLines, conflictLineIndex, false, true);
                return;

            } else if (lineNumber == currentLine) { // They clicked undo, but it was not yet handled
                showNoModificationToUndo();
                return;
            }
        }
    }

    private void removeChangeFromMiddleDoc(ArrayList<ConflictLine> conflictLines, int conflictLineIndex, int i) {
        int startOfConflict = middleDoc.getCurrentParagraph();
        int numLinesToRemove = conflictLines.get(conflictLineIndex).getLines().size();
        int numLinesAbove;

        if (i == 0) { // Was added first, meaning the second changed added (if there) is above it in CodeArea
            numLinesAbove = middleConflictLines.get(conflictLineIndex).getLines().size();
        } else {
            numLinesAbove = 0;
        }
        middleDoc.selectRange(startOfConflict + numLinesAbove, 0, startOfConflict + numLinesAbove + numLinesToRemove, 0);
        middleDoc.deleteText(middleDoc.getSelection());
    }

    @FXML
    private void setFileToEdit() {
        logger.info("New file selected to edit in conflict management tool.");
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
        leftDocLabel.setText(mergeResult.get("baseBranch"));
        middleDocLabel.setText("Result");
        rightDocLabel.setText(mergeResult.get("mergedBranch"));
    }

    public void setFile(String filePathWithoutFileName, String fileName) {
        fileSelected = true;
        conflictingFilesDropdown.setPromptText(fileName);
        ConflictManagementModel conflictManagementModel = new ConflictManagementModel();
        ArrayList<ArrayList> results = conflictManagementModel.parseConflicts(filePathWithoutFileName +
                File.separator + fileName, getBaseParentFiles(fileName), getMergedParentFiles(fileName));

        setLabels(conflictManagementModel);

        leftAllConflictLines = results.get(0);
        middleAllConflictLines = results.get(1);
        rightAllConflictLines = results.get(2);

        getActualConflictingLines(leftAllConflictLines, leftConflictLines, leftConflictingLineNumbers);
        getActualConflictingLines(middleAllConflictLines, middleConflictLines, middleConflictingLineNumbers);
        getActualConflictingLines(rightAllConflictLines, rightConflictLines, rightConflictingLineNumbers);

        // This means that the user is required to accept or reject conflicts on both sides before applying. Could be handled differently
        conflictsLeftToHandle = leftConflictLines.size() + rightConflictLines.size();

        setLines(leftAllConflictLines, leftDoc);
        CodeArea middle = setLines(middleAllConflictLines, middleDoc);
        setLines(rightAllConflictLines, rightDoc);
        files.put(fileName, middle);
        //getParentFiles(fileName);
        // Allow the user to click buttons
        setButtonsDisabled(false);
        // Move the caret and doc to the first conflict
        setInitialPositions(leftDoc, leftConflictingLineNumbers);
        setInitialPositions(middleDoc, middleConflictingLineNumbers);
        setInitialPositions(rightDoc, rightConflictingLineNumbers);

        bindMouseMovementToConflict(leftDoc, leftConflictingLineNumbers, leftConflictLines);
        bindMouseMovementToConflict(middleDoc, middleConflictingLineNumbers, middleConflictLines);
        bindMouseMovementToConflict(rightDoc, rightConflictingLineNumbers, rightConflictLines);
    }

    private ArrayList<String> getParentFiles(ObjectId parent, String fileName) {
        try {
            Repository repository = SessionModel.getSessionModel().getCurrentRepoHelper().getRepo();
            RevWalk revWalk = new RevWalk(repository);
            RevTree revTree = revWalk.parseCommit(parent).getTree();
            TreeWalk treeWalk = new TreeWalk(repository);
            treeWalk.addTree(revTree);
            treeWalk.setRecursive(true);
            treeWalk.setFilter(PathFilter.create(fileName));
            if (!treeWalk.next()) {
                throw new IllegalStateException("Did not find expected file");
            }
            ObjectId objectId = treeWalk.getObjectId(0);

            ObjectLoader loader = repository.open(objectId);
            String string = new String(loader.getBytes());
            return new ArrayList<>(Arrays.asList(string.split("\n")));
        } catch (IOException e) {
            throw new ExceptionAdapter(e);
        }
    }

    private ArrayList<String> getBaseParentFiles(String fileName) {
        ObjectId baseParent = ObjectId.fromString(mergeResult.get("baseParent").substring(7, 47));
        //System.out.println(mergeResult.get("baseParent"));
        //System.out.println(baseParent);
        return getParentFiles(baseParent, fileName);
    }

    private ArrayList<String> getMergedParentFiles(String fileName) {
        ObjectId mergedParent = ObjectId.fromString(mergeResult.get("mergedParent").substring(7, 47));
        return getParentFiles(mergedParent, fileName);
    }

    private void getActualConflictingLines(ArrayList<ConflictLine> allConflictLines, ArrayList<ConflictLine> conflictLines, ArrayList<Integer> conflictingLineNumbers) {
        int lineNumber = 0;
        for (ConflictLine conflictLine : allConflictLines) {
            if (conflictLine.isConflicting()) {
                conflictingLineNumbers.add(lineNumber);
                conflictLines.add(conflictLine);
            }
            // Increment the number after add so that the arrow points to the beginning of the block.
            lineNumber += conflictLine.getLines().size();
        }
    }

    private void setInitialPositions(CodeArea doc, ArrayList<Integer> conflictingLineNumbers) {
        doc.moveTo(conflictingLineNumbers.get(0), 0);
        doc.requestFollowCaret();
    }

    private void bindMouseMovementToConflict(CodeArea doc, ArrayList<Integer> conflictingLineNumbers, ArrayList<ConflictLine> conflictLines) {
        doc.setOnMouseClicked(e -> {
            int currentLine = doc.getCurrentParagraph();

            for (int conflictLineIndex = 0; conflictLineIndex < conflictingLineNumbers.size(); conflictLineIndex++) {
                int lineNumber = conflictingLineNumbers.get(conflictLineIndex);

                // If the user clicks withing a line of a conflict on any of the documents, move all the docs there and all them to click buttons
                if ((currentLine == lineNumber || currentLine == lineNumber - 1) && conflictLines.get(conflictLineIndex).isConflicting()) {
                    moveDocCarets(leftDoc, leftConflictingLineNumbers.get(conflictLineIndex));
                    moveDocCarets(middleDoc, middleConflictingLineNumbers.get(conflictLineIndex));
                    moveDocCarets(rightDoc, rightConflictingLineNumbers.get(conflictLineIndex));

                    setModifyingButtonsDisabled(false);
                    return;
                } else if (currentLine < lineNumber) { // If they didn't click close enough don't let them use the buttons
                    setModifyingButtonsDisabled(true);
                    return;
                }
            }
        });
    }

    private void setModifyingButtonsDisabled(boolean disabled) {
        leftUndo.setDisable(disabled);
        leftReject.setDisable(disabled);
        leftAccept.setDisable(disabled);

        rightUndo.setDisable(disabled);
        rightReject.setDisable(disabled);
        rightAccept.setDisable(disabled);
    }

    private CodeArea setLines(ArrayList<ConflictLine> lines, CodeArea doc) {
        for (ConflictLine conflict : lines) {
            ArrayList<String> conflictLines = conflict.getLines();
            for (String line : conflictLines) {
                int startIndex = doc.getCaretPosition();
                doc.appendText(line + "\n");
                int endIndex = doc.getCaretPosition();

                if (conflict.isConflicting()) {
                    setCSSSelector(doc, startIndex, endIndex, "conflict");
                } else if (conflict.isChanged()) {
                    setCSSSelector(doc, startIndex, endIndex, "changed");
                }
            }
        }
        return doc;
    }

    private void setCSSSelector(CodeArea doc, int startIndex, int endIndex, String selector) {
        doc.setStyle(startIndex, endIndex, Collections.singleton(selector));
    }

    private void showAllConflictsHandledNotification() {
        logger.info("All conflicts were handled.");
        notificationPaneController.addNotification("All conflicts were handled. Click apply to use them.");
    }

    // TODO: add a button or something that allows them to continue
    private void showNotAllConflictHandledNotification() {
        logger.info("Apply clicked before finishing merge.");
        notificationPaneController.addNotification("Not all conflicts have been handled. Are you sure you want to continue?");
    }

    private void showAttemptingToAcceptANonConflictNotification() {
        logger.info("Accept conflict clicked when there is not a conflict to add (either not a conflict or already handled).");
        notificationPaneController.addNotification("You are either trying to integrate something that is not "
                + "conflicting \n or you already handled. Click the undo button if you made a mistake");
    }

    private void showAttemptingToRejectANonConflictNotification() {
        logger.info("Reject conflict clicked when there is not a conflict to reject (either not a conflict or already handled).");
        notificationPaneController.addNotification("You are either trying to ignore something that is not "
                + "conflicting \n or you already handled. Click the undo button if you made a mistake");
    }

    private void showNoModificationToUndo() {
        logger.info("Undo clicked when there was not previous modification made.");
        notificationPaneController.addNotification("Neither accept nor reject was clicked for this change \n"
                + "in this document, so there is nothing to undo.");
    }
}
