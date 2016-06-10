package elegit;

import org.eclipse.jgit.api.Git;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.*;

/**
 * Created by dmusican on 2/24/16.
 */
public class ExistingRepoHelperTest {
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


    @Test
    public void testExistingRepoOpen() throws Exception {
        File localPath = File.createTempFile("TestGitRepo","");
        localPath.delete();

        Git git = Git.cloneRepository()
                    .setURI("https://github.com/dmusican/testrepo.git")
                    .setDirectory(localPath)
                    .call();

        SessionModel.getSessionModel().setAuthPref(localPath.toString(), AuthMethod.HTTPS);

        String username = null;
        ExistingRepoHelper repoHelper = new ExistingRepoHelper(Paths.get(localPath.getAbsolutePath()));
        git.close();
    }
}