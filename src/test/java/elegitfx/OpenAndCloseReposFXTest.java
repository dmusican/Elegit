package elegitfx;

import elegit.Main;
import elegit.controllers.BusyWindow;
import elegit.controllers.SessionController;
import elegit.models.ExistingRepoHelper;
import elegit.models.RepoHelper;
import elegit.models.SessionModel;
import elegit.monitors.RepositoryMonitor;
import elegit.sshauthentication.ElegitUserInfoTest;
import elegit.treefx.CommitTreeModel;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.controlsfx.control.CheckListView;
import org.eclipse.jgit.api.Git;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.testfx.framework.junit.ApplicationTest;
import org.testfx.framework.junit.TestFXRule;
import org.testfx.util.WaitForAsyncUtils;
import sharedrules.TestUtilities;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class OpenAndCloseReposFXTest extends ApplicationTest {

    public static final int timeoutDelay = 20;

    static {
        // -----------------------Logging Initialization Start---------------------------
        Path logPath = Paths.get("logs");
        String s = logPath.toAbsolutePath().toString();
        System.setProperty("logFolder", s);
    }

    private static final Logger logger = LogManager.getLogger();
    private static final Logger console = LogManager.getLogger("briefconsolelogger");

    private SessionController sessionController;

    @Rule
    public TestFXRule testFXRule = new TestFXRule();

    @Override
    public void start(Stage stage) throws Exception {
        sessionController = TestUtilities.commonTestFxStart(stage);

        if (!Main.initializationComplete.get()) {
            BusyWindow.show();
        }

    }

    @Before
    public void setup() {
        logger.info("Unit test started");
    }

    @After
    public void tearDown() {
        console.info("Tearing down");
        TestUtilities.cleanupTestEnvironment();
        assertEquals(0,Main.getAssertionCount());
    }

    // Helper method to avoid annoying traces from logger
    void initializeLogger() {
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


    @Test
    public void test() throws Exception {
        TestUtilities.commonStartupOffFXThread();

        TestUtilities.startComplete.await();
        WaitForAsyncUtils.waitFor(20, TimeUnit.SECONDS,
                                  () -> !BusyWindow.window.isShowing());
        initializeLogger();
        Path directoryPath = Files.createTempDirectory("unitTestRepos");
        directoryPath.toFile().deleteOnExit();

        console.info("Setting up initial repos");
        Path repoPath1 = makeTempLocalRepo(directoryPath,"repo1");
        Path repoPath2 = makeTempLocalRepo(directoryPath,"repo2");
        console.info("Done setting up initial repos");

        CommitTreeModel.setAddCommitDelay(5);

        SessionController.gitStatusCompletedOnce = new CountDownLatch(1);

        for (int i=0; i < 3; i++) {
            addSwapAndRemoveRepos(repoPath1, repoPath2);
            interact(() -> console.info("Pass completed"));
        }


    }

    private Path makeTempLocalRepo(Path directoryPath, String localName) throws Exception {
        Path remote = directoryPath.resolve("remote");
        Path local = directoryPath.resolve(localName);
        Git.init().setDirectory(remote.toFile()).setBare(true).call();
        Git.cloneRepository().setDirectory(local.toFile()).setURI("file://"+remote).call();

        ExistingRepoHelper helper = new ExistingRepoHelper(local, new ElegitUserInfoTest());

        Path fileLocation = local.resolve("README.md");

        for (int i=0; i < 5; i++) {
            FileWriter fw = new FileWriter(fileLocation.toString(), true);
            fw.write("start");
            fw.close();
            helper.addFilePathTest(fileLocation);
            helper.commit("Appended to file");
        }
        return local;

    }

    private void addSwapAndRemoveRepos(Path repoPath, Path repoPath2) throws InterruptedException, TimeoutException {
        interact(() -> sessionController.handleLoadExistingRepoOption(repoPath));
        SessionController.gitStatusCompletedOnce.await();
        RepositoryMonitor.pause();

        final ComboBox<RepoHelper> dropdown = lookup("#repoDropdown").query();

        WaitForAsyncUtils.waitFor(timeoutDelay, TimeUnit.SECONDS,
                                  () -> !BusyWindow.window.isShowing());


        // Now that both repos have been added, the dropdown should contain both of them.
        // It's important that test happens on the FX thread, since the above update happens there.
        interact(() -> assertEquals(1, dropdown.getItems().size()));
        interact(() -> assertEquals(1, SessionModel.getSessionModel().getAllRepoHelpers().size()));

        console.info("Loading second repo.");

        interact(() -> sessionController.handleLoadExistingRepoOption(repoPath2));

        WaitForAsyncUtils.waitFor(timeoutDelay, TimeUnit.SECONDS,
                                  () -> !BusyWindow.window.isShowing());

        // Now that both repos have been added, the dropdown should contain both of them.
        // It's important that test happens on the FX thread, since the above update happens there.
        interact(() -> assertEquals(2, dropdown.getItems().size()));
        interact(() -> assertEquals(2, SessionModel.getSessionModel().getAllRepoHelpers().size()));

        for (int i=0; i < 3; i++) {
            WaitForAsyncUtils.waitFor(timeoutDelay, TimeUnit.SECONDS,
                                      () -> dropdown.getValue().toString().equals("repo2"));
            WaitForAsyncUtils.waitForFxEvents();
            sleep(100);
            clickOn(dropdown);

            // The below awful hack is very likely related to this bug:
            // https://github.com/dmusican/Elegit/issues/539
            // For unknown (as of yet) reasons, the dropbox sometimes requires a second click to fire.
            // This should be fixed, but that's a separate non-critical issue form what this test is trying to test.
            interact(() -> {
                if (!dropdown.isShowing())
                    clickOn(dropdown);
            });

            clickOn("repo1");
            WaitForAsyncUtils.waitFor(timeoutDelay, TimeUnit.SECONDS,
                                      () -> !BusyWindow.window.isShowing());

            WaitForAsyncUtils.waitFor(10, TimeUnit.SECONDS,
                                      () -> dropdown.getValue().toString().equals("repo1"));
            WaitForAsyncUtils.waitForFxEvents();
            sleep(100);
            clickOn(dropdown);

            // See comment above regarding bug #539.
            interact(() -> {
                if (!dropdown.isShowing())
                    clickOn(dropdown);
            });
            clickOn("repo2");
            WaitForAsyncUtils.waitFor(timeoutDelay, TimeUnit.SECONDS,
                                      () -> !BusyWindow.window.isShowing());
        }


        // Verify that right number of repos remain on the list
        interact(() -> assertEquals(2, dropdown.getItems().size()));
        interact(() -> assertEquals(2, SessionModel.getSessionModel().getAllRepoHelpers().size()));

        WaitForAsyncUtils.waitFor(timeoutDelay, TimeUnit.SECONDS,
                                  () -> dropdown.getValue().toString().equals("repo2"));
        interact(() -> console.info(dropdown.getItems() + " " + dropdown.getValue()));
        clickOn("#removeRecentReposButton");

        CheckListView<RepoHelper> repoCheckList = lookup("#repoCheckList").query();

        // Selects testrepo
        interact(() -> {
            repoCheckList.getItemBooleanProperty(0).set(true);
        });

        interact(() -> console.info(dropdown.getItems() + " " + dropdown.getValue()));

        // Clicks button to remove testrepo
        clickOn((Node)(lookup("#reposDeleteRemoveSelectedButton").query()));

        assertEquals(0, sessionController.getNotificationPaneController().getNotificationNum());

        assertNotEquals(null,lookup("repo2").query());

        WaitForAsyncUtils.waitFor(timeoutDelay, TimeUnit.SECONDS,
                                  () -> dropdown.getValue().toString().equals("repo2"));

        // Verify that now only one repo remains on the list
        interact(() -> assertEquals(1, dropdown.getItems().size()));

        clickOn("#removeRecentReposButton");

        // Selects otherrepo
        interact(() -> {
            repoCheckList.getItemBooleanProperty(0).set(true);
        });

        clickOn((Node)(lookup("#reposDeleteRemoveSelectedButton").query()));

        assertEquals(0, sessionController.getNotificationPaneController().getNotificationNum());

        // Verify that now no repos remains on the list
        interact(() -> assertEquals(0, dropdown.getItems().size()));
    }

}
