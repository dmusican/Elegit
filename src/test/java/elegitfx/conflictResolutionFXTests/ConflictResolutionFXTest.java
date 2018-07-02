package elegitfx.conflictResolutionFXTests;

import elegit.Main;
import elegit.controllers.BusyWindow;
import elegit.controllers.SessionController;
import elegit.exceptions.ExceptionAdapter;
import elegit.models.*;
import elegit.monitors.RepositoryMonitor;
import elegit.sshauthentication.ElegitUserInfoTest;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.revwalk.RevCommit;
import org.fxmisc.richtext.CodeArea;
import org.junit.*;

import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import org.eclipse.jgit.api.MergeCommand;
import org.eclipse.jgit.api.MergeResult;
import org.junit.rules.TestName;
import org.testfx.framework.junit.TestFXRule;
import org.testfx.util.WaitForAsyncUtils;
import org.testfx.framework.junit.ApplicationTest;
import sharedrules.TestUtilities;
import sharedrules.TestingLogPathRule;
import sharedrules.TestingRemoteAndLocalReposRule;

import static junit.framework.TestCase.assertEquals;


/**
 * Created by gorram on 6/26/18.
 * Test some basic operation with a file with only one conflict.
 */
public class ConflictResolutionFXTest extends ApplicationTest{
    static {
        // -----------------------Logging Initialization Start---------------------------
        Path logPath = Paths.get("logs");
        String s = logPath.toAbsolutePath().toString();
        System.setProperty("logFolder", s);
    }

    private static final Logger logger = LogManager.getLogger();
    private static final Logger console = LogManager.getLogger("briefconsolelogger");

    private SessionController sessionController;

    private Path directoryPath;

    private final ConflictResolutionUtilities conflictResolutionUtilities = new ConflictResolutionUtilities();

    @ClassRule
    public static final TestingLogPathRule testingLogPath = new TestingLogPathRule();

    @Rule
    public TestFXRule testFXRule = new TestFXRule();

    @Rule
    public TestName testName = new TestName();

    @Rule
    public final TestingRemoteAndLocalReposRule testingRemoteAndLocalRepos =
            new TestingRemoteAndLocalReposRule(false);

    @Override
    public void start(Stage stage) throws Exception {
        sessionController = TestUtilities.commonTestFxStart(stage);
    }

    @Before
    public void setup() throws Exception {
        logger.info("Unit test started");
        directoryPath = Files.createTempDirectory("unitTestRepos");
        directoryPath.toFile().deleteOnExit();
        logger.info("Test name: " + testName.getMethodName());
        directoryPath = testingRemoteAndLocalRepos.getDirectoryPath();
    }

    @After
    public void tearDown() {
        assertEquals(0, Main.getAssertionCount());
    }

    @Test
    public void testResolveConflicts() throws Exception{
        SessionModel sessionModel = SessionModel.getSessionModel();
        Path local = createConflictingLocalRepo();
        System.out.println(local);
        System.out.println(SessionModel.getSessionModel().getCurrentRepoHelper());
        interact(() -> sessionController.handleLoadExistingRepoOption(local));

        interact(() -> sessionController.handleOpenConflictManagementTool(local.toString(),"test.txt"));

        CodeArea middleDoc = lookup("#middleDoc").query();

        // The line in focus is at the bottom of the viewport because it can't be in the middle
        interact(() -> assertEquals(middleDoc.getCurrentParagraph(), 100));

        clickOn("#rightAccept");
        interact(() -> assertEquals(middleDoc.getText(middleDoc.getCurrentParagraph()), "added in mergeBranch"));

        clickOn("#leftAccept");
        // left should show up above, but should still be the current paragraph
        interact(() -> assertEquals(middleDoc.getText(middleDoc.getCurrentParagraph()), "added in master"));
        interact(() -> assertEquals(middleDoc.getText(middleDoc.getCurrentParagraph() + 1), "added in mergeBranch"));

        clickOn("#leftUndo");
        interact(() -> assertEquals(middleDoc.getText(middleDoc.getCurrentParagraph()), "added in mergeBranch"));

        clickOn("#rightUndo");
        interact(() -> assertEquals(middleDoc.getText(middleDoc.getCurrentParagraph()), ""));

        // Toggling should stay in one place because there is only one conflict
        interact(() -> {
            int currParagraph = middleDoc.getCurrentParagraph();
            clickOn("#upToggle");
            assertEquals(currParagraph, middleDoc.getCurrentParagraph());
            clickOn("#downToggle");
            assertEquals(currParagraph, middleDoc.getCurrentParagraph());
        });

        clickOn("#leftReject");
        interact(() -> assertEquals(middleDoc.getText(middleDoc.getCurrentParagraph()), ""));
        clickOn("#rightReject");
        interact(() -> assertEquals(middleDoc.getText(middleDoc.getCurrentParagraph()), ""));
    }

    private Path createConflictingLocalRepo() {
        try {
            Path local = testingRemoteAndLocalRepos.getLocalFull();
            Git.init().setDirectory(local.toFile()).setBare(false).call();
            //Git.init().setDirectory(remote.toFile()).setBare(true).call();
            //Git.cloneRepository().setDirectory(local.toFile()).setURI("file://"+remote).call();

            ExistingRepoHelper helper = new ExistingRepoHelper(local, new ElegitUserInfoTest());
            SessionModel.getSessionModel().openRepoFromHelper(helper);
            Path fileLocation = local.resolve("test.txt");

            FileWriter fw = new FileWriter(fileLocation.toString(), true);
            for (int i = 0; i < 100; i++) {
                fw.write("This is a line that was added at the beginning \n");
            }
            fw.close();
            helper.addFilePathTest(fileLocation);
            helper.commit("Appended to file");
            BranchHelper baseBranch = helper.getBranchModel().getCurrentBranch();
            LocalBranchHelper mergeBranch = helper.getBranchModel().createNewLocalBranch("mergeBranch");
            fw = new FileWriter(fileLocation.toString(), true);
            fw.write("added in master");
            fw.close();
            helper.addFilePathTest(fileLocation);
            helper.commit("Appended to file");
            mergeBranch.checkoutBranch();
            fw = new FileWriter(fileLocation.toString(), true);
            fw.write("added in mergeBranch");
            fw.close();
            helper.addFilePathTest(fileLocation);
            helper.commit("Appended to file");
            baseBranch.checkoutBranch();
            System.out.println(SessionModel.getSessionModel().getCurrentRepoHelper());
            System.out.println(SessionModel.getSessionModel().getCurrentRepoHelper().getRepo());
            MergeResult mergeResult = helper.getBranchModel().mergeWithBranch(mergeBranch);
            //System.out.println(mergeResult.getMergeStatus());
            assert (mergeResult.getMergeStatus().equals(MergeResult.MergeStatus.CONFLICTING));
            return local;
        } catch (Exception e) {
            throw new ExceptionAdapter(e);
        }
    }
}
