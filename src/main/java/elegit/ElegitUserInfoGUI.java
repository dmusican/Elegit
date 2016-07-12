package elegit;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.UserInfo;
import elegit.exceptions.CancelledAuthorizationException;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Optional;

/**
 * Class for purposes for JSch authentication (which JGit uses). This is the text-based version used
 * for unit tests.
 */
public class ElegitUserInfoGUI implements UserInfo {

    private Optional<String> password;
    private Optional<String> passphrase;

    public ElegitUserInfoGUI() {
        password = Optional.empty();
        passphrase = Optional.empty();
    }

    @Override
    public String getPassphrase() {
        return passphrase.get();
    }

    @Override
    public String getPassword() {
        return password.get();
    }

    @Override
    public boolean promptPassword(String s) {
        password = prompt(s,"SSH password authentication",
                                             "SSH password authentication",
                                             "Enter your password:");
        return password.isPresent();
    }

    @Override
    public boolean promptPassphrase(String s) {

        passphrase = prompt(s,"SSH public key authentication",
                                           "SSH public key authentication",
                                           "Enter your passphrase:");
        //return false;
        return passphrase.isPresent();
    }

    private Optional<String> prompt(String s, String title, String headerText, String contentText) {
        System.out.println(s);

        Dialog<String> dialog = new Dialog<>();

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10, 10, 10, 10));

        PasswordField passwordField = new PasswordField();
        grid.add(passwordField,2,0);

        dialog.getDialogPane().setContent(grid);

        dialog.setTitle(title);
        dialog.setHeaderText(s);
        dialog.setContentText(s);

        dialog.getDialogPane().getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK)
                return passwordField.getText();
            else {
                return null;
            }
        });

        Optional<String> result = dialog.showAndWait();
        return result;
    }

    @Override
    public boolean promptYesNo(String s) {
        System.out.println(s);

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("SSH yes/no confirmation");
        alert.setHeaderText("SSH yes/no question.");
        alert.setContentText(s);

        alert.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
        Optional<ButtonType> result = alert.showAndWait();
        if (result.get() == ButtonType.YES)
            return true;
        else if (result.get() == ButtonType.NO)
            return false;
        else {
            SessionModel.logger.error("Internal error with SSH yes/no prompt.");
            return false;
        }
    }

    @Override
    public void showMessage(String s) {
        System.out.println(s);
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("SSH message");
        alert.setHeaderText("SSH message");
        alert.setContentText(s);

        alert.getButtonTypes().setAll(ButtonType.OK);
        alert.showAndWait();
        return;
    }
}
