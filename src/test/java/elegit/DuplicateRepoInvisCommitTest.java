package elegit;

import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;

import static org.junit.Assert.*;

/**
 * Test for issue #165
 */
public class DuplicateRepoInvisCommitTest {

    private static final String REMOTE_URL = "https://github.com/TheElegitTeam/DuplicateRepoInvisCommitTestRepo.git";

    private Path directoryPath;
    private String testFileLocation;
    Path logPath;

    @Before
    public void setUp() throws Exception {
        initializeLogger();
        this.directoryPath = Files.createTempDirectory("unitTestRepos");
        //directoryPath.toFile().deleteOnExit();
        testFileLocation = System.getProperty("user.home") + File.separator +
                "elegitTests" + File.separator;
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
    public void testSwitchingRepoInvisCommit() throws Exception {
        File authData = new File(testFileLocation + "httpUsernamePassword.txt");

        // If a developer does not have this file present, test should just pass.
        if (!authData.exists())
            return;

        Scanner scanner = new Scanner(authData);
        String ignoreURL = scanner.next();
        String username = scanner.next();
        String password = scanner.next();
        UsernamePasswordCredentialsProvider credentials = new UsernamePasswordCredentialsProvider(username, password);

        // First copy of the repo
        Path repoPath1 = directoryPath.resolve("repo1");
        ClonedRepoHelper repo1 = new ClonedRepoHelper(repoPath1, REMOTE_URL, credentials);
        assertNotNull(repo1);
        repo1.obtainRepository(REMOTE_URL);

        CommitHelper repo1OldHead = repo1.getCommit("master");
        assertNotNull(repo1OldHead);

        // Make a change in repo1 and commit it
        File file = Paths.get(repoPath1.toString(), "File.txt").toFile();
        assertTrue(file.exists());

        try(PrintWriter fileTextWriter = new PrintWriter( file )){
            fileTextWriter.println("Add a line");
        }

        repo1.addFilePathTest(file.toPath());
        repo1.commit("Modified file.txt in a unit test!");

        CommitHelper repo1NewHead = repo1.getCommit("master");
        assertNotNull(repo1NewHead);

        // Second copy of the repo
        Path repoPath2 = directoryPath.resolve("repo2");
        ClonedRepoHelper repo2 = new ClonedRepoHelper(repoPath2, REMOTE_URL, credentials);
        assertNotNull(repo2);
        repo2.obtainRepository(REMOTE_URL);

        CommitHelper repo2OldHead = repo2.getCommit("master");
        assertNotNull(repo2OldHead);
        assertEquals(repo1OldHead.getId(), repo2OldHead.getId());

        // Push the previous commit
        repo1.prepareToPushAll();

        // Fetch into the second repo
        repo2.fetch();
        repo2.mergeFromFetch();

        CommitHelper repo2NewHead = repo2.getCommit("master");
        assertNotNull(repo2NewHead);
        assertEquals(repo1NewHead.getId(), repo2NewHead.getId());

        repo1.updateModel();
        List<CommitHelper> repo1LocalCommits = repo1.getLocalCommits();
        List<CommitHelper> repo1RemoteCommits = repo1.getRemoteCommits();

        assertTrue(repo1LocalCommits.contains(repo1OldHead));
        assertTrue(repo1RemoteCommits.contains(repo1OldHead));
        assertTrue(repo1LocalCommits.contains(repo1NewHead));
        assertTrue(repo1RemoteCommits.contains(repo1NewHead));

        repo2.updateModel();
        List<CommitHelper> repo2LocalCommits = repo2.getLocalCommits();
        List<CommitHelper> repo2RemoteCommits = repo2.getRemoteCommits();

        assertTrue(repo2LocalCommits.contains(repo2OldHead));
        assertTrue(repo2RemoteCommits.contains(repo2OldHead));
        assertTrue(repo2LocalCommits.contains(repo2NewHead));
        assertTrue(repo2RemoteCommits.contains(repo2NewHead));

    }
}
