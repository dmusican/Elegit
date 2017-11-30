package elegitfx;

import elegit.Main;
import elegit.models.ClonedRepoHelper;
import elegit.models.ExistingRepoHelper;
import elegit.models.SessionModel;
import elegit.monitors.RepositoryMonitor;
import elegit.sshauthentication.ElegitUserInfoTest;
import javafx.application.Platform;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.testfx.framework.junit.ApplicationTest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Scanner;

import static elegit.monitors.RepositoryMonitor.REMOTE_CHECK_INTERVAL;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by dmusican. Enjoy!
 */
public class PushPullFXTest extends ApplicationTest {


    private Path directoryPath;
    private String testFileLocation;
    Path logPath;

    // Used to indicate that if password files are missing, then tests should just pass
    private boolean looseTesting;




    @Before
    public void setUp() throws Exception {
        initializeLogger();
        this.directoryPath = Files.createTempDirectory("unitTestRepos");
        //directoryPath.toFile().deleteOnExit();
        testFileLocation = System.getProperty("user.home") + File.separator +
                           "elegitTests" + File.separator;
        File strictTestingFile = new File(testFileLocation + "strictAuthenticationTesting.txt");
        looseTesting = !strictTestingFile.exists();
    }

    @After
    public void tearDown() throws Exception {
        removeAllFilesFromDirectory(this.logPath.toFile());
        Main.allSubscriptions.clear();
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
    // This test has some thread safety issues that should probably be fixed; the RepositoryMonitor is started
    // in the FX thread (which is right), but there's a lot of code here in this test not run in the FX thread
    // that probably should be. Fix it if this test starts causing trouble. In the meantime... it's a test.
    public void testPushPullBothCloned() throws Exception {
        File authData = new File(testFileLocation + "httpUsernamePassword.txt");

        // If a developer does not have this file present, test should just pass.
        if (!authData.exists() && looseTesting)
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
        helperPush.obtainRepository(remoteURL);

        // Repo that will pull
        Path repoPathPull = directoryPath.resolve("pushpull2");
        ClonedRepoHelper helperPull = new ClonedRepoHelper(repoPathPull, remoteURL, credentials);
        assertNotNull(helperPull);
        helperPull.obtainRepository(remoteURL);

        Platform.runLater(() -> {
            try {
                // Create a session model for helperPull so can later verify if changes have been seen
                SessionModel model = SessionModel.getSessionModel();
                model.openRepoFromHelper(helperPull);
                RepositoryMonitor.initRemote();
            } catch (Exception e) {
                throw new AssertionError(e);
            }
        });

        // The RepositoryMonitor posts updates to hasFoundNewRemoteChanges to the FX thread, so need to run
        // this separately to be able to observe changes
        assertFalse(RepositoryMonitor.hasFoundNewRemoteChanges.get());

        // Update the file, then commit and push
        Path readmePath = repoPathPush.resolve("README.md");
        System.out.println(readmePath);
        String timestamp = (new Date()).toString() + "\n";
        Files.write(readmePath, timestamp.getBytes(), StandardOpenOption.APPEND);
        helperPush.addFilePathTest(readmePath);
        helperPush.commit("added a character");
        PushCommand command = helperPush.prepareToPushAll();
        helperPush.pushAll(command);

        // Verify that RepositoryMonitor can see the changes relative to the original
        Thread.sleep(REMOTE_CHECK_INTERVAL);
        assertTrue(RepositoryMonitor.hasFoundNewRemoteChanges.get());

        // Add a tag named for the current timestamp
        ObjectId headId = helperPush.getBranchModel().getCurrentBranch().getHeadId();
        String tagName = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmSSS"));
        assertFalse(helperPush.getCommit(headId).hasTag(tagName));
        helperPush.getTagModel().tag(tagName, headId.name());
        assertTrue(helperPush.getCommit(headId).hasTag(tagName));
        helperPush.pushTags();

        // Remove the tag we just added
        helperPush.getTagModel().deleteTag(tagName);
        assertFalse(helperPush.getCommit(headId).hasTag(tagName));
        command = helperPush.prepareToPushAll();
        command.setRemote("origin").add(":refs/tags/" + tagName);
        helperPush.pushAll(command);

        // Now do the pull (well, a fetch)
        helperPull.fetch(false);
        helperPull.mergeFromFetch();

        // Update lists of branches
        helperPull.getBranchModel().updateAllBranches();
        System.out.println("done");
    }




}
