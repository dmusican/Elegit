package elegitfx.commandLineTests;

import elegit.controllers.SessionController;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.revwalk.RevCommit;
import org.hamcrest.Matchers;
import org.junit.*;
import org.junit.rules.TestName;
import org.testfx.framework.junit.ApplicationTest;
import org.testfx.framework.junit.TestFXRule;
import sharedrules.TestUtilities;
import sharedrules.TestingLogPathRule;

import java.nio.file.Path;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertNotEquals;

/**
 * Created by grenche on 6/13/18.
 * Tests the Terminal Command window after checkingout a file by right clicking a commit in the tree
 */
public class CheckoutFileFXTest extends ApplicationTest {
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

    @Before
    public void setup() throws Exception {
        directoryPath = commonRulesAndSetup.setup(testName);
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
    public void checkoutFiles() throws Exception {
        // Set up a test repo and get the last commit
        RevCommit commit = commandLineTestUtilities.setupTestRepo(directoryPath, sessionController);
        console.info("Set up done.");

        // Click on last commit and checkout the README.md file
        rightClickOn("#"+commit.getName())
                .clickOn("Checkout files...")
                .clickOn("#fileField")
                .write("README.md")
                .clickOn("#checkoutAddButton")
                .clickOn("#checkoutFilesButton");

        // Get the name of the commit
        final String[] id = new String[1];
        interact(() -> id[0] = commit.getName());

        console.info("Finished checking out file.");

        // Make sure the command line window updated correctly
        commandLineTestUtilities.checkCommandLineText("git checkout " + id[0] + " README.md");
    }
}
