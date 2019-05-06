package elegit;

import elegit.models.AuthMethod;
import elegit.models.ExistingRepoHelper;
import elegit.models.LocalBranchHelper;
import elegit.models.SessionModel;
import elegit.sshauthentication.ElegitUserInfoTest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PushCommand;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import sharedrules.TestUtilities;
import sharedrules.TestingLogPathRule;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.fail;

/**
 * Created by dmusican on 2/24/16.
 */
public class ExistingRepoHelperTest {

    @ClassRule
    public static final TestingLogPathRule testingLogPath = new TestingLogPathRule();

    private Path directoryPath;

    private static final Logger logger = LogManager.getLogger("consolelogger");

    @Before
    public void setUp() throws Exception {
        TestUtilities.setupTestEnvironment();
        logger.info("Unit test started");
        directoryPath = Files.createTempDirectory("unitTestRepos");
        directoryPath.toFile().deleteOnExit();
    }

    @After
    public void tearDown()  {
        TestUtilities.cleanupTestEnvironment();
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
        ExistingRepoHelper repoHelper = new ExistingRepoHelper(Paths.get(localPath.getAbsolutePath()),
                new ElegitUserInfoTest());
        git.close();
    }

    @Test
    public void newLocalRepo() throws Exception {
        logger.info("Temp directory: " + directoryPath);
        Path remote = directoryPath.resolve("remote");
        Path local = directoryPath.resolve("local");
        Git.init().setDirectory(remote.toFile()).setBare(true).call();
        Git.cloneRepository().setDirectory(local.toFile()).setURI("file://"+remote).call();

        ExistingRepoHelper helper = new ExistingRepoHelper(local, new ElegitUserInfoTest());

        Path fileLocation = local.resolve("README.md");

        FileWriter fw = new FileWriter(fileLocation.toString(), true);
        fw.write("start");
        fw.close();
        helper.addFilePathTest(fileLocation);
        helper.commit("Appended to file");

        for (int i=0; i < 100; i++) {
            LocalBranchHelper branchHelper = helper.getBranchModel().createNewLocalBranch("branch" + i);
            branchHelper.checkoutBranch();
            fw = new FileWriter(fileLocation.toString(), true);
            fw.write(""+i);
            fw.close();
            helper.addFilePathTest(fileLocation);
            helper.commit("Appended to file");
        }

        // Just push all untracked local branches
        PushCommand command = helper.prepareToPushAll(untrackedLocalBranches -> untrackedLocalBranches);
        helper.pushAll(command);

        logger.info(remote);
        logger.info(local);
    }
}
