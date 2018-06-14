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
import org.testfx.robot.Motion;
import sharedrules.TestUtilities;
import sharedrules.TestingLogPathRule;

import java.nio.file.Path;

/**
 * Created by grenche on 6/14/18.
 * Test revert by clicking on a commit in the tree
 * NOTE: There is still a bug in revert
 */
public class RevertFXTest extends ApplicationTest {
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
        // Should be commented back in when the threading bug with revert if fixec
//        commonRulesAndSetup.tearDown();
    }

    @Override
    public void start(Stage stage) throws Exception {
        sessionController = TestUtilities.commonTestFxStart(stage);
    }

    @Test
    public void revertTest() throws Exception {
        // Set up a test repo and get the last commit
        RevCommit commit = commandLineTestUtilities.setupTestRepo(directoryPath, sessionController);
        console.info("Set up done.");

        RevCommit prevCommit = commit.getParent(0);

        rightClickOn(Matchers.hasToString(prevCommit.getName()))
                .clickOn("Revert...")
                .clickOn("Revert this commit");

        // Get the name of the commit
        final String[] id = commandLineTestUtilities.getCommitId(prevCommit);

        console.info("Finished clicking revert.");

        commandLineTestUtilities.checkCommandLineText("git revert " + id[0]);
    }
}
