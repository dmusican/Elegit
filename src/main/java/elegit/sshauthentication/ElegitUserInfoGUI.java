package elegit.sshauthentication;

import com.jcraft.jsch.UserInfo;
import elegit.Main;
import elegit.controllers.SessionController;
import elegit.controllers.SshPromptController;
import elegit.exceptions.CancelledDialogException;
import elegit.exceptions.ExceptionAdapter;
import io.reactivex.Single;
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler;
import javafx.application.Platform;
import javafx.scene.control.*;
import net.jcip.annotations.ThreadSafe;
import net.jcip.annotations.GuardedBy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Class for purposes for JSch authentication (which JGit uses). This is the text-based version used
 * for unit tests.
 */
@ThreadSafe
public class ElegitUserInfoGUI implements UserInfo {

    private static final AtomicReference<SessionController> sessionController = new AtomicReference<>();
    @GuardedBy("this") private Optional<String> password;
    @GuardedBy("this") private Optional<String> passphrase;

    private static final Logger logger = LogManager.getLogger();

    public ElegitUserInfoGUI() {
        password = Optional.empty();
        passphrase = Optional.empty();
    }

    public static void setSessionController(SessionController sessionController) {
        ElegitUserInfoGUI.sessionController.set(sessionController);
    }

    @Override
    public synchronized String getPassphrase() {
        System.out.println("ElegitUserInfoGUI.getPassphrase");
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

        System.out.println("ElegitUserInfoGUI.promptPassphrase");
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

        SshPromptController sshPromptController = new SshPromptController();
        FutureTask<Optional<String>> futureTask = new FutureTask<>(
                () -> sshPromptController.showAndWait(s, title, headerText, contentText));
        Platform.runLater(futureTask);
        Optional<String> result = Optional.empty();
        try {
            Thread.yield();
            result = futureTask.get();
        } catch (InterruptedException e) {
            sessionController.get().showSshPasswordCancelledNotification();
            Platform.runLater(sshPromptController::hide);
        } catch (ExecutionException e) {
            e.printStackTrace();
            throw new ExceptionAdapter(e);
        }

        if (!result.isPresent()) {
            throw new CancelledDialogException();
        }
        return result;
    }

    // This method doesn't need to be synchronized, as it does not interact with the shared instance variables
    // at all.
    @Override
    public boolean promptYesNo(String s) {
        Main.assertNotFxThread();

        return Single.fromCallable(() -> {
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
                .blockingGet();

    }

    // This method doesn't need to be synchronized, as it does not interact with the shared instance variables
    // at all.
    @Override
    public void showMessage(String s) {
        Main.assertNotFxThread();

        Single.fromCallable(() -> {
            System.out.println("ElegitUserInfoGUI.showMessage");
            System.out.println(s);
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("SSH message");
            alert.setHeaderText("SSH message");
            alert.setContentText(s);

            alert.getButtonTypes().setAll(ButtonType.OK);
            alert.showAndWait();
            return true;
        })
                .subscribeOn(JavaFxScheduler.platform())
                .blockingGet();
    }
}
