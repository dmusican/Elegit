package elegit;

import elegit.treefx.TreeGraph;
import elegit.treefx.TreeGraphModel;
import elegit.treefx.TreeLayout;
import javafx.application.Application;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

import static org.junit.Assert.assertNotNull;

/**
 * Created by connellyj on 6/28/16.
 */

public class MoveAllCellsTest {
    private Path directoryPath;
    private String testFileLocation;
    Path logPath;

    // Used to indicate that if password files are missing, then tests should just pass
    private boolean looseTesting;

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

        // Sleep until the JavaFX environment is up and running
        Main.startLatch.await();
    }

    @Before
    public void setUp() throws Exception {
        initializeLogger();
        this.directoryPath = Files.createTempDirectory("unitTestRepos");
        System.out.println(directoryPath);
        directoryPath.toFile().deleteOnExit();
        testFileLocation = System.getProperty("user.home") + File.separator +
                "elegitTests" + File.separator;
        File strictTestingFile = new File(testFileLocation + "strictAuthenticationTesting.txt");
        looseTesting = !strictTestingFile.exists();
    }

    @After
    public void tearDown() throws Exception {
        removeAllFilesFromDirectory(this.logPath.toFile());
    }

    // Helper method to avoid annoying traces from logger
    void initializeLogger() {
        // Create a temp directory for the files to be placed in
        try {
            this.logPath = Files.createTempDirectory("elegitLogs");
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.logPath.toFile().deleteOnExit();
        System.setProperty("logFolder", logPath.toString());
    }

    // Helper tear-down method:
    void removeAllFilesFromDirectory(File dir) {
        for (File file: dir.listFiles()) {
            if (file.isDirectory()) removeAllFilesFromDirectory(file);
            file.delete();
        }
    }

    @Test
    public void testMoveAllCells() throws Exception {
        File authData = new File(testFileLocation + "httpUsernamePassword.txt");

        // If a developer does not have this file present, test should just pass.
        if (!authData.exists() && looseTesting)
            return;

        Scanner scanner = new Scanner(authData);
        String ignoreURL = scanner.next();
        String username = scanner.next();
        String password = scanner.next();
        UsernamePasswordCredentialsProvider credentials = new UsernamePasswordCredentialsProvider(username, password);

        String remoteURL = "https://github.com/TheElegitTeam/MoveAllCellsTestRepo.git";

        Path repoPath = directoryPath.resolve("movecells");
        ClonedRepoHelper repoHelper = new ClonedRepoHelper(repoPath, remoteURL, credentials);
        assertNotNull(repoHelper);

        Git git = new Git(repoHelper.getRepo());
        git.reset().setMode(ResetCommand.ResetType.HARD).setRef("f2bf5e733baa532fbc7f1bee2766f09fa7fcc772").call();

        Main.sessionController.theModel.currentRepoHelperProperty.set(repoHelper);
        Main.sessionController.handleMergeFromFetchButton();

        /*SessionModel sessionModel = SessionModel.getSessionModel();
        sessionModel.currentRepoHelperProperty.setValue(repoHelper);

        LocalCommitTreeModel localCommitTreeModel = new LocalCommitTreeModel(sessionModel, null);
        RemoteCommitTreeModel remoteCommitTreeModel = new RemoteCommitTreeModel(sessionModel, null);

        localCommitTreeModel.createNewTreeGraph();
        localCommitTreeModel.addAllCommitsToTree();

        remoteCommitTreeModel.createNewTreeGraph();
        remoteCommitTreeModel.addAllCommitsToTree();

        TreeLayout.getTreeLayoutTask(localCommitTreeModel.treeGraph);*/
    }
}
