package elegit;

import elegit.*;
import elegit.treefx.Cell;
import javafx.application.Application;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import org.controlsfx.control.spreadsheet.Grid;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.*;

public class CommitLabelTest {

    // The URL of the testing repository for this unit test
    private static final String remoteURL = "https://bitbucket.org/makik/commitlabeltestrepo.git";
    // This is the ID of the initial commit in the testing repository
    private static final String INITIAL_COMMIT_ID = "5b5be4419d6efa935a6af1b9bfc5602f9d925e12";

    private CommitTreeModel localCommitTreeModel;
    private CommitTreeModel remoteCommitTreeModel;

    private Path directoryPath;
    private Path repoPath;
    private ClonedRepoHelper helper;

    @BeforeClass
    public static void setUpJFX() throws Exception{
        // Launch the Elegit application in a thread so we get control back
        Thread t = new Thread("JavaFX Init Thread"){
            public void run(){
                Application.launch(Main.class);
            }
        };
        t.setDaemon(true);
        t.start();

        Main.startLatch.await();
        // Sleep until the JavaFX environment is up and running
        //Thread.sleep(1000);
    }

    @Before
    public void setUp() throws Exception {
        // Clone the testing repo into a temporary location
        this.directoryPath = Files.createTempDirectory("commitLabelTestRepos");
        this.directoryPath.toFile().deleteOnExit();
        this.repoPath = directoryPath.resolve("commitlabeltestrepo");

        // This repo doesn't check username/password for read-only
        UsernamePasswordCredentialsProvider credentials = new UsernamePasswordCredentialsProvider("", "");
        helper = new ClonedRepoHelper(repoPath, remoteURL, credentials);
        assertNotNull(helper);

        // Get the commit trees
        this.localCommitTreeModel = Main.sessionController.localCommitTreeModel;
        this.remoteCommitTreeModel = Main.sessionController.remoteCommitTreeModel;

        // Load this repo in Elegit, and initialize
        SessionModel.getSessionModel().openRepoFromHelper(helper);
        localCommitTreeModel.init();
        remoteCommitTreeModel.init();

        // Sleep to ensure completion of all worker threads
        Thread.sleep(5000);
    }

    @After
    public void tearDown() throws Exception {
        // Delete the cloned files.
        removeAllFilesFromDirectory(this.directoryPath.toFile());
    }

    // Helper tear-down method:
    void removeAllFilesFromDirectory(File dir) {
        for (File file: dir.listFiles()) {
            if (file.isDirectory()) removeAllFilesFromDirectory(file);
            file.delete();
        }
    }

    @Test
    public void testAddFileAndCommit() throws Exception {
        // Make sure both "master" and "origin/master" labels are on the inital commit
        testCellLabelContainsMaster(localCommitTreeModel, INITIAL_COMMIT_ID, true, true);
        testCellLabelContainsMaster(remoteCommitTreeModel, INITIAL_COMMIT_ID, true, true);

        // Get the tracked file in the testing repo, add a line and commit
        File file = Paths.get(this.repoPath.toString(), "file.txt").toFile();
        assertTrue(file.exists());

        try(PrintWriter fileTextWriter = new PrintWriter( file )){
            fileTextWriter.println("Add a line to the file");
        }

        this.helper.addFilePath(file.toPath());
        this.helper.commit("Modified file.txt in a unit test!");

        Main.sessionController.gitStatus();

        //localCommitTreeModel.update();
        //remoteCommitTreeModel.update();

        // Sleep to ensure worker threads finish
        Thread.sleep(5000);

        // Get the information about the new commit
        CommitHelper newHead = this.helper.getCommit("master");
        assertNotNull(newHead);
        String newHeadID = newHead.getId();
        assertNotEquals(INITIAL_COMMIT_ID, newHeadID);

        // Check the labels are appropriate again
        this.testCellLabelContainsMaster(localCommitTreeModel, newHeadID, true, false);
        this.testCellLabelContainsMaster(remoteCommitTreeModel, newHeadID, true, false);

        this.testCellLabelContainsMaster(localCommitTreeModel, INITIAL_COMMIT_ID, false, true);
        this.testCellLabelContainsMaster(remoteCommitTreeModel, INITIAL_COMMIT_ID, false, true);

        // Make another commit
        try(PrintWriter fileTextWriter = new PrintWriter( file )){
            fileTextWriter.println("Add another line to the file");
        }

        this.helper.addFilePath(file.toPath());
        this.helper.commit("Modified file.txt in a unit test again!");

        Main.sessionController.gitStatus();
        localCommitTreeModel.update();
        remoteCommitTreeModel.update();

        // Sleep to ensure worker threads finish
        Thread.sleep(5000);

        // Get the information about this new commit
        String oldHeadID = newHeadID;
        newHead = this.helper.getCommit("master");
        assertNotNull(newHead);
        newHeadID = newHead.getId();
        assertNotEquals(oldHeadID, newHeadID);

        // Check the labels on every commit again
        this.testCellLabelContainsMaster(localCommitTreeModel, newHeadID, true, false);
        this.testCellLabelContainsMaster(remoteCommitTreeModel, newHeadID, true, false);

        this.testCellLabelContainsMaster(localCommitTreeModel, oldHeadID, false, false);
        this.testCellLabelContainsMaster(remoteCommitTreeModel, oldHeadID, false, false);

        this.testCellLabelContainsMaster(localCommitTreeModel, INITIAL_COMMIT_ID, false, true);
        this.testCellLabelContainsMaster(remoteCommitTreeModel, INITIAL_COMMIT_ID, false, true);
    }

    /**
     * Checks the cell with ID cellID in commitTreeModel has the appropriate labels
     * @param commitTreeModel the tree to check
     * @param cellID the cell in the tree to check
     * @param matchLocal whether the label should contain "master"
     * @param matchRemote whether the label should contain "origin/master"
     */
    private void testCellLabelContainsMaster(CommitTreeModel commitTreeModel, String cellID, boolean matchLocal, boolean matchRemote) {
        // Get the cell from the tree
        assertTrue(commitTreeModel.containsID(cellID));
        Cell cell = commitTreeModel.treeGraph.treeGraphModel.cellMap.get(cellID);
        assertNotNull(cell);

        // Pull the labels from the cell
        Pane cellLabel = (Pane) cell.getLabel();
        assertNotNull(cellLabel);

        List<Node> labelNodes = cellLabel.getChildrenUnmodifiable();
        List<String> labels = new LinkedList<>();
        for (Node n : labelNodes) {
            if (n instanceof Label) {
                Label l = (Label) n;
                Collections.addAll(labels, l.getText().split("\n"));
            } else if (n instanceof GridPane) {
                for (Node m : ((GridPane) n).getChildren()) {
                    Label l = (Label) m;
                    Collections.addAll(labels, l.getText());
                }
            }
        }

        // Count the occurrences of "master" and "origin/master"
        int localCount = 0;
        int remoteCount = 0;
        for(String label : labels){
            if(label.equals("master")){
                localCount++;
            }else if(label.equals("origin/master")){
                remoteCount++;
            }
        }

        // If we want "master" to be present, it's going to show up twice: once in the basic display
        // and once in the extended display
        int expectedLocalCount = matchLocal ? (matchRemote ? 1 : 2) : 0;
        // Likewise for "origin/master", except if "master" is also present then "origin/master" will
        // only show up in extended
        int expectedRemoteCount = matchRemote ? 2 : 0;

        assertEquals(expectedLocalCount, localCount);
        assertEquals(expectedRemoteCount, remoteCount);
    }
}