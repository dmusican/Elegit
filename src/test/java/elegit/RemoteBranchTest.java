package elegit;

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
public class RemoteBranchTest {
    private Path directoryPath;
    private String testFileLocation;
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

        String remoteURL = "https://github.com/TheElegitTeam/RemoteBranchDeletion.git";

        // Repo that will commit to master
        Path repoPathPush = directoryPath.resolve("pusher");
        ClonedRepoHelper helperPush = new ClonedRepoHelper(repoPathPush, remoteURL, credentials);
        assertNotNull(helperPush);
        helperPush.obtainRepository(remoteURL);


        /* ********************* BRANCH AND PUSH SECTION ********************* */
        // Check that a previous test wasn't interrupted, if so make sure our branch is not there
        for (RemoteBranchHelper helper : helperPush.getBranchModel().getRemoteBranchesTyped()) {
            if (helper.getBranchName().contains("new_branch")) {
                helperPush.getBranchModel().deleteRemoteBranch(helper);
            }
        }

        // Make a new branch and push it
        helperPush.getBranchModel().createNewLocalBranch("new_branch");
        helperPush.getBranchModel().getBranchByName(BranchModel.BranchType.LOCAL, "new_branch").checkoutBranch();
        helperPush.updateModel();
        // Check that we can push
        assertEquals(true, helperPush.canPush());

        helperPush.prepareToPushCurrentBranch(true);
        helperPush.updateModel();
        // Check that there is a remote branch now
        assertEquals(true, helperPush.getBranchModel().getRemoteBranchesTyped().toString().contains("new_branch"));



        // Test that we can delete the branch too
        helperPush.getBranchModel().updateRemoteBranches();
        BranchHelper remoteBranchHelper = helperPush.getBranchModel().getBranchByName(BranchModel.BranchType.REMOTE, "origin/new_branch");
        helperPush.getBranchModel().deleteRemoteBranch((RemoteBranchHelper) remoteBranchHelper);

        // Check that the branch was deleted
        assertEquals(true, helperPush.getBranchModel().getCurrentRemoteBranch()==null);
    }
}
