package elegit.controllers;

import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.GridPane;

import java.util.Optional;

public class SshPromptController {

    private static Dialog<String> dialog;
    private static PasswordField passwordField;


    static {

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

    public static Optional<String> showAndWait(String s, String title, String headerText, String contentText) {

        dialog.setTitle(title);
        dialog.setHeaderText(s);
        dialog.setContentText(s);
        passwordField.setText("");

        return dialog.showAndWait();
    }

    public static void hide() {
        dialog.hide();
    }

    public static boolean isShowing() {
        return dialog.isShowing();
    }

    public static String getPassword() {
        return passwordField.getText();
    }

}


