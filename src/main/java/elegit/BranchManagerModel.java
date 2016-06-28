package elegit;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Model class that keeps track of all BranchModels for a RepoHelper
 *
 * Also implements git stuff that concerns multiple branches
 *      -adding, removing
 *      -tracking, untracking
 *      -merging
 *      -checkouts
 */

public class BranchManagerModel {

    private RepoHelper repoHelper;
    private BranchHelper currentBranch;
    private List<LocalBranchHelper> localBranchesTyped;
    private List<RemoteBranchHelper> remoteBranchesTyped;

    public BranchManagerModel(RepoHelper repoHelper) {
        this.repoHelper = repoHelper;
    }

    /**
     * Checks out the specified branch, updating the branch model to reflect that checkout
     * @param helper the branch helper to checkout
     * @throws GitAPIException
     */
    public void checkoutBranch(BranchHelper helper) throws GitAPIException {
        new Git(this.repoHelper.getRepo()).checkout().setName(helper.getBranchName()).call();
        currentBranch = helper;
    }

    /**
     * Utilizes JGit to get a list of all local branches and refills
     * the model's list of local branches
     *
     * @throws GitAPIException
     * @throws IOException
     */
    public void updateLocalBranches() throws GitAPIException, IOException {
        List<Ref> getBranchesCall = new Git(this.repoHelper.getRepo()).branchList().call();

        this.localBranchesTyped = new ArrayList<>();
        for (Ref ref : getBranchesCall) {
            this.localBranchesTyped.add(new LocalBranchHelper(ref, this.repoHelper));
        }
    }

    /**
     * Utilizes JGit to get a list of all remote branches and refills
     * the model's list of remote branches
     *
     * @throws GitAPIException
     */
    public void updateRemoteBranches() throws GitAPIException, IOException {
        List<Ref> getBranchesCall = new Git(this.repoHelper.getRepo())
                .branchList()
                .setListMode(ListBranchCommand.ListMode.REMOTE)
                .call();

        // Rebuild the remote branches list from scratch.
        this.remoteBranchesTyped = new ArrayList<>();
        for (Ref ref : getBranchesCall) {
            // Listing the remote branches also grabs HEAD, which isn't a branch we want
            if (!ref.getName().equals("HEAD")) {
                this.remoteBranchesTyped.add(new RemoteBranchHelper(ref, this.repoHelper));
            }
        }
    }

    // ************************* GETTERS AND SETTERS **************************

    /**
     * Getter for the current branch in the model
     * @return the branch helper for the current branch
     */
    public BranchHelper getCurrentBranch() { return this.currentBranch; }

    /**
     * Getter for list of branches
     * @param type the type of list of branches to return
     * @return
     */
    public List<? extends BranchHelper> getBranchListTyped(BranchType type) {
        switch (type) {
            case LOCAL:
        }
        return type==BranchType.LOCAL ? this.localBranchesTyped : this.remoteBranchesTyped;
    }

    public BranchHelper getBranchByName(BranchType type, String branchName) {
        List<? extends BranchHelper> branchList = type==BranchType.LOCAL ? this.localBranchesTyped : this.remoteBranchesTyped;
        for (BranchHelper helper: branchList) {
            if (helper.getBranchName().equals(branchName))
                return helper;
        }
    }


    /**
     * Type for branches, can be local or remote
     */
    public enum BranchType {
        LOCAL, REMOTE, ALL;
    }


}
