package main.java.edugit;

import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;

import java.io.IOException;
import java.util.Arrays;

/**
 * Created by grahamearley on 6/23/15.
 */
public class RemoteBranchHelper extends BranchHelper {

//    /// Class level stuff:
//    private static ArrayList<RemoteBranchHelper> remoteBranchHelpers = new ArrayList<>();
//
//    private static void storeRemoteBranchHelper(RemoteBranchHelper helperCandidate) {
//        boolean candidateNotStoredYet = true;
//        for (RemoteBranchHelper remoteBranchHelper : remoteBranchHelpers) {
//            if (remoteBranchHelper.getRefPathString().equals(helperCandidate.getRefPathString())) {
//                // The candidate is already in the list
//                candidateNotStoredYet = false;
//                break;
//            }
//        }
//        if (candidateNotStoredYet) remoteBranchHelpers.add(helperCandidate);
//    }
//
//    public static RemoteBranchHelper getRemoteBranchHelperByRefPath(String refPath, Repository repo) {
//        for (RemoteBranchHelper remoteBranchHelper : remoteBranchHelpers) {
//            if (remoteBranchHelper.getRefPathString().equals(refPath)) {
//                return remoteBranchHelper;
//            }
//        }
//        return new RemoteBranchHelper(refPath, repo);
//    }
//
//    public static RemoteBranchHelper getRemoteBranchHelperByRef(Ref ref, Repository repo) {
//        return getRemoteBranchHelperByRefPath(ref.getName(), repo);
//    }

    /// Instance level:

    public RemoteBranchHelper(String refPathString, Repository repo) {
        super(refPathString, repo);
//        RemoteBranchHelper.storeRemoteBranchHelper(this);
    }

    public RemoteBranchHelper(Ref ref, Repository repo) {
        super(ref.getName(), repo);
    }

    @Override
    public String getBranchName() {
        String[] slashSplit = this.refPathString.split("/");

        /*
        Branches in the remote are stored in the .git directory like this:
        `/refs/remotes/REMOTE_NAME/BRANCH_NAME`.

        For example:
        `refs/remotes/origin/master`.
(index): 0    1       2      3

        If possible, we want to cut out the `refs/remotes/origin/` part to get at the branch name.
        This means cutting the first three parts of the array, split at the '/' char.
        */

        String[] removedFirstThreeDirectoriesInPath = Arrays.copyOfRange(slashSplit, 3, slashSplit.length);

        // Now rejoin at the '/' key, which we split at earlier (in case there is a slash in the branch
        //   name or something):
        String branchName = String.join("/", removedFirstThreeDirectoriesInPath);

        return branchName;
    }

    @Override
    public void checkoutBranch() throws GitAPIException, IOException {

        if (this.trackingBranch != null) {
            this.trackingBranch.checkoutBranch();
        } else {
            Ref trackingBranchRef = new Git(this.repo).checkout().
                    setCreateBranch(true).
                    setName(this.branchName).
                    setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK).
                    setStartPoint(this.refPathString).
                    call();
            LocalBranchHelper trackingBranch = new LocalBranchHelper(trackingBranchRef, this.repo);
            this.setTrackingBranch(trackingBranch);
        }
    }

    @Override
    public String toString() {
        return "REMOTE: " + super.toString();
    }

    public void setTrackingBranch(LocalBranchHelper trackingBranch){
        this.trackingBranch = trackingBranch;
    }
}
