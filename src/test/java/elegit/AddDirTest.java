package elegit;

import elegit.models.ClonedRepoHelper;
import elegit.models.ExistingRepoHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.api.Git;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import sharedrules.TestUtilities;
import sharedrules.TestingLogPathRule;
import sharedrules.TestingRemoteAndLocalReposRule;

import java.io.File;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by Eric Walker, modified by Dave Musicant
 */
public class AddDirTest {

    @ClassRule
    public static final TestingLogPathRule testingLogPath = new TestingLogPathRule();

    @Rule
    public final TestingRemoteAndLocalReposRule testingRemoteAndLocalRepos =
            new TestingRemoteAndLocalReposRule(false);

    private static final Logger console = LogManager.getLogger("briefconsolelogger");

    @Before
    public void setUp() throws Exception {
        console.info("Unit test started");
        TestUtilities.setupTestEnvironment();
    }

    @After
    public void tearDown() {
        TestUtilities.cleanupTestEnvironment();
    }


    @Test
    public void testAdd() throws Exception {
        Path local = testingRemoteAndLocalRepos.getLocalFull();
        Path remote = testingRemoteAndLocalRepos.getRemoteFull();

        // Set up remote repo
        Files.createDirectory(remote.resolve("foo"));
        Path remoteBarFilePath = remote.resolve("foo"+File.separator+"bar.txt");
        String timestamp1 = "initial setup " + (new Date()).toString() + "\n";
        Files.write(remoteBarFilePath, timestamp1.getBytes());
        //ArrayList<Path> paths = new ArrayList<>();
        //paths.add(remoteBarFilePath);
        ExistingRepoHelper helperServer = new ExistingRepoHelper(remote, null);
        helperServer.addFilePathTest(remoteBarFilePath);
        helperServer.commit("Initial unit test commit");


        // Clone remote to local repo
        String remoteURL = "file://"+testingRemoteAndLocalRepos.getRemoteFull();
        ClonedRepoHelper helperAdd = new ClonedRepoHelper(local, null);
        assertNotNull(helperAdd);
        helperAdd.obtainRepository(remoteURL);

        // Make some changes
        Path filePath = local.resolve("foo"+File.separator+"bar.txt");
        Path filePathNew = local.resolve("foo"+File.separator+"new.txt");
        String timestamp = "testInDirAdd " + (new Date()).toString() + "\n";
        Files.write(filePath, timestamp.getBytes(), StandardOpenOption.APPEND);
        Files.write(filePathNew, timestamp.getBytes());
        ArrayList<Path> paths = new ArrayList<>();
        paths.add(filePath);
        paths.add(filePathNew);
        helperAdd.addFilePathsTest(paths, false);
        Git git = new Git(helperAdd.getRepo());

        // Check that the file was added.
        System.out.println(git.status().call().getAdded());
        System.out.println(git.status().call().getChanged());
        System.out.println(git.status().call().getModified());
        assertEquals(git.status().call().getAdded().size(), 1);
        assertEquals(git.status().call().getChanged().size(), 1);
    }

//    public void testAddOrig() throws Exception {
//        File authData = new File(testFileLocation + "httpUsernamePassword.txt");
//
//        // If a developer does not have this file present, test should just pass.
//        if (!authData.exists() && looseTesting)
//            return;
//
//        Scanner scanner = new Scanner(authData);
//        String ignoreURL = scanner.next();
//        String username = scanner.next();
//        String password = scanner.next();
//        UsernamePasswordCredentialsProvider credentials = new UsernamePasswordCredentialsProvider(username, password);
//
//        String remoteURL = "https://github.com/TheElegitTeam/AddDir.git";
//
//        // Repo that will add to master
//        Path repoPathAdd = directoryPath.resolve("adder");
//        ClonedRepoHelper helperAdd = new ClonedRepoHelper(repoPathAdd, remoteURL, credentials);
//        assertNotNull(helperAdd);
//        helperAdd.obtainRepository(remoteURL);
//
//
//        /* ********************* EDIT AND PUSH SECTION ********************* */
//        // Make some changes
//        Path filePath = repoPathAdd.resolve("foo"+File.separator+"bar.txt");
//        Path filePathNew = repoPathAdd.resolve("foo"+File.separator+"new.txt");
//        String timestamp = "testInDirAdd " + (new Date()).toString() + "\n";
//        Files.write(filePath, timestamp.getBytes(), StandardOpenOption.APPEND);
//        Files.write(filePathNew, timestamp.getBytes());
//        ArrayList<Path> paths = new ArrayList<>();
//        paths.add(filePath);
//        paths.add(filePathNew);
//        helperAdd.addFilePathsTest(paths);
//
//        Git git = new Git(helperAdd.getRepo());
//
//        // Check that the file was added.
//        System.out.println(git.status().call().getAdded());
//        System.out.println(git.status().call().getChanged());
//        System.out.println(git.status().call().getModified());
//        assertEquals(git.status().call().getAdded().size(), 1);
//        assertEquals(git.status().call().getChanged().size(), 1);
//    }
}
