package elegitfx.conflictManagementFXTests;

import elegit.exceptions.ExceptionAdapter;
import elegit.exceptions.MissingRepoException;
import elegit.models.BranchHelper;
import elegit.models.ExistingRepoHelper;
import elegit.models.LocalBranchHelper;
import elegit.models.SessionModel;
import elegit.sshauthentication.ElegitUserInfoTest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.errors.GitAPIException;
import sharedrules.TestingRemoteAndLocalReposRule;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Created by grenche on 7/2/18.
 * Methods that are commonly used in testing the conflict management tool go here
 * (e.g. setting up a repo with conflicting files)
 */
public class ConflictManagementUtilities {

    private static final Logger console = LogManager.getLogger("briefconsolelogger");

    public Path createSimpleConflictingLocalRepo(TestingRemoteAndLocalReposRule testingRemoteAndLocalRepos){
        try {
            Path local = testingRemoteAndLocalRepos.getLocalFull();
            Git.init().setDirectory(local.toFile()).setBare(false).call();

            ExistingRepoHelper helper = new ExistingRepoHelper(local, new ElegitUserInfoTest());
            SessionModel.getSessionModel().openRepoFromHelper(helper);
            Path fileLocation = local.resolve("test.txt");

            FileWriter fw = new FileWriter(fileLocation.toString(), true);
            fw.write("start");
            for (int i = 0; i < 100; i++) {
                fw.write("This is a line that was added at the beginning\n");
            }
            fw.close();
            helper.addFilePathTest(fileLocation);
            helper.commit("Appended initial text");
            BranchHelper baseBranch = helper.getBranchModel().getCurrentBranch();
            LocalBranchHelper mergeBranch = helper.getBranchModel().createNewLocalBranch("mergeBranch");
            fw = new FileWriter(fileLocation.toString(), true);
            fw.write("added in master");
            fw.close();
            helper.addFilePathTest(fileLocation);
            helper.commit("Appended something in master");
            mergeBranch.checkoutBranch();
            fw = new FileWriter(fileLocation.toString(), true);
            fw.write("added in mergeBranch");
            fw.close();
            helper.addFilePathTest(fileLocation);
            helper.commit("Appended something else in mergeBranch");
            baseBranch.checkoutBranch();
            System.out.println(SessionModel.getSessionModel().getCurrentRepoHelper());
            System.out.println(SessionModel.getSessionModel().getCurrentRepoHelper().getRepo());
            MergeResult mergeResult = helper.getBranchModel().mergeWithBranch(mergeBranch);
            assert(mergeResult.getMergeStatus().equals(MergeResult.MergeStatus.CONFLICTING));
            return local;
        } catch (Exception e){
            throw new ExceptionAdapter(e);
        }
    }

    public Path createMultipleConflicts(TestingRemoteAndLocalReposRule testingRemoteAndLocalRepos, boolean containsNonConflictingChanges) {
        try {
            Path local = testingRemoteAndLocalRepos.getLocalFull();
            Git.init().setDirectory(local.toFile()).setBare(false).call();

            ExistingRepoHelper helper = new ExistingRepoHelper(local, new ElegitUserInfoTest());
            SessionModel.getSessionModel().openRepoFromHelper(helper);
            Path fileLocation = local.resolve("test.txt");

            FileWriter fw = new FileWriter(fileLocation.toString(), true);
            for (int i = 0; i < 100; i++) {
                fw.write("This is a line that was added at the beginning\n");
            }
            fw.close();
            helper.addFilePathTest(fileLocation);
            helper.commit("Appended initial text");
            BranchHelper baseBranch = helper.getBranchModel().getCurrentBranch();
            LocalBranchHelper mergeBranch = helper.getBranchModel().createNewLocalBranch("mergeBranch");

            makeChangesToFile(fw, local, helper, fileLocation, "added in master\n", containsNonConflictingChanges);

            mergeBranch.checkoutBranch();

            makeChangesToFile(fw, local, helper, fileLocation, "added in mergeBranch\n", containsNonConflictingChanges);

            baseBranch.checkoutBranch();
            System.out.println(SessionModel.getSessionModel().getCurrentRepoHelper());
            System.out.println(SessionModel.getSessionModel().getCurrentRepoHelper().getRepo());
            MergeResult mergeResult = helper.getBranchModel().mergeWithBranch(mergeBranch);
            assert(mergeResult.getMergeStatus().equals(MergeResult.MergeStatus.CONFLICTING));
            return local;
        } catch (Exception e){
            throw new ExceptionAdapter(e);
        }
    }

    private void makeChangesToFile(FileWriter fw, Path local, ExistingRepoHelper helper, Path fileLocation, String change, boolean notConflictingChanges) throws IOException, GitAPIException, MissingRepoException {
        fileLocation.toFile().delete();

        fw = new FileWriter(fileLocation.toString(), true);

        if (notConflictingChanges) {
            if (change.equals("added in master\n")) {
                fw.write("This is a line that was added in master, but not conflicting\n" +
                        "But it has two lines so that should make the line numbers different\n");
            }
        }
        for (int i = 0; i < 25; i++) {
            fw.write("This is a line that was added at the beginning\n");
        }
        fw.write(change);
        for (int i = 0; i < 25; i++) {
            fw.write("This is a line that was added at the beginning\n");
        }
        fw.write(change);
        for (int i = 0; i < 25; i++) {
            fw.write("This is a line that was added at the beginning\n");
        }
        fw.write(change);
        for (int i = 0; i < 25; i++) {
            fw.write("This is a line that was added at the beginning\n");
        }
        fw.write(change);

        fw.close();
        helper.addFilePathTest(fileLocation);
        helper.commit("Added some things to the file");
    }
}
