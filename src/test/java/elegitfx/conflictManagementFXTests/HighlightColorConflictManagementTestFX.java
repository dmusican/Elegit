package elegitfx.conflictManagementFXTests;

import elegit.Main;
import elegit.controllers.SessionController;
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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static junit.framework.TestCase.assertEquals;

/**
 * Created by grenche on 7/3/18.
 * Tests to make sure the color of the conflicts is changing correctly is all three documents.
 */
public class HighlightColorConflictManagementTestFX extends ApplicationTest{
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

    private final ConflictManagementUtilities conflictManagementUtilities = new ConflictManagementUtilities();

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
    public void testConflictHighlighting() throws Exception {
        Path local = conflictManagementUtilities.createMultipleConflicts(testingRemoteAndLocalRepos, true);
        interact(() -> sessionController.handleLoadExistingRepoOption(local));
        interact(() -> sessionController.handleOpenConflictManagementTool(local.toString(), "test.txt"));

        CodeArea leftDoc = lookup("#leftDoc").query();
        CodeArea middleDoc = lookup("#middleDoc").query();
        CodeArea rightDoc = lookup("#rightDoc").query();

        // Check the initial style (should be conflict because docs should go to the first conflict.
        interact(() -> assertEquals("conflict", leftDoc.getStyleAtPosition(leftDoc.getCurrentParagraph(), 0).toArray()[0]));
        interact(() -> assertEquals("conflict", rightDoc.getStyleAtPosition(rightDoc.getCurrentParagraph(), 0).toArray()[0]));

        clickOn("#leftReject");
        interact(() -> assertEquals("handled-conflict", leftDoc.getStyleAtPosition(leftDoc.getCurrentParagraph(), 0).toArray()[0]));

        clickOn("#leftUndo");
        interact(() -> assertEquals("conflict", leftDoc.getStyleAtPosition(leftDoc.getCurrentParagraph(), 0).toArray()[0]));

        clickOn("#rightAccept");
        interact(() -> assertEquals("handled-conflict", rightDoc.getStyleAtPosition(rightDoc.getCurrentParagraph(), 0).toArray()[0]));
        interact(() -> assertEquals("handled-conflict", middleDoc.getStyleAtPosition(middleDoc.getCurrentParagraph(), 0).toArray()[0]));

        clickOn("#rightUndo");
        interact(() -> assertEquals("conflict", rightDoc.getStyleAtPosition(rightDoc.getCurrentParagraph(), 0).toArray()[0]));

        clickOn("#rightAccept");
        clickOn("#leftAccept");

        // Should have automatically toggled down because both sides were handled.
        WaitForAsyncUtils.waitForFxEvents();
        sleep(200);
        clickOn("#upToggle");

        interact(() -> assertEquals("handled-conflict", leftDoc.getStyleAtPosition(leftDoc.getCurrentParagraph(), 0).toArray()[0]));
        interact(() -> assertEquals("handled-conflict", middleDoc.getStyleAtPosition(middleDoc.getCurrentParagraph(), 0).toArray()[0]));
        interact(() -> assertEquals("handled-conflict", rightDoc.getStyleAtPosition(rightDoc.getCurrentParagraph(), 0).toArray()[0]));

        clickOn("#rightUndo");
        interact(() -> assertEquals("handled-conflict", leftDoc.getStyleAtPosition(leftDoc.getCurrentParagraph(), 0).toArray()[0]));
        interact(() -> assertEquals("handled-conflict", middleDoc.getStyleAtPosition(middleDoc.getCurrentParagraph(), 0).toArray()[0]));
        interact(() -> assertEquals("conflict", rightDoc.getStyleAtPosition(rightDoc.getCurrentParagraph(), 0).toArray()[0]));

        clickOn("#rightReject");

        // Should have automatically toggled down because both sides were handled.
        WaitForAsyncUtils.waitForFxEvents();
        sleep(200);
        clickOn("#downToggle");
        clickOn("#downToggle");
        clickOn("#downToggle");

        interact(() -> assertEquals("handled-conflict", leftDoc.getStyleAtPosition(leftDoc.getCurrentParagraph(), 0).toArray()[0]));
        interact(() -> assertEquals("handled-conflict", middleDoc.getStyleAtPosition(middleDoc.getCurrentParagraph(), 0).toArray()[0]));
        interact(() -> assertEquals("handled-conflict", rightDoc.getStyleAtPosition(rightDoc.getCurrentParagraph(), 0).toArray()[0]));

        // Check that the added non conflicting line is green
        interact(() -> leftDoc.moveTo(0,0));
        interact(() -> leftDoc.requestFollowCaret());
        interact(() -> middleDoc.moveTo(0,0));
        interact(() -> middleDoc.requestFollowCaret());

        interact(() -> assertEquals("changed-conflict", leftDoc.getStyleAtPosition(leftDoc.getCurrentParagraph(), 0).toArray()[0]));
        interact(() -> assertEquals("changed-conflict", middleDoc.getStyleAtPosition(middleDoc.getCurrentParagraph(), 0).toArray()[0]));

    }
}
