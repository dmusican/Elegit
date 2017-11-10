package elegitfx;

import elegit.Main;
import elegit.controllers.BusyWindow;
import elegit.controllers.SessionController;
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
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.TransportCommand;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.loadui.testfx.GuiTest;
import org.slf4j.LoggerFactory;
import org.testfx.framework.junit.ApplicationTest;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.prefs.Preferences;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.fail;

public class RepoCreationTests extends ApplicationTest {

    static {
        // -----------------------Logging Initialization Start---------------------------
        Path logPath = Paths.get("logs");
        String s = logPath.toAbsolutePath().toString();
        System.setProperty("logFolder", s);
    }

    private static final Logger logger = LogManager.getLogger("consolelogger");

    private SessionController sessionController;
    private static GuiTest testController;

    private Path directoryPath;

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
        Scene scene = new Scene(root, screenWidth * 4 / 5, screenHeight * 4 / 5);
        stage.setScene(scene);
        sessionController.setStageForNotifications(stage);
        //stage.show();
        stage.toFront();
        // TODO: Remove this pause and keep test working; no good reason for it to be necessary
        RepositoryMonitor.pause();

    }

    @Before
    public void setup() throws Exception {
        logger.info("Unit test started");
        directoryPath = Files.createTempDirectory("unitTestRepos");
        //directoryPath.toFile().deleteOnExit();
    }

    @After
    public void tearDown() {
        logger.info("Tearing down");
        assertEquals(0, Main.getAssertionCount());
    }

    @Test
    public void highlightCommitTest() throws Exception {
        logger.info("Temp directory: " + directoryPath);
        Path remote = directoryPath.resolve("remote");
        Path local = directoryPath.resolve("local");
        Git.init().setDirectory(remote.toFile()).setBare(true).call();
        Git.cloneRepository().setDirectory(local.toFile()).setURI("file://"+remote).call();


        logger.info(remote);
        logger.info(local);
        fail();
    }
}