package elegit;

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
public abstract class BranchHelper extends RefHelper {

    // Full string representation of this branch, e.g. 'remotes/origin/master'
    String refPathString;
    // The repository this branch is a part of
    public RepoHelper repoHelper;

    /**
     * Creates a new BranchHelper for the given reference and repository.
     * Pulls the head from the given repository.
     * @param refPathString the full string representation of this branch
     * @param repoHelper the repository this branch is in
     */
    public BranchHelper(String refPathString, RepoHelper repoHelper) {
        this.refPathString = refPathString;
        this.repoHelper = repoHelper;
        this.setHead(this.repoHelper.getCommit(refPathString));
        this.refName = this.parseBranchName();
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
    public abstract void checkoutBranch() throws GitAPIException, IOException;

    /**
     * Sets the head of this branch.
     * @param head the new head
     */
    private void setHead(CommitHelper head){
        commit = head;
    }

    @Override
    public String toString() {
        return this.refName;
    }

    /**
     * @return Full string representation of this branch, e.g. 'remotes/origin/master'
     */
    String getRefPathString() {
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
        if(commit != null){
            return commit.getObjectId();
        }else{
            ObjectId headId = repoHelper.getRepo().resolve(refPathString);
            setHead(repoHelper.getCommit(headId));
            return headId;
        }
    }

    public BranchTrackingStatus getStatus() throws IOException {
        return BranchTrackingStatus.of(this.repoHelper.repo, this.refName);
    }
}
