package elegitfx;

import elegit.Main;
import elegit.controllers.BusyWindow;
import elegit.controllers.SessionController;
import elegit.exceptions.CancelledAuthorizationException;
import elegit.exceptions.MissingRepoException;
import elegit.exceptions.NoCommitsToPushException;
import elegit.exceptions.PushToAheadRemoteError;
import elegit.models.ExistingRepoHelper;
import elegit.monitors.RepositoryMonitor;
import elegit.sshauthentication.ElegitUserInfoTest;
import elegit.treefx.Cell;
import javafx.stage.Stage;
import junit.framework.TestCase;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.api.Git;
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
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static junit.framework.TestCase.assertEquals;

public class ResetFXTest extends ApplicationTest {

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
        int numCells = 2;
        RevCommit firstCommit1 = makeTestRepo(remote, local, numFiles, numCells);

        console.info("Loading up repo");

        SessionController.gitStatusCompletedOnce = new CountDownLatch(1);
        interact(() -> sessionController.handleLoadExistingRepoOption(local));

        WaitForAsyncUtils.waitFor(15, TimeUnit.SECONDS,
                                  () -> !BusyWindow.window.isShowing());
        SessionController.gitStatusCompletedOnce.await();

        SessionController.gitStatusCompletedOnce = new CountDownLatch(1);

        rightClickOn("#"+firstCommit1.getName())
                .clickOn("#resetMenuReset")
                .moveTo("#resetMenuResetItem")
                .clickOn("#resetMenuAdvanced")
                .clickOn("#resetMenuHard");

        SessionController.gitStatusCompletedOnce.await();

        // Verify that file contents have reverted back to what they should be; do this check in the FX queue
        // to make sure it follows the above
        Scanner scanner = new Scanner(local.resolve("file0"));
        TestCase.assertTrue(scanner.next().startsWith("start"));
        scanner.close();


        assertEquals(0, Main.getAssertionCount());
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