package main.java.elegit;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;
import javafx.util.Pair;
import main.java.elegit.exceptions.CancelledAuthorizationException;
import main.java.elegit.exceptions.NoRepoSelectedException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

/**
 * An abstract class for building RepoHelpers by presenting dialogs to
 * get the required parameters.
 */
public abstract class RepoHelperBuilder {

    SessionModel sessionModel;
    private String defaultFilePickerStartFolder = System.getProperty("user.home");

    public RepoHelperBuilder(SessionModel sessionModel) {
        this.sessionModel = sessionModel;
    }

    static final Logger logger = LogManager.getLogger();

    public abstract RepoHelper getRepoHelperFromDialogs() throws GitAPIException, IOException, NoRepoSelectedException, CancelledAuthorizationException;

    /**
     * Presents a file chooser and returns the chosen file.
     *
     * @param title the title of the file chooser window.
     * @param parent the parent Window for the file chooser. Can be null (then
     *               the chooser won't be anchored to any window).
     * @return the chosen file from the file chooser.
     */
    public File getDirectoryPathFromChooser(String title, Window parent) {
        File path = new File(this.defaultFilePickerStartFolder); // start the file browser in the user's home folder

        File returnFile;
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle(title);
        chooser.setInitialDirectory(path);

        returnFile = chooser.showDialog(parent);
        return returnFile;
    }

    /**
     * Presents dialogs that request the user's username and password,
     * and sets the username and password fields accordingly.
     *
     * @throws CancelledAuthorizationException if the user presses cancel or closes the dialog.
     * @param repoHelper
     */
    public UsernamePasswordCredentialsProvider getRepoHelperAuthCredentialFromDialog(RepoHelper repoHelper) throws CancelledAuthorizationException {
        logger.info("Creating authorization dialog");
        // Create the custom dialog.
        Dialog<Pair<String,Pair<String,Boolean>>> dialog = new Dialog<>();
        dialog.setTitle("Authorize");
        dialog.setHeaderText("Please enter your remote repository password.");

        // Set the button types.
        ButtonType loginButtonType = new ButtonType("Authorize", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);

        dialog.setOnCloseRequest(new EventHandler<DialogEvent>() {
            @Override
            public void handle(DialogEvent event) {
                logger.info("Closing authorization dialog");
            }
        });

        // Create the username and password labels and fields.
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        grid.add(new Label("Username:"), 0, 0);

        // Conditionally ask for the username if it hasn't yet been set.
        TextField username = new TextField();
        if (repoHelper.username == null) {
            username.setPromptText("Username");
        } else {
            username.setText(repoHelper.username);
            username.setEditable(false);
        }
        grid.add(username, 1, 0);

        grid.add(new Label("Password:"), 0, 1);

        PasswordField password = new PasswordField();
        CheckBox remember = new CheckBox("Remember Password");

        if (repoHelper.password != null) {
            password.setText(repoHelper.password);
            remember.setSelected(true);
        }
        password.setPromptText("Password");
        grid.add(password, 1, 1);

        //Edit username button
        Button editUsername = new Button();
        editUsername.setText("Edit");
        editUsername.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                username.setEditable(true);
                password.setText("");
                remember.setSelected(false);
            }
        });
        if (repoHelper.username == null) {
            editUsername.setVisible(false);
        }
        grid.add(editUsername,2,0);

        remember.setIndeterminate(false);
        grid.add(remember, 1, 2);

        // Enable/Disable login button depending on whether a password was entered.
        Node loginButton = dialog.getDialogPane().lookupButton(loginButtonType);
        if (repoHelper.password == null || repoHelper.username == null) {
            loginButton.setDisable(true);
        }

        // Do some validation (using the Java 8 lambda syntax).
        password.textProperty().addListener((observable, oldValue, newValue) -> {
            loginButton.setDisable(newValue.trim().isEmpty());
        });
        username.textProperty().addListener((observable, oldValue, newValue) -> {
            loginButton.setDisable(newValue.trim().isEmpty());
        });

        dialog.getDialogPane().setContent(grid);

        // Request focus for the first text field by default.
        if (username.getText() != null) {
            Platform.runLater(() -> username.requestFocus());
        } else {
            Platform.runLater(() -> password.requestFocus());
        }

        // Return the password when the authorize button is clicked.
        // If the username hasn't been set yet, then update the username.
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == loginButtonType) {
                return new Pair<>(username.getText(), new Pair<>(password.getText(), new Boolean(remember.isSelected())));
            }
            return null;
        });

        Optional<Pair<String, Pair<String, Boolean>>> result = dialog.showAndWait();

        UsernamePasswordCredentialsProvider ownerAuth;

        if (result.isPresent()) {
            repoHelper.username = result.get().getKey();
            //Only store password if remember password was selected
            if (result.get().getValue().getValue()) {
                logger.info("Selected remember password");
                repoHelper.password = result.get().getValue().getKey();
            }
            ownerAuth = new UsernamePasswordCredentialsProvider(repoHelper.username, result.get().getValue().getKey());
        } else {
            logger.info("Cancelled authorization dialog");
            throw new CancelledAuthorizationException();
        }
        logger.info("Entered authorization credentials");
        return ownerAuth;
    }
}
