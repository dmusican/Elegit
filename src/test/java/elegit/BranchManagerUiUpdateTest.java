package elegit;

import javafx.application.Application;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author shangd
 */
public class BranchManagerUiUpdateTest {

    private static final String REMOTE_URL = "https://bitbucket.org/makik/commitlabeltestrepo.git";
    private static final String BRANCH_NAME = "random";

    private CommitTreeModel localCommitTreeModel;
    private CommitTreeModel remoteCommitTreeModel;

    private Path directoryPath;
    private Path repoPath;
    private ClonedRepoHelper helper;

    @BeforeClass
    public static void setUpJFX() throws Exception{
        Thread t = new Thread("JavaFX Init Thread") {
            @Override
            public void run(){
                Application.launch(Main.class);
            }
        };
        t.setDaemon(true);
        t.start();

        // Sleep until the JavaFX environment is up and running
        Thread.sleep(5000);
    }

    @Before
    public void setUp() throws Exception {
        cloneTestRepo();
        openClonedRepo();
    }

    @After
    public void tearDown() throws Exception {
        removeAllFilesFromDirectory(directoryPath.toFile());
    }

    void cloneTestRepo() {
        try {
            directoryPath = Files.createTempDirectory("branchManagerUiUpdateTest");
        } catch (IOException e) {
            e.printStackTrace();
        }
        directoryPath.toFile().deleteOnExit();

        repoPath = directoryPath.resolve("testRepo");
        try {
            helper = new ClonedRepoHelper(repoPath, REMOTE_URL);
        } catch (Exception e) {
            e.printStackTrace();
        }

        assertNotNull(helper);
    }

    void openClonedRepo() {
        localCommitTreeModel = Main.sessionController.localCommitTreeModel;
        remoteCommitTreeModel = Main.sessionController.remoteCommitTreeModel;

        try {
            /*new Git(helper.getRepo()).branchCreate()
                    .setName(BRANCH_NAME)
                    .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK)
                    .setStartPoint("refs/remotes/origin/" + BRANCH_NAME)
                    .call();*/

            SessionModel.getSessionModel().openRepoFromHelper(helper);
            helper.fetch();
            for (RemoteBranchHelper r : helper.getListOfRemoteBranches()) {
                System.out.println(r.getRefPathString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        localCommitTreeModel.init();
        remoteCommitTreeModel.init();

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    void removeAllFilesFromDirectory(File dir) {
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) removeAllFilesFromDirectory(file);
            file.delete();
        }
    }

    @Test
    public void testMergeSelectedBranchWithCurrent() throws Exception {
        File file = Paths.get(this.repoPath.toString(), "test.txt").toFile();
        assertTrue(file.exists());

        try(PrintWriter fileTextWriter = new PrintWriter( file )){
            fileTextWriter.println("Add a line to the file");
        }

        this.helper.addFilePath(file.toPath());
        this.helper.commit("Modified test.txt in a unit test!");

        localCommitTreeModel.update();
        remoteCommitTreeModel.update();
    }
}
