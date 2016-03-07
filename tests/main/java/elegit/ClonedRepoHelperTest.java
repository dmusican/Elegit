package main.java.elegit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.*;

/**
 * Created by grahamearley on 2/21/16.
 */
public class ClonedRepoHelperTest {

    Path directoryPath;
    String remoteURL;
    String username;
    ClonedRepoHelper helper;

    @Before
    public void setUp() throws Exception {
        // Clone to a directory "/unitTestRepos":
        this.directoryPath = Paths.get("unitTestRepos").toAbsolutePath();

        // Clone from dummy repo:
        this.remoteURL = "https://github.com/TheElegitTeam/TestRepository.git";
        this.username = "Dummy_Username";

        helper = new ClonedRepoHelper(directoryPath, remoteURL, username);
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
    public void testClonedRepoHelperConstructor() throws Exception {
        assertNotNull(helper.getRepo());
    }

    @Test
    public void testExists() throws Exception {
        assertTrue(this.helper.exists());
    }

    @Test
    public void testObtainRepository() throws Exception {
        assertNotNull(this.helper.obtainRepository());
    }

    @Test
    public void testAddFileAndCommit() throws Exception {
        assertFalse(this.helper.hasUnpushedCommits());

        Path newPath = Paths.get(this.directoryPath.toString(), "new.txt");

        // Need to make the "newFile.txt" actually exist:
        Files.createFile(newPath);

        try(PrintWriter newPathTextWriter = new PrintWriter( newPath.toString() )){
            newPathTextWriter.println("Dummy text for the new file to commit");
        }

        this.helper.addFilePath(newPath);
        this.helper.commit("Added a new file in a unit test!");

        assertTrue(this.helper.hasUnpushedCommits());
    }

    @Test
    public void testGetLinkedRemoteRepoURLs() throws Exception {

    }

    @Test
    public void testHasRemote() throws Exception {

    }

    @Test
    public void testHasUnpushedCommits() throws Exception {

    }

    @Test
    public void testHasUnpushedTags() throws Exception {

    }

    @Test
    public void testHasUnmergedCommits() throws Exception {

    }

    @Test
    public void testCommit() throws Exception {

    }

    @Test
    public void testTag() throws Exception {

    }

    @Test
    public void testPushAll() throws Exception {

    }

    @Test
    public void testPushTags() throws Exception {

    }

    @Test
    public void testFetch() throws Exception {

    }

    @Test
    public void testMergeFromFetch() throws Exception {

    }

    @Test
    public void testPresentUsernameDialog() throws Exception {

    }

    @Test
    public void testCloseRepo() throws Exception {

    }

    @Test
    public void testGetRepo() throws Exception {

    }

    @Test
    public void testGetLocalPath() throws Exception {

    }

    @Test
    public void testGetLocalCommits() throws Exception {

    }

    @Test
    public void testGetRemoteCommits() throws Exception {

    }

    @Test
    public void testGetCommit() throws Exception {

    }

    @Test
    public void testGetCommit1() throws Exception {

    }

    @Test
    public void testGetTag() throws Exception {

    }

    @Test
    public void testDeleteTag() throws Exception {

    }

    @Test
    public void testGetAllCommitIDs() throws Exception {

    }

    @Test
    public void testGetAllTagNames() throws Exception {

    }

    @Test
    public void testGetHead() throws Exception {

    }

    @Test
    public void testGetNewLocalCommits() throws Exception {

    }

    @Test
    public void testGetNewRemoteCommits() throws Exception {

    }

    @Test
    public void testGetAllLocalTags() throws Exception {

    }

    @Test
    public void testUpdateTags() throws Exception {

    }

    @Test
    public void testParseRawCommit() throws Exception {

    }

    @Test
    public void testToString() throws Exception {

    }

    @Test
    public void testCallGitForLocalBranches() throws Exception {

    }

    @Test
    public void testGetLocalBranchesFromManager() throws Exception {

    }

    @Test
    public void testCallGitForRemoteBranches() throws Exception {

    }

    @Test
    public void testSetCurrentBranch() throws Exception {

    }

    @Test
    public void testGetCurrentBranch() throws Exception {

    }

    @Test
    public void testRefreshCurrentBranch() throws Exception {

    }

    @Test
    public void testShowBranchManagerWindow() throws Exception {

    }

    @Test
    public void testGetLocalBranches() throws Exception {

    }

    @Test
    public void testGetRemoteBranches() throws Exception {

    }

    @Test
    public void testIsBranchTracked() throws Exception {

    }

    @Test
    public void testGetRefsFromRemote() throws Exception {

    }

    @Test
    public void testGetBranchManagerModel() throws Exception {

    }

    @Test
    public void testGetUsername() throws Exception {

    }

    @Test
    public void testSetUsername() throws Exception {

    }

    @Test
    public void testGetPassword() throws Exception {

    }

    @Test
    public void testSetPassword() throws Exception {

    }

    @Test
    public void testSetAuthCredentials() throws Exception {

    }

    @Test
    public void testGetOwnerAuthCredentials() throws Exception {

    }
}