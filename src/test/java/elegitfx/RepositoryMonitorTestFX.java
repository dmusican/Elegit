package elegitfx;

import com.sun.org.apache.regexp.internal.RE;
import elegit.Main;
import elegit.controllers.BusyWindow;
import elegit.controllers.SessionController;
import elegit.models.ClonedRepoHelper;
import elegit.models.CommitHelper;
import elegit.models.RepoHelper;
import elegit.models.SessionModel;
import elegit.monitors.RepositoryMonitor;
import elegit.treefx.Cell;
import elegit.treefx.CommitTreeModel;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.controlsfx.control.CheckListView;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.loadui.testfx.GuiTest;
import org.testfx.api.FxToolkit;
import org.testfx.framework.junit.ApplicationTest;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.prefs.Preferences;

import static javafx.scene.input.KeyCode.ENTER;
import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class RepositoryMonitorTestFX extends ApplicationTest {

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
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setX(0);
        stage.setY(0);
        sessionController.setStageForNotifications(stage);
        stage.show();
        stage.toFront();
        // TODO: Remove this pause and keep test working; no good reason for it to be necessary
        RepositoryMonitor.pause();

    }

    @Test
    public void repoMonitorOnNoRepoOpenTest() throws Exception {
        RepositoryMonitor.unpause();
        sleep(Math.max(RepositoryMonitor.REMOTE_CHECK_INTERVAL,
                RepositoryMonitor.LOCAL_CHECK_INTERVAL)+1000);
        int numLocalChecks = RepositoryMonitor.getNumLocalChecks();
        System.out.println("Number of local checks = " + numLocalChecks);
        int numRemoteChecks = RepositoryMonitor.getNumRemoteChecks();
        System.out.println("Number of remote checks = " + numRemoteChecks);
        assertEquals(0, RepositoryMonitor.getExceptionCounter());
        assertEquals(0, SessionController.getGenericExceptionCount());
        //assertTrue(numLocalChecks > 0 && numLocalChecks < 5);
        //assertTrue(numRemoteChecks > 0 && numRemoteChecks < 5);
    }

    @Test
    public void openLocalRepoTest() throws Exception {
        initializeLogger();
        Path directoryPath = Files.createTempDirectory("unitTestRepos");
        directoryPath.toFile().deleteOnExit();
        Path repoPath = directoryPath.resolve("testrepo");
        // Clone from dummy repo:
        String remoteURL = "https://github.com/TheElegitTeam/TestRepository.git";

        // This repo doesn't check username/password for read-only
        UsernamePasswordCredentialsProvider credentials = new UsernamePasswordCredentialsProvider("", "");
        ClonedRepoHelper helper = new ClonedRepoHelper(repoPath, remoteURL, credentials);
        helper.obtainRepository(remoteURL);
        assertNotNull(helper);

        interact(() -> {
            ScrollPane sp = sessionController.getCommitTreeModel().getTreeGraph().getScrollPane();
            System.out.println("scroll pane info" + sp.getContent().getBoundsInLocal());
            // Test that scroll pane has no content yet
            System.out.println("scene = " + sp.getScene());
            assertTrue(sp.getScene() == null);
        });



        clickOn("#loadNewRepoButton")
                .clickOn("#loadExistingRepoOption")
                .clickOn("#repoInputDialog")
                .write(repoPath.toString())
                .clickOn("#repoInputDialogOK");


        interact(() -> {
            ScrollPane sp = sessionController.getCommitTreeModel().getTreeGraph().getScrollPane();
            System.out.println("scroll pane info" + sp.getContent().getBoundsInLocal());
            // Test that scroll pane now has content in it
            System.out.println("scene = " + sp.getScene());
            assertTrue(sp.getScene() != null);
        });



        assertEquals(0,sessionController.getNotificationPaneController().getNotificationNum());

        RepositoryMonitor.unpause();
        sleep(Math.max(RepositoryMonitor.REMOTE_CHECK_INTERVAL,
                RepositoryMonitor.LOCAL_CHECK_INTERVAL)+1000);
        int numLocalChecks = RepositoryMonitor.getNumLocalChecks();
        System.out.println("Number of local checks = " + numLocalChecks);
        int numRemoteChecks = RepositoryMonitor.getNumRemoteChecks();
        System.out.println("Number of remote checks = " + numRemoteChecks);
        assertTrue(numLocalChecks > 0 && numLocalChecks < 5);
        assertTrue(numRemoteChecks > 0 && numRemoteChecks < 5);

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
        ClonedRepoHelper helper = new ClonedRepoHelper(repoPath, remoteURL, credentials);
        helper.obtainRepository(remoteURL);
        assertNotNull(helper);

        clickOn("#loadNewRepoButton")
                .clickOn("#loadExistingRepoOption")
                .clickOn("#repoInputDialog")
                .write(repoPath.toString())
                .clickOn("#repoInputDialogOK");

        Path repoPath2 = directoryPath.resolve("otherrepo");

        remoteURL = "https://github.com/TheElegitTeam/testrepo.git";
        ClonedRepoHelper helper2 = new ClonedRepoHelper(repoPath2, remoteURL, credentials);
        helper2.obtainRepository(remoteURL);
        assertNotNull(helper2);

        clickOn("#loadNewRepoButton")
                .clickOn("#loadExistingRepoOption")
                .clickOn("#repoInputDialog")
                .write(repoPath2.toString())
                .clickOn("#repoInputDialogOK");

        Node dropdown = lookup("#repoDropdown").query();
        System.out.println(dropdown);
        clickOn(dropdown).clickOn("testrepo");
        clickOn(dropdown).clickOn("otherrepo");
        clickOn(dropdown).clickOn("testrepo");
        clickOn(dropdown).clickOn("otherrepo");
        clickOn(dropdown).clickOn("testrepo");
        clickOn(dropdown).clickOn("otherrepo");

        clickOn("#removeRecentReposButton");

        CheckListView<RepoHelper> repoCheckList = lookup("#repoCheckList").query();
        interact(() -> {
            repoCheckList.getItemBooleanProperty(0).set(true);
        });

        clickOn((Node)(lookup("#reposDeleteRemoveSelectedButton").query()));

        assertEquals(0, sessionController.getNotificationPaneController().getNotificationNum());

        clickOn("#removeRecentReposButton");

        interact(() -> {
            repoCheckList.getItemBooleanProperty(0).set(true);
        });

        clickOn((Node)(lookup("#reposDeleteRemoveSelectedButton").query()));

        assertEquals(0, sessionController.getNotificationPaneController().getNotificationNum());
    }

}
