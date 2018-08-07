package elegitfx;

import elegit.Main;
import elegit.controllers.SessionController;
import elegit.exceptions.CancelledAuthorizationException;
import elegit.exceptions.MissingRepoException;
import elegit.exceptions.NoCommitsToPushException;
import elegit.exceptions.PushToAheadRemoteError;
import elegit.models.*;
import elegit.monitors.RepositoryMonitor;
import elegit.sshauthentication.ElegitUserInfoTest;
import elegit.treefx.Cell;
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
import java.util.concurrent.TimeUnit;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class RepoCreation3FXTest extends ApplicationTest {

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
        directoryPath = Files.createTempDirectory("unitTestRepos");
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
        TestUtilities.cleanupTestFXEnvironment();
        assertEquals(0, Main.getAssertionCount());
    }


    @Override
    public void start(Stage stage) throws Exception {
        sessionController = TestUtilities.commonTestFxStart(stage);
    }


    @Test
    public void countOfCommitsInTreeTest() throws Exception {
        TestUtilities.commonStartupOffFXThread();

        // Make two repos; swap between them, make sure number of commits is correct in tree
        console.info("Temp directory: " + directoryPath);

        Path remote1 = directoryPath.resolve("remote1");
        Path local1 = directoryPath.resolve("local1");
        RevCommit firstCommit1 = makeTestRepo(remote1, local1, 5);
        console.info(remote1);
        console.info(local1);

        Path remote2 = directoryPath.resolve("remote2");
        Path local2 = directoryPath.resolve("local2");
        RevCommit firstCommit2 = makeTestRepo(remote2, local2, 5);
        console.info(remote2);
        console.info(local2);

        interact(() -> sessionController.handleLoadExistingRepoOption(local1));

        // Wait for cell to appear; will time out of it doesn't
        WaitForAsyncUtils.waitFor(30, TimeUnit.SECONDS,
                                  () -> lookup(Matchers.hasToString(firstCommit1.getName())).query() != null);
        sleep(100);
        Set<Cell> cells1 = lookup(Matchers.instanceOf(Cell.class)).queryAll();
        console.info("Commits added 1");
        cells1.stream().forEach(console::info);
        assertEquals(6,cells1.size());

        interact(() -> sessionController.handleLoadExistingRepoOption(local2));


        // Wait for cell to appear; will time out of it doesn't
        WaitForAsyncUtils.waitFor(30, TimeUnit.SECONDS,
                                  () -> lookup(Matchers.hasToString(firstCommit2.getName())).query() != null);


        sleep(3000);

        Set<Cell> cells2 = lookup(Matchers.instanceOf(Cell.class)).queryAll();
        console.info("Commits added 2");
        cells2.stream().forEach(logger::info);
        assertEquals(6,cells2.size());

        RepositoryMonitor.pause();
    }

    private RevCommit makeTestRepo(Path remote, Path local, int numCommits) throws GitAPIException, IOException, CancelledAuthorizationException, MissingRepoException, PushToAheadRemoteError, NoCommitsToPushException {
        Git.init().setDirectory(remote.toFile()).setBare(true).call();
        Git.cloneRepository().setDirectory(local.toFile()).setURI("file://" + remote).call();

        ExistingRepoHelper helper = new ExistingRepoHelper(local, new ElegitUserInfoTest());

        Path fileLocation = local.resolve("README.md");

        FileWriter fw = new FileWriter(fileLocation.toString(), true);
        fw.write("start"+random.nextInt()); // need this to make sure each repo comes out with different hashes
        fw.close();
        helper.addFilePathTest(fileLocation);
        RevCommit firstCommit = helper.commit("Appended to file");
        Cell firstCellAttempt = lookup(firstCommit.getName()).query();
        console.info("firstCell = " + firstCellAttempt);

        for (int i = 0; i < numCommits; i++) {
            fw = new FileWriter(fileLocation.toString(), true);
            fw.write("" + i);
            fw.close();
            helper.addFilePathTest(fileLocation);
            helper.commit("Appended to file");
        }

        // Just push all untracked local branches
        PushCommand command = helper.prepareToPushAll(untrackedLocalBranches -> untrackedLocalBranches);
        helper.pushAll(command);

        return firstCommit;
    }

}