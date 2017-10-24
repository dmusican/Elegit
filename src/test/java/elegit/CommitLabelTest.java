package elegit;

import elegit.models.ClonedRepoHelper;
import elegit.models.SessionModel;
import elegit.treefx.Cell;
import elegit.treefx.CommitTreeModel;
import javafx.application.Application;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.*;

public class CommitLabelTest {

    // The URL of the testing repository for this unit test
    private static final String remoteURL = "https://bitbucket.org/makik/commitlabeltestrepo.git";
    // This is the ID of the initial commit in the testing repository
    private static final String INITIAL_COMMIT_ID = "5b5be4419d6efa935a6af1b9bfc5602f9d925e12";

    private CommitTreeModel commitTreeModel;

    private Path directoryPath;
    private Path repoPath;
    private ClonedRepoHelper helper;

    //@BeforeClass
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

    //@Before
    public void setUp() throws Exception {
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
        this.commitTreeModel = Main.sessionController.getCommitTreeModel();

        // Load this repo in Elegit, and initialize
        SessionModel.getSessionModel().openRepoFromHelper(helper);
        commitTreeModel.init();

        // Sleep to ensure completion of all worker threads
        Thread.sleep(1000);
    }

    //@After
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
        // Note that @BeforeClass, @Before, and @After are all commented out above
        // TODO: This test fails because it uses way too much JavaFX stuff, and makes lots of other code complicated. Need to fix the test.
        // Make sure both "master" and "origin/master" labels are on the inital commit
        /*testCellLabelContainsMaster(commitTreeModel, INITIAL_COMMIT_ID, true, true);

        // Get the tracked file in the testing repo, add a line and commit
        File file = Paths.get(this.repoPath.toString(), "file.txt").toFile();
        assertTrue(file.exists());

        try(PrintWriter fileTextWriter = new PrintWriter( file )){
            fileTextWriter.println("Add a line to the file");
        }

        this.helper.addFilePathTest(file.toPath());
        this.helper.commit("Modified file.txt in a unit test!");

        Main.sessionController.gitStatus();

        //commitTreeModel.update();

        // Sleep to ensure worker threads finish
        Thread.sleep(6000);

        // Get the information about the new commit
        CommitHelper newHead = this.helper.getCommit("master");
        assertNotNull(newHead);
        String newHeadID = newHead.getName();
        assertNotEquals(INITIAL_COMMIT_ID, newHeadID);

        // Check the labels are appropriate again
        this.testCellLabelContainsMaster(commitTreeModel, newHeadID, true, false);

        this.testCellLabelContainsMaster(commitTreeModel, INITIAL_COMMIT_ID, false, true);

        // Make another commit
        try(PrintWriter fileTextWriter = new PrintWriter( file )){
            fileTextWriter.println("Add another line to the file");
        }

        this.helper.addFilePathTest(file.toPath());
        this.helper.commit("Modified file.txt in a unit test again!");

        // Sleep to ensure worker threads finish
        Thread.sleep(6000);

        // Get the information about this new commit
        String oldHeadID = newHeadID;
        newHead = this.helper.getCommit("master");
        assertNotNull(newHead);
        newHeadID = newHead.getName();
        assertNotEquals(oldHeadID, newHeadID);

        // Check the labels on every commit again
        this.testCellLabelContainsMaster(commitTreeModel, newHeadID, true, false);

        this.testCellLabelContainsMaster(commitTreeModel, oldHeadID, false, false);

        this.testCellLabelContainsMaster(commitTreeModel, INITIAL_COMMIT_ID, false, true);
        */
    }

    /**
     * Checks the cell with ID cellID in commitTreeModel has the appropriate labels
     * @param commitTreeModel the tree to check
     * @param cellID the cell in the tree to check
     * @param matchLocal whether the label should contain "master"
     * @param matchRemote whether the label should contain "origin/master"
     */
    private void testCellLabelContainsMaster(CommitTreeModel commitTreeModel, String cellID, boolean matchLocal, boolean matchRemote) {
        fail("This test fails because it uses way too much JavaFX stuff, and makes lots of other code complicated. Need to fix the test.");
        // Get the cell from the tree
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
        for(String label : labels){
            if(label.equals("master")){
                localCount++;
            }else if(label.equals("origin/master")){
                remoteCount++;
            }
        }

        // If we want "master" to be present, it's going to show up twice: once in the basic display
        // and once in the extended display
        int expectedLocalCount = matchLocal ? 1 : 0;
        // Likewise for "origin/master", except if "master" is also present then "origin/master" will
        // only show up in extended
        int expectedRemoteCount = matchRemote ? 1 : 0;

        assertEquals(expectedLocalCount, localCount);
        assertEquals(expectedRemoteCount, remoteCount);
    }
}