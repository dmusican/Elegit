package elegit.controllers;

import com.sun.javafx.binding.BidirectionalBinding;
import de.jensd.fx.glyphs.GlyphIcons;
import de.jensd.fx.glyphs.GlyphsDude;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import elegit.Main;
import elegit.exceptions.ExceptionAdapter;
import elegit.gui.ConflictLineSection;
import elegit.models.ConflictManagementModel;
import elegit.models.RepoHelper;
import elegit.models.SessionModel;
import elegit.models.ConflictLine;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
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
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.RevCommit;
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

    private ArrayList<ConflictLine> leftConflictLines;

    private ArrayList<ConflictLine> middleConflictLines;

    private ArrayList<ConflictLine> rightConflictLines;

    private ArrayList<Integer> leftConflictingLineNumbers = new ArrayList<>();

    private ArrayList<Integer> middleConflictingLineNumbers = new ArrayList<>();

    private ArrayList<Integer> rightConflictingLineNumbers = new ArrayList<>();

    private boolean fileSelected = false;

    private IntFunction<Node> numberFactory;

    private static final Logger console = LogManager.getLogger("briefconsolelogger");

    private HashMap<String, CodeArea> files = new HashMap<>();

    private HashMap<String, String> mergeResult;

    synchronized void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public void initialize() {
        mergeResult=SessionModel.getSessionModel().getMergeResult();
        System.out.println(mergeResult.get("baseBranch")+"    "+mergeResult.get("mergedBranch"));
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

    private void initCodeAreas() {
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
        numberFactory = LineNumberFactory.get(doc);
//        doc.setParagraphGraphicFactory(LineNumberFactory.get(doc));
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
    private void handleAbort() {
        stage.close();
    }

    @FXML
    private void handleApplyChanges() {
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
        } catch (IOException e) {
            throw new ExceptionAdapter(e);
        }
    }

    @FXML
    private void handleToggleUp() {
        int currentLine = middleDoc.getCurrentParagraph();
        if (currentLine <= middleConflictingLineNumbers.get(0)) { // Go to the last conflict if at or above the first one
            moveDocCaretsToLastConflict();

        } else if (middleConflictingLineNumbers.size() == 1) { // Go to the only conflict
            moveDocCaretsToFirstConflict();

        } else { // Go to the previous conflict
            findConflictToGoTo(currentLine, 1);
        }
    }

    @FXML
    private void handleToggleDown() {
        int currentLine = middleDoc.getCurrentParagraph();
        if (currentLine >= middleConflictingLineNumbers.get(leftConflictingLineNumbers.size() - 1)) { // Go to the first conflict if at or above the last one
            moveDocCaretsToFirstConflict();

        } else if (middleConflictingLineNumbers.size() == 1) { // Go to the only conflict
            moveDocCaretsToFirstConflict();

        } else { // Go to the previous conflict
            findConflictToGoTo(currentLine, -1);
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

    private void findConflictToGoTo(int currentLine, int direction) {
        for (int i = 0; i < middleConflictingLineNumbers.size(); i++) {
            if (currentLine <= middleConflictingLineNumbers.get(i)) {
                moveDocCarets(leftDoc, leftConflictingLineNumbers.get(i - direction));
                moveDocCarets(middleDoc, middleConflictingLineNumbers.get(i - direction));
                moveDocCarets(rightDoc, rightConflictingLineNumbers.get(i - direction));
                return;
            }
        }
    }

    private void moveDocCarets(CodeArea doc, int lineNumber) {
        doc.moveTo(lineNumber, 0);
        doc.requestFollowCaret();
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

        leftConflictLines = results.get(0);
        middleConflictLines = results.get(1);
        rightConflictLines = results.get(2);
        getConflictingLineNumbers(leftConflictLines, leftConflictingLineNumbers);
        getConflictingLineNumbers(middleConflictLines, middleConflictingLineNumbers);
        getConflictingLineNumbers(rightConflictLines, rightConflictingLineNumbers);

        setLines(leftConflictLines, leftDoc);
        CodeArea middle = setLines(middleConflictLines, middleDoc);
        setLines(rightConflictLines, rightDoc);
        files.put(fileName, middle);
        //getParentFiles(fileName);
        // Allow the user to click buttons
        setButtonsDisabled(false);
        // Move the caret and doc to the first conflict
        setInitialPositions(leftDoc, leftConflictingLineNumbers);
        setInitialPositions(middleDoc, middleConflictingLineNumbers);
        setInitialPositions(rightDoc, rightConflictingLineNumbers);
    }

    private ArrayList<String> getBaseParentFiles(String fileName){
        try {
            Repository repository = SessionModel.getSessionModel().getCurrentRepoHelper().getRepo();
            RevWalk revWalk = new RevWalk(repository);
            System.out.println(mergeResult.get("baseParent"));
            ObjectId baseParent = ObjectId.fromString(mergeResult.get("baseParent").substring(7,47));
            System.out.println(baseParent);
            RevTree baseTree = revWalk.parseCommit(baseParent).getTree();
            TreeWalk baseTreeWalk = new TreeWalk(repository);
            baseTreeWalk.addTree(baseTree);
            baseTreeWalk.setRecursive(true);
            baseTreeWalk.setFilter(PathFilter.create(fileName));
            if (!baseTreeWalk.next()) {
                throw new IllegalStateException("Did not find expected file");
            }
            ObjectId baseObjectId = baseTreeWalk.getObjectId(0);

            ObjectLoader baseLoader = repository.open(baseObjectId);
            String baseString = new String(baseLoader.getBytes());
            System.out.println(baseString);
            return new  ArrayList<String>(Arrays.asList(baseString.split("\n")));
        } catch (IOException e){
            throw new ExceptionAdapter(e);
        }


    }
    private ArrayList<String> getMergedParentFiles(String fileName){
        try {
            Repository repository = SessionModel.getSessionModel().getCurrentRepoHelper().getRepo();
            RevWalk revWalk = new RevWalk(repository);
            System.out.println(mergeResult.get("mergedParent"));
            ObjectId mergedParent = ObjectId.fromString(mergeResult.get("mergedParent").substring(7,47));
            System.out.println(mergedParent);
            RevTree mergedTree = revWalk.parseCommit(mergedParent).getTree();
            TreeWalk mergedTreeWalk = new TreeWalk(repository);
            mergedTreeWalk.addTree(mergedTree);
            mergedTreeWalk.setRecursive(true);
            mergedTreeWalk.setFilter(PathFilter.create(fileName));
            if (!mergedTreeWalk.next()) {
                throw new IllegalStateException("Did not find expected file");
            }
            ObjectId mergedObjectId = mergedTreeWalk.getObjectId(0);

            ObjectLoader mergedLoader = repository.open(mergedObjectId);
            String mergedString = new String(mergedLoader.getBytes());
            System.out.println(mergedString);
            return new  ArrayList<String>(Arrays.asList(mergedString.split("\n")));
        } catch (IOException e){
            throw new ExceptionAdapter(e);
        }


    }
    private void getConflictingLineNumbers(ArrayList<ConflictLine> doc, ArrayList<Integer> conflictingLineNumbers) {
        int lineNumber = -1;
        for (ConflictLine conflictLine : doc) {
            lineNumber += conflictLine.getLines().size();
            if (conflictLine.isConflicting()) {
                conflictingLineNumbers.add(lineNumber);
            }
        }
    }

    private void setInitialPositions(CodeArea doc, ArrayList<Integer> conflictingLineNumbers) {
        doc.moveTo(conflictingLineNumbers.get(0), 0);
        doc.requestFollowCaret();
    }
    private CodeArea setLines(ArrayList lines, CodeArea doc) {
        for (int i = 0; i < lines.size(); i++) {
            ConflictLine conflict = (ConflictLine) lines.get(i);
            ArrayList<String> conflictLines = conflict.getLines();
            if (conflict.isConflicting()) {
                setConflictLineDividers(doc, doc.currentParagraphProperty(), conflictLines.size());
            }
            for (String line : conflictLines) {

                int startIndex = doc.getCaretPosition();
                // update the document
                doc.appendText(line + "\n");
                int endIndex = doc.getLength();
                if (conflict.isConflicting()) {
                    setCSSSelector(doc, startIndex, endIndex);
                }
            }
        }
        return doc;
    }

    private void setCSSSelector(CodeArea doc, int startIndex, int endIndex) {
        doc.setStyle(startIndex, endIndex, Collections.singleton("conflict"));
    }

    private void setConflictLineDividers(CodeArea doc, ObservableValue<Integer> lineNumber, int height) {
        // Get the number of lines somehow
        IntFunction<Node> conflictLineSection = new ConflictLineSection(lineNumber, height);
        IntFunction<Node> graphicFactory = line -> {
            HBox hbox = new HBox(
                    numberFactory.apply(line),
                    conflictLineSection.apply(line));
            hbox.setAlignment(Pos.CENTER_LEFT);
            return hbox;
        };
        doc.setParagraphGraphicFactory(graphicFactory);
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
