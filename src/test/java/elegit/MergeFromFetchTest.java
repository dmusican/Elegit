package elegit;

import elegit.exceptions.ConflictingFilesException;
import elegit.exceptions.MissingRepoException;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.Scanner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by Eric Walker.
 */
public class MergeFromFetchTest {
    private Path directoryPath;
    private String testFileLocation;
    private RemoteBranchHelper remote_helper_push, remote_helper_fetch;
    private LocalBranchHelper new_branch_push_helper, master_push_helper, new_branch_fetch_helper;
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
    public void testFastForward() throws Exception {
        File authData = new File(testFileLocation + "httpUsernamePassword.txt");

        // If a developer does not have this file present, test should just pass.
        if (!authData.exists() && looseTesting)
            return;

        Scanner scanner = new Scanner(authData);
        String ignoreURL = scanner.next();
        String username = scanner.next();
        String password = scanner.next();
        UsernamePasswordCredentialsProvider credentials = new UsernamePasswordCredentialsProvider(username, password);

        String remoteURL = "https://github.com/TheElegitTeam/MergeFetch.git";

        // Repo that will commit to new_branch
        Path repoPathPush = directoryPath.resolve("pusher");
        ClonedRepoHelper helperPush = new ClonedRepoHelper(repoPathPush, remoteURL, credentials);
        assertNotNull(helperPush);

        // Repo that will fetch and mergefromfetch
        Path repoPathFetch = directoryPath.resolve("fetcher");
        ClonedRepoHelper helperFetch = new ClonedRepoHelper(repoPathFetch, remoteURL, credentials);
        assertNotNull(helperPush);


        /* ********************* EDIT AND PUSH SECTION ********************* */

        // Find the remote 'new_branch' for push repo
        remote_helper_push = (RemoteBranchHelper) helperPush.getBranchModel().getBranchByName(BranchModel.BranchType.REMOTE, "origin/new_branch");
        assertNotNull(remote_helper_push);
        // Track new_branch and check it out
        new_branch_push_helper = helperPush.getBranchModel().trackRemoteBranch(remote_helper_push);
        new_branch_push_helper.checkoutBranch();

        // Make some different changes in new_branch
        Path filePath = repoPathPush.resolve("README.md");
        String newBranchLine = "Line for new branch\n";
        Files.write(filePath, newBranchLine.getBytes(), StandardOpenOption.APPEND);
        helperPush.addFilePathTest(filePath);

        // Commit changes in new_branch
        helperPush.commit("added line in new_branch");

        // Make some changes in master
        master_push_helper = (LocalBranchHelper) helperPush.getBranchModel().getBranchByName(BranchModel.BranchType.LOCAL, "master");
        master_push_helper.checkoutBranch();
        filePath = repoPathPush.resolve("README.md");
        newBranchLine = "Line for master\n";
        Files.write(filePath, newBranchLine.getBytes(), StandardOpenOption.APPEND);
        helperPush.addFilePathTest(filePath);

        // Commit the changes in master and push
        helperPush.commit("added line in master");

        helperPush.pushAll();


        /* ******************** FETCH AND MERGE SECTION ******************** */

        // Checkout new_branch
        remote_helper_fetch = (RemoteBranchHelper) helperFetch.getBranchModel().getBranchByName(BranchModel.BranchType.REMOTE, "origin/new_branch");
        assertNotNull(remote_helper_fetch);
        // Track new_branch and check it out
        new_branch_fetch_helper = helperFetch.getBranchModel().trackRemoteBranch(remote_helper_fetch);
        new_branch_fetch_helper.checkoutBranch();

        // Fetch changes
        helperFetch.fetch();

        // Merge from the fetch
        boolean is_fast_forward = true;
        try {
            is_fast_forward = helperFetch.mergeFromFetch() == MergeResult.MergeStatus.FAST_FORWARD;
        } catch (IOException | GitAPIException | MissingRepoException e) { }
        catch (ConflictingFilesException e) {
            is_fast_forward = false;
        }

        // Check that new_branch was fast-forwarded instead of merged with master
        assert(is_fast_forward);

    }
}
