package elegitfx;

import elegit.Main;
import elegit.controllers.SessionController;
import elegit.controllers.SshPromptController;
import elegit.exceptions.CancelledDialogException;
import elegit.exceptions.ExceptionAdapter;
import elegit.models.ClonedRepoHelper;
import elegit.models.ExistingRepoHelper;
import elegit.monitors.RepositoryMonitor;
import elegit.sshauthentication.ElegitUserInfoGUI;
import elegit.sshauthentication.ElegitUserInfoTest;
import io.reactivex.Completable;
import io.reactivex.schedulers.Schedulers;
import javafx.scene.control.PasswordField;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import junit.framework.TestCase;
import junit.framework.TestFailure;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.sshd.server.SshServer;
import org.eclipse.jgit.revwalk.RevCommit;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.testfx.framework.junit.ApplicationTest;
import org.testfx.framework.junit.TestFXRule;
import org.testfx.util.WaitForAsyncUtils;
import sharedrules.TestUtilities;
import sharedrules.TestingLogPathRule;
import sharedrules.TestingRemoteAndLocalReposRule;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

public class SshPopupInterruptTest extends ApplicationTest {

    @ClassRule
    public static final TestingLogPathRule testingLogPath = new TestingLogPathRule();

    @Rule
    public final TestingRemoteAndLocalReposRule testingRemoteAndLocalRepos =
            new TestingRemoteAndLocalReposRule(false);

    private static final Logger console = LogManager.getLogger("briefconsolelogger");




    static {
        // -----------------------Logging Initialization Start---------------------------
        Path logPath = Paths.get("logs");
        String s = logPath.toAbsolutePath().toString();
        System.setProperty("logFolder", s);
    }

    private static final Logger logger = LogManager.getLogger("consolelogger");

    @Rule
    public TestFXRule testFXRule = new TestFXRule();

    @Rule
    public TestName testName = new TestName();

    @Before
    public void setup() throws Exception {
        logger.info("Unit test started");
        initializeLogger();
    }


    // Helper method to avoid annoying traces from logger
    void initializeLogger() throws IOException {
        // Create a temp directory for the files to be placed in
        Path logPath = Files.createTempDirectory("elegitLogs");
        logPath.toFile().deleteOnExit();
        System.setProperty("logFolder", logPath.toString());
    }

    @After
    public void tearDown() {
        logger.info("Tearing down");
        TestUtilities.commonShutDown();
        TestCase.assertEquals(0, Main.getAssertionCount());
    }


    @Override
    public void start(Stage stage) throws Exception {
        SessionController sessionController = TestUtilities.commonTestFxStart(stage);
    }

    /**
     * Verify that a passphrase prompt works as expected when clicking the cancel button
     * @throws Exception
     */
    @Test
    public void test() throws Exception {


        ElegitUserInfoGUI userInfo = new ElegitUserInfoGUI();

        for (int i=0; i < 2; i++) {

            // A CancelledDialogException should get thrown as a result of the interrupt later on.
            // Therefore, catch the exception and move on to make the test succeed.
            // If the exception _doesn't_ get thrown, that's evidence that the test failed, so throw
            // an exception if get to that point.
            Thread t1 = new Thread(() -> {
                try {
                    userInfo.promptPassphrase("passphrase prompt");
                    throw new RuntimeException("Dialog was not cancelled as expected");
                } catch (CancelledDialogException e) {
                    // All is good, exception was thrown as expected
                }
            });
            t1.start();

            // This is here so you can actually see the popup when running test interactively; without it,
            // the waitFor that follows happens instantly, the click follows, and you can't see what happens
            sleep(1000);

            WaitForAsyncUtils.waitFor(10, TimeUnit.SECONDS,
                                      () -> lookup("#sshprompt").query() != null);

            PasswordField passwordField = (PasswordField)lookup("#sshprompt").query();
            interact(() -> assertEquals("",passwordField.getText()));

            // Enter passphrase
            clickOn("#sshprompt")
                    .write("testphrase");

            // Issue interrupt to thread running popup, which results in task with dialog getting cancelled
            t1.interrupt();

            WaitForAsyncUtils.waitFor(10, TimeUnit.SECONDS,
                                      () -> lookup("#sshprompt").query() == null);

        }
    }
}