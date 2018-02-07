package elegitfx;

import elegit.Main;
import elegit.controllers.SessionController;
import elegit.models.ClonedRepoHelper;
import elegit.monitors.RepositoryMonitor;
import elegit.treefx.CommitTreeModel;
import javafx.scene.control.ScrollPane;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.testfx.framework.junit.ApplicationTest;
import sharedrules.TestUtilities;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.*;

public class OpenLocalRepoFXTest extends ApplicationTest {

    static {
        // -----------------------Logging Initialization Start---------------------------
        Path logPath = Paths.get("logs");
        String s = logPath.toAbsolutePath().toString();
        System.setProperty("logFolder", s);
    }

    private static final Logger logger = LogManager.getLogger();

    private SessionController sessionController;

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
        assertEquals(0,Main.getAssertionCount());
    }



    @Test
    public void openLocalRepoTest() throws Exception {
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
            ScrollPane sp = sessionController.getCommitTreeModel().getTreeGraph().getScrollPane();
            // Test that scroll pane has no content yet
            assertTrue(sp.getScene() == null);
        });

        CommitTreeModel.setAddCommitDelay(500);



        clickOn("#loadNewRepoButton")
                .clickOn("#loadExistingRepoOption")
                .clickOn("#repoInputDialog")
                .write(repoPath.toString())
                .clickOn("#repoInputDialogOK");

        SessionController.gitStatusCompletedOnce.await();

        interact(() -> {
            ScrollPane sp = sessionController.getCommitTreeModel().getTreeGraph().getScrollPane();
            // Test that scroll pane now has content in it
            assertTrue(sp.getScene() != null);
        });



        assertEquals(0,sessionController.getNotificationPaneController().getNotificationNum());

        RepositoryMonitor.unpause();
        sleep(Math.max(RepositoryMonitor.REMOTE_CHECK_INTERVAL,
                RepositoryMonitor.LOCAL_CHECK_INTERVAL)+1000);
        int numLocalChecks = RepositoryMonitor.getNumLocalChecks();
        System.out.println("Number of local checks = " + numLocalChecks);
        int numRemoteChecks = RepositoryMonitor.getNumRemoteChecks();
        System.out.println("Number of remote checks = " + numRemoteChecks);
        assertTrue(numLocalChecks > 0 && numLocalChecks < 5);
        assertTrue(numRemoteChecks > 0 && numRemoteChecks < 5);

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
