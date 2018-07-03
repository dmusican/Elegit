package elegitfx.conflictResolutionFXTests;

import elegit.Main;
import elegit.controllers.SessionController;
import elegit.models.SessionModel;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fxmisc.richtext.CodeArea;
import org.junit.*;
import org.junit.rules.TestName;
import org.testfx.framework.junit.ApplicationTest;
import org.testfx.framework.junit.TestFXRule;
import org.testfx.util.WaitForAsyncUtils;
import sharedrules.TestUtilities;
import sharedrules.TestingLogPathRule;
import sharedrules.TestingRemoteAndLocalReposRule;

import javax.xml.soap.Node;
import javax.xml.soap.Text;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static junit.framework.TestCase.assertEquals;

/**
 * Created by grenche on 7/2/18.
 * Tests the conflict management tool when there are multiple conflicts
 */
public class MultipleConflictsResolutionTestFX extends ApplicationTest {
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
        Path local = conflictResolutionUtilities.createMultipleConflicts(testingRemoteAndLocalRepos);
        interact(() -> sessionController.handleLoadExistingRepoOption(local));
        interact(() -> sessionController.handleOpenConflictManagementTool(local.toString(),"test.txt"));

        CodeArea middleDoc = lookup("#middleDoc").query();

        // The line in focus should be the first conflict (line 25)
        interact(() -> assertEquals(25, middleDoc.getCurrentParagraph()));

        clickOn("#rightAccept");
        interact(() -> assertEquals("added in mergeBranch", middleDoc.getText(middleDoc.getCurrentParagraph())));
        clickOn("#leftAccept");
        interact(() -> assertEquals("added in master", middleDoc.getText(middleDoc.getCurrentParagraph())));

        // Needed to make sure the jump to the next conflict happens before checking it happened.
        WaitForAsyncUtils.waitForFxEvents();
        sleep(200);

        // Check if the jump happened automatically and line numbers updated (added two lines)
        interact(() -> assertEquals(52, middleDoc.getCurrentParagraph()));

        clickOn("#conflictManagementToolMenuButton")
                .clickOn("#disableAutoSwitchOption");

        clickOn("#rightReject");
        interact(() -> assertEquals("This is a line that was added at the beginning", middleDoc.getText(middleDoc.getCurrentParagraph())));
        clickOn("#leftReject");
        interact(() -> assertEquals("This is a line that was added at the beginning", middleDoc.getText(middleDoc.getCurrentParagraph())));

        WaitForAsyncUtils.waitForFxEvents();
        sleep(200);

        // Check if the jump didn't happened automatically and line numbers didn't update
        interact(() -> assertEquals(52, middleDoc.getCurrentParagraph()));

        // Go up to previous conflict and check that it is what we changed it to be.
        clickOn("#upToggle");
        interact(() -> assertEquals("added in master", middleDoc.getText(middleDoc.getCurrentParagraph())));
        interact(() -> assertEquals(25, middleDoc.getCurrentParagraph()));

        clickOn("#leftUndo");
        interact(() -> assertEquals("added in mergeBranch", middleDoc.getText(middleDoc.getCurrentParagraph())));

        clickOn("#downToggle");
        // Should be up a line because we removed one above
        interact(() -> assertEquals(51, middleDoc.getCurrentParagraph()));

        clickOn("#upToggle");
        clickOn("#upToggle");
        // Should have moved to the last conflict after toggling up from the first one
        interact(() -> assertEquals(101, middleDoc.getCurrentParagraph()));


    }
}
