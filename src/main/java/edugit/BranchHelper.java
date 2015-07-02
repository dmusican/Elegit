package main.java.edugit;

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

    protected String refPathString;
    protected CommitHelper branchHead;
    protected RepoHelper repoHelper;
    protected String branchName;

    public BranchHelper(String refPathString, RepoHelper repoHelper) {
        this.refPathString = refPathString;
        this.branchHead = repoHelper.getCommit(refPathString);
        this.repoHelper = repoHelper;
        this.branchName = this.getBranchName();
    }

    public abstract String getBranchName();

    public abstract void checkoutBranch() throws GitAPIException, IOException;

    public CommitHelper getHead(){
        return branchHead;
    }

    @Override
    public String toString() {
        return this.branchName;
    }

    public String getRefPathString() {
        return refPathString;
    }

    public ObjectId getHeadID() throws IOException{
        if(branchHead != null){
            return branchHead.getObjectId();
        }else{
            return repoHelper.getRepo().resolve(refPathString);
        }
    }
}
