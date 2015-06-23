package edugit;

import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;

/**
 * Created by grahamearley on 6/23/15.
 */
public abstract class BranchHelper {

    String refPathString;
    Repository repo;

    public BranchHelper(String refPathString, Repository repo) {
        this.refPathString = refPathString;
        this.repo = repo;
    }

    public abstract String getBranchName();

    public abstract void checkoutBranch() throws GitAPIException;
}
