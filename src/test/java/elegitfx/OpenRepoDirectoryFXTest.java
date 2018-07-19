package elegitfx;

import elegit.Main;
import elegit.controllers.SessionController;
import elegit.models.ExistingRepoHelper;
import elegit.monitors.RepositoryMonitor;
import elegit.sshauthentication.ElegitUserInfoTest;
import elegit.treefx.CommitTreeModel;
import javafx.scene.Node;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.api.Git;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.testfx.framework.junit.ApplicationTest;
import sharedrules.TestUtilities;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.testfx.api.FxAssert.verifyThat;

public class OpenRepoDirectoryFXTest extends ApplicationTest {
    static {
        // -----------------------Logging Initialization Start---------------------------
        Path logPath = Paths.get("logs");
        String s = logPath.toAbsolutePath().toString();
        System.setProperty("logFolder", s);
    }

    private static final Logger logger = LogManager.getLogger();
    private static final Logger console = LogManager.getLogger("briefconsolelogger");

    //private static final Random random = new Random(90125);

    private SessionController sessionController;

    private Path directoryPath;

    @Override
    public void start(Stage stage) throws Exception {
        sessionController = TestUtilities.commonTestFxStart(stage);
    }

    @Rule
    public TestName testName = new TestName();

    @Before
    public void setup() throws Exception {
        logger.info("Unit test started");
        console.info("Unit test started");
        console.info("Directory = " + directoryPath);
        directoryPath = Files.createTempDirectory("unitTestRepos");
        console.info("Directory = " + directoryPath);
        directoryPath.toFile().deleteOnExit();
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
        TestUtilities.cleanupTestFXEnvironment();
        assertEquals(0, Main.getAssertionCount());
    }

    @Test
    /*
     * Tests the open directory button on a loaded test repo
     */
    public void openLoadedRepoTest() throws Exception {
        TestUtilities.commonStartupOffFXThread();

        // Initial setup and tear down instructions
        initializeLogger();
        Path directoryPath = Files.createTempDirectory("unitTestRepos");
        directoryPath.toFile().deleteOnExit();

        // Setup the test repo
        Path repoPath1 = makeTempLocalRepo(directoryPath, "repo1");
        CommitTreeModel.setAddCommitDelay(5);
        SessionController.gitStatusCompletedOnce = new CountDownLatch(1);

        // Load the repo and click the openRepoDirectory button
        openDirectory(repoPath1);
        interact(() -> console.info("Pass completed"));

        // Makes sure this didn't cause an error
        assertEquals(0, sessionController.getNotificationPaneController().getNotificationNum());

        // Makes sure the method was called
        assertEquals(true, sessionController.getMethodCalled());

        RepositoryMonitor.pause();
    }

    /*
     * Loads the test repo, clicks the open directory button, makes sure no errors occurred.
     */
    private void openDirectory(Path directoryPath) throws InterruptedException {
        interact(() -> sessionController.handleLoadExistingRepoOption(directoryPath));
        SessionController.gitStatusCompletedOnce.await();

        // Clicks button to open testrepo directory
        clickOn((Node) (lookup("#openRepoDirButton").query()));
    }

    /*
     * Makes a temporary repository to use for the test
     */
    private Path makeTempLocalRepo(Path directoryPath, String localName) throws Exception {
        Path remote = directoryPath.resolve("remote");
        Path local = directoryPath.resolve(localName);
        Git.init().setDirectory(remote.toFile()).setBare(true).call();
        Git.cloneRepository().setDirectory(local.toFile()).setURI("file://" + remote).call();

        ExistingRepoHelper helper = new ExistingRepoHelper(local, new ElegitUserInfoTest());

        Path fileLocation = local.resolve("README.md");

        for(int i = 0; i < 5; i++) {
            FileWriter fw = new FileWriter(fileLocation.toString(), true);
            fw.write("start");
            fw.close();
            helper.addFilePathTest(fileLocation);
            helper.commit("Appended to file");
        }
        return local;
    }
}
