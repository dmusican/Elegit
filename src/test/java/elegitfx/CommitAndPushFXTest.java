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
import elegit.monitors.RepositoryMonitor;
import elegit.sshauthentication.ElegitUserInfoTest;
import elegit.treefx.Cell;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import org.testfx.framework.junit.TestFXRule;
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

import static junit.framework.TestCase.assertEquals;

public class CommitAndPushFXTest extends ApplicationTest {

    static {
        // -----------------------Logging Initialization Start---------------------------
        Path logPath = Paths.get("logs");
        String s = logPath.toAbsolutePath().toString();
        System.setProperty("logFolder", s);
    }

    private static final Logger logger = LogManager.getLogger("consolelogger");
    private static final Logger console = LogManager.getLogger("briefconsolelogger");

    private static final Random random = new Random(90125);

    private SessionController sessionController;

    private Path directoryPath;

    private Stage stage;

    @Rule
    public TestFXRule testFXRule = new TestFXRule();

    @Rule
    public TestName testName = new TestName();

    @Before
    public void setup() throws Exception {
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
        console.info("Tearing down");
        assertEquals(0, Main.getAssertionCount());
    }


    @Override
    public void start(Stage stage) throws Exception {
        sessionController = TestUtilities.commonTestFxStart(stage);
    }


    @Test
    public void test() throws Exception {

        Path remote = directoryPath.resolve("remote1");
        Path local = directoryPath.resolve("local1");
        int numFiles = 1;
        int numCells = 500;
        RevCommit firstCommit1 = makeTestRepo(remote, local, numFiles, numCells);

        console.info("Loading up repo");

        SessionController.gitStatusCompletedOnce = new CountDownLatch(1);
        interact(() -> sessionController.handleLoadExistingRepoOption(local));

        WaitForAsyncUtils.waitFor(15, TimeUnit.SECONDS,
                                  () -> !BusyWindow.window.isShowing());
        SessionController.gitStatusCompletedOnce.await();

        clickOn("#mainCommitButton");

        WaitForAsyncUtils.waitFor(15, TimeUnit.SECONDS,
                                  () -> lookup("#commitMessage") != null);
        WaitForAsyncUtils.waitFor(15, TimeUnit.SECONDS,
                                  () -> lookup("#commitMessage").query().isVisible());

        // There's some kind of TestFX bug where sometimes it misses the click on the modal window the first time.
        // At any rate, by doing a click first below, that helps.
        clickOn("#commitViewWindow");

        clickOn("#commitMessage")
                .write("a")
                .clickOn("#commitViewCommitButton");

        Set<Cell> cells = lookup(Matchers.instanceOf(Cell.class)).queryAll();
        console.info("cells = " + cells.size());

        WaitForAsyncUtils.waitFor(10, TimeUnit.SECONDS,
                                  () -> lookup(Matchers.instanceOf(Cell.class)).queryAll().size() == numCells + 1);



        // Do the push
        clickOn("#pushButton");


        WaitForAsyncUtils.waitFor(10, TimeUnit.SECONDS,
                                  () -> lookup("Yes").query() != null);
        sleep(100);

        clickOn("Yes");

        // Wait for at least one round of RepositoryMonitor to follow up
        sleep(Math.max(RepositoryMonitor.LOCAL_CHECK_INTERVAL, RepositoryMonitor.REMOTE_CHECK_INTERVAL));

    }

    private RevCommit makeTestRepo(Path remote, Path local, int numFiles, int numCommits) throws GitAPIException,
            IOException, CancelledAuthorizationException, MissingRepoException, PushToAheadRemoteError, NoCommitsToPushException {
        Git.init().setDirectory(remote.toFile()).setBare(true).call();
        Git.cloneRepository().setDirectory(local.toFile()).setURI("file://" + remote).call();

        ExistingRepoHelper helper = new ExistingRepoHelper(local, new ElegitUserInfoTest());

        for (int fileNum = 0; fileNum < numFiles; fileNum++) {
            Path thisFileLocation = local.resolve("file" + fileNum);
            FileWriter fw = new FileWriter(thisFileLocation.toString(), true);
            fw.write("start"+random.nextInt()); // need this to make sure each repo comes out with different hashes
            fw.close();
            helper.addFilePathTest(thisFileLocation);
        }

        RevCommit firstCommit = helper.commit("Appended to file");
        Cell firstCellAttempt = lookup(firstCommit.getName()).query();
        console.info("firstCell = " + firstCellAttempt);

        for (int i = 0; i < numCommits; i++) {
            if (i % 100 == 0) {
                console.info("commit num = " + i);
            }
            for (int fileNum = 0; fileNum < numFiles; fileNum++) {
                Path thisFileLocation = local.resolve("file" + fileNum);
                FileWriter fw = new FileWriter(thisFileLocation.toString(), false);
                fw.write("" + i);
                fw.close();
                helper.addFilePathTest(thisFileLocation);
            }

            // Commit all but last one, to leave something behind to actually commit in GUI
            if (i < numCommits - 1) {
                helper.commit("Appended to file");
            }
        }

        return firstCommit;
    }

}