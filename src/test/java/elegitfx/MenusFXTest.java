package elegitfx;

import elegit.Main;
import elegit.controllers.BusyWindow;
import elegit.controllers.SessionController;
import elegit.models.RepoHelper;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.*;
import org.junit.rules.TestName;
import org.testfx.framework.junit.ApplicationTest;
import org.testfx.framework.junit.TestFXRule;
import org.testfx.util.WaitForAsyncUtils;
import sharedrules.TestUtilities;
import sharedrules.TestingLogPathRule;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static junit.framework.TestCase.assertEquals;
import static sharedrules.TestUtilities.makeTestRepo;

/**
 * Created by grenche on 7/9/18.
 * Tests some of the menu options that don't just call SessionController methods.
 * NOTE: currently doesn't test if clicking the menu items for the different repos actually does what it's supposed to
 * because I can't get testFX to click on them (by ids or strings), but tests that the sub menus update correctly
 */
public class MenusFXTest extends ApplicationTest {
    static {
        // -----------------------Logging Initialization Start---------------------------
        Path logPath = Paths.get("logs");
        String s = logPath.toAbsolutePath().toString();
        System.setProperty("logFolder", s);
    }

    private static final Logger logger = LogManager.getLogger();
    private static final Logger console = LogManager.getLogger("briefconsolelogger");

    @ClassRule
    public static final TestingLogPathRule testingLogPath = new TestingLogPathRule();

    private SessionController sessionController;

    private Path directoryPath;

    @Rule
    public TestFXRule testFXRule = new TestFXRule();

    @Rule
    public TestName testName = new TestName();

    @Override
    public void start(Stage stage) throws Exception {
        sessionController = TestUtilities.commonTestFxStart(stage);
    }

    @Before
    public void setup() throws IOException {
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
        TestUtilities.cleanupTestEnvironment();
        assertEquals(0,Main.getAssertionCount());
    }

    @Test
    public void menusTest() throws Exception {
        TestUtilities.commonStartupOffFXThread();

        Path remote1 = directoryPath.resolve("remote1");
        Path local1 = directoryPath.resolve("local1");
        makeTestRepo(remote1, local1, 1, 2, true);

        Path remote2 = directoryPath.resolve("remote2");
        Path local2 = directoryPath.resolve("local2");
        makeTestRepo(remote2, local2, 1, 2, true);

        console.info("Loading up repo1");
        SessionController.gitStatusCompletedOnce = new CountDownLatch(1);
        interact(() -> sessionController.handleLoadExistingRepoOption(local1));

        WaitForAsyncUtils.waitFor(15, TimeUnit.SECONDS,
                () -> !BusyWindow.window.isShowing());
        SessionController.gitStatusCompletedOnce.await();

        console.info("Loading up repo2");
        SessionController.gitStatusCompletedOnce = new CountDownLatch(1);
        interact(() -> sessionController.handleLoadExistingRepoOption(local2));

        WaitForAsyncUtils.waitFor(15, TimeUnit.SECONDS,
                () -> !BusyWindow.window.isShowing());
        SessionController.gitStatusCompletedOnce.await();

        final ComboBox<RepoHelper> dropdown = lookup("#repoDropdown").query();

        interact(() -> assertEquals("local2", dropdown.getValue().toString()));

        // TODO: for some reason testfx won't click any of the menu items
        // This is what I want to do, but instead I have to get it manually
//        moveTo("File")
//                .moveTo("Open")
//                .clickOn("local1");

        final MenuBar menuBar = lookup("#menuBar").query();
        final ObservableList<Menu> children = menuBar.getMenus();
        final Menu file = children.get(0);
        final Menu open = (Menu) file.getItems().get(1);
        final Menu close = (Menu) file.getItems().get(2);

        interact(() -> assertEquals("local1", open.getItems().get(0).getText()));
        interact(() -> assertEquals("local2", open.getItems().get(1).getText()));
        interact(() -> assertEquals(2, open.getItems().size()));

        interact(() -> assertEquals("local1", close.getItems().get(0).getText()));
        interact(() -> assertEquals("local2", close.getItems().get(1).getText()));
        interact(() -> assertEquals(2, close.getItems().size()));

        // local1 is initially selected
        clickOn("#removeRecentReposButton")
                .clickOn("#reposDeleteRemoveSelectedButton");

        interact(() -> assertEquals("local2", open.getItems().get(0).getText()));
        interact(() -> assertEquals(1, open.getItems().size()));

        interact(() -> assertEquals("local2", close.getItems().get(0).getText()));
        interact(() -> assertEquals(1, close.getItems().size()));
    }
}
