package elegitfx;

import elegit.Main;
import elegit.controllers.BusyWindow;
import elegit.controllers.MenuController;
import elegit.controllers.SessionController;
import elegit.models.ClonedRepoHelper;
import elegit.models.SessionModel;
import elegit.monitors.RepositoryMonitor;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.testfx.framework.junit.ApplicationTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.prefs.Preferences;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ChangeRemoteMonitoringStatus extends ApplicationTest {

    static {
        // -----------------------Logging Initialization Start---------------------------
        Path logPath = Paths.get("logs");
        String s = logPath.toAbsolutePath().toString();
        System.setProperty("logFolder", s);
    }

    private static final Logger logger = LogManager.getLogger();

    private SessionController sessionController;
    private MenuController menuController;

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

        menuController = sessionController.getMenuController();

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



    @Test
    public void changeRemoteMonitoringStatusTest() throws Exception {
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


        clickOn("#loadNewRepoButton")
                .clickOn("#loadExistingRepoOption")
                .clickOn("#repoInputDialog")
                .write(repoPath.toString())
                .clickOn("#repoInputDialogOK");

        SessionController.gitStatusCompletedOnce.await();

        interact(() -> {
            assertEquals(false, sessionController.getRemoteConnectedStatus());
        });

        clickOn("#remoteConnected");

        interact(() -> {
            assertEquals(true, sessionController.getRemoteConnectedStatus());
        });
//
//        interact(() -> {
//            assertEquals(true, menuController.getRemoteToggleStatus());
//            assertEquals(true, RepositoryMonitor.getRemoteStatusChecking());
//        });
//
//        clickOn("Preferences")
//                .clickOn("#remoteStatusCheckToggle");
//
//        interact(() -> {
//            assertEquals(false, menuController.getRemoteToggleStatus());
//            assertEquals(false, RepositoryMonitor.getRemoteStatusChecking());
//        });
//
//        clickOn("Preferences")
//                .clickOn("#remoteStatusCheckToggle");
//
//        interact(() -> {
//            assertEquals(true, menuController.getRemoteToggleStatus());
//            assertEquals(true, RepositoryMonitor.getRemoteStatusChecking());
//        });


    }

    // Helper method to avoid annoying traces from logger
    private void initializeLogger() {
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


}
