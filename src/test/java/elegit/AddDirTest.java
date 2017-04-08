package elegit;

import elegit.exceptions.ConflictingFilesException;
import elegit.exceptions.MissingRepoException;
import elegit.exceptions.NoTrackingException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.Scanner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by Eric Walker.
 */
public class AddDirTest {
    private Path directoryPath;
    private String testFileLocation;
    private LocalBranchHelper new_branch_fetch_helper;
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

    @Test
    public void testAdd() throws Exception {
        File authData = new File(testFileLocation + "httpUsernamePassword.txt");

        // If a developer does not have this file present, test should just pass.
        if (!authData.exists() && looseTesting)
            return;

        Scanner scanner = new Scanner(authData);
        String ignoreURL = scanner.next();
        String username = scanner.next();
        String password = scanner.next();
        UsernamePasswordCredentialsProvider credentials = new UsernamePasswordCredentialsProvider(username, password);

        String remoteURL = "https://github.com/TheElegitTeam/AddDir.git";

        // Repo that will add to master
        Path repoPathAdd = directoryPath.resolve("adder");
        ClonedRepoHelper helperAdd = new ClonedRepoHelper(repoPathAdd, remoteURL, credentials);
        assertNotNull(helperAdd);
        helperAdd.obtainRepository(remoteURL);


        /* ********************* EDIT AND PUSH SECTION ********************* */
        // Make some changes
        Path filePath = repoPathAdd.resolve("foo"+File.separator+"bar.txt");
        Path filePathNew = repoPathAdd.resolve("foo"+File.separator+"new.txt");
        String timestamp = "testInDirAdd " + (new Date()).toString() + "\n";
        Files.write(filePath, timestamp.getBytes(), StandardOpenOption.APPEND);
        Files.write(filePathNew, timestamp.getBytes());
        ArrayList paths = new ArrayList();
        paths.add(filePath);
        paths.add(filePathNew);
        helperAdd.addFilePathsTest(paths);

        Git git = new Git(helperAdd.repo);

        // Check that the file was added.
        System.out.println(git.status().call().getAdded());
        System.out.println(git.status().call().getChanged());
        System.out.println(git.status().call().getModified());
        assertEquals(git.status().call().getAdded().size(), 1);
        assertEquals(git.status().call().getChanged().size(), 1);
    }
}
