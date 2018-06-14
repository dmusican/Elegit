package elegitfx.commandLineTests;

import elegit.controllers.SessionController;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.testfx.framework.junit.ApplicationTest;
import org.testfx.framework.junit.TestFXRule;
import sharedrules.TestUtilities;
import sharedrules.TestingLogPathRule;

import java.nio.file.Path;

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

    @Override
    public void start(Stage stage) throws Exception {
        sessionController = TestUtilities.commonTestFxStart(stage);
    }

    @Test
    public void checkoutFiles() throws Exception {
        commandLineTestUtilities.setupTestRepo(directoryPath, sessionController);
        console.info("Set up done.");
    }
}
