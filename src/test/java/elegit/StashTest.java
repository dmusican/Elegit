package elegit;

import elegit.models.ClonedRepoHelper;
import elegit.models.CommitHelper;
import elegit.models.RepoHelper;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by Eric Walker.
 */
public class StashTest {
    private Path directoryPath;
    private String testFileLocation;
    Path logPath, repoPath;
    File authData;
    ClonedRepoHelper helper;
    Git git;

    // Used to indicate that if password files are missing, then tests should just pass
    private boolean looseTesting;

    @Before
    public void setUp() throws Exception {
        initializeLogger();
        this.directoryPath = Files.createTempDirectory("unitTestRepos");
        directoryPath.toFile().deleteOnExit();
        testFileLocation = System.getProperty("user.home") + File.separator +
                "elegitTests" + File.separator;
        File strictTestingFile = new File(testFileLocation + "strictAuthenticationTesting.txt");
        looseTesting = !strictTestingFile.exists();

        initializeRepo();
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

    void initializeRepo() throws Exception {
        authData = new File(testFileLocation + "httpUsernamePassword.txt");

        // This repo doesn't check username/password for read-only
        UsernamePasswordCredentialsProvider credentials = new UsernamePasswordCredentialsProvider("", "");

        String remoteURL = "https://github.com/TheElegitTeam/ResetTesting.git";

        // Repo that will commit to master
        repoPath = directoryPath.resolve("repo");
        helper = new ClonedRepoHelper(repoPath, remoteURL, credentials);
        assertNotNull(helper);
        helper.obtainRepository(remoteURL);

        git = new Git(helper.getRepo());
    }

    @Test
    public void testStash() throws Exception {
        Path modify = repoPath.resolve("modify.txt");
        modifyFile(modify);

        Path untracked = Paths.get(repoPath.toString(), "new.txt");
        Files.createFile(untracked);

        Status status = git.status().call();
        assertEquals(1, status.getModified().size());
        assertEquals(1, status.getUntracked().size());

        // Save only tracked files
        helper.stashSave(false);

        status = git.status().call();
        assertEquals(0, status.getModified().size());
        assertEquals(1, status.getUntracked().size());

        // Stash untracked files
        helper.stashSave(true);

        status = git.status().call();
        assertEquals(0, status.getModified().size());
        assertEquals(0, status.getUntracked().size());

        List<CommitHelper> stashList = helper.stashList();
        assertEquals(2, stashList.size());

        // Apply a given stash
        helper.stashApply(stashList.get(0).getName(), false);

        status = git.status().call();
        assertEquals(0, status.getModified().size());
        assertEquals(1, status.getUntracked().size());

        // Clear all the stashes
        helper.stashClear();

        stashList = helper.stashList();
        assertEquals(0, stashList.size());
    }

    private void modifyFile(Path file) throws Exception {
        String text = "Lorem Ipsum";
        Files.write(file, text.getBytes(), StandardOpenOption.APPEND);
    }

    private void modifyAddFile(RepoHelper helper, Path file) throws Exception {
        String text = "Lorem Ipsum";
        Files.write(file, text.getBytes(), StandardOpenOption.APPEND);
        helper.addFilePathTest(file);
    }
}
