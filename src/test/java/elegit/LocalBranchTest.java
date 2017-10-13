package elegit;

import elegit.exceptions.ConflictingFilesException;
import elegit.exceptions.MissingRepoException;
import elegit.exceptions.NoTrackingException;
import elegit.models.LocalBranchHelper;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.Date;
import java.util.Scanner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by Eric Walker.
 */
public class LocalBranchTest {
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
    public void testLocalBranchMerge() throws Exception {
        File authData = new File(testFileLocation + "httpUsernamePassword.txt");

        // If a developer does not have this file present, test should just pass.
        if (!authData.exists() && looseTesting)
            return;

        Scanner scanner = new Scanner(authData);
        String ignoreURL = scanner.next();
        String username = scanner.next();
        String password = scanner.next();
        UsernamePasswordCredentialsProvider credentials = new UsernamePasswordCredentialsProvider(username, password);

        String remoteURL = "https://github.com/TheElegitTeam/LocalBranch.git";

        // Repo that will commit to master
        Path repoPathPush = directoryPath.resolve("pusher");
        ClonedRepoHelper helperPush = new ClonedRepoHelper(repoPathPush, remoteURL, credentials);
        assertNotNull(helperPush);
        helperPush.obtainRepository(remoteURL);

        // Repo that will fetch and mergefromfetch
        Path repoPathFetch = directoryPath.resolve("fetcher");
        ClonedRepoHelper helperFetch = new ClonedRepoHelper(repoPathFetch, remoteURL, credentials);
        assertNotNull(helperPush);
        helperFetch.obtainRepository(remoteURL);


        /* ********************* EDIT AND PUSH SECTION ********************* */
        // Make some changes in master in pusher
        Path filePath = repoPathPush.resolve("README.md");
        String timestamp = "testLocalBranchPush " + (new Date()).toString() + "\n";
        Files.write(filePath, timestamp.getBytes(), StandardOpenOption.APPEND);
        helperPush.addFilePathTest(filePath);

        // Commit changes in master in pusher and push
        helperPush.commit("added line in master");
        PushCommand command = helperPush.prepareToPushAll();
        helperPush.pushAll(command);


        // Make a new branch in fetcher and check it out
        new_branch_fetch_helper = helperFetch.getBranchModel().createNewLocalBranch("new_branch_name");
        new_branch_fetch_helper.checkoutBranch();

        // Make some changes in new_branch in pusher
        filePath = repoPathFetch.resolve("README.md");
        timestamp = "testLocalBranchFetch " + (new Date()).toString() + "\n";
        Files.write(filePath, timestamp.getBytes(), StandardOpenOption.APPEND);
        helperFetch.addFilePathTest(filePath);
        /* ******************** FETCH AND MERGE SECTION ******************** */

        // Fetch changes
        helperFetch.fetch(false);

        // Merge from the fetch
        boolean local_branch_is_tracked = false;
        try {
            helperFetch.mergeFromFetch();
        } catch (IOException | GitAPIException | MissingRepoException | ConflictingFilesException e) { }
        catch (NoTrackingException e) {
            local_branch_is_tracked = true;
        } catch (Exception e) { }

        // Check that new_branch was fast-forwarded instead of merged with master
        assert(local_branch_is_tracked);

    }
}
