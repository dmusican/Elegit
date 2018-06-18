package elegit.controllers;

import de.jensd.fx.glyphs.GlyphsDude;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import net.jcip.annotations.GuardedBy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

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
    private TextArea leftDocLabel;
    @FXML
    private TextArea middleDocLabel;
    @FXML
    private TextArea rightDocLabel;
    @FXML
    private Button rightAccept;
    @FXML
    private Button rightReject;
    @FXML
    private Button leftAccept;
    @FXML
    private Button leftReject;
    @FXML
    private Button upToggle;
    @FXML
    private Button downToggle;
    @FXML
    private NotificationController notificationPaneController;

    private static final Logger console = LogManager.getLogger("briefconsolelogger");

    synchronized void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public void initialize() {
        initButtons();
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

    public void setFile(String filePath){
        ArrayList<ArrayList> results = parseConflicts(filePath);
    }
}
