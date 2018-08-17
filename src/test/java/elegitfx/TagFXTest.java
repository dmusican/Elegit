package elegitfx;

import elegit.Main;
import elegit.controllers.BusyWindow;
import elegit.controllers.SessionController;
import elegit.monitors.RepositoryMonitor;
import javafx.stage.Stage;
import junit.framework.TestCase;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LsRemoteCommand;
import org.eclipse.jgit.api.TransportCommand;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.testfx.framework.junit.ApplicationTest;
import org.testfx.framework.junit.TestFXRule;
import org.testfx.util.WaitForAsyncUtils;
import sharedrules.TestUtilities;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertTrue;
import static sharedrules.TestUtilities.makeTestRepo;

public class TagFXTest extends ApplicationTest {

    static {
        // -----------------------Logging Initialization Start---------------------------
        Path logPath = Paths.get("logs");
        String s = logPath.toAbsolutePath().toString();
        System.setProperty("logFolder", s);
    }

    private static final Logger logger = LogManager.getLogger("consolelogger");
    private static final Logger console = LogManager.getLogger("briefconsolelogger");

    private SessionController sessionController;

    private Path directoryPath;

    private Stage stage;

    @Rule
    public TestFXRule testFXRule = new TestFXRule();

    @Rule
    public TestName testName = new TestName();

    @Before
    public void setup() throws Exception {
        Random random = new Random();
        ThreadContext.put("id", ""+Math.abs(random.nextLong()));
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
        TestUtilities.cleanupTestFXEnvironment();
        assertEquals(0, Main.getAssertionCount());
    }


    @Override
    public void start(Stage stage) throws Exception {
        sessionController = TestUtilities.commonTestFxStart(stage);
    }


    @Test
    public void test() throws Exception {
        TestUtilities.commonStartupOffFXThread();

        Path remote = directoryPath.resolve("remote1");
        console.info("remote = " + remote);
        Path local = directoryPath.resolve("local1");
        int numFiles = 1;
        int numCells = 2;
        List<RevCommit> allCommits = makeTestRepo(remote, local, numFiles, numCells, true);
        RevCommit firstCommit1 = allCommits.get(0);

        console.info("Loading up repo");
        SessionController.gitStatusCompletedOnce = new CountDownLatch(1);
        interact(() -> sessionController.handleLoadExistingRepoOption(local));

        WaitForAsyncUtils.waitFor(15, TimeUnit.SECONDS,
                                  () -> !BusyWindow.window.isShowing());
        SessionController.gitStatusCompletedOnce.await();


        clickOn("#"+firstCommit1.getName());
        clickOn("#tagNameField");
        write("testTag");
        clickOn("#tagButton");

        WaitForAsyncUtils.waitFor(3, TimeUnit.SECONDS,
                                  () -> lookup("testTag").query() != null);

        assertNotNull(lookup("testTag").query());

        clickOn("#tagNameField");
        write("testTag2");
        clickOn("#tagButton");

        WaitForAsyncUtils.waitFor(3, TimeUnit.SECONDS,
                                  () -> lookup("testTag").query() != null);

        assertNotNull(lookup("testTag2").query());

        rightClickOn("#pushButton")
                .clickOn("#pushTagsContext");
        WaitForAsyncUtils.waitFor(3, TimeUnit.SECONDS,
                                  () -> lookup("Yes").query() != null);
        clickOn("Yes");

        // Make sure that push operation has actually completed; it happens in a different thread than this one.
        SessionController.gitStatusCompletedOnce = new CountDownLatch(1);
        WaitForAsyncUtils.waitFor(15, TimeUnit.SECONDS,
                                  () -> !BusyWindow.window.isShowing());
        WaitForAsyncUtils.waitForFxEvents();
        SessionController.gitStatusCompletedOnce.await();

        // Verify tag made it to remote
        LsRemoteCommand command = Git.lsRemoteRepository().setHeads(true).setTags(true).setRemote("file://" +remote.toString());
        System.out.println("coming");
        Collection<Ref> refs = command.call();
        boolean foundTag1 = false;
        boolean foundTag2 = false;
        for (Ref ref : refs) {
            if (ref.getName().equals("refs/tags/testTag"))
                foundTag1 = true;
            else if (ref.getName().equals("refs/tags/testTag2"))
                foundTag2 = true;
        }

        assert(foundTag1 && foundTag2);

        assertEquals(0, Main.getAssertionCount());

        RepositoryMonitor.pause();
    }


}