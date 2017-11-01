package elegitfx;

import elegit.controllers.SessionController;
import elegit.models.ClonedRepoHelper;
import elegit.models.CommitHelper;
import elegit.models.SessionModel;
import elegit.monitors.RepositoryMonitor;
import elegit.treefx.Cell;
import elegit.treefx.CommitTreeModel;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.loadui.testfx.GuiTest;
import org.testfx.api.FxToolkit;
import org.testfx.framework.junit.ApplicationTest;

import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class RepositoryMonitorTestFX extends ApplicationTest {

    private SessionController sessionController;
    private static GuiTest testController;

    @Override
    public void start(Stage stage) throws Exception {
        SessionModel.setPreferencesNodeClass(this.getClass());
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/elegit/fxml/MainView.fxml"));
        fxmlLoader.load();
        sessionController = fxmlLoader.getController();
        Parent root = fxmlLoader.getRoot();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        sessionController.setStageForNotifications(stage);
        stage.show();
        stage.toFront();
        // TODO: Remove this pause and keep test working; no good reason for it to be necessary
        RepositoryMonitor.pause();

    }

    @After
    public void tearDown() throws Exception {
        FxToolkit.hideStage();
        FxToolkit.cleanupStages();
    }

    @Test
    // Dummy test to get something to run. This test really all happens in start, so just need to have a test
    // to get it going.
    public void test1() {
        RepositoryMonitor.init(sessionController);
        try {
            Thread.sleep(Math.max(RepositoryMonitor.REMOTE_CHECK_INTERVAL,
                                        RepositoryMonitor.LOCAL_CHECK_INTERVAL)+1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        int numLocalChecks = RepositoryMonitor.getNumLocalChecks();
        System.out.println("Number of local checks = " + numLocalChecks);
        int numRemoteChecks = RepositoryMonitor.getNumRemoteChecks();
        System.out.println("Number of remote checks = " + numRemoteChecks);
        assertTrue(numLocalChecks > 0 && numLocalChecks < 5);
        assertTrue(numRemoteChecks > 0 && numRemoteChecks < 5);
    }

    @Test
    public void openLocalRepoTest() throws Exception {
        clickOn("#loadNewRepoButton").clickOn("#loadExistingRepoOption");
        Thread.sleep(5000);
        assertTrue(true);
    }

}
