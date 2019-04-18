package elegitfx;

import elegit.Main;
import javafx.stage.Stage;
import org.junit.After;
import org.testfx.framework.junit.TestFXRule;
import org.testfx.util.WaitForAsyncUtils;
import sharedrules.TestUtilities;
import sharedrules.TestingLogPathRule;
import elegit.models.ClonedRepoHelper;
import elegit.models.SessionModel;
import elegit.monitors.RepositoryMonitor;
import javafx.application.Platform;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.testfx.framework.junit.ApplicationTest;
import sharedrules.JGitTestingRepositoryRule;
import sharedrules.TestingRemoteAndLocalReposRule;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static elegit.monitors.RepositoryMonitor.REMOTE_CHECK_INTERVAL;
import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by dmusican. Enjoy!
 */
public class PushPullFXTest extends ApplicationTest {


    @ClassRule
    public static final TestingLogPathRule testingLogPath = new TestingLogPathRule();

    @Rule
    public TestFXRule testFXRule = new TestFXRule();

    @Rule
    public final TestingRemoteAndLocalReposRule testingRemoteAndLocalRepos =
            new TestingRemoteAndLocalReposRule(false);

    @Rule
    public final JGitTestingRepositoryRule jGitTestingRepositoryRule = new JGitTestingRepositoryRule();

    @Override
    public void start(Stage stage) throws Exception {
        TestUtilities.commonTestFxStart(stage);
    }

    @After
    public void tearDown() {
        TestUtilities.cleanupTestFXEnvironment();
        assertEquals(0,Main.getAssertionCount());
    }

    @Test
    // This test has some thread safety issues that should probably be fixed; the RepositoryMonitor is started
    // in the FX thread (which is right), but there's a lot of code here in this test not run in the FX thread
    // that probably should be. Fix it if this test starts causing trouble. In the meantime... it's a test.
    public void testPushPullBothCloned() throws Exception {
        TestUtilities.commonStartupOffFXThread();

        UsernamePasswordCredentialsProvider credentials = new UsernamePasswordCredentialsProvider("agitter",
                                                                                                  "letmein");

        Path directoryPath = testingRemoteAndLocalRepos.getDirectoryPath();

        String authURI = jGitTestingRepositoryRule.getAuthURI().toString();

        // Repo that will push
        Path repoPathPush = directoryPath.resolve("pushpull1");
        ClonedRepoHelper helperPush = new ClonedRepoHelper(repoPathPush, credentials);
        assertNotNull(helperPush);
        helperPush.obtainRepository(authURI);

        // Repo that will pull
        Path repoPathPull = directoryPath.resolve("pushpull2");
        ClonedRepoHelper helperPull = new ClonedRepoHelper(repoPathPull, credentials);
        assertNotNull(helperPull);
        helperPull.obtainRepository(authURI);

        Platform.runLater(() -> {
            try {
                // Create a session model for helperPull so can later verify if changes have been seen
                SessionModel model = SessionModel.getSessionModel();
                model.openRepoFromHelper(helperPull);
                helperPull.setRemoteStatusChecking(true);
                RepositoryMonitor.initRemote();
            } catch (Exception e) {
                throw new AssertionError(e);
            }
        });

        // The RepositoryMonitor posts updates to hasFoundNewRemoteChanges to the FX thread, so need to run
        // this separately to be able to observe changes
        interact( () -> assertFalse(RepositoryMonitor.hasFoundNewRemoteChanges.get()));

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
        // If system is overloaded, the monitor may not trigger right on time, so wait
        // an extra while (hence the *3).
        WaitForAsyncUtils.waitFor(REMOTE_CHECK_INTERVAL*3, TimeUnit.SECONDS,
                                  () -> RepositoryMonitor.hasFoundNewRemoteChanges.get());

        // Add a tag named for the current timestamp
        ObjectId headId = helperPush.getBranchModel().getCurrentBranch().getHeadId();
        String tagName = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmSSS"));
        assertFalse(helperPush.getCommit(headId).hasTag(tagName));
        helperPush.getTagModel().tag(tagName, headId.name());
        assertTrue(helperPush.getCommit(headId).hasTag(tagName));
        command = helperPush.prepareToPushTags(true);
        helperPush.pushTags(command);

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
