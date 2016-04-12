package main.java.elegit;

import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Date;
import java.util.Scanner;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by dmusican. Enjoy!
 */
public class PushPullTest {


    private Path directoryPath;
    private String testFileLocation;


    @Before
    public void setUp() throws Exception {
        this.directoryPath = Files.createTempDirectory("unitTestRepos");
        //directoryPath.toFile().deleteOnExit();
        testFileLocation = System.getProperty("user.home") + File.separator +
                           "elegitTests" + File.separator;
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testPushPullBothCloned() throws Exception {
        File authData = new File(testFileLocation + "httpUsernamePassword.txt");

        // If a developer does not have this file present, test should just pass.
        if (!authData.exists())
            return;

        Scanner scanner = new Scanner(authData);
        String ignoreURL = scanner.next();
        String username = scanner.next();
        String password = scanner.next();
        UsernamePasswordCredentialsProvider credentials = new UsernamePasswordCredentialsProvider(username, password);

        String remoteURL = "https://github.com/TheElegitTeam/PushPullTests.git";

        // Repo that will push
        Path repoPathPush = directoryPath.resolve("pushpull1");
        ClonedRepoHelper helperPush = new ClonedRepoHelper(repoPathPush, remoteURL, credentials);
        assertNotNull(helperPush);

        // Repo that will pull
        Path repoPathPull = directoryPath.resolve("pushpull2");
        ClonedRepoHelper helperPull = new ClonedRepoHelper(repoPathPull, remoteURL, credentials);
        assertNotNull(helperPull);

        // Update the file, then commit and push
        Path readmePath = repoPathPush.resolve("README.md");
        System.out.println(readmePath);
        String timestamp = (new Date()).toString() + "\n";
        Files.write(readmePath, timestamp.getBytes(), StandardOpenOption.APPEND);
        helperPush.addFilePath(readmePath);
        helperPush.commit("added a character");
        helperPush.pushAll();

        // Now do the pull (well, a fetch)
        helperPull.fetch();
        helperPull.mergeFromFetch();

        // Update lists of remote branches.
        helperPull.getListOfLocalBranches();
        helperPull.getListOfRemoteBranches();

    }

    @Test
    public void testPushPullBothClonedExisting() throws Exception {
        File authData = new File(testFileLocation + "httpUsernamePassword.txt");

        // If a developer does not have this file present, test should just pass.
        if (!authData.exists())
            return;

        Scanner scanner = new Scanner(authData);
        String ignoreURL = scanner.next();
        String username = scanner.next();
        String password = scanner.next();
        UsernamePasswordCredentialsProvider credentials = new UsernamePasswordCredentialsProvider(username, password);

        String remoteURL = "https://github.com/TheElegitTeam/PushPullTests.git";

        // Repo that will push
        Path repoPathPush = directoryPath.resolve("pushpull1");
        ClonedRepoHelper helperPush = new ClonedRepoHelper(repoPathPush, remoteURL, credentials);
        assertNotNull(helperPush);

        // Repo that will pull
        Path repoPathPull = directoryPath.resolve("pushpull2");
        ClonedRepoHelper clonedHelperPull = new ClonedRepoHelper(repoPathPull, remoteURL, credentials);
        assertNotNull(clonedHelperPull);
        ExistingRepoHelper existingHelperPull = new ExistingRepoHelper(repoPathPull);

        // Update the file, then commit and push
        Path readmePath = repoPathPush.resolve("README.md");
        System.out.println(readmePath);
        String timestamp = "testPushPullBothClonedExisting " + (new Date()).toString() + "\n";
        Files.write(readmePath, timestamp.getBytes(), StandardOpenOption.APPEND);
        helperPush.addFilePath(readmePath);
        helperPush.commit("added a character");
        helperPush.pushAll();

        // Now do the pull (well, a fetch)
        existingHelperPull.fetch();
        existingHelperPull.mergeFromFetch();

    }


}
