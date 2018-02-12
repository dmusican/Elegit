package elegitfx;

import elegit.Main;
import elegit.controllers.SessionController;
import elegit.models.ClonedRepoHelper;
import elegit.models.ExistingRepoHelper;
import elegit.monitors.RepositoryMonitor;
import elegit.sshauthentication.ElegitUserInfoTest;
import elegit.treefx.CommitTreeModel;
import javafx.scene.Node;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TreeItem;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.testfx.framework.junit.ApplicationTest;
import sharedrules.TestUtilities;
import sharedrules.TestingRemoteAndLocalReposRule;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class AddFXTest extends ApplicationTest {

    static {
        // -----------------------Logging Initialization Start---------------------------
        Path logPath = Paths.get("logs");
        String s = logPath.toAbsolutePath().toString();
        System.setProperty("logFolder", s);
    }

    @Rule
    public final TestingRemoteAndLocalReposRule testingRemoteAndLocalRepos =
            new TestingRemoteAndLocalReposRule(false);

    private static final Logger logger = LogManager.getLogger();
    private static final Logger console = LogManager.getLogger("briefconsolelogger");

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
    // This test will spit some errors about not connecting to a remore, but that's correct.
    // In order to test add, a remote is not necessary.
    public void test() throws Exception {

        Path local = testingRemoteAndLocalRepos.getLocalFull();
        Git.init().setDirectory(local.toFile()).setBare(false).call();

        ExistingRepoHelper helper = new ExistingRepoHelper(local, new ElegitUserInfoTest());

        Path fileLocation = local.resolve("README.md");

        FileWriter fw = new FileWriter(fileLocation.toString(), true);
        fw.write("start");
        fw.close();
        helper.addFilePathTest(fileLocation);
        helper.commit("Appended to file");


        // Get repo into Elegit
        clickOn("#loadNewRepoButton")
                .clickOn("#loadExistingRepoOption")
                .clickOn("#repoInputDialog")
                .write(local.toString())
                .clickOn("#repoInputDialogOK");
        RepositoryMonitor.unpause();

        // Make another modification to the file
        fw = new FileWriter(fileLocation.toString(), true);
        fw.write("update");
        fw.close();

        // Wait for RepositoryMonitor to pick up change
        sleep(10000);

        CheckBoxTreeItem changedFile = (CheckBoxTreeItem)lookup("README.md").query();

        interact(() -> ((CheckBoxTreeItem)changedFile).setSelected(true);
        clickOn(changedFile);


        //
//        SessionController.gitStatusCompletedOnce.await();
//
//        interact(() -> {
//            ScrollPane sp = sessionController.getCommitTreeModel().getTreeGraph().getScrollPane();
//            // Test that scroll pane now has content in it
//            assertTrue(sp.getScene() != null);
//        });
//
//
//
//        assertEquals(0,sessionController.getNotificationPaneController().getNotificationNum());
//
//        RepositoryMonitor.unpause();
//        sleep(Math.max(RepositoryMonitor.REMOTE_CHECK_INTERVAL,
//                RepositoryMonitor.LOCAL_CHECK_INTERVAL)+1000);
//        int numLocalChecks = RepositoryMonitor.getNumLocalChecks();
//        System.out.println("Number of local checks = " + numLocalChecks);
//        int numRemoteChecks = RepositoryMonitor.getNumRemoteChecks();
//        System.out.println("Number of remote checks = " + numRemoteChecks);
//        assertTrue(numLocalChecks > 0 && numLocalChecks < 5);
//        assertTrue(numRemoteChecks > 0 && numRemoteChecks < 5);

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
