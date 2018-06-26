package elegitfx;

import elegit.controllers.BusyWindow;
import elegit.controllers.SessionController;
import elegit.exceptions.ExceptionAdapter;
import elegit.models.BranchModel;
import elegit.models.ExistingRepoHelper;
import elegit.models.LocalBranchHelper;
import elegit.sshauthentication.ElegitUserInfoTest;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.Rule;
import org.junit.Test;

import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import elegit.models.BranchHelper;
import org.eclipse.jgit.api.MergeCommand;
import org.eclipse.jgit.api.MergeResult;
import org.testfx.framework.junit.TestFXRule;
import org.testfx.util.WaitForAsyncUtils;
import org.testfx.framework.junit.ApplicationTest;
import sharedrules.TestUtilities;
import sharedrules.TestingRemoteAndLocalReposRule;


/**
 * Created by gorram on 6/26/18.
 */
public class ConflictResolutionFXTest extends ApplicationTest{
    private static final Logger logger = LogManager.getLogger("consolelogger");
    private Path directoryPath;
    private SessionController sessionController;


    private Path createConflictingLocalRepo(){
        try {
            Path local = testingRemoteAndLocalRepos.getLocalFull();
            Git.init().setDirectory(local.toFile()).setBare(false).call();
            //Git.init().setDirectory(remote.toFile()).setBare(true).call();
            //Git.cloneRepository().setDirectory(local.toFile()).setURI("file://"+remote).call();

            ExistingRepoHelper helper = new ExistingRepoHelper(local, new ElegitUserInfoTest());
            Path fileLocation = local.resolve("test.txt");

            FileWriter fw = new FileWriter(fileLocation.toString(), true);
            fw.write("start");
            fw.close();
            helper.addFilePathTest(fileLocation);
            helper.commit("Appended to file");
            BranchHelper baseBranch = helper.getBranchModel().getCurrentBranch();
            LocalBranchHelper mergeBranch = helper.getBranchModel().createNewLocalBranch("mergeBranch");
            fw = new FileWriter(fileLocation.toString(), true);
            fw.write("added in master");
            fw.close();
            helper.addFilePathTest(fileLocation);
            helper.commit("Appended to file");
            mergeBranch.checkoutBranch();
            fw = new FileWriter(fileLocation.toString(), true);
            fw.write("added in mergeBranch");
            fw.close();
            helper.addFilePathTest(fileLocation);
            helper.commit("Appended to file");
            baseBranch.checkoutBranch();
            MergeResult mergeResult = helper.getBranchModel().mergeWithBranch(mergeBranch);
            //System.out.println(mergeResult.getMergeStatus());
            assert(mergeResult.getMergeStatus().equals(MergeResult.MergeStatus.CONFLICTING));
            return local;
        } catch (Exception e){
            throw new ExceptionAdapter(e);
        }
    }

    @Rule
    public TestFXRule testFXRule = new TestFXRule();

    @Rule
    public final TestingRemoteAndLocalReposRule testingRemoteAndLocalRepos =
            new TestingRemoteAndLocalReposRule(false);

    @Override
    public void start(Stage stage) throws Exception {
        sessionController = TestUtilities.commonTestFxStart(stage);
    }

    @Test
    public void testResolveConflicts(){
        try {
            Path local = createConflictingLocalRepo();
            interact(() -> sessionController.handleLoadExistingRepoOption(local));
            WaitForAsyncUtils.waitFor(10, TimeUnit.SECONDS,
                    () -> lookup("test.txt").queryAll().size() == 2);
            //WaitForAsyncUtils.waitFor(10, TimeUnit.SECONDS,
            //        () -> !);
            rightClickOn("test.txt").clickOn("Resolve conflict...");
            //clickOn();
        } catch (Exception e){
            throw new ExceptionAdapter(e);
        }
    }
}
