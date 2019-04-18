package elegit.controllers;

import elegit.Main;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.GridPane;
import net.jcip.annotations.ThreadSafe;

import java.util.Optional;

/**
 * A class used to manage prompting the user for an ssh password. Designed to be used from ElegitUserInfoGUI.
 */
// Threadsafe because every method is synchronized, and all variables are private
@ThreadSafe
public class SshPromptController {

    private final Dialog<String> dialog;
    private final PasswordField passwordField;


    // Build the dialog structure. It is built off-FX thread, then displayed as needed.
    public SshPromptController() {
        Main.assertFxThread();
        dialog = new Dialog<>();

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10, 10, 10, 10));

        passwordField = new PasswordField();
        passwordField.setId("sshprompt");
        grid.add(passwordField,2,0);

        dialog.getDialogPane().setContent(grid);

        dialog.getDialogPane().getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK)
                return passwordField.getText();
            else {
                return null;
            }
        });
    }

    /**
     * Prompt the user.
     * @param s
     * @param title
     * @param headerText
     * @param contentText
     * @return the password entered.
     */
    public synchronized Optional<String> showAndWait(String s, String title, String headerText, String contentText) {
        Main.assertFxThread();

        dialog.setTitle(title);
        dialog.headerTextProperty().setValue(s + "\n" + contentText);
        passwordField.setText("");

        return dialog.showAndWait();
    }

    public synchronized void hide() {
        Main.assertFxThread();
        dialog.hide();
    }

    public synchronized String getPassword() {
        Main.assertFxThread();
        return passwordField.getText();
    }


    public synchronized boolean isShowing() {
        Main.assertFxThread();
        return dialog.isShowing();
    }


}


