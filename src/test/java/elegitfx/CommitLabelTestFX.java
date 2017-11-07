package elegitfx;

import elegit.controllers.SessionController;
import elegit.models.ClonedRepoHelper;
import elegit.models.CommitHelper;
import elegit.models.SessionModel;
import elegit.monitors.RepositoryMonitor;
import elegit.treefx.Cell;
import elegit.treefx.CommitTreeModel;
import io.reactivex.Single;
import javafx.application.Platform;
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
import org.junit.Before;
import org.junit.Test;
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
import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.*;

public class CommitLabelTestFX extends ApplicationTest {

    // The URL of the testing repository for this unit test
    private static final String remoteURL = "https://bitbucket.org/makik/commitlabeltestrepo.git";
    // This is the ID of the initial commit in the testing repository
    private static final String INITIAL_COMMIT_ID = "5b5be4419d6efa935a6af1b9bfc5602f9d925e12";

    private CommitTreeModel commitTreeModel;

    private Path directoryPath;
    private Path repoPath;
    private ClonedRepoHelper helper;


    private SessionController sessionController;

    private Throwable testFailures;

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/elegit/fxml/MainView.fxml"));
        fxmlLoader.load();
        RepositoryMonitor.pause();
        sessionController = fxmlLoader.getController();
        BorderPane root = fxmlLoader.getRoot();
        Rectangle2D primScreenBounds = Screen.getPrimary().getVisualBounds();
        int screenWidth = (int) primScreenBounds.getWidth();
        int screenHeight = (int) primScreenBounds.getHeight();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        System.out.println("Showing stage");
        stage.show();
        /* Do not forget to put the GUI in front of windows. Otherwise, the robots may interact with another
        window, the one in front of all the windows... */
        //stage.toFront();

        // Clone the testing repo into a temporary location
        this.directoryPath = Files.createTempDirectory("commitLabelTestRepos");
        this.directoryPath.toFile().deleteOnExit();
        this.repoPath = directoryPath.resolve("commitlabeltestrepo");

        // This repo doesn't check username/password for read-only
        UsernamePasswordCredentialsProvider credentials = new UsernamePasswordCredentialsProvider("", "");
        helper = new ClonedRepoHelper(repoPath, remoteURL, credentials);
        assertNotNull(helper);
        helper.obtainRepository(remoteURL);

        // Get the commit tree
        this.commitTreeModel = sessionController.getCommitTreeModel();

        // Load this repo in Elegit, and initialize
        SessionModel.getSessionModel().openRepoFromHelper(helper);

        commitTreeModel.init()
                .flatMap((unused) -> testAddFileAndCommit())

