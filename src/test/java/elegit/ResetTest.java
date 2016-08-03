package elegit;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by Eric Walker.
 */
public class ResetTest {
    private Path directoryPath;
    private String testFileLocation;
    private RepoHelper helper;
    Path logPath;

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

    // Test to make sure creating a local branch lets us push and
    // that pushing will create the new branch.
    @Test
    public void testBranchPushAndDelete() throws Exception {
        File authData = new File(testFileLocation + "httpUsernamePassword.txt");

        // If a developer does not have this file present, test should just pass.
        if (!authData.exists() && looseTesting)
            return;

        Scanner scanner = new Scanner(authData);
        String ignoreURL = scanner.next();
        String username = scanner.next();
        String password = scanner.next();
        UsernamePasswordCredentialsProvider credentials = new UsernamePasswordCredentialsProvider(username, password);

        String remoteURL = "https://github.com/TheElegitTeam/ResetTesting.git";

        // Repo that will commit to master
        Path repoPathPush = directoryPath.resolve("repo");
        helper = new ClonedRepoHelper(repoPathPush, remoteURL, credentials);
        assertNotNull(helper);

        /*
        test reset file
            1 file
            >1 files
        test reset commit
            hard
            mixed
            soft
            merge
            keep
         */

        Git git = new Git(helper.repo);

        /* ********************* FILE RESET SECTION ********************* */
        // Single file reset
        Path filePath = repoPathPush.resolve("modify.txt");
        String text = "Lorem Ipsum";
        Files.write(filePath, text.getBytes(), StandardOpenOption.APPEND);
        helper.addFilePath(filePath);
        // Check that the file is staged
        assertEquals(1,git.status().call().getChanged().size());
        // Reset the file and check that it worked
        helper.reset(filePath);
        assertEquals(0,git.status().call().getChanged().size());

        // Multiple file reset
        Path readPath = repoPathPush.resolve("README.md");
        Files.write(readPath, text.getBytes(), StandardOpenOption.APPEND);
        ArrayList<Path> paths = new ArrayList<>();
        paths.add(filePath);
        paths.add(readPath);
        // Add both files and check that they are staged.
        helper.addFilePaths(paths);
        assertEquals(2,git.status().call().getChanged().size());
        // Reset both the files and check that it worked
        helper.reset(paths);
        assertEquals(0,git.status().call().getChanged().size());
    }
}
