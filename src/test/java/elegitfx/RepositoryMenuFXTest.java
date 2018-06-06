package elegitfx;

import elegit.Main;
import elegit.controllers.SessionController;
import elegit.gui.WorkingTreePanelView;
import elegit.models.ExistingRepoHelper;
import elegit.monitors.RepositoryMonitor;
import elegit.sshauthentication.ElegitUserInfoTest;
import elegit.treefx.CommitTreeModel;
import javafx.scene.Node;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.api.Git;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.testfx.framework.junit.ApplicationTest;
import org.testfx.util.WaitForAsyncUtils;
import sharedrules.TestUtilities;
import sharedrules.TestingRemoteAndLocalReposRule;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static junit.framework.TestCase.assertEquals;

public class RepositoryMenuFXTest extends ApplicationTest{

    static {
        // -----------------------Logging Initialization Start---------------------------
        Path logPath = Paths.get("logs");
        String s = logPath.toAbsolutePath().toString();
        System.setProperty("logFolder", s);
    }

    @Rule
    public final TestingRemoteAndLocalReposRule testingRemoteAndLocalRepos =
            new TestingRemoteAndLocalReposRule(false);

    @Rule
    public TestName testName = new TestName();

    private static final Logger logger = LogManager.getLogger();
    private static final Logger console = LogManager.getLogger("briefconsolelogger");

    private SessionController sessionController;

    private Path directoryPath;

    @Override
    public void start(Stage stage) throws Exception {
        sessionController = TestUtilities.commonTestFxStart(stage);
    }

    // Helper method to avoid annoying traces from logger
    void initializeLogger() throws IOException {
        // Create a temp directory for the files to be placed in
        Path logPath = Files.createTempDirectory("elegitLogs");
        logPath.toFile().deleteOnExit();
        System.setProperty("logFolder", logPath.toString());
    }

    @Before
    public void setup() throws Exception {
        logger.info("Unit test started");
        logger.info("Unit test started");
        console.info("Unit test started");
        console.info("Directory = " + directoryPath);
        directoryPath = Files.createTempDirectory("unitTestRepos");
        console.info("Directory = " + directoryPath);
        directoryPath.toFile().deleteOnExit();
        initializeLogger();
        console.info("Test name: " + testName.getMethodName());
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

        clickOn((Node) (lookup("#cloneMenuItem").query()));


        //interact(() -> sessionController.handleLoadExistingRepoOption(local));
        RepositoryMonitor.unpause();

        // Make another modification to the file
        fw = new FileWriter(fileLocation.toString(), true);
        fw.write("update");
        fw.close();

        WaitForAsyncUtils.waitFor(10, TimeUnit.SECONDS,
                                  () -> lookup("README.md").queryAll().size() == 2);


        // When looking up README.md, it registers multiple nodes since it is nested inside a tree. Pick the
        // checkbox of interest.
        WorkingTreePanelView workingTree = lookup("#workingTreePanelView").query();
        interact(() -> workingTree.checkSelectAll());


        console.info("Before clicking add");
        clickOn("Add");

        // Wait for file to also be added to index pane
        WaitForAsyncUtils.waitFor(10, TimeUnit.SECONDS,
                                  () -> lookup("README.md").queryAll().size() == 3);

        console.info("When add seems to be done");

        clickOn((Node) (lookup("#commitNormalMenuItem").query()));
    }
}
