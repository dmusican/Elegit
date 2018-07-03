package elegitfx;

import elegit.Main;
import elegit.controllers.BusyWindow;
import elegit.controllers.CommitController;
import elegit.controllers.SessionController;
import elegit.exceptions.CancelledAuthorizationException;
import elegit.exceptions.MissingRepoException;
import elegit.exceptions.NoCommitsToPushException;
import elegit.exceptions.PushToAheadRemoteError;
import elegit.models.ExistingRepoHelper;
import elegit.models.LocalBranchHelper;
import elegit.models.RepoHelper;
import elegit.models.SessionModel;
import elegit.monitors.RepositoryMonitor;
import elegit.sshauthentication.ElegitUserInfoTest;
import elegit.treefx.Cell;
import elegit.treefx.CommitTreeModel;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.controlsfx.control.CheckListView;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.testfx.framework.junit.ApplicationTest;
import org.testfx.util.WaitForAsyncUtils;
import sharedrules.TestUtilities;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.testfx.api.FxAssert.verifyThat;
import static org.testfx.matcher.base.NodeMatchers.isDisabled;
import static org.testfx.util.NodeQueryUtils.hasText;
import static org.testfx.util.NodeQueryUtils.isVisible;

public class OpenRepoDirectory2FXTest extends ApplicationTest {
    static {
        // -----------------------Logging Initialization Start---------------------------
        Path logPath = Paths.get("logs");
        String s = logPath.toAbsolutePath().toString();
        System.setProperty("logFolder", s);
    }

    private static final Logger logger = LogManager.getLogger();
    private static final Logger console = LogManager.getLogger("briefconsolelogger");

    private static final Random random = new Random(90125);

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
        TestUtilities.commonShutDown();
        assertEquals(0, Main.getAssertionCount());
    }

    @Test
    /*
     * Tests the open directory button before a repo is loaded (i.e. the button is disabled)
     */
    public void noRepoLoadedTest() throws InterruptedException {
        TestUtilities.startComplete.await();
        // Clicks button to open a nonexistent directory
        clickOn((Node) (lookup("#openRepoDirButton").query()));
        WaitForAsyncUtils.waitForFxEvents();

        // Makes sure the method was not called because the button is disabled
        assertEquals(false, sessionController.getMethodCalled());
    }
}
