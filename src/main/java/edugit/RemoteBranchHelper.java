package main.java.edugit;

import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;

import java.io.IOException;
import java.util.Arrays;

/**
 * An implementation of the abstract BranchHelper class that holds
 * and interacts with remote branches.
 */
public class RemoteBranchHelper extends BranchHelper {
    public RemoteBranchHelper(Ref ref, Repository repo) {
        super(ref.getName(), repo);
    }

    @Override
    /**
     * Parses the branch's refPath in order to get its name.
     */
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
    public void checkoutBranch() throws GitAPIException, IOException {
         // Don't check out remote branches!
    }
}
