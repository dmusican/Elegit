package elegit;

import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeCommand;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.rmi.Remote;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by erictwalker18. Enjoy!
 */
public class FastForwardTest {
    private Path directoryPath;
    private String testFileLocation;
    private RemoteBranchHelper remote_helper, remote_helperb;
    private LocalBranchHelper fast_helper, master_helper, fast_helperb;
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

        String remoteURL = "https://github.com/TheElegitTeam/FastForwards.git";

        // Repo that will commit to make a fast forward commit
        Path repoPathFast = directoryPath.resolve("fastforward");
        ClonedRepoHelper helperFast = new ClonedRepoHelper(repoPathFast, remoteURL, credentials);
        assertNotNull(helperFast);

        // Find the remote 'fast_branch'
        remote_helper = helperFast.getRemoteBranchByName("origin/fast_branch");

        // Track fast_branch and check it out
        fast_helper = helperFast.trackRemoteBranch(remote_helper);
        fast_helper.checkoutBranch();

        // Update the file in fast_branch
        Path filePath = repoPathFast.resolve("fastforward.txt");
        String timestamp = (new Date()).toString() + "\n";
        Files.write(filePath, timestamp.getBytes(), StandardOpenOption.APPEND);
        helperFast.addFilePath(filePath);

        // Commit changes in fast_branch and push
        helperFast.commit("added a character");
        helperFast.pushAll();

        //Checkout master
        master_helper = helperFast.getLocalBranchByName("master");
        master_helper.checkoutBranch();

        // Merge fast_forward into master
        helperFast.mergeWithBranch(fast_helper);

        // Check that Elegit recognizes there are unpushed commits
        assertEquals(true, helperFast.hasUnpushedCommits());

        // Push changes
        helperFast.pushAll();

    }

    @Test
    public void testFastForwardCommitCanPush() throws Exception {
        File authData = new File(testFileLocation + "httpUsernamePassword.txt");

        // If a developer does not have this file present, test should just pass.
        if (!authData.exists() && looseTesting)
            return;

        // a and b at the same spot
        // make changes, commit to a
        // push changes in a
        // merge a into b
        // check that we can push

        Scanner scanner = new Scanner(authData);
        String ignoreURL = scanner.next();
        String username = scanner.next();
        String password = scanner.next();
        UsernamePasswordCredentialsProvider credentials = new UsernamePasswordCredentialsProvider(username, password);

        String remoteURL = "https://github.com/TheElegitTeam/FastForwards.git";

        // Repo that will commit to make a fast forward commit
        Path repoPathFast = directoryPath.resolve("fastforward");
        ClonedRepoHelper helperFast = new ClonedRepoHelper(repoPathFast, remoteURL, credentials);
        assertNotNull(helperFast);

        // Find the remote 'can_push'
        remote_helper = helperFast.getRemoteBranchByName("origin/can_push");

        // Track can_push and check it out
        fast_helper = helperFast.trackRemoteBranch(remote_helper);
        fast_helper.checkoutBranch();

        // Update the file in can_push
        Path filePath = repoPathFast.resolve("fastforward.txt");
        String timestamp = (new Date()).toString() + "\n";
        Files.write(filePath, timestamp.getBytes(), StandardOpenOption.APPEND);
        helperFast.addFilePath(filePath);

        // Commit changes in can_push and push
        helperFast.commit("added a character");
        helperFast.pushAll();

        // Find the remote 'can_pushb' and check it out
        remote_helperb = helperFast.getRemoteBranchByName("origin/can_pushb");
        fast_helperb = helperFast.trackRemoteBranch(remote_helperb);
        fast_helperb.checkoutBranch();

        // Merge can_push into can_pushb
        helperFast.mergeWithBranch(fast_helper);

        // Check that Elegit recognizes there are unpushed commits
        assertEquals(true, helperFast.hasUnpushedCommits());

        // Push changes
        helperFast.pushAll();

    }
}
