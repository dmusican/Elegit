package edugit;

import javafx.scene.control.Alert;

/**
 * A static class for holding all our error alert dialogs.
 */
public class ERROR_ALERT_CONSTANTS {
    public static Alert noRepoLoaded() {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("No repository loaded!");
        alert.setHeaderText("No repository");
        alert.setContentText("You need to load a repository before you can perform operations on it!");
        return alert;
    }

    public static Alert notAuthorized() {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Access denied");
        alert.setHeaderText("Not authorized");
        alert.setContentText("The login information you gave does not allow you to modify this repository.");
        return alert;
    }

    public static Alert invalidRepo() {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Invalid repository");
        alert.setHeaderText("Invalid repository");
        alert.setContentText("Make sure the directory you selected contains an existing Git repository.");
        return alert;
    }

    public static Alert nonemptyFolder() {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Nonempty Destination Directory");
        alert.setHeaderText("Can't clone there");
        alert.setContentText("Make sure the directory you selected is completely empty. The best " +
                "way to do this is to create a new folder from the directory chooser.");
        return alert;
    }

    public static Alert genericError() {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("Error");
        alert.setContentText("Sorry, there was an error.");
        return alert;
    }

    public static Alert invalidRemote() {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Invalid remote");
        alert.setHeaderText("Can't clone from that remote");
        alert.setContentText("Make sure you entered the correct remote URL.");
        return alert;
    }

    public static Alert notLoggedIn() {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Not logged in");
        alert.setHeaderText("Not logged in");
        alert.setContentText("You need to log in to do that.");
        return alert;
    }
}
