package elegitfx;

import elegit.Main;
import elegit.controllers.BusyWindow;
import elegit.controllers.SessionController;
import elegit.controllers.SshPromptController;
import elegit.exceptions.ExceptionAdapter;
import elegit.models.ClonedRepoHelper;
import elegit.models.ExistingRepoHelper;
import elegit.models.PrefObj;
import elegit.models.SessionModel;
import elegit.monitors.RepositoryMonitor;
import elegit.sshauthentication.ElegitUserInfoTest;
import javafx.scene.Node;
import javafx.scene.control.Dialog;
import javafx.scene.control.PasswordField;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import junit.framework.TestCase;
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
import org.testfx.service.query.NodeQuery;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.prefs.Preferences;

import static elegit.models.SessionModel.LAST_OPENED_REPO_PATH_KEY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class SshPrivateKeyPasswordOpeningCancelFXTest extends ApplicationTest {

    @ClassRule
    public static final TestingLogPathRule testingLogPath = new TestingLogPathRule();

    @Rule
    public final TestingRemoteAndLocalReposRule testingRemoteAndLocalRepos =
            new TestingRemoteAndLocalReposRule(false);
    private Path directoryPath;

    private static final Logger console = LogManager.getLogger("briefconsolelogger");

    private CountDownLatch startComplete = new CountDownLatch(1);

    static {
        // -----------------------Logging Initialization Start---------------------------
        Path logPath = Paths.get("logs");
        String s = logPath.toAbsolutePath().toString();
        System.setProperty("logFolder", s);
    }

    private SessionController sessionController;
    private String remoteURL;
    private SshServer sshd;

    @Rule
    public TestFXRule testFXRule = new TestFXRule();

    @Rule
    public TestName testName = new TestName();

    @Before
    public void setup() throws Exception {
        console.info("Unit test started");
        initializeLogger();
        console.info("Test name: " + testName.getMethodName());
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
        console.info("Tearing down");
        TestCase.assertEquals(0, Main.getAssertionCount());
    }



    @Override
    public void start(Stage stage) throws Exception {

        directoryPath = Files.createTempDirectory("unitTestRepos");
        directoryPath.toFile().deleteOnExit();
        directoryPath = testingRemoteAndLocalRepos.getDirectoryPath();

        // For this test, the existing repo has to first be setup before the app starts. Since the app starts
        // in the FX thread, that needs to be done here.
        sshd = SshServer.setUpDefaultServer();

        remoteURL = TestUtilities.setUpTestSshServer(sshd,
                                                     directoryPath,
                                                     testingRemoteAndLocalRepos.getRemoteFull(),
                                                     testingRemoteAndLocalRepos.getRemoteBrief());

        console.info("Connecting to " + remoteURL);
        Path local = testingRemoteAndLocalRepos.getLocalFull();
        console.info("local = " + local);
        InputStream passwordFileStream = TestUtilities.class.getResourceAsStream("/rsa_key1_passphrase.txt");
        Scanner scanner = new Scanner(passwordFileStream);
        String passphrase = scanner.next();
        console.info("phrase is " + passphrase);
        String privateKeyFileLocation = "/rsa_key1";


        ClonedRepoHelper helper =
                new ClonedRepoHelper(local, "",
                                     new ElegitUserInfoTest(null, passphrase),
                                     getClass().getResource(privateKeyFileLocation).getFile(),
                                     directoryPath.resolve("testing_known_hosts").toString());
        helper.obtainRepository(remoteURL);

        TestUtilities.initializePreferences();

        Preferences preferences = TestUtilities.getPreferences();

        console.info("preferences = " + preferences);
        console.info(SessionModel.getPreferencesNodeClass().toString());
        PrefObj.putObject(preferences, LAST_OPENED_REPO_PATH_KEY, local.toString());
        ArrayList<String> recentRepos = new ArrayList<>();
        recentRepos.add(local.toString());
        PrefObj.putObject(preferences, SessionModel.RECENT_REPOS_LIST_KEY, recentRepos);

        sessionController = TestUtilities.startupFxApp(stage);

        startComplete.countDown();
    }

    /**
     * Verify that cancelling password prompts behaves gracefully, when starting with an ssh private key
     * repo being the active one when you start up Elegit.
     *
     * The sleep code throughout is for visual debugging purposes; the code goes by so fast it's hard for a human to
     * see. Adjust the timing as appropriate.
     * @throws Exception
     */
    @Test
    public void test() throws Exception {

        int delay = 0;

        startComplete.await();

        WaitForAsyncUtils.waitForFxEvents();

        WaitForAsyncUtils.waitFor(10, TimeUnit.SECONDS,
                                  () -> lookup("Yes").query() != null);
        WaitForAsyncUtils.waitFor(10, TimeUnit.SECONDS,
                                  () -> lookup("Yes").query().isVisible());
        WaitForAsyncUtils.waitForFxEvents();
        sleep(100);  // Additional catchup; seems to be necessary
        clickOn("Yes");
        WaitForAsyncUtils.waitFor(10, TimeUnit.SECONDS,
                                  () -> lookup("Yes").query() == null);
        sleep(delay);

        // Wait for ssh prompt, then click cancel
        WaitForAsyncUtils.waitFor(10, TimeUnit.SECONDS,
                                  () -> lookup("Cancel").query() != null);
        WaitForAsyncUtils.waitFor(10, TimeUnit.SECONDS,
                                  () -> lookup("Cancel").query().isVisible());
        WaitForAsyncUtils.waitForFxEvents();
        sleep(100);  // Additional catchup; seems to be necessary
        clickOn("Cancel");
        sleep(delay);
        WaitForAsyncUtils.waitFor(10, TimeUnit.SECONDS,
                                  () -> lookup("Cancel").query() == null);

        // Test that trying to fetch after cancelling works gracefully, then try cancelling again
        clickOn("Fetch");
        WaitForAsyncUtils.waitFor(10, TimeUnit.SECONDS,
                                  () -> lookup("Cancel").query() != null);
        WaitForAsyncUtils.waitFor(10, TimeUnit.SECONDS,
                                  () -> lookup("Cancel").query().isVisible());
        WaitForAsyncUtils.waitForFxEvents();
        sleep(100);  // Additional catchup; seems to be necessary
        clickOn("Cancel");
        sleep(delay);


        // HTTP cancel. Should someday get rid of this, but for now, gets rid of error messages to acknowledge it.
        WaitForAsyncUtils.waitFor(10, TimeUnit.SECONDS,
                                  () -> lookup("Cancel").query() != null);
        WaitForAsyncUtils.waitFor(10, TimeUnit.SECONDS,
                                  () -> lookup("Cancel").query().isVisible());
        WaitForAsyncUtils.waitForFxEvents();
        sleep(100);  // Additional catchup; seems to be necessary
        clickOn("Cancel");
        sleep(delay);

        assertEquals(0, ExceptionAdapter.getWrappedCount());

        // Stop repository monitor, so it doesn't keep trying to work after sshd shuts down
        RepositoryMonitor.pause();

        // Shut down test SSH server
        sshd.stop();
    }

}