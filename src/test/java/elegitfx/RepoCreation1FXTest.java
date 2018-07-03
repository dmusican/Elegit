package elegitfx;

import elegit.Main;
import elegit.controllers.SessionController;
import elegit.models.BranchHelper;
import elegit.models.BranchModel;
import elegit.models.ExistingRepoHelper;
import elegit.models.LocalBranchHelper;
import elegit.sshauthentication.ElegitUserInfoTest;
import elegit.treefx.Cell;
import elegit.treefx.TreeLayout;
import javafx.scene.layout.HBox;
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
import org.testfx.util.WaitForAsyncUtils;
import sharedrules.TestUtilities;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;

public class RepoCreation1FXTest extends ApplicationTest {

    public final int timeoutDelay = 20;
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
    public void highlightCommitTest() throws Exception {
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

        for (int i=0; i < 100; i++) {
            LocalBranchHelper branchHelper = helper.getBranchModel().createNewLocalBranch("branch" + i);
            branchHelper.checkoutBranch();
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

        // Checkout a branch in the middle to see if commit jumps properly
        BranchHelper branchHelper =
                helper.getBranchModel().getBranchByName(BranchModel.BranchType.LOCAL, "branch50");

        branchHelper.checkoutBranch();

        // Used to slow down commit adds. Set to help cause bug we saw where highlight commit was happening before
        // layout was done
        TreeLayout.cellRenderTimeDelay.set(1000);

        interact(() -> sessionController.handleLoadExistingRepoOption(local));

        HBox barAndLabel= lookup("#commitTreeProgressBarAndLabel").query();

        // The bug I'm witnessing involves layout getting called twice in rapid succession. The below code
        // waits for the layout to start and stop; then below that, verifies that the first commit is in there
//        GuiTest.waitUntil(barAndLabel, (HBox box) -> (box.isVisible()));
//        GuiTest.waitUntil(barAndLabel, (HBox box) -> !(box.isVisible()));
        WaitForAsyncUtils.waitFor(timeoutDelay, TimeUnit.SECONDS,
                                  () -> barAndLabel.isVisible());
        WaitForAsyncUtils.waitFor(timeoutDelay, TimeUnit.SECONDS,
                                  () -> !barAndLabel.isVisible());

        // Verify that first commit is actually added at end of first layout call
        interact( () -> {
            Cell firstCell = lookup(Matchers.hasToString(firstCommit.getName())).query();
            assertNotEquals(null, firstCell);
            FxAssert.verifyThat(firstCell, (Cell cell) -> (cell.isVisible()));
        });
        logger.info("Layout done");
    }


}