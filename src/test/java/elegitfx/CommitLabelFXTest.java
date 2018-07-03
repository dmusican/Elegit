package elegitfx;

import elegit.Main;
import elegit.controllers.SessionController;
import elegit.models.ClonedRepoHelper;
import elegit.models.CommitHelper;
import elegit.models.SessionModel;
import elegit.treefx.Cell;
import elegit.treefx.CommitTreeModel;
import io.reactivex.Single;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.junit.After;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.testfx.framework.junit.ApplicationTest;
import org.testfx.framework.junit.TestFXRule;
import org.testfx.util.WaitForAsyncUtils;
import sharedrules.TestUtilities;

import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class CommitLabelFXTest extends ApplicationTest {

    static {
        // -----------------------Logging Initialization Start---------------------------
        Path logPath = Paths.get("logs");
        String s = logPath.toAbsolutePath().toString();
        System.setProperty("logFolder", s);
    }

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

    @Rule
    public TestFXRule testFXRule = new TestFXRule();

    public static final CountDownLatch startComplete = new CountDownLatch(1);

    @After
    public void tearDown() {
        TestUtilities.commonShutDown();
    }



    @Override
    public void start(Stage stage) throws Exception {
        sessionController = TestUtilities.commonTestFxStart(stage);

        // Clone the testing repo into a temporary location
        this.directoryPath = Files.createTempDirectory("commitLabelTestRepos");
        this.directoryPath.toFile().deleteOnExit();
        this.repoPath = directoryPath.resolve("commitlabeltestrepo");

        // This repo doesn't check username/password for read-only
        UsernamePasswordCredentialsProvider credentials = new UsernamePasswordCredentialsProvider("", "");
        helper = new ClonedRepoHelper(repoPath, credentials);
        assertNotNull(helper);
        helper.obtainRepository(remoteURL);

        // Get the commit tree
        this.commitTreeModel = sessionController.getCommitTreeModel();

        // Load this repo in Elegit, and initialize
        SessionModel.getSessionModel().openRepoFromHelper(helper);

        startComplete.countDown();
    }

    // Helper tear-down method:
    private void removeAllFilesFromDirectory(File dir) {
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) removeAllFilesFromDirectory(file);
            file.delete();
        }
    }

    @Test
    // Dummy test to get something to run. This test really all happens in start, so just need to have a test
    // to get it going.
    public void test1() throws InterruptedException{

        // Make sure that start is complete before jumping in here
        startComplete.await();

        interact( () -> {
            commitTreeModel.initializeModelForNewRepoWhenSubscribed()
                    .flatMap((unused) -> testAddFileAndCommit())

                    .doOnSuccess((unused) -> {
                        // Delete the cloned files.
                        removeAllFilesFromDirectory(this.directoryPath.toFile());
                    })

                .subscribe((unused) -> {}, t -> testFailures = t);
            assertNull(testFailures);
            assertEquals(1, 1);

        });

        // Helps avoid random TestFX shutdown errors
        WaitForAsyncUtils.waitForFxEvents();
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

        return sessionController.doGitStatusWhenSubscribed()
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

                .flatMap((unused) -> sessionController.doGitStatusWhenSubscribed())

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
