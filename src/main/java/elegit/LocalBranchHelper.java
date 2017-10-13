package elegit;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;

import java.io.IOException;
import java.util.Arrays;

/**
 * An implementation of the abstract BranchHelper that holds
 * and interacts with local branches.
 */
public class LocalBranchHelper extends BranchHelper {
    public LocalBranchHelper(String refPathString, RepoHelper repo) throws IOException {
        super(refPathString, repo);
    }

    public LocalBranchHelper(Ref branchRef, RepoHelper repo) throws IOException {
        this(branchRef.getName(),  repo);
    }

    @Override
    /**
     * Parses the branch's refPath in order to get its name.
     */
    protected String parseBranchName() {
        String[] slashSplit = this.refPathString.split("/");
        if (slashSplit.length >= 2) {

            /*
            Local branches are stored in the .git directory like this:
            `refs/heads/BRANCH_NAME`.

            For example:
            `refs/heads/master`.
    (index): 0    1     2

            We want to cut out the `refs/remotes/origin/` part to get at the branch name.
            This means cutting the first two parts of the array, split at the '/' char.
            */

            String[] removedFirstTwoDirectoriesInPath = Arrays.copyOfRange(slashSplit, 2, slashSplit.length);

            // Now rejoin at the '/' key, which we split at earlier (in case there is a slash in the branch
            //   name or something):
            String branchName = String.join("/", removedFirstTwoDirectoriesInPath);
            return branchName;

        } else {

            /*
            However, if we're getting a ref that's not nested in the directory, like HEAD or FETCH_HEAD,
            we just want to return the original string.
             */

            return this.refPathString;

        }
    }

    @Override
    /**
     * Checks out the branch in git.
     */
    public void checkoutBranch() throws GitAPIException, IOException {
        new Git(this.repoHelper.getRepo()).checkout().setName(getRefName()).call();
        this.repoHelper.getBranchModel().refreshCurrentBranch();
    }
}
