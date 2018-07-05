package elegitfx;

import elegit.Main;
import elegit.controllers.SessionController;
import elegit.monitors.RepositoryMonitor;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.testfx.framework.junit.ApplicationTest;
import org.testfx.framework.junit.TestFXRule;
import sharedrules.TestUtilities;

import java.nio.file.Path;
import java.nio.file.Paths;

import static junit.framework.TestCase.assertEquals;

public class RepositoryMonitor3FXTest extends ApplicationTest {

    static {
        // -----------------------Logging Initialization Start---------------------------
        Path logPath = Paths.get("logs");
        String s = logPath.toAbsolutePath().toString();
        System.setProperty("logFolder", s);
    }

    private static final Logger logger = LogManager.getLogger();

    private SessionController sessionController;

    @Rule
    public TestFXRule testFXRule = new TestFXRule();

    @Override
    public void start(Stage stage) throws Exception {
        sessionController = TestUtilities.commonTestFxStart(stage);
    }

    @Before
    public void setup() {
        logger.info("Unit test started");
    }

    @After
    public void tearDown() {
        TestUtilities.commonShutDown();
        assertEquals(0,Main.getAssertionCount());
    }

    @Test
    public void repoMonitorOnNoRepoOpenTest() throws Exception {
        TestUtilities.commonStartupOffFXThread();

        RepositoryMonitor.unpause();
        sleep(Math.max(RepositoryMonitor.REMOTE_CHECK_INTERVAL,
                RepositoryMonitor.LOCAL_CHECK_INTERVAL)+1000);
        int numLocalChecks = RepositoryMonitor.getNumLocalChecks();
        System.out.println("Number of local checks = " + numLocalChecks);
        int numRemoteChecks = RepositoryMonitor.getNumRemoteChecks();
        System.out.println("Number of remote checks = " + numRemoteChecks);
        assertEquals(0, RepositoryMonitor.getExceptionCounter());
        assertEquals(0, SessionController.getGenericExceptionCount());
        //assertTrue(numLocalChecks > 0 && numLocalChecks < 5);
        //assertTrue(numRemoteChecks > 0 && numRemoteChecks < 5);
    }

}
