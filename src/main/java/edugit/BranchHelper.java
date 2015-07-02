package main.java.edugit;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;

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
    protected Repository repo;
    protected String branchName;

    public BranchHelper(String refPathString, Repository repo) {
        this.refPathString = refPathString;
        this.repo = repo;
        this.branchName = this.getBranchName();
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
}
