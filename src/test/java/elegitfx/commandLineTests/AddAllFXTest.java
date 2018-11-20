package elegitfx.commandLineTests;

import elegit.controllers.BusyWindow;
import elegit.controllers.SessionController;
import elegit.gui.WorkingTreePanelView;
import elegit.models.ExistingRepoHelper;
import elegit.monitors.RepositoryMonitor;
import elegit.sshauthentication.ElegitUserInfoTest;
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
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Created by gorram on 6/14/18.
 */
public class AddAllFXTest extends ApplicationTest{
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
    public void AddAllTest() throws Exception{
        //sessionController = TestUtilities.commonTestFxStart(stage);
        Path local = testingRemoteAndLocalRepos.getLocalFull();
        Git.init().setDirectory(local.toFile()).setBare(false).call();
        ArrayList<Path> filePaths = new ArrayList<>();
        ExistingRepoHelper helper = new ExistingRepoHelper(local, new ElegitUserInfoTest());
        for(int i=0;i<5;i++){
            Path fileLocation = local.resolve("addTest"+i+".txt");
            filePaths.add(fileLocation);
            FileWriter fw = new FileWriter(fileLocation.toString(), true);
            fw.write("start");
            fw.close();
        }
        helper.addFilePathsTest(filePaths, false);
        helper.commit("made initial file");
        interact(() -> sessionController.handleLoadExistingRepoOption(local));
        RepositoryMonitor.unpause();
        for(Path filePath : filePaths) {
            FileWriter fw = new FileWriter(filePath.toString(), true);
            fw.write("update");
            fw.close();
        }
        SessionController.gitStatusCompletedOnce = new CountDownLatch(1);
        SessionController.gitStatusCompletedOnce.await();
        WorkingTreePanelView workingTree = lookup("#workingTreePanelView").query();
        interact(() -> workingTree.checkSelectAll());

        console.info("Before clicking add");
        clickOn("Add");

        WaitForAsyncUtils.waitForFxEvents();
        WaitForAsyncUtils.waitFor(10, TimeUnit.SECONDS, () -> !BusyWindow.window.isShowing());

        console.info("When add seems to be done");

        commandLineTestUtilities.checkCommandLineText("git add *");

        console.info("Test passed.");
    }
}
