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
 * Created by Eric Walker on 5/10/2016.
 */

// Create branch, commit1 to it, switch to master, untrack branch, retrack branch
// check if branch is called origin/branch or just branch
public class BranchSwitchTest {

    Path directoryPath;
    Path repoPath;
    String remoteURL;
    String username;
    ClonedRepoHelper helper1;
    ClonedRepoHelper helper2;

    @Before
    public void setUp() throws Exception {
        this.directoryPath = Files.createTempDirectory("unitTestRepos");
        directoryPath.toFile().deleteOnExit();
        this.repoPath = directoryPath.resolve("testrepo");
        // Clone from dummy repo:
        this.remoteURL = "https://github.com/TheElegitTeam/TestRepository.git";

        helper1 = new ClonedRepoHelper(repoPath, remoteURL);
        helper2 = new ClonedRepoHelper(repoPath, remoteURL);
        assertNotNull(helper1);
        assertNotNull(helper2);
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
    public void testBranchCreate() throws Exception {
        assertNotNull(helper1.getRepo());
    }

    @Test
    public void testExists() throws Exception {
        assertTrue(this.helper1.exists());
    }

    @Test
    public void testCommitFileAndSwitchBranches() throws Exception {
        assertFalse(this.helper1.hasUnpushedCommits());

        Path newPath = Paths.get(this.directoryPath.toString(), "new.txt");

        // Need to make the "newFile.txt" actually exist:
        Files.createFile(newPath);

        try(PrintWriter newPathTextWriter = new PrintWriter( newPath.toString() )){
            newPathTextWriter.println("Dummy text for the new file to commit");
        }

        this.helper1.addFilePath(newPath);
        this.helper1.commit("Added a new file in a unit test!");

        assertTrue(this.helper1.hasUnpushedCommits());



    }

}