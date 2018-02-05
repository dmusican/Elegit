package elegit.controllers;

import elegit.Main;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.GridPane;
import net.jcip.annotations.ThreadSafe;
import org.eclipse.jgit.api.errors.TransportException;

import java.util.Optional;

/**
 * A class used to manage prompting the user for an ssh password. Designed to be used from ElegitUserInfoGUI.
 */
// Only threadsafe because every method must be run on FX thread
@ThreadSafe
public class SshPromptController {

    private static Dialog<String> dialog;
    private static PasswordField passwordField;


    // Build the dialog structure.
    static {
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
    public static Optional<String> showAndWait(String s, String title, String headerText, String contentText) {
        Main.assertFxThread();

        dialog.setTitle(title);
        dialog.headerTextProperty().setValue(s + "\n" + contentText);
        passwordField.setText("");

        return dialog.showAndWait();
    }

    public static void hide() {
        Main.assertFxThread();
        dialog.hide();
    }

    public static String getPassword() {
        Main.assertFxThread();
        return passwordField.getText();
    }


    public static boolean isShowing() {
        Main.assertFxThread();
        return dialog.isShowing();
    }


}


