package elegitfx.conflictManagementFXTests;

import elegit.Main;
import elegit.controllers.SessionController;
import javafx.scene.control.Button;
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
 * Tests that the line numbers that are toggled to are correct and relative to the length of the conflict/document
 * and updated correctly in the middle doc when changes are made.
 */
public class LineNumbersConflictManagementTestFX extends ApplicationTest {
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
    public void testConflictingLineNumbers() throws Exception {
        Path local = conflictManagementUtilities.createMultipleConflicts(testingRemoteAndLocalRepos, true);
        interact(() -> sessionController.handleLoadExistingRepoOption(local));
        interact(() -> sessionController.handleOpenConflictManagementTool(local.toString(), "test.txt"));

        CodeArea leftDoc = lookup("#leftDoc").query();
        CodeArea middleDoc = lookup("#middleDoc").query();
        CodeArea rightDoc = lookup("#rightDoc").query();

        // Check line numbers at the beginning. Should all be at the first conflict, but would be different line numbers.
        checkLineNumbers(leftDoc, middleDoc, rightDoc, 27, 27, 25);


        clickOn("#upToggle");
        checkLineNumbers(leftDoc, middleDoc, rightDoc, 105, 102, 103);

        clickOn("#downToggle");
        checkLineNumbers(leftDoc, middleDoc, rightDoc, 27, 27, 25);

        // Always seems to click below the conflict, but if this test is failing it might be because this is clicking
        // on an actual conflict
        clickOn("#leftDoc");
        Button leftAccept = lookup("#leftAccept").query();
        Button leftReject = lookup("#leftReject").query();
        Button leftUndo = lookup("#leftUndo").query();

        Button rightAccept = lookup("#rightAccept").query();
        Button rightReject = lookup("#rightReject").query();
        Button rightUndo = lookup("#rightUndo").query();

        checkButtonsDisabled(leftAccept, leftReject, leftUndo, rightAccept, rightReject, rightUndo, true);

        clickOn("#upToggle");
        checkLineNumbers(leftDoc, middleDoc, rightDoc, 105, 102, 103);
        checkButtonsDisabled(leftAccept, leftReject, leftUndo, rightAccept, rightReject, rightUndo, false);

        clickOn("#downToggle");
        clickOn("#rightAccept");
        clickOn("#leftAccept");

        // Wait for the auto switch conflict to happen
        WaitForAsyncUtils.waitForFxEvents();
        sleep(200);

        checkLineNumbers(leftDoc, middleDoc, rightDoc, 53, 54, 51);

        clickOn("#upToggle");
        clickOn("#leftUndo");
        clickOn("#downToggle");

        checkLineNumbers(leftDoc, middleDoc, rightDoc, 53, 53, 51);

        clickOn("#leftReject");
        clickOn("#rightReject");

        // Wait for the auto switch conflict to happen
        WaitForAsyncUtils.waitForFxEvents();
        sleep(200);

        // Rejecting should not have added a line to the middle doc
        checkLineNumbers(leftDoc, middleDoc, rightDoc, 79, 78, 77);
    }

    private void checkButtonsDisabled(Button leftAccept, Button leftReject, Button leftUndo, Button rightAccept, Button rightReject, Button rightUndo, boolean disabled) {
        interact(() -> assertEquals(leftAccept.isDisabled(), disabled));
        interact(() -> assertEquals(leftReject.isDisabled(), disabled));
        interact(() -> assertEquals(leftUndo.isDisabled(), disabled));

        interact(() -> assertEquals(rightAccept.isDisabled(), disabled));
        interact(() -> assertEquals(rightReject.isDisabled(), disabled));
        interact(() -> assertEquals(rightUndo.isDisabled(), disabled));
    }

    private void checkLineNumbers(CodeArea leftDoc, CodeArea middleDoc, CodeArea rightDoc, int leftLineNo, int middleLineNo, int rightLineNo) {
        interact(() -> assertEquals(leftLineNo, leftDoc.getCurrentParagraph()));
        interact(() -> assertEquals(middleLineNo, middleDoc.getCurrentParagraph()));
        interact(() -> assertEquals(rightLineNo, rightDoc.getCurrentParagraph()));
    }
}
