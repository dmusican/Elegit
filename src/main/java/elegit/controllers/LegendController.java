package elegit.controllers;

import elegit.SessionModel;
import javafx.fxml.FXML;
import javafx.scene.layout.GridPane;

/**
 *
 * A controller for the Help Page view that provides information about
 * what all of the
 *
 */
public class LegendController {
    @FXML
    private GridPane gridPane;

    private SessionModel sessionModel;

    public void initialize() throws Exception {
        this.sessionModel = SessionModel.getSessionModel();
    }
}
