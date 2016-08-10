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
    Path logPath;

    @Before
    public void setUp() throws Exception {
        initializeLogger();
        this.directoryPath = Files.createTempDirectory("unitTestRepos");
        directoryPath.toFile().deleteOnExit();
        this.repoPath = directoryPath.resolve("testrepo");
        // Clone from dummy repo:
        this.remoteURL = "https://github.com/TheElegitTeam/TestRepository.git";

        // This repo doesn't check username/password for read-only
        UsernamePasswordCredentialsProvider credentials = new UsernamePasswordCredentialsProvider("", "");
        helper = new ClonedRepoHelper(repoPath, remoteURL, credentials);
        helper.obtainRepository(remoteURL);
        assertNotNull(helper);
    }

    @After
    public void tearDown() throws Exception {
        // Delete the cloned files.
        removeAllFilesFromDirectory(this.directoryPath.toFile());
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
        assertFalse(this.helper.getAheadCount()>0);

        Path newPath = Paths.get(this.directoryPath.toString(), "new.txt");

        // Need to make the "newFile.txt" actually exist:
        Files.createFile(newPath);

        try(PrintWriter newPathTextWriter = new PrintWriter( newPath.toString() )){
            newPathTextWriter.println("Dummy text for the new file to commit");
        }

        this.helper.addFilePathTest(newPath);
        this.helper.commit("Added a new file in a unit test!");

        assertTrue(this.helper.getAheadCount()>0);

    }

}