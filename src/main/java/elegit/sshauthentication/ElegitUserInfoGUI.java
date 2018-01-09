package elegit.sshauthentication;

import com.jcraft.jsch.UserInfo;
import elegit.Main;
import elegit.exceptions.ExceptionAdapter;
import io.reactivex.Single;
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler;
import io.reactivex.schedulers.Schedulers;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import org.apache.http.annotation.GuardedBy;
import org.apache.http.annotation.ThreadSafe;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

/**
 * Class for purposes for JSch authentication (which JGit uses). This is the text-based version used
 * for unit tests.
 */
@ThreadSafe
public class ElegitUserInfoGUI implements UserInfo {

    @GuardedBy("this") private Optional<String> password;
    @GuardedBy("this") private Optional<String> passphrase;
    private static final Logger logger = LogManager.getLogger();


    public ElegitUserInfoGUI() {
        password = Optional.empty();
        passphrase = Optional.empty();
    }

    @Override
    public synchronized String getPassphrase() {
        return passphrase.orElse("");
    }

    @Override
    public synchronized String getPassword() {
        return password.orElse("");
    }

    @Override
    public synchronized boolean promptPassword(String s) {
        password = prompt(s,"SSH password authentication",
                                             "SSH password authentication",
                                             "Enter your password:");
        return password.isPresent();
    }

    @Override
    public synchronized boolean promptPassphrase(String s) {

        passphrase = prompt(s,"SSH public key authentication",
                                           "SSH public key authentication",
                                           "Enter your passphrase:");
        //return false;
        return passphrase.isPresent();
    }

    // This method doesn't need to be synchronized, as it does not interact with the shared instance variables
    // at all.
    // Note that it is critical that this code MUST be run from a worker thread, not from the FX thread.
    // That's because it has to block on getting the result from a dialog that goes on the FX thread. If
    // this code gets run on the FX thread, it will deadlock. That's fine anyway, as an ssh connection will
    // be slow, and should never be attempted from the FX thread at any rate.
    private Optional<String> prompt(String s, String title, String headerText, String contentText) {
            Main.assertNotFxThread();
        System.out.println("ElegitUserInfoGUI.prompt start");
        FutureTask<Optional<String>> futureTask = new FutureTask<>(() -> {
            System.out.println("ElegitUserInfoGUI.prompt");

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

            return dialog.showAndWait();

        });
        Platform.runLater(futureTask);
        Optional<String> result = Optional.of("");
        try {
            result = futureTask.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            throw new ExceptionAdapter(e);
        }
        return result;
    }

    // This method doesn't need to be synchronized, as it does not interact with the shared instance variables
    // at all.
    // TODO: This method will only work on FX thread, but likely gets called off it. Something is missing in testing.
    @Override
    public boolean promptYesNo(String s) {
        Main.assertNotFxThread();

        System.out.println("ElegitUserInfoGUI.promptYesNo");
        System.out.println("a" + Thread.currentThread());
        return Single.fromCallable(() -> {
            System.out.println("ElegitUserInfoGUI.promptYesNo inside thread");

            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("SSH yes/no confirmation");
            alert.setHeaderText("SSH yes/no question.");
            alert.setContentText(s);

            alert.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
            Optional<ButtonType> result = alert.showAndWait();
            if (result.orElse(null) == ButtonType.YES)
                return true;
            else if (result.orElse(null) == ButtonType.NO)
                return false;
            else {
                logger.error("Internal error with SSH yes/no prompt.");
                return false;
            }
        })
                .subscribeOn(JavaFxScheduler.platform())
                .doOnSuccess((a) -> {
                    System.out.println("b " + Thread.currentThread());
                })
                //.observeOn(Schedulers.io())
                .blockingGet();

    }

    // This method doesn't need to be synchronized, as it does not interact with the shared instance variables
    // at all.
    // TODO: This method will only work on FX thread, but likely gets called off it. Something is missing in testing.
    @Override
    public void showMessage(String s) {
        System.out.println("ElegitUserInfoGUI.showMessage");
        System.out.println(s);
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("SSH message");
        alert.setHeaderText("SSH message");
        alert.setContentText(s);

        alert.getButtonTypes().setAll(ButtonType.OK);
        alert.showAndWait();
    }
}
