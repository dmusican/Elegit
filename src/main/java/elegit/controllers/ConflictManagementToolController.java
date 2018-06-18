package elegit.controllers;

import de.jensd.fx.glyphs.GlyphsDude;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import elegit.Main;
import elegit.models.RepoHelper;
import elegit.models.SessionModel;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.AnchorPane;
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

    private boolean fileSelected = false;

    private String selectedFileDirectory;

    private static final Logger console = LogManager.getLogger("briefconsolelogger");

    synchronized void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public void initialize() {
        initButtons();
        initDropdown();
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
        Text checkRight = GlyphsDude.createIcon(FontAwesomeIcon.CHECK);
        rightAccept.setGraphic(checkRight);
        Text checkLeft = GlyphsDude.createIcon(FontAwesomeIcon.CHECK);
        leftAccept.setGraphic(checkLeft);
        rightAccept.setTooltip(new Tooltip("Integrate the highlighted commit."));
        leftAccept.setTooltip(new Tooltip("Integrate the highlighted commit."));

        // Reject change buttons
        Text xRight = GlyphsDude.createIcon(FontAwesomeIcon.TIMES);
        rightReject.setGraphic(xRight);
        Text xLeft = GlyphsDude.createIcon(FontAwesomeIcon.TIMES);
        leftReject.setGraphic(xLeft);
        rightReject.setTooltip(new Tooltip("Ignore the highlighted commit."));
        leftReject.setTooltip(new Tooltip("Ignore the highlighted commit."));

        // Toggle between changes buttons
        Text arrowUp = GlyphsDude.createIcon(FontAwesomeIcon.ARROW_UP);
        upToggle.setGraphic(arrowUp);
        Text arrowDown = GlyphsDude.createIcon(FontAwesomeIcon.ARROW_DOWN);
        downToggle.setGraphic(arrowDown);
        upToggle.setTooltip(new Tooltip("Go to previous change."));
        downToggle.setTooltip(new Tooltip("Go to next change."));
    }

    private void initDropdown() {
        try {
            // Get the names of all the conflicting files and put them in the dropdown
            Set<String> conflictingFiles = SessionModel.getSessionModel().getConflictingFiles(null);

            for(String item : conflictingFiles) {
                conflictingFilesDropdown.getItems().add(item);
            }

            conflictingFilesDropdown.setPromptText("None");
        } catch (GitAPIException e) {
            e.printStackTrace();
        }
    }

    private void initLabels() {
        leftDocLabel.setText("Left");
        middleDocLabel.setText("Result");
        rightDoc.setText("Right");
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
    }

    @FXML
    private void setFileToEdit() {
        Main.assertFxThread();
        // Show file in dropdown
        EventHandler<ActionEvent> handler = conflictingFilesDropdown.getOnAction();
        conflictingFilesDropdown.setOnAction(handler);

        // Get the path of the selected file
        Path directory = (new File(SessionModel.getSessionModel().getCurrentRepoHelper().getRepo().getDirectory()
                .getParent())).toPath();
        String filePathWithoutFileName = directory.toString();

        // Get the name of the file
        String fileName = conflictingFilesDropdown.getValue();

        // Show files in ScrollPanes
        setFile(filePathWithoutFileName, fileName);

        // Allow the user to click buttons
        setButtonsDisabled(false);
    }

    private ArrayList<ArrayList> parseConflicts(String path){
        ArrayList<String> left = new ArrayList<>();
        ArrayList<String> center = new ArrayList<>();
        ArrayList<String> right = new ArrayList<>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(path));
            String line = reader.readLine();
            while(line!=null){
                if(line.contains("<<<<<<<")){
                    line = reader.readLine();
                    while(!line.contains("=======")){
                        left.add(line);
                        line = reader.readLine();
                    }
                    line = reader.readLine();
                    while(!line.contains(">>>>>>>")){
                        right.add(line);
                        line = reader.readLine();
                    }
                    line = reader.readLine();
                }
                else{
                    left.add(line);
                    center.add(line);
                    right.add(line);
                    line = reader.readLine();
                }
            }

        }
        catch (IOException e){
            console.info(e);
        }
        ArrayList<ArrayList> list = new ArrayList<>();
        list.add(left);
        list.add(center);
        list.add(right);
        return list;
    }

    public void setFile(String filePathWithoutFileName, String fileName){
        fileSelected = true;
        conflictingFilesDropdown.setPromptText(fileName);

        ArrayList<ArrayList> results = parseConflicts(filePathWithoutFileName + File.separator + fileName);
        ArrayList leftLines = results.get(0);
        ArrayList middleLines = results.get(1);
        ArrayList rightLines = results.get(2);

        setLines(leftLines, leftDoc);
        setLines(middleLines, middleDoc);
        setLines(rightLines, rightDoc);
    }

    private void setLines(ArrayList lines, TextArea doc) {
        for (int i = 0; i < lines.size(); i++) {
            String line = (String) lines.get(i);
            doc.appendText(line + "\n");
        }
    }
}
