package elegitfx;

import elegit.Main;
import elegit.controllers.BusyWindow;
import elegit.controllers.SessionController;
import elegit.exceptions.ExceptionAdapter;
import elegit.models.ClonedRepoHelper;
import elegit.models.ExistingRepoHelper;
import elegit.models.PrefObj;
import elegit.models.SessionModel;
import elegit.monitors.RepositoryMonitor;
import elegit.sshauthentication.ElegitUserInfoTest;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import junit.framework.TestCase;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.sshd.server.SshServer;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.testfx.framework.junit.ApplicationTest;
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


//        sshd.stop();

        TestUtilities.initializePreferences();

        Preferences preferences = TestUtilities.getPreferences();

        console.info("preferences = " + preferences);
        console.info(SessionModel.getPreferencesNodeClass().toString());
        PrefObj.putObject(preferences, LAST_OPENED_REPO_PATH_KEY, local.toString());
        ArrayList<String> recentRepos = new ArrayList<>();
        recentRepos.add(local.toString());
        PrefObj.putObject(preferences, SessionModel.RECENT_REPOS_LIST_KEY, recentRepos);

        console.info("start started");
        sessionController = TestUtilities.startupFxApp(stage);


        startComplete.countDown();
    }

    @Test
    public void test() throws Exception {

        startComplete.await();

        WaitForAsyncUtils.waitFor(10, TimeUnit.SECONDS,
                                  () -> lookup("Yes").query() != null);
        clickOn("Yes");

        WaitForAsyncUtils.waitFor(10, TimeUnit.SECONDS,
                                  () -> lookup("#sshprompt").query() != null);
        // Enter passphrase
        clickOn("Cancel");

        // Test that trying to fetch after cancelling works gracefully
        clickOn("Fetch");

        assertEquals(0, ExceptionAdapter.getWrappedCount());

        // Stop repository monitor, so it doesn't keep trying to work after sshd shuts down
        RepositoryMonitor.pause();

        // Shut down test SSH server
        sshd.stop();
    }

}