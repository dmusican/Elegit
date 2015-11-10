package main.java.elegit;

import main.java.elegit.exceptions.CancelledAuthorizationException;
import main.java.elegit.exceptions.CancelledLoginException;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.util.Pair;

import java.util.Optional;

/**
 * A simple class for holding a repository user's login information
 * during the session. RepoOwners can also present dialogs for logging in.
 */
public class RepoOwner {

    private String username;
    private String password;

    public RepoOwner(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public RepoOwner(String type) throws CancelledAuthorizationException {
        this.presentAuthorizeDialog();
    }

    public RepoOwner() throws CancelledLoginException {
        this.presentLoginDialogsToSetValues();
    }

    /**
     * Presents dialogs that request the user's username and password,
     * and sets the username and password fields accordingly.
     *
     * @throws CancelledLoginException if the user presses cancel or closes the dialog.
     */
    public void presentLoginDialogsToSetValues() throws CancelledLoginException {
        // Create the custom dialog.
        Dialog<Pair<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Log in");
        dialog.setHeaderText("Please log in with your remote repository's credentials.");

        // Set the button types.
        ButtonType loginButtonType = new ButtonType("Log in", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);

        // Create the username and password labels and fields.
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField username = new TextField();
        username.setPromptText("Username");
        PasswordField password = new PasswordField();
        password.setPromptText("Password");

        grid.add(new Label("Username:"), 0, 0);
        grid.add(username, 1, 0);
        grid.add(new Label("Password:"), 0, 1);
        grid.add(password, 1, 1);

        // Enable/Disable login button depending on whether a username was entered.
        Node loginButton = dialog.getDialogPane().lookupButton(loginButtonType);
        loginButton.setDisable(true);

        // Do some validation (using the Java 8 lambda syntax).
        username.textProperty().addListener((observable, oldValue, newValue) -> {
            loginButton.setDisable(newValue.trim().isEmpty());
        });

        dialog.getDialogPane().setContent(grid);

        // Request focus on the username field by default.
        Platform.runLater(() -> username.requestFocus());

        // Convert the result to a username-password-pair when the login button is clicked.
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == loginButtonType) {
                return new Pair<>(username.getText(), password.getText());
            }
            return null;
        });

        Optional<Pair<String, String>> result = dialog.showAndWait();

        if (result.isPresent()) {
            this.username = result.get().getKey();
            this.password = result.get().getValue();
        } else {
            throw new CancelledLoginException();
        }
    }

    /**
     * Presents dialogs that request the user's username and password,
     * and sets the username and password fields accordingly.
     *
     * @throws CancelledAuthorizationException if the user presses cancel or closes the dialog.
     */
    public void presentAuthorizeDialog() throws CancelledAuthorizationException {
        // Create the custom dialog.
        Dialog<Pair<String,String>> dialog = new Dialog<>();
        dialog.setTitle("Authorize");
        dialog.setHeaderText("Please enter your remote repository password.");

        // Set the button types.
        ButtonType loginButtonType = new ButtonType("Authorize", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);

        // Create the username and password labels and fields.
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        PasswordField password = new PasswordField();
        password.setPromptText("Password");

        grid.add(new Label("Username:"), 0, 0);

        // Conditionally ask for the username if it hasn't yet been set.
        TextField username = new TextField();
        if (this.username == null) {
            username.setPromptText("Username");
            grid.add(username, 1, 0);
        } else {
            grid.add(new Label(this.username), 1, 0);
        }

        grid.add(new Label("Password:"), 0, 1);
        grid.add(password, 1, 1);

        // Enable/Disable login button depending on whether a password was entered.
        Node loginButton = dialog.getDialogPane().lookupButton(loginButtonType);
        loginButton.setDisable(true);

        // Do some validation (using the Java 8 lambda syntax).
        password.textProperty().addListener((observable, oldValue, newValue) -> {
            loginButton.setDisable(newValue.trim().isEmpty());
        });
        username.textProperty().addListener((observable, oldValue, newValue) -> {
            loginButton.setDisable(newValue.trim().isEmpty());
        });

        dialog.getDialogPane().setContent(grid);

        // Request focus for the first text field by default.
        if (this.username == null) {
            Platform.runLater(() -> username.requestFocus());
        } else {
            Platform.runLater(() -> password.requestFocus());
        }

        // Return the password when the authorize button is clicked.
        // If the username hasn't been set yet, then update the username.
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == loginButtonType) {
                if (this.username != null)
                    return new Pair<>(this.username, password.getText());
                else
                    return new Pair<>(username.getText(), password.getText());
            }
            return null;
        });

        Optional<Pair<String, String>> result = dialog.showAndWait();

        if (result.isPresent()) {
            if (this.username == null) {
                this.username = username.getText();
            } else {
                this.username = result.get().getKey();
            }
            this.password = result.get().getValue();
        } else {
            throw new CancelledAuthorizationException();
        }
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public void setUsername(String username) { this.username = username; }

}
