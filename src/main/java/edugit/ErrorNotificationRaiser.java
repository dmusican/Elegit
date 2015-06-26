package main.java.edugit;

import main.java.edugit.exceptions.CancelledLoginException;
import javafx.scene.control.Alert;
import org.controlsfx.control.NotificationPane;
import org.controlsfx.control.action.Action;

import java.util.List;

/**
 * A static class for holding all our error alert dialogs.
 */
public class ErrorNotificationRaiser {

    SessionModel sessionModel;
    NotificationPane notificationPane;

    public ErrorNotificationRaiser(SessionModel model, NotificationPane pane) {
        this.sessionModel = model;
        this.notificationPane = pane;
    }

    public void showNotLoggedInNotification() {
        this.notificationPane.setText("You need to log in to do that.");
        Action loginAction = new Action("Enter login info", e -> {
            this.notificationPane.hide();
            try {
                this.sessionModel.getDefaultOwner().presentLoginDialogsToSetValues();
            } catch (CancelledLoginException e1) {
                // Do nothing if they cancel login
            }
        });

        this.notificationPane.getActions().setAll(loginAction);

        this.notificationPane.show();
    }


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
        alert.setContentText("The login information you gave does not allow you to modify this repository. Try switching your login.");
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

    public static Alert repoWasNotLoaded() {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Load failed");
        alert.setHeaderText("Repository was not loaded");
        alert.setContentText("No repository was loaded.");
        return alert;
    }

    public static Alert checkoutConflictWithPaths(List<String> conflictingPaths) {
        String conflictList = "";
        for (String pathName : conflictingPaths) {
            conflictList += "\n" + pathName;
        }

        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Checkout error");
        alert.setHeaderText("Can't checkout that branch");
        alert.setContentText("You can't switch to that branch because of the following conflicting files: "
                + conflictList);
        return alert;
    }
}
