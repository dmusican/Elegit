package elegitfx;

import elegit.Main;
import elegit.controllers.BusyWindow;
import elegit.controllers.SessionController;
import elegit.models.ClonedRepoHelper;
import elegit.models.RepoHelper;
import elegit.models.SessionModel;
import elegit.monitors.RepositoryMonitor;
import elegit.treefx.CommitTreeModel;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.controlsfx.control.CheckListView;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.loadui.testfx.GuiTest;
import org.testfx.framework.junit.ApplicationTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import java.util.prefs.Preferences;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class RepositoryMonitor1FXTest extends ApplicationTest {

    static {
        // -----------------------Logging Initialization Start---------------------------
        Path logPath = Paths.get("logs");
        String s = logPath.toAbsolutePath().toString();
        System.setProperty("logFolder", s);
    }

    private static final Logger logger = LogManager.getLogger();

    private SessionController sessionController;
    private static GuiTest testController;

    @Override
    public void start(Stage stage) throws Exception {
        Main.testMode = true;
        BusyWindow.setParentWindow(stage);

        Preferences prefs = Preferences.userNodeForPackage(this.getClass());
        prefs.removeNode();

        SessionModel.setPreferencesNodeClass(this.getClass());
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/elegit/fxml/MainView.fxml"));
        fxmlLoader.load();
        sessionController = fxmlLoader.getController();
        Parent root = fxmlLoader.getRoot();
        Rectangle2D primScreenBounds = Screen.getPrimary().getVisualBounds();
        int screenWidth = (int) primScreenBounds.getWidth();
        int screenHeight = (int) primScreenBounds.getHeight();
        Scene scene = new Scene(root, screenWidth*4/5, screenHeight*4/5);
        stage.setScene(scene);
        sessionController.setStageForNotifications(stage);
        stage.show();
        stage.toFront();
        // TODO: Remove this pause and keep test working; no good reason for it to be necessary
        RepositoryMonitor.pause();

    }

    @Before
    public void setup() {
        logger.info("Unit test started");
    }

    @After
    public void tearDown() {
        System.out.println("Tearing down");
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
    public void openAndCloseReposTest() throws Exception {
        initializeLogger();
        Path directoryPath = Files.createTempDirectory("unitTestRepos");
        directoryPath.toFile().deleteOnExit();
        Path repoPath = directoryPath.resolve("testrepo");
        // Clone from dummy repo:
        String remoteURL = "https://github.com/TheElegitTeam/TestRepository.git";

        // This repo doesn't check username/password for read-only
        UsernamePasswordCredentialsProvider credentials = new UsernamePasswordCredentialsProvider("", "");
        ClonedRepoHelper helper = new ClonedRepoHelper(repoPath, credentials);
        helper.obtainRepository(remoteURL);
        assertNotNull(helper);

        CommitTreeModel.setAddCommitDelay(5);

        SessionController.gitStatusCompletedOnce = new CountDownLatch(1);

        clickOn("#loadNewRepoButton")
                .clickOn("#loadExistingRepoOption")
                .clickOn("#repoInputDialog")
                .write(repoPath.toString())
                .clickOn("#repoInputDialogOK");

        SessionController.gitStatusCompletedOnce.await();

        Path repoPath2 = directoryPath.resolve("otherrepo");

        remoteURL = "https://github.com/TheElegitTeam/testrepo.git";
        ClonedRepoHelper helper2 = new ClonedRepoHelper(repoPath2, credentials);
        helper2.obtainRepository(remoteURL);
        assertNotNull(helper2);

        clickOn("#loadNewRepoButton")
                .clickOn("#loadExistingRepoOption")
                .clickOn("#repoInputDialog")
                .write(repoPath2.toString())
                .clickOn("#repoInputDialogOK");

        final ComboBox<RepoHelper> dropdown = lookup("#repoDropdown").query();


        for (int i=0; i < 3; i++) {
            GuiTest.waitUntil(dropdown, (ComboBox<RepoHelper> d) -> d.getValue().toString().equals("otherrepo"));
            clickOn(dropdown).clickOn("testrepo");
//            sleep(5000);
            GuiTest.waitUntil(BusyWindow.window.isShowing(),org.hamcrest.Matchers.is(false));
//            GuiTest.waitUntil(dropdown, (ComboBox<RepoHelper> d) -> d.getValue().toString().equals("testrepo"));
//            GuiTest.waitUntil(dropdown, (ComboBox<RepoHelper> d) -> d.getItems().stream().anyMatch( (RepoHelper repo) -> repo.toString().equals("otherrepo")));
            interact(() -> System.out.println(dropdown.getItems()));
            clickOn(dropdown).clickOn("otherrepo");
            sleep(5000);
        }

        GuiTest.waitUntil(dropdown, (ComboBox<RepoHelper> d) -> d.getValue().toString().equals("otherrepo"));
        clickOn("#removeRecentReposButton");

        CheckListView<RepoHelper> repoCheckList = lookup("#repoCheckList").query();

        // Selects testrepo
        interact(() -> {
            repoCheckList.getItemBooleanProperty(0).set(true);
        });

        // Clicks button to remove testrepo
        clickOn((Node)(lookup("#reposDeleteRemoveSelectedButton").query()));

        assertEquals(0, sessionController.getNotificationPaneController().getNotificationNum());

        assertNotEquals(null,lookup("otherrepo").query());

        GuiTest.waitUntil(dropdown, (ComboBox<RepoHelper> d) -> d.getValue().toString().equals("otherrepo"));


        clickOn("#removeRecentReposButton");

        // Selects otherrepo
        interact(() -> {
            repoCheckList.getItemBooleanProperty(0).set(true);
        });

        clickOn((Node)(lookup("#reposDeleteRemoveSelectedButton").query()));

        assertEquals(0, sessionController.getNotificationPaneController().getNotificationNum());
    }

}
