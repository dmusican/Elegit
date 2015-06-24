package edugit;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;

import java.io.IOException;

/**
 * Created by grahamearley on 6/23/15.
 */
public abstract class BranchHelper {

    String refPathString;
    Repository repo;
    String branchName;

    LocalBranchHelper trackingBranch;

    public BranchHelper(String refPathString, Repository repo) {
        this.refPathString = refPathString;
        this.repo = repo;
        this.branchName = this.getBranchName();
    }

    public BranchHelper(Ref branchRef, Repository repo) {
        this(branchRef.getName(), repo);
    }

    public abstract String getBranchName();

    public abstract void checkoutBranch() throws GitAPIException, IOException;

    @Override
    public String toString() {
        return this.branchName;
    }

    public String getRefPathString() {
        return refPathString;
    }

    public LocalBranchHelper getTrackingBranch() {
        return trackingBranch;
    }
}
