package elegitfx;

import elegit.Main;
import elegit.controllers.BusyWindow;
import elegit.controllers.SessionController;
import elegit.exceptions.CancelledAuthorizationException;
import elegit.exceptions.MissingRepoException;
import elegit.exceptions.NoCommitsToPushException;
import elegit.exceptions.PushToAheadRemoteError;
import elegit.models.BranchHelper;
import elegit.models.BranchModel;
import elegit.models.ExistingRepoHelper;
import elegit.models.LocalBranchHelper;
import elegit.models.SessionModel;
import elegit.monitors.RepositoryMonitor;
import elegit.sshauthentication.ElegitUserInfoTest;
import elegit.treefx.Cell;
import elegit.treefx.CellState;
import elegit.treefx.TreeLayout;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
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
import org.junit.Test;
import org.loadui.testfx.GuiTest;
import org.testfx.api.FxAssert;
import org.testfx.framework.junit.ApplicationTest;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.prefs.Preferences;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;

public class RepoCreationTests extends ApplicationTest {

    static {
        // -----------------------Logging Initialization Start---------------------------
        Path logPath = Paths.get("logs");
        String s = logPath.toAbsolutePath().toString();
        System.setProperty("logFolder", s);
    }

    private static final Logger logger = LogManager.getLogger("consolelogger");

    private static final Random random = new Random(90125);

    private SessionController sessionController;
    private static GuiTest testController;

    private Path directoryPath;

    @Before
    public void setUp() throws Exception {
        logger.info("Unit test started");
        directoryPath = Files.createTempDirectory("unitTestRepos");
        directoryPath.toFile().deleteOnExit();
        initializeLogger();
    }

    // Helper method to avoid annoying traces from logger
    void initializeLogger() throws IOException {
        // Create a temp directory for the files to be placed in
        Path logPath = Files.createTempDirectory("elegitLogs");
        logPath.toFile().deleteOnExit();
        System.setProperty("logFolder", logPath.toString());
    }


    @Override
    public void start(Stage stage) throws Exception {
        Main.testMode = true;
        BusyWindow.setParentWindow(stage);

        Preferences prefs = Preferences.userNodeForPackage(this.getClass());
        prefs.removeNode();

        SessionModel.setPreferencesNodeClass(this.getClass());
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/elegit/fxml/MainView.fxml"));
        fxmlLoader.load();
        sessionController = fxmlLoader.getController();
        Parent root = fxmlLoader.getRoot();
        Rectangle2D primScreenBounds = Screen.getPrimary().getVisualBounds();
        Scene scene = new Scene(root, 800, 600);
        stage.setScene(scene);
        sessionController.setStageForNotifications(stage);
        stage.show();
        stage.toFront();
        // TODO: Remove this pause and keep test working; no good reason for it to be necessary
        RepositoryMonitor.pause();

    }

    @Before
    public void setup() throws Exception {
        logger.info("Unit test started");
        directoryPath = Files.createTempDirectory("unitTestRepos");
        directoryPath.toFile().deleteOnExit();
    }

    @After
    public void tearDown() {
        logger.info("Tearing down");
        assertEquals(0, Main.getAssertionCount());
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

        clickOn("#loadNewRepoButton")
                .clickOn("#loadExistingRepoOption")
                .clickOn("#repoInputDialog")
                .write(local.toString())
                .clickOn("#repoInputDialogOK");

        HBox barAndLabel= lookup("#commitTreeProgressBarAndLabel").query();

        // The bug I'm witnessing involves layout getting called twice in rapid succession. The below code
        // waits for the layout to start and stop; then below that, verifies that the first commit is in there
        GuiTest.waitUntil(barAndLabel, (HBox box) -> (box.isVisible()));
        GuiTest.waitUntil(barAndLabel, (HBox box) -> !(box.isVisible()));

        // Verify that first commit is actually added at end of first layout call
        interact( () -> {
            Cell firstCell = lookup(Matchers.hasToString(firstCommit.getName())).query();
            assertNotEquals(null, firstCell);
            FxAssert.verifyThat(firstCell, (Cell cell) -> (cell.isVisible()));
        });
        logger.info("Layout done");
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

        clickOn("#loadNewRepoButton")
                .clickOn("#loadExistingRepoOption")
                .clickOn("#repoInputDialog")
                .write(local.toString())
                .clickOn("#repoInputDialogOK");

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




    @Test
    public void countOfCommitsInTreeTest() throws Exception {

        // Make two repos; swap between them, make sure number of commits is correct in tree
        logger.info("Temp directory: " + directoryPath);

        Path remote1 = directoryPath.resolve("remote1");
        Path local1 = directoryPath.resolve("local1");
        RevCommit firstCommit1 = makeTestRepo(remote1, local1, 5);
        logger.info(remote1);
        logger.info(local1);

        Path remote2 = directoryPath.resolve("remote2");
        Path local2 = directoryPath.resolve("local2");
        RevCommit firstCommit2 = makeTestRepo(remote2, local2, 5);
        logger.info(remote2);
        logger.info(local2);

        SessionController.gitStatusCompletedOnce = new CountDownLatch(1);

        clickOn("#loadNewRepoButton")
                .clickOn("#loadExistingRepoOption")
                .clickOn("#repoInputDialog")
                .write(local1.toString())
                .clickOn("#repoInputDialogOK");

        SessionController.gitStatusCompletedOnce.await();

        Cell firstCell1 = lookup(Matchers.hasToString(firstCommit1.getName())).query();
        assertNotEquals(null, firstCell1);

        Set<Cell> cells1 = lookup(Matchers.instanceOf(Cell.class)).queryAll();
        logger.info("Commits added 1");
        cells1.stream().forEach(logger::info);
        assertEquals(6,cells1.size());

        clickOn("#loadNewRepoButton")
                .clickOn("#loadExistingRepoOption")
                .clickOn("#repoInputDialog")
                .write(local2.toString())
                .clickOn("#repoInputDialogOK");


        Cell firstCell2 = lookup(Matchers.hasToString(firstCommit2.getName())).query();
        assertNotEquals(null, firstCell2);

        sleep(3000);

        Set<Cell> cells2 = lookup(Matchers.instanceOf(Cell.class)).queryAll();
        logger.info("Commits added 2");
        cells2.stream().forEach(logger::info);
        assertEquals(6,cells2.size());
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
        logger.info("firstCell = " + firstCellAttempt);

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