package elegitfx.conflictResolutionFXTests;

import elegit.exceptions.ExceptionAdapter;
import elegit.models.BranchHelper;
import elegit.models.ExistingRepoHelper;
import elegit.models.LocalBranchHelper;
import elegit.models.SessionModel;
import elegit.sshauthentication.ElegitUserInfoTest;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeResult;
import org.junit.Rule;
import org.testfx.framework.junit.TestFXRule;
import sharedrules.TestingRemoteAndLocalReposRule;

import java.io.FileWriter;
import java.nio.file.Path;

/**
 * Created by grenche on 7/2/18.
 * Methods that are commonly used in testing the conflict management tool go here
 * (e.g. setting up a repo with conflicting files)
 */
public class ConflictResolutionUtilities {
    @Rule
    public final TestingRemoteAndLocalReposRule testingRemoteAndLocalRepos =
            new TestingRemoteAndLocalReposRule(false);

//    public Path createConflictingLocalRepo(){
//        try {
//            Path local = testingRemoteAndLocalRepos.getLocalFull();
//            Git.init().setDirectory(local.toFile()).setBare(false).call();
//            //Git.init().setDirectory(remote.toFile()).setBare(true).call();
//            //Git.cloneRepository().setDirectory(local.toFile()).setURI("file://"+remote).call();
//
//            ExistingRepoHelper helper = new ExistingRepoHelper(local, new ElegitUserInfoTest());
//            SessionModel.getSessionModel().openRepoFromHelper(helper);
//            Path fileLocation = local.resolve("test.txt");
//
//            FileWriter fw = new FileWriter(fileLocation.toString(), true);
//            fw.write("start");
//            fw.close();
//            helper.addFilePathTest(fileLocation);
//            helper.commit("Appended to file");
//            BranchHelper baseBranch = helper.getBranchModel().getCurrentBranch();
//            LocalBranchHelper mergeBranch = helper.getBranchModel().createNewLocalBranch("mergeBranch");
//            fw = new FileWriter(fileLocation.toString(), true);
//            fw.write("added in master");
//            fw.close();
//            helper.addFilePathTest(fileLocation);
//            helper.commit("Appended to file");
//            mergeBranch.checkoutBranch();
//            fw = new FileWriter(fileLocation.toString(), true);
//            fw.write("added in mergeBranch");
//            fw.close();
//            helper.addFilePathTest(fileLocation);
//            helper.commit("Appended to file");
//            baseBranch.checkoutBranch();
//            System.out.println(SessionModel.getSessionModel().getCurrentRepoHelper());
//            System.out.println(SessionModel.getSessionModel().getCurrentRepoHelper().getRepo());
//            MergeResult mergeResult = helper.getBranchModel().mergeWithBranch(mergeBranch);
//            //System.out.println(mergeResult.getMergeStatus());
//            assert(mergeResult.getMergeStatus().equals(MergeResult.MergeStatus.CONFLICTING));
//            return local;
//        } catch (Exception e){
//            throw new ExceptionAdapter(e);
//        }
//    }
}
