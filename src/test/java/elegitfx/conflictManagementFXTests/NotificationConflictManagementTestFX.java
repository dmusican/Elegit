package elegitfx.conflictManagementFXTests;

import elegit.Main;
import elegit.controllers.SessionController;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.*;
import org.junit.rules.TestName;
import org.testfx.framework.junit.ApplicationTest;
import org.testfx.framework.junit.TestFXRule;
import sharedrules.TestUtilities;
import sharedrules.TestingLogPathRule;
import sharedrules.TestingRemoteAndLocalReposRule;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static junit.framework.TestCase.assertEquals;

/**
 * Created by grenche on 7/3/18.
 * Tests the notification system of the conflict management tool by checking how many notifications are showing. It
 * doesn't check the notification method, but that would be a good extension to this test.
 */
public class NotificationConflictManagementTestFX extends ApplicationTest {
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
    public void testNotifications() throws Exception {
        Path local = conflictManagementUtilities.createMultipleConflicts(testingRemoteAndLocalRepos, false);
        interact(() -> sessionController.handleLoadExistingRepoOption(local));
        interact(() -> sessionController.handleOpenConflictManagementTool(local.toString(), "test.txt"));

        clickOn("#leftAccept");
        clickOn("#leftReject");
        // They should get a notification if they try to reject a conflict that has already been handled
        interact(() -> assertEquals(1, sessionController.getConflictManagementToolController().getNotificationPaneController().getNotificationNum()));

        clickOn("#leftUndo");
        clickOn("#leftReject");
        // They shouldn't get a notification if they've undo the modification before hand
        interact(() -> assertEquals(1, sessionController.getConflictManagementToolController().getNotificationPaneController().getNotificationNum()));

        // Need to wait for the bubble to go away.
        sleep(4000);

        clickOn("#applyChanges");
        // They should get a notification if they click apply before handling all conflicts
        interact(() -> assertEquals(2, sessionController.getConflictManagementToolController().getNotificationPaneController().getNotificationNum()));

        clickOn("#rightUndo");
        // They should get a notification if they click undo and they didn't modify anything
        interact(() -> assertEquals(3, sessionController.getConflictManagementToolController().getNotificationPaneController().getNotificationNum()));

        clickOn("#leftAccept");
        // They should get a notification if they try to accept a conflict that has already been handled
        interact(() -> assertEquals(4, sessionController.getConflictManagementToolController().getNotificationPaneController().getNotificationNum()));

        clickOn("#downToggle");
        clickOn("#rightAccept");
        clickOn("#downToggle");
        clickOn("#rightReject");
        clickOn("#downToggle");
        clickOn("#leftAccept");
        // They should get a notification that all conflicts have been handled
        interact(() -> assertEquals(5, sessionController.getConflictManagementToolController().getNotificationPaneController().getNotificationNum()));

        clickOn("#leftUndo");
        clickOn("#leftAccept");
        // They should get a notification that all conflicts have been handled
        interact(() -> assertEquals(6, sessionController.getConflictManagementToolController().getNotificationPaneController().getNotificationNum()));

        // This should undo all of the changes
        clickOn("#leftUndo");
        clickOn("#upToggle");
        clickOn("#rightUndo");
        clickOn("#upToggle");
        clickOn("#rightUndo");
        clickOn("#upToggle");
        clickOn("#leftUndo");
        interact(() -> assertEquals(6, sessionController.getConflictManagementToolController().getNotificationPaneController().getNotificationNum()));

        clickOn("#leftAccept");
        clickOn("#rightAccept");
        sleep(200);
        clickOn("#leftReject");
        clickOn("#rightReject");
        sleep(200);
        // They should not get a notification to apply even though 4 conflict have been handled because not all in the middle have been handled.
        interact(() -> assertEquals(6, sessionController.getConflictManagementToolController().getNotificationPaneController().getNotificationNum()));
        clickOn("#leftAccept");
        clickOn("#rightReject");
        sleep(200);
        clickOn("#leftReject");
        // They should get a notification to apply because all conflict have been handled in the middle.
        interact(() -> assertEquals(7, sessionController.getConflictManagementToolController().getNotificationPaneController().getNotificationNum()));
        clickOn("#rightAccept");
        sleep(200);
        // They shouldn't get a notification even though they handled another conflict.
        interact(() -> assertEquals(7, sessionController.getConflictManagementToolController().getNotificationPaneController().getNotificationNum()));
    }
}
