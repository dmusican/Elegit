package elegitfx.commandLineTests;

import elegit.controllers.SessionController;
import elegit.gui.WorkingTreePanelView;
import javafx.scene.input.MouseButton;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.*;
import org.junit.rules.TestName;
import org.testfx.framework.junit.ApplicationTest;
import org.testfx.framework.junit.TestFXRule;
import sharedrules.TestUtilities;
import sharedrules.TestingLogPathRule;

import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;

/**
 * Created by grenche on 7/13/18.
 * Test removing files from the staging area (but specifically the command line textArea).
 */
public class RemoveFXTest extends ApplicationTest {
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
    public void removeFileTest() throws Exception {
        // Set up a test repo and get the last commit
        RevCommit commit = commandLineTestUtilities.setupTestRepo(directoryPath, sessionController);
        commandLineTestUtilities.addChangeToFile(directoryPath);
        console.info("Set up done.");

        SessionController.gitStatusCompletedOnce = new CountDownLatch(1);
        SessionController.gitStatusCompletedOnce.await();

        WorkingTreePanelView workingTree = lookup("#workingTreePanelView").query();
        interact(workingTree::checkSelectAll);

        clickOn("#removeButton")
                .clickOn("OK");

        commandLineTestUtilities.checkCommandLineText("git rm README.md");
    }
}
