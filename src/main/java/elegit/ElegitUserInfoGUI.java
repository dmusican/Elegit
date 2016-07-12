package elegit;

import com.jcraft.jsch.UserInfo;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Optional;

/**
 * Class for purposes for JSch authentication (which JGit uses). This is the text-based version used
 * for unit tests.
 */
public class ElegitUserInfoGUI implements UserInfo {

    private String password;
    private String passphrase;

    @Override
    public String getPassphrase() {
        return passphrase;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public boolean promptPassword(String s) {
        Optional<String> result = prompt(s,"SSH password authentication",
                                           "SSH password authentication",
                                           "Enter your password:");
        if (result.isPresent()) {
            password = result.get();
            return true;
        } else {
            password = "";
            return false;
        }
    }

    @Override
    public boolean promptPassphrase(String s) {

        Optional<String> result = prompt(s,"SSH public key authentication",
                                           "SSH public key authentication",
                                           "Enter your passphrase:");
        if (result.isPresent()) {
            passphrase = result.get();
            return true;
        } else {
            passphrase = "";
            return false;
        }
    }

    private Optional<String> prompt(String s, String title, String headerText, String contentText) {
        System.out.println(s);

        TextInputDialog prompt = new TextInputDialog();
        prompt.setTitle(title);
        prompt.setHeaderText(headerText);
        prompt.setContentText(contentText);
        Optional<String> result = prompt.showAndWait();
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
        Optional<ButtonType> result = alert.showAndWait();
        return;
    }
}
