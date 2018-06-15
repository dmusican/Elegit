package elegitfx.commandLineTests;

import elegit.controllers.BusyWindow;
import elegit.controllers.CommitController;
import elegit.controllers.SessionController;
import elegit.models.ExistingRepoHelper;
import elegit.monitors.RepositoryMonitor;
import elegit.sshauthentication.ElegitUserInfoTest;
import elegit.treefx.TreeGraph;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.api.Git;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.testfx.framework.junit.ApplicationTest;
import org.testfx.framework.junit.TestFXRule;
import org.testfx.util.WaitForAsyncUtils;
import sharedrules.TestUtilities;
import sharedrules.TestingLogPathRule;
import sharedrules.TestingRemoteAndLocalReposRule;

import java.io.FileWriter;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import javafx.scene.control.TextArea;
import javafx.scene.control.Button;
import javafx.scene.Scene;
import javafx.scene.Node;

/**
 * Created by gorram on 6/14/18.
 */
public class CommitFXTest extends ApplicationTest {
    @Rule
    public final CommonRulesAndSetup commonRulesAndSetup = new CommonRulesAndSetup();
    @ClassRule
    public static final LoggingInitializationStart loggingInitializationStart = new LoggingInitializationStart();
    @ClassRule
    public static final TestingLogPathRule testingLogPath = new TestingLogPathRule();
    @Rule
    public TestFXRule testFXRule = new TestFXRule();
    @Rule
    public TestName testName = new TestName();
    @Rule
    public final TestingRemoteAndLocalReposRule testingRemoteAndLocalRepos =
            new TestingRemoteAndLocalReposRule(false);
    public final CommandLineTestUtilities commandLineTestUtilities = new CommandLineTestUtilities();

    private static final Logger console = LogManager.getLogger("briefconsolelogger");
    private static final Logger logger = LogManager.getLogger("consolelogger");
    private SessionController sessionController;


    @Override
    public void start(Stage stage) throws Exception {
        sessionController = TestUtilities.commonTestFxStart(stage);
    }

    @Test
    public void CommitTest() throws Exception{
        Path local = testingRemoteAndLocalRepos.getLocalFull();
        Git.init().setDirectory(local.toFile()).setBare(false).call();

        ExistingRepoHelper helper = new ExistingRepoHelper(local, new ElegitUserInfoTest());

        Path fileLocation = local.resolve("commitTest.txt");
        FileWriter fw = new FileWriter(fileLocation.toString(), true);
        fw.write("start");
        fw.close();
        helper.addFilePathTest(fileLocation);
        interact(() -> sessionController.handleLoadExistingRepoOption(local));
        RepositoryMonitor.unpause();
        WaitForAsyncUtils.waitFor(10, TimeUnit.SECONDS,
                () -> lookup("commitTest.txt").queryAll().size() == 3);
        console.info("Before clicking commit");
        clickOn("Commit");
        Node node = lookup("#commitViewWindow").query();
        Node area = node.lookup("#commitMessage");
        console.info(area);
        clickOn(area).write("testing");
        clickOn(node.lookup("#commitViewCommitButton"));
        //this should really have something that checks if the new commit has shown up, but just waiting a second seems to work
        //sleep(1500);
        WaitForAsyncUtils.waitFor(10, TimeUnit.SECONDS,
                () -> lookup("commitTest.txt").queryAll().size() != 3);
        commandLineTestUtilities.checkCommandLineText("git commit -m\"testing\"");



    }

}
