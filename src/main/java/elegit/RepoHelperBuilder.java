package elegit;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;
import elegit.exceptions.CancelledAuthorizationException;
import elegit.exceptions.NoRepoSelectedException;
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

    private static class UsernamePassword {
        public String username;
        public String password;
    }

    public static class AuthDialogResponse {
        AuthMethod protocol;
        public String username;
        public String password;
        boolean isSelected;
        AuthDialogResponse(AuthMethod protocol, String username, String password, boolean isSelected) {
            this.protocol = protocol;
            this.username = username;
            this.password = password;
            this.isSelected = isSelected;
        }
    }

    SessionModel sessionModel;
    private String defaultFilePickerStartFolder = System.getProperty("user.home");
    //private static HashMap<String, UsernamePassword> repoToAuth = new HashMap<>();

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
    File getDirectoryPathFromChooser(String title, Window parent) {
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
     */
    public static AuthDialogResponse getAuthCredentialFromDialog() throws
            CancelledAuthorizationException {
        logger.info("Creating authorization dialog");
        // Create the custom dialog.
        Dialog<AuthDialogResponse> dialog = new Dialog<>();
        dialog.setTitle("Authorize");
        dialog.setHeaderText("Please enter your remote repository authentication.");

        // Set the button types.
        ButtonType loginButtonType = new ButtonType("Authorize", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);

        dialog.setOnCloseRequest(event -> logger.info("Closing authorization dialog"));

        // Create the username and password labels and fields.
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        grid.add(new Label("Protocol:"),0,0);

        ObservableList<String> protocols =
                FXCollections.observableArrayList(
                        AuthMethod.getStrings()
                );
        ComboBox<String> protocol = new ComboBox<>(protocols);
        protocol.setValue("HTTPS");
        grid.add(protocol,1,0);

        grid.add(new Label("Username:"), 0, 1);

        String hashedUsername = null;
        String hashedPassword = null;
//        if (repoToAuth.containsKey(remoteURL)) {
//            hashedUsername = repoToAuth.get(remoteURL).username;
//            hashedPassword = repoToAuth.get(remoteURL).password;
//        }
        // Conditionally ask for the username if it hasn't yet been set.
        TextField username = new TextField();
//        if (hashedUsername == null) {
            username.setPromptText("Username");
//        } else {
//            username.setText(hashedUsername);
//            username.setEditable(false);
//        }
        grid.add(username, 1, 1);

        grid.add(new Label("Password:"), 0, 2);

        PasswordField password = new PasswordField();
        CheckBox remember = new CheckBox("Remember Password");

        //TODO: remember the password somehow
        /*
        if (hashedPassword != null) {
            password.setText(hashedPassword);
            remember.setSelected(true);
        }*/
        password.setPromptText("Password");
        grid.add(password, 1, 2);

        //Edit username button
        Button editUsername = new Button();
        editUsername.setText("Edit");
        editUsername.setOnAction(event -> {
            username.setEditable(true);
            password.setText("");
            remember.setSelected(false);
        });
        //TODO remember username or pull it from git config
        /*
        if (hashedUsername == null) {
            editUsername.setVisible(false);
        }*/
        grid.add(editUsername,2,1);

        remember.setIndeterminate(false);
        grid.add(remember, 1, 3);

        // Enable/Disable login button depending on whether a password was entered.
        Node loginButton = dialog.getDialogPane().lookupButton(loginButtonType);
        //TODO: see above about password and username
        /*
        if (hashedPassword == null || hashedUsername == null) {
            loginButton.setDisable(true);
        }*/

        // Do some validation (using the Java 8 lambda syntax).
        password.textProperty().addListener((observable, oldValue, newValue) -> loginButton.setDisable(newValue.trim().isEmpty()));
        username.textProperty().addListener((observable, oldValue, newValue) -> loginButton.setDisable(newValue.trim().isEmpty()));

        dialog.getDialogPane().setContent(grid);

        // Request focus for the first text field by default.
        if (username.getText() != null) {
            Platform.runLater(username::requestFocus);
        } else {
            Platform.runLater(password::requestFocus);
        }

        // Return the password when the authorize button is clicked.
        // If the username hasn't been set yet, then update the username.
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == loginButtonType) {
                AuthMethod protocolEnum = AuthMethod.getEnumFromString(protocol.getValue());
                return new AuthDialogResponse(protocolEnum, username.getText(), password.getText(),
                                              remember.isSelected());
            }
            return null;
        });

        Optional<AuthDialogResponse> result = dialog.showAndWait();

        UsernamePasswordCredentialsProvider ownerAuth;

        if (result.isPresent()) {
            UsernamePassword usernamePassword = new UsernamePassword();
            usernamePassword.username = result.get().username;
            //Only store password if remember password was selected
            if (result.get().isSelected) {
                logger.info("Selected remember password");
                usernamePassword.password = result.get().password;
            }
            //repoToAuth.put(remoteURL,usernamePassword);
        } else {
            logger.info("Cancelled authorization dialog");
            throw new CancelledAuthorizationException();
        }
        logger.info("Entered authorization credentials");
        return result.get();
    }
}
