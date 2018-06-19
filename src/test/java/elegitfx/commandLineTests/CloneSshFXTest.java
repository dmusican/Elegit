package elegitfx.commandLineTests;

import elegit.controllers.CommandLineController;
import elegit.controllers.SessionController;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.*;
import org.junit.rules.TestName;
import org.testfx.framework.junit.ApplicationTest;
import org.testfx.framework.junit.TestFXRule;
import org.testfx.util.WaitForAsyncUtils;
import sharedrules.TestUtilities;
import sharedrules.TestingLogPathRule;
import sharedrules.TestingRemoteAndLocalReposRule;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

/**
 * Created by grenche on 6/13/18.
 * Tests the Terminal Command window after cloning a repo via SSH
 */
public class CloneSshFXTest extends ApplicationTest {
    @Rule
    public final CommonRulesAndSetup commonRulesAndSetup = new CommonRulesAndSetup();
    @ClassRule
    public static final LoggingInitializationStart loggingInitializationStart = new LoggingInitializationStart();
    @ClassRule
    public static final TestingLogPathRule testingLogPath = new TestingLogPathRule();
    @Rule
    public TestFXRule testFXRule = new TestFXRule();
    @Rule
    public TestName testName = new TestName();

    private static final Logger console = LogManager.getLogger("briefconsolelogger");
    private static final Logger logger = LogManager.getLogger("consolelogger");

    public Path directoryPath;

    @Rule
    public final TestingRemoteAndLocalReposRule testingRemoteAndLocalRepos =
            new TestingRemoteAndLocalReposRule(false);

    public final CommandLineTestUtilities commandLineTestUtilities = new CommandLineTestUtilities();

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
        SessionController sessionController = TestUtilities.commonTestFxStart(stage);
    }

    @Test
    public void sshCloneTest() throws Exception {
        CommandLineController.setMethodCalled(false);

        // Clones a test repo via clicking the ribbon button
        String paths = commandLineTestUtilities.cloneRepoUsingButtons(testingRemoteAndLocalRepos, directoryPath);

        // Make sure the text has been updated before checking it
        WaitForAsyncUtils.waitFor(30, TimeUnit.SECONDS, () -> CommandLineController.getMethodCalled());

        console.info("Update text was called: " + CommandLineController.getMethodCalled());

        // Checks that the text in the command line box is what is expected.
        commandLineTestUtilities.checkCommandLineText("git clone " + paths);

        console.info("Test passed.");
    }
}
