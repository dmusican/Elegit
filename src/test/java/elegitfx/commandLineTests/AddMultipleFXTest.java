package elegitfx.commandLineTests;

import elegit.controllers.SessionController;
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
import sharedrules.TestUtilities;
import sharedrules.TestingLogPathRule;
import sharedrules.TestingRemoteAndLocalReposRule;

import java.nio.file.Path;
import java.util.ArrayList;

/**
 * Created by gorram on 6/14/18.
 */
public class AddMultipleFXTest extends ApplicationTest{
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
    public void AddMultipleTest() throws Exception{
        //sessionController = TestUtilities.commonTestFxStart(stage);
        Path local = testingRemoteAndLocalRepos.getLocalFull();
        Git.init().setDirectory(local.toFile()).setBare(false).call();
        ArrayList<Path> files = new ArrayList<>();
        for(int i=0;i<5;i++){
            Path fileLocation = local.resolve("addTest"+i+".txt");
            files.add(fileLocation);

        }

        //ExistingRepoHelper helper = new ExistingRepoHelper(local, new ElegitUserInfoTest());

        commandLineTestUtilities.addFile(files, local, sessionController);
        commandLineTestUtilities.checkCommandLineText("git add addTest0.txt addTest1.txt addTest2.txt addTest3.txt addTest4.txt");

        console.info("Test passed.");
    }
}
