package edugit;

import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

/**
 * Created by grahamearley on 6/23/15.
 */
public class RemoteBranchHelper extends BranchHelper {
    public RemoteBranchHelper(String refPathString, Repository repo) {
        super(refPathString, repo);
    }

    public RemoteBranchHelper(Ref branchRef, Repository repo) {
        super(branchRef, repo);
    }

    @Override
    public String getBranchName() {
        String[] slashSplit = this.refPathString.split("/");

        /*
        Branches in the remote are stored in the .git directory like this:
        `/refs/remotes/REMOTE_NAME/BRANCH_NAME`.

        For example:
        `refs/remotes/origin/master`.
(index): 0    1       2      3

        If possible, we want to cut out the `refs/remotes/origin/` part to get at the branch name.
        This means cutting the first three parts of the array, split at the '/' char.
        */

        String[] removedFirstThreeDirectoriesInPath = Arrays.copyOfRange(slashSplit, 3, slashSplit.length);

        // Now rejoin at the '/' key, which we split at earlier (in case there is a slash in the branch
        //   name or something):
        String branchName = String.join("/", removedFirstThreeDirectoriesInPath);

        return branchName;
    }

    @Override
    public void checkoutBranch() throws GitAPIException {
        new Git(this.repo).checkout().
                setCreateBranch(true).
                setName(this.branchName).
                setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK).
                setStartPoint(this.refPathString).
                call();
    }

    @Override
    public String toString() {
        return "REMOTE:" + super.toString();
    }
}
