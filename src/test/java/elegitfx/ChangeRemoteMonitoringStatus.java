package elegitfx;

import elegit.Main;
import elegit.controllers.MenuController;
import elegit.controllers.SessionController;
import elegit.models.ClonedRepoHelper;
import elegit.monitors.RepositoryMonitor;
import javafx.scene.Node;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.testfx.framework.junit.ApplicationTest;
import org.testfx.framework.junit.TestFXRule;
import sharedrules.TestUtilities;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ChangeRemoteMonitoringStatus extends ApplicationTest {

    static {
        // -----------------------Logging Initialization Start---------------------------
        Path logPath = Paths.get("logs");
        String s = logPath.toAbsolutePath().toString();
        System.setProperty("logFolder", s);
    }

    private static final Logger logger = LogManager.getLogger();

    private SessionController sessionController;
    private MenuController menuController;

    @Rule
    public TestFXRule testFXRule = new TestFXRule();

    @Override
    public void start(Stage stage) throws Exception {
        sessionController = TestUtilities.commonTestFxStart(stage);
        menuController = sessionController.getMenuController();

    }

    @Before
    public void setup() {
        logger.info("Unit test started");
    }

    @After
    public void tearDown() {
        System.out.println("Tearing down");
        assertEquals(0,Main.getAssertionCount());
    }



    @Test
    public void changeRemoteMonitoringStatusTest() throws Exception {
        initializeLogger();
        Path directoryPath = Files.createTempDirectory("unitTestRepos");
        directoryPath.toFile().deleteOnExit();
        Path repoPath = directoryPath.resolve("testrepo");
        // Clone from dummy repo:
        String remoteURL = "https://github.com/TheElegitTeam/TestRepository.git";

        // This repo doesn't check username/password for read-only
        UsernamePasswordCredentialsProvider credentials = new UsernamePasswordCredentialsProvider("", "");
        ClonedRepoHelper helper = new ClonedRepoHelper(repoPath, credentials);
        helper.obtainRepository(remoteURL);
        assertNotNull(helper);

        interact(() -> {
            assertEquals(false, sessionController.getRemoteConnectedStatus());
            assertEquals(true, sessionController.getRemoteConnectedDisabledStatus());
        });


        interact(() -> sessionController.handleLoadExistingRepoOption(repoPath));
        SessionController.gitStatusCompletedOnce.await();

        int initNumRemoteChecks = RepositoryMonitor.getNumRemoteChecks();
        Thread.sleep(RepositoryMonitor.REMOTE_CHECK_INTERVAL);
        assertTrue(initNumRemoteChecks < RepositoryMonitor.getNumRemoteChecks());

        clickOn("#remoteConnected");

        initNumRemoteChecks = RepositoryMonitor.getNumRemoteChecks();
        Thread.sleep(RepositoryMonitor.REMOTE_CHECK_INTERVAL);
        assertEquals(initNumRemoteChecks, RepositoryMonitor.getNumRemoteChecks());


    }

    // Helper method to avoid annoying traces from logger
    private void initializeLogger() {
        // Create a temp directory for the files to be placed in
        Path logPath = null;
        try {
            logPath = Files.createTempDirectory("elegitLogs");
        } catch (IOException e) {
            e.printStackTrace();
        }
        logPath.toFile().deleteOnExit();
        System.setProperty("logFolder", logPath.toString());
    }


}
