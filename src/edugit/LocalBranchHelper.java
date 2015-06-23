package edugit;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;

import java.util.Arrays;

/**
 * Created by grahamearley on 6/23/15.
 */
public class LocalBranchHelper extends BranchHelper {
    public LocalBranchHelper(String refPathString, Repository repo) {
        super(refPathString, repo);
    }

    @Override
    public String getBranchName() {
        String[] slashSplit = this.refPathString.split("/");

        /*
        Local branches are stored in the .git directory like this:
        `/refs/heads/BRANCH_NAME`.

        For example:
        `/refs/heads/master`.
 (index): 0    1     2

        We want to cut out the `refs/remotes/origin/` part to get at the branch name.
        This means cutting the first two parts of the array, split at the '/' char.
        */

        String[] removedFirstThreeDirectoriesInPath = Arrays.copyOfRange(slashSplit, 2, slashSplit.length);

        // Now rejoin at the '/' key, which we split at earlier (in case there is a slash in the branch
        //   name or something):
        String branchName = String.join("/", removedFirstThreeDirectoriesInPath);

        return branchName;
    }

    @Override
    public void checkoutBranch() throws GitAPIException {
        new Git(this.repo).checkout().setName(this.getBranchName()).call();
    }
}
