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
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.geometry.Side;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
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
    private Button applyAllChanges;
    @FXML
    private Button applyChanges;
    @FXML
    private Button abortMerge;
    @FXML
    private Button conflictManagementToolMenuButton;
    @FXML
    private ContextMenu conflictManagementToolMenu;
    @FXML
    private MenuItem disableAutoSwitchOption;
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

    private AtomicInteger conflictsLeftToHandle = new AtomicInteger(Integer.MAX_VALUE);

    private AtomicBoolean fileSelected = new AtomicBoolean(false);

    private AtomicBoolean autoSwitchConflicts = new AtomicBoolean(true);

    private AtomicBoolean applyWarningGiven = new AtomicBoolean(false);

    private HashMap<String, ArrayList<ArrayList>> savedParsedFiles = new HashMap<>();

    private HashMap<String, String> mergeResult;

    private static final Logger logger = LogManager.getLogger();

    private static final Logger console = LogManager.getLogger("briefconsolelogger");

    synchronized void setSessionController(SessionController sessionController) {
        Main.assertFxThread();
        this.sessionController = sessionController;
    }

    public void initialize() {
        Main.assertFxThread();
        try {
            mergeResult = SessionModel.getSessionModel().getMergeResult();
        } catch (NullPointerException e){
            //this should never happen
            sessionController.showNoRepoLoadedNotification();
            console.info("No current repo set");
            e.printStackTrace();
        }
        initButtons();
        initDropdown();
        initCodeAreas();
        // Disable everything except the dropdown if the user has not specified a file to solve merge conflicts for yet
        if (!fileSelected.get()) {
            setButtonsDisabled(true);
        }
    }

    /**
     * Add graphics and tool tips for all of the buttons in the tool.
     */
    private void initButtons() {
        Main.assertFxThread();
        // Accept change buttons
        initButton(FontAwesomeIcon.CHECK, "checkIcon", leftAccept, "Integrate the selected commit.");
        initButton(FontAwesomeIcon.CHECK, "checkIcon", rightAccept, "Integrate the selected commit.");

        // Reject change buttons
        initButton(FontAwesomeIcon.TIMES, "xIcon", leftReject, "Ignore the selected commit.");
        initButton(FontAwesomeIcon.TIMES, "xIcon", rightReject, "Ignore the selected commit.");

        // Undo change buttons
        initButton(FontAwesomeIcon.UNDO, "undoIcon", leftUndo, "Undo previous choice.");
        initButton(FontAwesomeIcon.UNDO, "undoIcon", rightUndo, "Undo previous choice.");

        // Toggle change buttons
        initButton(FontAwesomeIcon.ARROW_UP, "arrowIcon", upToggle, "Go to previous change.");
        initButton(FontAwesomeIcon.ARROW_DOWN, "arrowIcon", downToggle, "Go to next change.");

        // Menu, abort, and apply buttons
        initButton(FontAwesomeIcon.BARS, "menuIcon", conflictManagementToolMenuButton, "Menu for the conflict management tool.");
        abortMerge.setTooltip(new Tooltip("Ignore all changes made and \n return to previous state."));
        applyChanges.setTooltip(new Tooltip("Use the \"result\" document with \n the changes you've made."));
    }

    private void initButton(GlyphIcons glyphIcon, String id, Button button, String toolTip) {
        Main.assertFxThread();
        Text icon = GlyphsDude.createIcon(glyphIcon);
        icon.setId(id);
        button.setGraphic(icon);
        button.setTooltip(new Tooltip(toolTip));
    }

    private void initDropdown() {
        Main.assertFxThread();
        try {
            // Get the names of all the conflicting files and put them in the dropdown
            Set<String> conflictingFiles = SessionModel.getSessionModel().getConflictingFiles(null);
            for (String file : conflictingFiles) {
                conflictingFilesDropdown.getItems().add(file);
            }
            conflictingFilesDropdown.setPromptText("None");

        } catch (GitAPIException e) {
            throw new ExceptionAdapter(e);
        }
    }

    private void initCodeAreas() {
        Main.assertFxThread();
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
        Main.assertFxThread();
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
        Main.assertFxThread();
        doc1.estimatedScrollXProperty().bindBidirectional(doc2.estimatedScrollXProperty());
    }

    private void bindVerticalScroll(CodeArea doc1, CodeArea doc2) {
        Main.assertFxThread();
        // TODO: once bug #535 in RichTextFX is resolved, hopefully this can be added back in. Currently acts very weirdly with toggle buttons
//        doc1.estimatedScrollYProperty().bindBidirectional(doc2.estimatedScrollYProperty());
    }

    private void setButtonsDisabled(boolean disabled) {
        Main.assertFxThread();
        leftAccept.setDisable(disabled);
        leftReject.setDisable(disabled);
        leftUndo.setDisable(disabled);
        rightAccept.setDisable(disabled);
        rightReject.setDisable(disabled);
        rightUndo.setDisable(disabled);
        upToggle.setDisable(disabled);
        downToggle.setDisable(disabled);
        applyChanges.setDisable(disabled);
        applyAllChanges.setDisable(disabled);
        conflictManagementToolMenuButton.setDisable(disabled);
    }

    /**
     * Shows the tool.
     */
    void showStage(AnchorPane pane) {
        Main.assertFxThread();
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

    @FXML
    private void handleApplyAllChanges() {
        Main.assertFxThread();
        // TODO: add a check for if they have changes left
        saveParsedFiles();
        Path directory = (new File(SessionModel.getSessionModel().getCurrentRepoHelper().getRepo().getDirectory()
                .getParent())).toPath();
        String filePathWithoutFileName = directory.toString();
        String fileName = "";
        boolean unfinished = false;
        Iterator<String> files = conflictingFilesDropdown.getItems().iterator();
        while(files.hasNext()){
            String file = files.next();
            ArrayList<ArrayList> results = savedParsedFiles.get(file);
            if (results == null || getNumberOfConflicts(results.get(1)) != 0){
                fileName = file;
                unfinished = true;
            } else {
                ArrayList<ConflictLine> middle = results.get(1);
                try {
                    BufferedWriter writer = new BufferedWriter(new FileWriter(filePathWithoutFileName + File.separator + file));
                    for (ConflictLine conflictLine : middle) {
                        for (String line : conflictLine.getLines()) {
                            writer.write(line + "\n");
                        }
                    }
                    writer.flush();
                    writer.close();
                    files.remove();
                } catch (Exception e) {
                    throw new ExceptionAdapter(e);
                }
            }
        }
        if (unfinished) {
            setFileToEdit(fileName);
            showNotAllConflictHandledNotification();
        } else {
            stage.close();
        }
    }

    private int getNumberOfConflicts(ArrayList<ConflictLine> conflictLines) {
        int conflictsNumber = 0;
        for (ConflictLine line : conflictLines) {
            if (line.isConflicting() && !line.isHandled()) {
                conflictsNumber++;
            }
        }
        return conflictsNumber;
    }

    @FXML
    private void handleApplyChanges() {
        Main.assertFxThread();
        logger.info("Apply button clicked.");
        if (applyWarningGiven.get() || getNumberOfConflicts(middleAllConflictLines) == 0) { //conflictsLeftToHandle.get() <= 0) { // They've already seen the warning or have handled everything
            try {
                Path directory = (new File(SessionModel.getSessionModel().getCurrentRepoHelper().getRepo().getDirectory()
                        .getParent())).toPath();
                String filePathWithoutFileName = directory.toString();
                String fileName = conflictingFilesDropdown.getPromptText();
                BufferedWriter writer = new BufferedWriter(new FileWriter(filePathWithoutFileName + File.separator + fileName));
                writer.write(middleDoc.getText());
                writer.flush();
                writer.close();
                if (conflictingFilesDropdown.getItems().size() == 1) {
                    stage.close();
                } else {
                    conflictingFilesDropdown.getItems().remove(fileName);
                    setFileToEdit(conflictingFilesDropdown.getItems().get(0));
                }
            } catch (IOException e) {
                throw new ExceptionAdapter(e);
            }

        } else if (!applyWarningGiven.get() && conflictsLeftToHandle.get() > 0) { // They haven't handled everything and haven't see the warning
            applyWarningGiven.set(true);
            showNotAllConflictHandledNotification();

        } else { // Not sure
            // not sure when this would happen
            console.info("Weird thing happened with the apply button.");
            console.info("applyWarningGiven.get(): " + applyWarningGiven.get());
            console.info("conflictsLeftToHandle: " + conflictsLeftToHandle.get());
        }
    }

    @FXML
    private void handleOpenConflictManagementMenu() {
        Main.assertFxThread();
        logger.info("Conflict management menu opened.");
        conflictManagementToolMenu.show(conflictManagementToolMenuButton, Side.BOTTOM, -260, 3);
    }

    @FXML
    private void handleDisableAutoSwitchOption() {
        Main.assertFxThread();
        if (autoSwitchConflicts.get()) {
            logger.info("Auto-switch between conflicts disabled.");
            disableAutoSwitchOption.setText("Enable auto-switching between conflicts");
            autoSwitchConflicts.set(false);
        } else {
            logger.info("Auto-switch between conflicts enabled.");
            disableAutoSwitchOption.setText("Disable auto-switching between conflicts");
            autoSwitchConflicts.set(true);
        }
    }

    @FXML
    private void handleAbort() {
        Main.assertFxThread();
        logger.info("Conflict management session aborted.");
        String fileName = conflictingFilesDropdown.getPromptText();
        if (conflictingFilesDropdown.getItems().size()>1){
            conflictingFilesDropdown.getItems().remove(fileName);
            setFileToEdit(conflictingFilesDropdown.getItems().get(0));
        } else {
            stage.close();
        }
    }

    @FXML
    private void handleToggleUp() {
        Main.assertFxThread();
        logger.info("Toggled up.");
        setModifyingButtonsDisabled(false);

        int currentLine = middleDoc.getCurrentParagraph();
        if (middleConflictingLineNumbers.size()>0) {
            if (currentLine <= middleConflictingLineNumbers.get(0)) { // Go to the last conflict if at or above the first one
                moveDocCaretsToLastConflict();

            } else if (middleConflictingLineNumbers.size() == 1) { // Go to the only conflict
                moveDocCaretsToFirstConflict();

            } else { // Go to the previous conflict
                findConflictToGoTo(currentLine, true);
            }
        }
    }

    @FXML
    private void handleToggleDown() {
        Main.assertFxThread();
        logger.info("Toggled down.");
        setModifyingButtonsDisabled(false);

        int currentLine = middleDoc.getCurrentParagraph();
        if(leftConflictingLineNumbers.size()>0) {
            if (currentLine >= middleConflictingLineNumbers.get(leftConflictingLineNumbers.size() - 1)) { // Go to the first conflict if at or above the last one
                moveDocCaretsToFirstConflict();

            } else if (middleConflictingLineNumbers.size() == 1) { // Go to the only conflict
                moveDocCaretsToFirstConflict();

            } else { // Go to the previous conflict
                findConflictToGoTo(currentLine, false);
            }
        }
    }

    private void moveDocCaretsToLastConflict() {
        Main.assertFxThread();
        moveDocCarets(leftDoc, leftConflictingLineNumbers.get(leftConflictingLineNumbers.size() - 1));
        moveDocCarets(middleDoc, middleConflictingLineNumbers.get(middleConflictingLineNumbers.size() - 1));
        moveDocCarets(rightDoc, rightConflictingLineNumbers.get(rightConflictingLineNumbers.size() - 1));
    }

    private void moveDocCaretsToFirstConflict() {
        Main.assertFxThread();
        moveDocCarets(leftDoc, leftConflictingLineNumbers.get(0));
        moveDocCarets(middleDoc, middleConflictingLineNumbers.get(0));
        moveDocCarets(rightDoc, rightConflictingLineNumbers.get(0));
    }

    private void findConflictToGoTo(int currentLine, boolean up) {
        Main.assertFxThread();
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
        Main.assertFxThread();
        moveDocCarets(leftDoc, leftConflictingLineNumbers.get(i + direction));
        moveDocCarets(middleDoc, middleConflictingLineNumbers.get(i + direction));
        moveDocCarets(rightDoc, rightConflictingLineNumbers.get(i + direction));
    }

    private void moveDocCarets(CodeArea doc, int lineNumber) {
        Main.assertFxThread();
        doc.moveTo(lineNumber, 0);
        doc.showParagraphAtTop(lineNumber);
        // Needed because of bug #389 in RichTextFX - see github thread for more information
        doc.layout();

        // This check really shouldn't be necessary, but sometimes (especially in tests and at start up) this is called
        // before the list of visible paragraphs are complete, so .get() throws an exception. In this case conflict just
        // shows up at the top.
        //previously this used doc.lastVisibleParToAllParIndex() instead of size - 1, but that had bugs, and we don't have any non-visible paragraphs
        if (doc.allParToVisibleParIndex(lineNumber).isPresent() && doc.allParToVisibleParIndex(doc.getVisibleParagraphs().size() - 1).isPresent()) {
            //System.err.println("inside");
            int lineInViewport = doc.allParToVisibleParIndex(lineNumber).get();
            int totalLinesInViewport = doc.allParToVisibleParIndex(doc.lastVisibleParToAllParIndex()).get();

            // Negative numbers scroll up
            double deltaY = -((doc.getViewportHeight() / 2) - 50);

            // If the conflict is at the bottom of the document (i.e. showParagraphAtTop() doesn't change its position),
            // we don't want to scroll up because it'll get cut off
            if (totalLinesInViewport / 2 < lineInViewport) {
                deltaY = -deltaY;
            }

            doc.scrollYBy(deltaY);
        }
    }

    @FXML
    private void handleAcceptLeftChange() {
        Main.assertFxThread();
        logger.info("Change on left document accepted.");
        handleChange(leftDoc, leftConflictingLineNumbers, leftConflictLines, true);
    }

    @FXML
    private void handleAcceptRightChange() {
        Main.assertFxThread();
        logger.info("Change on right document accepted.");
        handleChange(rightDoc, rightConflictingLineNumbers, rightConflictLines, true);
    }

    @FXML
    private void handleRejectLeftChange() {
        Main.assertFxThread();
        logger.info("Change on left document rejected.");
        handleChange(leftDoc, leftConflictingLineNumbers, leftConflictLines, false);
    }

    @FXML
    private void handleRejectRightChange() {
        Main.assertFxThread();
        logger.info("Change on right document rejected.");
        handleChange(rightDoc, rightConflictingLineNumbers, rightConflictLines, false);
    }

    private void handleChange(CodeArea doc, ArrayList<Integer> conflictingLineNumbers,
                              ArrayList<ConflictLine> conflictLines, boolean accepting) {
        Main.assertFxThread();
        // If they start editing after getting the apply warning, we should still give it if they click apply early later on.
        applyWarningGiven.set(false);

        int currentLine = doc.getCurrentParagraph();

        for (int conflictLineIndex = 0; conflictLineIndex < conflictingLineNumbers.size(); conflictLineIndex++) {
            int lineNumber = conflictingLineNumbers.get(conflictLineIndex);

            if (lineNumber == currentLine && !conflictLines.get(conflictLineIndex).isHandled()) {
                if (accepting) {
                    updateMiddleDoc(conflictLines, conflictLineIndex);
                }
                updateSideDocCSS(doc, conflictingLineNumbers.get(conflictLineIndex), conflictLines.get(conflictLineIndex).getLines().size(), "handled-conflict");
                updateAndCheckConflictsLeftToHandle(conflictLineIndex);
                updateConflictLineStatus(conflictLines, conflictLineIndex, true);
                switchConflictLineOnBothSidesHandled(conflictLineIndex);
                return;

            } else if (lineNumber == currentLine) { // Already handled this conflict
                showAcceptOrRejectWarning(accepting);
                return;
            }
        }
        showAcceptOrRejectWarning(accepting); // Not a conflict
    }

    private void updateMiddleDoc(ArrayList<ConflictLine> conflictLines, int conflictLineIndex) {
        Main.assertFxThread();
        for (String line : conflictLines.get(conflictLineIndex).getLines()) {
            updateCurrentLine(line);
            middleConflictLines.get(conflictLineIndex).addLine(line);
        }

        updateMiddleConflictingLineNumbers(conflictLines.get(conflictLineIndex).getLines().size(), conflictLineIndex);
        updateMiddleDocCaretPosition(conflictLineIndex);
    }

    private void updateCurrentLine(String line) {
        Main.assertFxThread();
        int startIndex = middleDoc.getCaretPosition();
        middleDoc.insertText(middleDoc.getCurrentParagraph(), 0, line + "\n");
        int endIndex = middleDoc.getCaretPosition();
        setCSSSelector(middleDoc, startIndex, endIndex, "handled-conflict");
    }

    private void updateMiddleConflictingLineNumbers(int numLines, int conflictLineIndex) {
        Main.assertFxThread();
        for (int i = conflictLineIndex + 1; i < middleConflictingLineNumbers.size(); i++) {
            middleConflictingLineNumbers.set(i, middleConflictingLineNumbers.get(i) + numLines);
        }
    }

    private void updateMiddleDocCaretPosition(int conflictLineIndex) {
        Main.assertFxThread();
        middleDoc.moveTo(middleConflictingLineNumbers.get(conflictLineIndex), 0);
    }

    private void updateSideDocCSS(CodeArea doc, int startOfConflict, int numLines, String selector) {
        Main.assertFxThread();
        int startIndex = doc.getCaretPosition();
        doc.moveTo(startOfConflict + numLines, 0);
        int endIndex = doc.getCaretPosition();
        setCSSSelector(doc, startIndex, endIndex, selector);
        doc.moveTo(startOfConflict, 0);
    }

    private void updateAndCheckConflictsLeftToHandle(int conflictLineIndex) {
        Main.assertFxThread();
        if (!middleConflictLines.get(conflictLineIndex).isHandled()) {
            conflictsLeftToHandle.decrementAndGet();
            if (conflictsLeftToHandle.get() == 0) {
                showAllConflictsHandledNotification();
            }
        }
    }

    private void updateConflictLineStatus(ArrayList<ConflictLine> conflictLines, int conflictLineIndex, boolean handled) {
        Main.assertFxThread();
        conflictLines.get(conflictLineIndex).setHandledStatus(handled);
        if (leftConflictLines.get(conflictLineIndex).isHandled() || rightConflictLines.get(conflictLineIndex).isHandled()) {
            middleConflictLines.get(conflictLineIndex).setHandledStatus(true);
        } else {
            middleConflictLines.get(conflictLineIndex).setHandledStatus(false);
        }
    }

    private void switchConflictLineOnBothSidesHandled(int conflictLineIndex) {
        Main.assertFxThread();
        if (autoSwitchConflicts.get()) {
            if (leftConflictLines.get(conflictLineIndex).isHandled() && rightConflictLines.get(conflictLineIndex).isHandled()) {
                // The key part in this is the Thread.sleep() and the handleToggleDown(). The rest is so the highlighting, etc. will still happen.
                Task<Void> sleeper = new Task<Void>() {
                    @Override
                    protected Void call() throws Exception {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            console.info("Something went wrong with the sleep timer for auto switching between conflicts.");
                            throw new ExceptionAdapter(e);
                        }
                        return null;
                    }
                };
                sleeper.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
                    @Override
                    public void handle(WorkerStateEvent event) {
                        if (conflictsLeftToHandle.get() > 0) {
                            handleToggleDown();
                        }
                    }
                });
                new Thread(sleeper).start();
            }
        }
    }

    private void showAcceptOrRejectWarning(boolean accepting) {
        Main.assertFxThread();
        if (accepting) {
            showAttemptingToAcceptANonConflictNotification();
        } else {
            showAttemptingToRejectANonConflictNotification();
        }
    }

    @FXML
    private void handleUndoLeftChange() {
        Main.assertFxThread();
        logger.info("Modification undone from left document.");
        handleUndoChange(leftDoc, leftConflictLines, leftConflictingLineNumbers);
    }

    @FXML
    private void handleUndoRightChange() {
        Main.assertFxThread();
        logger.info("Modification undone from right document.");
        handleUndoChange(rightDoc, rightConflictLines, rightConflictingLineNumbers);
    }

    private void handleUndoChange(CodeArea doc, ArrayList<ConflictLine> conflictLines, ArrayList<Integer> conflictingLineNumbers) {

        Main.assertFxThread();
        // If they start editing after getting the apply warning, we should still give it if they click apply early later on.
        applyWarningGiven.set(false);

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
                            middleConflictLines.get(conflictLineIndex).removeLine(i);
                        }
                        // Remove the text from the CodeArea
                        removeChangeFromMiddleDoc(conflictLines, conflictLineIndex, i);

                        // Update everything else
                        updateMiddleConflictingLineNumbers(-(conflictLines.get(conflictLineIndex).getLines().size()), conflictLineIndex);
                        updateMiddleDocCaretPosition(conflictLineIndex);
                        updateConflictLineStatus(conflictLines, conflictLineIndex, false);
                        updateSideDocCSS(doc, conflictingLineNumbers.get(conflictLineIndex), conflictLines.get(conflictLineIndex).getLines().size(), "conflict");
                        updateConflictsLeftToHandleIfNeeded(conflictLineIndex);
                        return;
                    }
                }

                // If it gets here, the modification must have been reject because the text is not in the ConflictLine
                // Only update the css and ConflictLine status
                updateSideDocCSS(doc, conflictingLineNumbers.get(conflictLineIndex), conflictLines.get(conflictLineIndex).getLines().size(), "conflict");
                updateConflictLineStatus(conflictLines, conflictLineIndex, false);
                updateConflictsLeftToHandleIfNeeded(conflictLineIndex);
                return;

            } else if (lineNumber == currentLine) { // They clicked undo, but it was not yet handled
                showNoModificationToUndo();
                return;
            }
        }
    }

    private void updateConflictsLeftToHandleIfNeeded(int conflictLineIndex) {
        Main.assertFxThread();
        if (!middleConflictLines.get(conflictLineIndex).isHandled()) {
            conflictsLeftToHandle.incrementAndGet();
        }
    }

    private void removeChangeFromMiddleDoc(ArrayList<ConflictLine> conflictLines, int conflictLineIndex, int i) {
        Main.assertFxThread();
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

    private void saveParsedFiles() {
        ArrayList<ArrayList> parsed = new ArrayList<>();
        parsed.add(leftAllConflictLines);
        parsed.add(middleAllConflictLines);
        parsed.add(rightAllConflictLines);
        savedParsedFiles.put(conflictingFilesDropdown.getPromptText(), parsed);
    }

    private void setFileToEdit(String fileName) {
        //conflictingFilesDropdown.
        conflictingFilesDropdown.setValue(fileName);
        setFileToEdit();
    }

    @FXML
    private void setFileToEdit() {
        Main.assertFxThread();
        logger.info("New file selected to edit in conflict management tool.");
        Main.assertFxThread();
        saveParsedFiles();

        setDropdownValueToFileName();

        // TODO: save the state of each file

        clearTextAreas();
        updateTextAreasWithNewFile();
        setButtonsDisabled(false);
    }

    private void setDropdownValueToFileName() {
        Main.assertFxThread();
        // Show file in dropdown
        EventHandler<ActionEvent> handler = conflictingFilesDropdown.getOnAction();
        conflictingFilesDropdown.setOnAction(handler);
    }

    private void clearTextAreas() {
        Main.assertFxThread();
        // Clear the documents before loading new ones
        leftDoc.clear();
        middleDoc.clear();
        rightDoc.clear();
    }

    private void updateTextAreasWithNewFile() {
        Main.assertFxThread();
        // Get the path of the selected file and the name of the file
        Path directory = (new File(SessionModel.getSessionModel().getCurrentRepoHelper().getRepo().getDirectory()
                .getParent())).toPath();
        String filePathWithoutFileName = directory.toString();
        String fileName = conflictingFilesDropdown.getValue();

        // Show files in TextAreas
        setFile(filePathWithoutFileName, fileName);
    }

    public void setFile(String filePathWithoutFileName, String fileName) {
        Main.assertFxThread();
        fileSelected.set(true);
        conflictingFilesDropdown.setPromptText(fileName);
        ConflictManagementModel conflictManagementModel = new ConflictManagementModel();
        setLabels(conflictManagementModel);
        ArrayList<ArrayList> results = savedParsedFiles.get(fileName);
        if (results == null) {
            //results = conflictManagementModel.parseConflicts(fileName, filePathWithoutFileName,
             //       getBaseParentFiles(fileName), getMergedParentFiles(fileName));
            ObjectId baseParent = ObjectId.fromString(mergeResult.get("baseParent").substring(7, 47));
            ObjectId mergedParent = ObjectId.fromString(mergeResult.get("mergedParent").substring(7, 47));
            results = conflictManagementModel.parseConflictsFromParents(baseParent, mergedParent, fileName, filePathWithoutFileName);

        }
        leftAllConflictLines = results.get(0);
        middleAllConflictLines = results.get(1);
        rightAllConflictLines = results.get(2);

        getActualConflictingLines(leftAllConflictLines, leftConflictLines, leftConflictingLineNumbers);
        getActualConflictingLines(middleAllConflictLines, middleConflictLines, middleConflictingLineNumbers);
        getActualConflictingLines(rightAllConflictLines, rightConflictLines, rightConflictingLineNumbers);

        // once every conflict in the middle doc has been handled the user will get a notification to apply
        conflictsLeftToHandle.set(middleConflictLines.size());

        setLines(leftAllConflictLines, leftDoc);
        setLines(middleAllConflictLines, middleDoc);
        setLines(rightAllConflictLines, rightDoc);

        // Allow the user to click buttons
        setButtonsDisabled(false);

        bindMouseMovementToConflict(leftDoc, leftConflictingLineNumbers, leftConflictLines);
        bindMouseMovementToConflict(middleDoc, middleConflictingLineNumbers, middleConflictLines);
        bindMouseMovementToConflict(rightDoc, rightConflictingLineNumbers, rightConflictLines);

        // Move the caret and doc to the first conflict
        setInitialPositions(leftDoc, leftConflictingLineNumbers);
        setInitialPositions(middleDoc, middleConflictingLineNumbers);
        setInitialPositions(rightDoc, rightConflictingLineNumbers);
    }

    private void setLabels(ConflictManagementModel conflictManagementModel) {
        Main.assertFxThread();
        leftDocLabel.setText(mergeResult.get("baseBranch"));
        middleDocLabel.setText("Result");
        rightDocLabel.setText(mergeResult.get("mergedBranch"));
    }

    private ArrayList<String> getBaseParentFiles(String fileName) {
        Main.assertFxThread();
        ObjectId baseParent = ObjectId.fromString(mergeResult.get("baseParent").substring(7, 47));
        return getParentFiles(baseParent, fileName);
    }

    private ArrayList<String> getMergedParentFiles(String fileName) {
        Main.assertFxThread();
        ObjectId mergedParent = ObjectId.fromString(mergeResult.get("mergedParent").substring(7, 47));
        return getParentFiles(mergedParent, fileName);
    }

    //Code in getParentFiles adapted from the jgit-cookbook
    private ArrayList<String> getParentFiles(ObjectId parent, String fileName) {
        Main.assertFxThread();
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

    private void getActualConflictingLines(ArrayList<ConflictLine> allConflictLines, ArrayList<ConflictLine> conflictLines, ArrayList<Integer> conflictingLineNumbers) {
        Main.assertFxThread();
        int lineNumber = 0;
        conflictingLineNumbers.clear();
        conflictLines.clear();
        for (ConflictLine conflictLine : allConflictLines) {
            if (conflictLine.isConflicting()) {
                conflictingLineNumbers.add(lineNumber);
                conflictLines.add(conflictLine);
            }
            // Increment the number after add so that the arrow points to the beginning of the block.
            lineNumber += conflictLine.getLines().size();
        }
    }

    private CodeArea setLines(ArrayList<ConflictLine> lines, CodeArea doc) {
        Main.assertFxThread();
        for (ConflictLine conflict : lines) {
            List<String> conflictLines = conflict.getLines();
            for (String line : conflictLines) {
                int startIndex = doc.getCaretPosition();
                doc.appendText(line + "\n");
                int endIndex = doc.getCaretPosition();

                if (conflict.isHandled()) {
                    setCSSSelector(doc, startIndex, endIndex, "handled-conflict");
                } else if (conflict.isConflicting()) {
                    setCSSSelector(doc, startIndex, endIndex, "conflict");
                } else if (conflict.isChanged()) {
                    setCSSSelector(doc, startIndex, endIndex, "changed-conflict");
                }
            }
        }
        return doc;
    }

    private void setCSSSelector(CodeArea doc, int startIndex, int endIndex, String selector) {
        Main.assertFxThread();
        doc.setStyle(startIndex, endIndex, Collections.singleton(selector));
    }

    private void setInitialPositions(CodeArea doc, ArrayList<Integer> conflictingLineNumbers) {
        Main.assertFxThread();
        if (conflictingLineNumbers.size() != 0) {

            // Need the delay because otherwise the codeAreas aren't done loading
            // TODO: find a cleaner work around that works more consistently
            new Timer().schedule(new TimerTask() {
                public void run() {
                    Platform.runLater(() -> {
                        moveDocCarets(doc, conflictingLineNumbers.get(0));
                    });
                }
            }, 100);
        }
    }

    private void bindMouseMovementToConflict(CodeArea doc, ArrayList<Integer> conflictingLineNumbers, ArrayList<ConflictLine> conflictLines) {
        Main.assertFxThread();
        doc.setOnMouseClicked(e -> {
            int currentLine = doc.getCurrentParagraph();

            for (int conflictLineIndex = 0; conflictLineIndex < conflictingLineNumbers.size(); conflictLineIndex++) {
                int lineNumber = conflictingLineNumbers.get(conflictLineIndex);

                // If the user clicks within a line of a conflict on any of the documents, move all the docs there and allow them to click buttons
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
        Main.assertFxThread();
        leftUndo.setDisable(disabled);
        leftReject.setDisable(disabled);
        leftAccept.setDisable(disabled);

        rightUndo.setDisable(disabled);
        rightReject.setDisable(disabled);
        rightAccept.setDisable(disabled);
    }

    public NotificationController getNotificationPaneController() {
        return notificationPaneController;
    }

    private void showAllConflictsHandledNotification() {
        Main.assertFxThread();
        logger.info("All conflicts were handled.");
        notificationPaneController.addNotification("All conflicts were handled. Click apply to use them.");
    }

    private void showNotAllConflictHandledNotification() {
        Main.assertFxThread();
        logger.info("Apply clicked before finishing merge.");
        notificationPaneController.addNotification("Not all conflicts have been handled. Are you sure you want to continue?");
    }

    private void showAttemptingToAcceptANonConflictNotification() {
        Main.assertFxThread();
        logger.info("Accept conflict clicked when there is not a conflict to add (either not a conflict or already handled).");
        notificationPaneController.addNotification("You are either trying to integrate something that is not "
                + "conflicting or you already handled. Click the undo button if you made a mistake");
    }

    private void showAttemptingToRejectANonConflictNotification() {
        Main.assertFxThread();
        logger.info("Reject conflict clicked when there is not a conflict to reject (either not a conflict or already handled).");
        notificationPaneController.addNotification("You are either trying to ignore something that is not "
                + "conflicting or you already handled. Click the undo button if you made a mistake");
    }

    private void showNoModificationToUndo() {
        logger.info("Undo clicked when there was not previous modification made.");
        notificationPaneController.addNotification("Neither accept nor reject was clicked for this change "
                + "in this document, so there is nothing to undo.");
    }
}
