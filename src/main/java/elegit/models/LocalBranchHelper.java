package elegit.models;

import net.jcip.annotations.ThreadSafe;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;

import java.io.IOException;
import java.util.Arrays;

/**
 * An implementation of the abstract BranchHelper that holds
 * and interacts with local branches.
 */
@ThreadSafe
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
    public String parseBranchName() {
        String[] slashSplit = this.getRefPathString().split("/");
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

            return this.getRefPathString();

        }
    }

    @Override
    /**
     * Checks out the branch in git.
     */
    // TODO: Make sure all Git operations are threadsafe
    // TODO: This code is in a strange location, should be near other git operations
    public void checkoutBranch() throws GitAPIException, IOException {
        new Git(this.repoHelper.getRepo()).checkout().setName(getRefName()).call();
        TranscriptHelper.post("git checkout "+this.getRefName());
        this.repoHelper.getBranchModel().refreshCurrentBranch();
    }
}
