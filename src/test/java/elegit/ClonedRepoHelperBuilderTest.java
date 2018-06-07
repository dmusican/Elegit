package elegit;

import elegit.gui.ClonedRepoHelperBuilder;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LsRemoteCommand;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;

/**
 * Created by dmusican on 2/13/16.
 */
public class ClonedRepoHelperBuilderTest {

    Path logPath;

    @Before
    public void setUp() throws Exception {
        initializeLogger();
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
    //test doesn't test anything, always passes
    @Test
    public void testGetPrevRepoName() throws Exception {
        assertTrue(4 == 2+2);
    }

    @Test
    public void testGuessRepoName() throws Exception {
        assertEquals(ClonedRepoHelperBuilder.guessRepoName("https://github.com/dmusican/Elegit"), "Elegit");
        assertEquals(ClonedRepoHelperBuilder.guessRepoName("https://github.com/dmusican/Elegit.git"), "Elegit");
        assertEquals(ClonedRepoHelperBuilder.guessRepoName("git@github.com:dmusican/Elegit.git"), "Elegit");
        assertEquals(ClonedRepoHelperBuilder.guessRepoName("git@github.com:dmusican/Elegit"), "Elegit");
    }

    @Test
    public void testLsRemoteOnHTTP() throws Exception {
        LsRemoteCommand command = Git.lsRemoteRepository();
        command.setRemote("https://github.com/TheElegitTeam/TestRepository.git");
        command.call();

    }

}