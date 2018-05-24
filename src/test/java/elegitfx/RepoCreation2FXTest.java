package elegitfx;

import elegit.Main;
import elegit.controllers.SessionController;
import elegit.models.*;
import elegit.sshauthentication.ElegitUserInfoTest;
import elegit.treefx.Cell;
import elegit.treefx.CellState;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.revwalk.RevCommit;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.testfx.api.FxAssert;
import org.testfx.framework.junit.ApplicationTest;
import org.testfx.framework.junit.TestFXRule;
import sharedrules.TestUtilities;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class RepoCreation2FXTest extends ApplicationTest {

    static {
        // -----------------------Logging Initialization Start---------------------------
        Path logPath = Paths.get("logs");
        String s = logPath.toAbsolutePath().toString();
        System.setProperty("logFolder", s);
    }

    private static final Logger logger = LogManager.getLogger("consolelogger");

    private static final Random random = new Random(90125);

    private SessionController sessionController;

    private Path directoryPath;

    @Rule
    public TestFXRule testFXRule = new TestFXRule();

    @Rule
    public TestName testName = new TestName();

    @Before
    public void setup() throws Exception {
        logger.info("Unit test started");
        directoryPath = Files.createTempDirectory("unitTestRepos");
        directoryPath.toFile().deleteOnExit();
        initializeLogger();
        logger.info("Test name: " + testName.getMethodName());
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
        logger.info("Tearing down");
        assertEquals(0, Main.getAssertionCount());
    }


    @Override
    public void start(Stage stage) throws Exception {
        sessionController = TestUtilities.commonTestFxStart(stage);
    }


    @Test
    public void clickCommitTest() throws Exception {
        logger.info("Temp directory: " + directoryPath);
        Path remote = directoryPath.resolve("remote");
        Path local = directoryPath.resolve("local");
        Git.init().setDirectory(remote.toFile()).setBare(true).call();
        Git.cloneRepository().setDirectory(local.toFile()).setURI("file://"+remote).call();

        ExistingRepoHelper helper = new ExistingRepoHelper(local, new ElegitUserInfoTest());

        Path fileLocation = local.resolve("README.md");

        FileWriter fw = new FileWriter(fileLocation.toString(), true);
        fw.write("start");
        fw.close();
        helper.addFilePathTest(fileLocation);
        RevCommit firstCommit = helper.commit("Appended to file");
        Cell firstCellAttempt = lookup(firstCommit.getName()).query();
        logger.info("firstCell = " + firstCellAttempt);

        for (int i=0; i < 3; i++) {
            fw = new FileWriter(fileLocation.toString(), true);
            fw.write(""+i);
            fw.close();
            helper.addFilePathTest(fileLocation);
            helper.commit("Appended to file");
        }

        // Just push all untracked local branches
        PushCommand command = helper.prepareToPushAll(untrackedLocalBranches -> untrackedLocalBranches);
        helper.pushAll(command);

        logger.info(remote);
        logger.info(local);

        SessionController.gitStatusCompletedOnce = new CountDownLatch(1);

        interact(() -> sessionController.handleLoadExistingRepoOption(local));
        SessionController.gitStatusCompletedOnce.await();

        logger.info("First commit is " + firstCommit.getName());
        interact( () -> {
            Cell firstCell = lookup(Matchers.hasToString(firstCommit.getName())).query();
            assertNotEquals(null, firstCell);
            FxAssert.verifyThat(firstCell, (Cell cell) -> (cell.isVisible()));
            assertEquals(CellState.STANDARD, firstCell.getPersistentCellState());
        });


        // Click on first commit
        clickOn(Matchers.hasToString(firstCommit.getName()));

        // Verify that when you click on it, it turns the appopriate color
        interact(() -> {
            Cell firstCell = lookup(Matchers.hasToString(firstCommit.getName())).query();
            assertEquals(Color.web(CellState.SELECTED.getBackgroundColor()).toString(),
                         Color.web(firstCell.getFxShapeObject().getFill().toString()).toString());
            assertEquals(CellState.SELECTED, firstCell.getPersistentCellState());
        });

        clickOn(Matchers.hasToString(firstCommit.getName()));

        // Verify that when you click on it again, it turns back
        interact(() -> {
            Cell firstCell = lookup(Matchers.hasToString(firstCommit.getName())).query();
            assertEquals(Color.web(CellState.STANDARD.getBackgroundColor()).toString(),
                         Color.web(firstCell.getFxShapeObject().getFill().toString()).toString());
            assertEquals(CellState.STANDARD, firstCell.getPersistentCellState());
        });


    }



}