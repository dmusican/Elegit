package elegitfx.commandLineTests;

import elegit.controllers.SessionController;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.*;
import org.junit.rules.TestName;
import org.testfx.framework.junit.ApplicationTest;
import org.testfx.framework.junit.TestFXRule;
import sharedrules.TestUtilities;
import sharedrules.TestingLogPathRule;
import sharedrules.TestingRemoteAndLocalReposRule;

import java.nio.file.Path;

/**
 * Created by grenche on 6/14/18.
 * Test fetch -p when there are changes to fetch. Tests the command line text
 */
public class FetchPruneFXTest extends ApplicationTest {
    @Rule
    public CommonRulesAndSetup commonRulesAndSetup = new CommonRulesAndSetup();
    @ClassRule
    public static final LoggingInitializationStart loggingInitializationStart = new LoggingInitializationStart();
    @ClassRule
    public static final TestingLogPathRule testingLogPath = new TestingLogPathRule();
    @Rule
    public TestFXRule testFXRule = new TestFXRule();
    @Rule
    public TestName testName = new TestName();

    public final CommandLineTestUtilities commandLineTestUtilities = new CommandLineTestUtilities();

    private static final Logger console = LogManager.getLogger("briefconsolelogger");
    private static final Logger logger = LogManager.getLogger("consolelogger");

    public Path directoryPath;
    SessionController sessionController;

    @Rule
    public final TestingRemoteAndLocalReposRule testingRemoteAndLocalRepos =
            new TestingRemoteAndLocalReposRule(false);

    @Before
    public void setup() throws Exception {
        commonRulesAndSetup.setup(testName);
        directoryPath = testingRemoteAndLocalRepos.getDirectoryPath();
    }

    @After
    public void teardown() {
        commonRulesAndSetup.tearDown();
    }

    @Override
    public void start(Stage stage) throws Exception {
        sessionController = TestUtilities.commonTestFxStart(stage);
    }

    @Test
    public void fetchPrueTest() throws Exception {
        // Set up a test repo
        commandLineTestUtilities.setupTestRepo(directoryPath, sessionController);
        console.info("Set up done.");

        // In this test there is nothing to fetch,
        rightClickOn("#fetchButton")
                .clickOn("#fetchContextMenu");

        // but the terminal window should still show that they tried to fetch
        commandLineTestUtilities.checkCommandLineText("git fetch -p");
    }
}
