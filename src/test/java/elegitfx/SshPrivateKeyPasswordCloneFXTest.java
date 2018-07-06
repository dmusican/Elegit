package elegitfx;

import elegit.Main;
import elegit.controllers.SessionController;
import elegit.exceptions.ExceptionAdapter;
import elegit.models.ExistingRepoHelper;
import elegit.monitors.RepositoryMonitor;
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
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class SshPrivateKeyPasswordCloneFXTest extends ApplicationTest {

    @ClassRule
    public static final TestingLogPathRule testingLogPath = new TestingLogPathRule();

    @Rule
    public final TestingRemoteAndLocalReposRule testingRemoteAndLocalRepos =
            new TestingRemoteAndLocalReposRule(false);
    private Path directoryPath;

    private static final Logger console = LogManager.getLogger("briefconsolelogger");




    static {
        // -----------------------Logging Initialization Start---------------------------
        Path logPath = Paths.get("logs");
        String s = logPath.toAbsolutePath().toString();
        System.setProperty("logFolder", s);
    }

    private static final Logger logger = LogManager.getLogger("consolelogger");

    private static final Random random = new Random(90125);

    private SessionController sessionController;

    @Rule
    public TestFXRule testFXRule = new TestFXRule();

    @Rule
    public TestName testName = new TestName();

    @Before
    public void setup() throws Exception {
        logger.info("Unit test started");
        directoryPath = Files.createTempDirectory("unitTestRepos");
        directoryPath.toFile().deleteOnExit();
        initializeLogger();
        logger.info("Test name: " + testName.getMethodName());
        directoryPath = testingRemoteAndLocalRepos.getDirectoryPath();
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
        TestUtilities.cleanupTestEnvironment();
        TestCase.assertEquals(0, Main.getAssertionCount());
    }


    @Override
    public void start(Stage stage) throws Exception {
        sessionController = TestUtilities.commonTestFxStart(stage);
    }

    @Test
    public void testSshPrivateKey() throws Exception {
        TestUtilities.commonStartupOffFXThread();

        // Set up remote repo
        Path remote = testingRemoteAndLocalRepos.getRemoteFull();
        Path remoteFilePath = remote.resolve("file.txt");
        Files.write(remoteFilePath, "testSshPassword".getBytes());
        //ArrayList<Path> paths = new ArrayList<>();
        //paths.add(remoteFilePath);
        ExistingRepoHelper helperServer = new ExistingRepoHelper(remote, null);
        helperServer.addFilePathTest(remoteFilePath);
        RevCommit firstCommit = helperServer.commit("Initial unit test commit");
        console.info("firstCommit name = " + firstCommit.getName());

        // Uncomment this to get detail SSH logging info, for debugging
//         JSch.setLogger(new DetailedSshLogger());

        // Set up test SSH server.
        try (SshServer sshd = SshServer.setUpDefaultServer()) {

            String remoteURL = TestUtilities.setUpTestSshServer(sshd,
                                                                directoryPath,
                                                                testingRemoteAndLocalRepos.getRemoteFull(),
                                                                testingRemoteAndLocalRepos.getRemoteBrief());

            console.info("Connecting to " + remoteURL);

            InputStream passwordFileStream = getClass().getResourceAsStream("/rsa_key1_passphrase.txt");
            Scanner scanner = new Scanner(passwordFileStream);
            String passphrase = scanner.next();
            String privateKeyFileLocation = "/rsa_key1";
            Path knownHostsFileLocation = directoryPath.resolve("testing_known_hosts");

            clickOn("#loadNewRepoButton")
                    .clickOn("#cloneOption")
                    .clickOn("#remoteURLField")
                    .write(remoteURL)
                    .clickOn("#enclosingFolderField")
                    .write(testingRemoteAndLocalRepos.getDirectoryPath().toString())
                    .doubleClickOn("#repoNameField")
                    .write(testingRemoteAndLocalRepos.getLocalBrief().toString())
                    .clickOn("#cloneButton")

                    // Enter in private key location
                    .clickOn("#repoInputDialog")
                    .write(getClass().getResource(privateKeyFileLocation).getFile())
                    .clickOn("#repoInputDialogOK")

                    // Enter in known hosts location
                    .clickOn("#repoInputDialog")
                    .write(knownHostsFileLocation.toString())
                    .clickOn("#repoInputDialogOK")

                    // Useless HTTP login screen that should eventually go away
                    .clickOn("#loginButton");


            RepositoryMonitor.pause();

            WaitForAsyncUtils.waitFor(10, TimeUnit.SECONDS,
                                      () -> lookup("Yes").query() != null);
            WaitForAsyncUtils.waitForFxEvents();
            clickOn("Yes");

            WaitForAsyncUtils.waitFor(10, TimeUnit.SECONDS,
                                      () -> lookup("#sshprompt").query() != null);
            WaitForAsyncUtils.waitForFxEvents();


            // Enter passphrase
            clickOn("#sshprompt")
                    .write(passphrase)
                    .write("\n");

            // Wait until a node is in the graph, indicating clone is done
            WaitForAsyncUtils.waitFor(10, TimeUnit.SECONDS,
                                      () -> lookup(".tree-cell").query() != null);
            WaitForAsyncUtils.waitForFxEvents();
            sleep(100);

            assertEquals(0, ExceptionAdapter.getWrappedCount());

            // Shut down test SSH server
            sshd.stop();
        }
    }

}