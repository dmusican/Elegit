package elegit;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;

import java.io.IOException;

/**
 *
 * An abstract class that holds and interacts with a git branch ref.
 *
 * It is implemented by LocalBranchHelper and RemoteBranchHelper.
 *
 */
public abstract class BranchHelper {

    // Full string representation of this branch, e.g. 'remotes/origin/master'
    protected String refPathString;
    // The commit that is the current head of this branch
    protected CommitHelper branchHead;
    // The repository this branch is a part of
    protected RepoHelper repoHelper;
    // The name of this branch, e.g. 'master'
    protected String branchName;

    /**
     * Creates a new BranchHelper for the given reference and repository.
     * Pulls the head from the given repository.
     * @param refPathString the full string representation of this branch
     * @param repoHelper the repository this branch is in
     */
    public BranchHelper(String refPathString, RepoHelper repoHelper) {
        this.refPathString = refPathString;
        this.setHead(repoHelper.getCommit(refPathString));
        this.repoHelper = repoHelper;
        this.branchName = this.getBranchName();
    }

    /**
     * @return the name of this branch, e.g. 'master'
     */
    public abstract String getBranchName();

    /**
     * Checks out this branch in the stored repository. Equivalent to
     * 'git checkout [branchName]
     * @throws GitAPIException
     * @throws IOException
     */
    public abstract void checkoutBranch() throws GitAPIException, IOException;

    /**
     * @return the commit that is the head of this branch, or null if it hasn't been set
     */
    public CommitHelper getHead(){
        return branchHead;
    }

    /**
     * Sets the head of this branch.
     * @param head the new head
     */
    private void setHead(CommitHelper head){
        if(branchHead != null) branchHead.removeAsHead(this);
        this.branchHead = head;
        if(branchHead != null) branchHead.setAsHead(this);
    }

    @Override
    public String toString() {
        return this.branchName;
    }

    /**
     * @return Full string representation of this branch, e.g. 'remotes/origin/master'
     */
    public String getRefPathString() {
        return refPathString;
    }

    /**
     * Gets the ID of this branch. If the head is non-null, uses branchHead's
     * ID. Otherwise, attempts to resolve its own reference string in the
     * stored repository and return the resolved ID
     * @return the id of this branch's head
     * @throws IOException
     */
    public ObjectId getHeadId() throws IOException{
        if(branchHead != null){
            return branchHead.getObjectId();
        }else{
            ObjectId headId = repoHelper.getRepo().resolve(refPathString);
            setHead(repoHelper.getCommit(headId));
            return headId;
        }
    }
}