                .doOnSuccess((unused) -> {
                    // Delete the cloned files.
                    removeAllFilesFromDirectory(this.directoryPath.toFile());
                }).subscribe((unused) -> {}, t -> testFailures = t);

    }

    // Helper tear-down method:
    void removeAllFilesFromDirectory(File dir) {
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) removeAllFilesFromDirectory(file);
            file.delete();
        }
    }

    @Test
    // Dummy test to get something to run. This test really all happens in start, so just need to have a test
    // to get it going.
    public void test1() {
        assertNull(testFailures);
        assertEquals(1, 1);
    }


    private String headIDForTesting;

    public Single<Boolean> testAddFileAndCommit() throws Exception {
        // Make sure both "master" and "origin/master" labels are on the inital commit
        testCellLabelContainsMaster(commitTreeModel, INITIAL_COMMIT_ID, true, true);

        // Get the tracked file in the testing repo, add a line and commit
        File file = Paths.get(this.repoPath.toString(), "file.txt").toFile();
        assertTrue(file.exists());

        try(PrintWriter fileTextWriter = new PrintWriter( file )){
            fileTextWriter.println("Add a line to the file");
        }

        this.helper.addFilePathTest(file.toPath());
        this.helper.commit("Modified file.txt in a unit test!");

        return sessionController.gitStatusWorkload()
                .doOnSuccess((unused) -> {

                    // Get the information about the new commit
                    CommitHelper newHead = this.helper.getCommit("master");
                    assertNotNull(newHead);
                    String newHeadID = newHead.getName();
                    assertNotEquals(INITIAL_COMMIT_ID, newHeadID);

                    // Check the labels are appropriate again
                    this.testCellLabelContainsMaster(commitTreeModel, newHeadID, true, false);

                    this.testCellLabelContainsMaster(commitTreeModel, INITIAL_COMMIT_ID, false, true);

                    // Make another commit
                    try (PrintWriter fileTextWriter = new PrintWriter(file)) {
                        fileTextWriter.println("Add another line to the file");
                    }

                    this.helper.addFilePathTest(file.toPath());
                    this.helper.commit("Modified file.txt in a unit test again!");
                    headIDForTesting = newHeadID;
                })

                .flatMap((unused) -> sessionController.gitStatusWorkload())

                .doOnSuccess((unused) -> {


                    // Get the information about this new commit
                    String oldHeadID = headIDForTesting;
                    CommitHelper newHead = this.helper.getCommit("master");
                    assertNotNull(newHead);
                    String newHeadID = newHead.getName();
                    assertNotEquals(oldHeadID, newHeadID);


                    // Check the labels on every commit again
                    this.testCellLabelContainsMaster(commitTreeModel, newHeadID, true, false);

                    this.testCellLabelContainsMaster(commitTreeModel, oldHeadID, false, false);

                    this.testCellLabelContainsMaster(commitTreeModel, INITIAL_COMMIT_ID, false, true);

                });

    }

    /**
     * Checks the cell with ID cellID in commitTreeModel has the appropriate labels
     *
     * @param commitTreeModel the tree to check
     * @param cellID          the cell in the tree to check
     * @param matchLocal      whether the label should contain "master"
     * @param matchRemote     whether the label should contain "origin/master"
     */
    private void testCellLabelContainsMaster(CommitTreeModel commitTreeModel, String cellID, boolean matchLocal, boolean matchRemote) {
        // Get the cell from the tree
        System.out.println("========");
        System.out.println(cellID);
        System.out.println(commitTreeModel.containsID(cellID));
        System.out.println(commitTreeModel.getTreeGraph().treeGraphModel.getCell(cellID));
        System.out.println("========");

        assertTrue(commitTreeModel.containsID(cellID));
        Cell cell = commitTreeModel.getTreeGraph().treeGraphModel.getCell(cellID);
        assertNotNull(cell);

        // Pull the labels from the cell
        GridPane cellLabel = (GridPane) cell.getLabel();
        assertNotNull(cellLabel);

        // Label is a gridpane with a bunch of hboxes that contain labels and icons
        List<Node> labelNodes = cellLabel.getChildrenUnmodifiable();
        List<String> labels = new LinkedList<>();
        for (Node m : labelNodes) {
            if (m instanceof HBox) {
                for (Node n : ((HBox) m).getChildren()) {
                    if (n instanceof HBox)
                        for (Node k : ((HBox) n).getChildren()) {
                            if (!(k instanceof Label)) continue;
                            Label l = (Label) k;
                            Collections.addAll(labels, l.getText());
                        }
                }
            }
        }

        // Count the occurrences of "master" and "origin/master"
        int localCount = 0;
        int remoteCount = 0;
        for (String label : labels) {
            if (label.equals("master")) {
                localCount++;
            } else if (label.equals("origin/master")) {
                remoteCount++;
            }
        }

        // If we want "master" to be present, it's going to show up twice: once in the basic display
        // and once in the extended display
        int expectedLocalCount = matchLocal ? 1 : 0;
        // Likewise for "origin/master", except if "master" is also present then "origin/master" will
        // only show up in extended
        int expectedRemoteCount = matchRemote ? 1 : 0;

        Assert.assertEquals(expectedLocalCount, localCount);
        Assert.assertEquals(expectedRemoteCount, remoteCount);
    }
}
