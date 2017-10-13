package elegit;

import elegit.models.CommitHelper;
import org.apache.http.annotation.ThreadSafe;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.BranchTrackingStatus;
import org.eclipse.jgit.lib.ObjectId;

import java.io.IOException;

/**
 *
 * An abstract class that holds and interacts with a git branch ref.
 *
 * It is implemented by LocalBranchHelper and RemoteBranchHelper.
 *
 */
@ThreadSafe
public abstract class BranchHelper extends RefHelper {
    // THe name of this ref, e.g. 'master' or 'tag1'
    private final String refName;

    // Full string representation of this branch, e.g. 'remotes/origin/master'
    private final String refPathString;

    // The repository this branch is a part of
    protected final RepoHelper repoHelper;

    /**
     * Creates a new BranchHelper for the given reference and repository.
     * Pulls the head from the given repository.
     * @param refPathString the full string representation of this branch
     * @param repoHelper the repository this branch is in
     */
    public BranchHelper(String refPathString, RepoHelper repoHelper) {
        this.refPathString = refPathString;
        this.repoHelper = repoHelper;
        commit.set(this.repoHelper.getCommit(refPathString));
        this.refName = this.parseBranchName();
    }

    /**
     * @return the name of the ref
     */
    @Override
    public String getRefName() {
        return this.refName;
    }


    /**
     * @return the name of this branch, e.g. 'master'
     */
    protected abstract String parseBranchName();

    /**
     * Checks out this branch in the stored repository. Equivalent to
     * 'git checkout [refName]
     * @throws GitAPIException
     * @throws IOException
     */
    // TODO: This code is in a strange location, should be near other git operations
    public abstract void checkoutBranch() throws GitAPIException, IOException;

    @Override
    public String toString() {
        return this.refName;
    }

    /**
     * @return Full string representation of this branch, e.g. 'remotes/origin/master'
     */
    public String getRefPathString() {
        return refPathString;
    }

    /**
     * Gets the ID of this branch. If the head is non-null, uses commit's
     * ID. Otherwise, attempts to resolve its own reference string in the
     * stored repository and return the resolved ID
     * @return the id of this branch's head
     * @throws IOException
     */
    public ObjectId getHeadId() throws IOException{
        // Below looks like a compare and set threading bug, but it's not a problem if commit gets set more than once.
        // The key issue is to make sure that it isn't dereferenced if it is null
        if(commit.get() != null){
            return commit.get().getObjectId();
        }else{
            ObjectId headId = repoHelper.getRepo().resolve(refPathString);
            commit.set(repoHelper.getCommit(headId));
            return headId;
        }
    }

    // TODO: Verify jgit thread safety
    public BranchTrackingStatus getStatus() throws IOException {
        return BranchTrackingStatus.of(this.repoHelper.repo, this.refName);
    }
}
