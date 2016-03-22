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
    Path repoPath;
    String remoteURL;
    String username;
    ClonedRepoHelper helper;

    @Before
    public void setUp() throws Exception {
        this.directoryPath = Files.createTempDirectory("unitTestRepos");
        directoryPath.toFile().deleteOnExit();
        this.repoPath = directoryPath.resolve("testrepo");
        // Clone from dummy repo:
        this.remoteURL = "https://github.com/TheElegitTeam/TestRepository.git";

        helper = new ClonedRepoHelper(repoPath, remoteURL);
        assertNotNull(helper);
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
        // Challenging to test directly because setUp already obtains
        // the repo, and obtaining it again causes an error; seems like
        // redundant to test here anyway
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

}