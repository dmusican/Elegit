package elegit.models;

import elegit.exceptions.ExceptionAdapter;
import elegit.treefx.CellLabel;
import net.jcip.annotations.GuardedBy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.*;
import org.eclipse.jgit.lib.BranchTrackingStatus;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RemoteRefUpdate;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Model class that keeps track of all BranchModels for a RepoHelper
 *
 * Also implements git stuff that concerns multiple branches
 *      -adding, removing
 *      -tracking, untracking
 *      -merging
 *      -checkouts
 */
// TODO: Make sure threadsafe
// This class is now threadsafe from a memory perspective. It does some git operations, so thought needs to be done
// with regards to the working directory, but it's otherwise ok.
public class BranchModel {

    private final RepoHelper repoHelper;
    @GuardedBy("this") private BranchHelper currentBranch;
    @GuardedBy("this") private final List<LocalBranchHelper> localBranchesTyped = new ArrayList<>();
    @GuardedBy("this") private final List<RemoteBranchHelper> remoteBranchesTyped = new ArrayList<>();

    static final Logger logger = LogManager.getLogger();
    private static final Logger console = LogManager.getLogger("briefconsolelogger");

    /**
     * Constructor. Sets the repo helper and updates the local and remote branches
     *
     * @param repoHelper the repohelper to get branches for
     */
    public BranchModel(RepoHelper repoHelper) {
        this.repoHelper = repoHelper;
        this.updateAllBranches();
    }

    /**
     * Updates local and remote branches in the model
     */
    public void updateAllBranches() {
        this.updateLocalBranches();
        this.updateRemoteBranches();
        this.refreshHeadIds();
        this.refreshCurrentBranch();
    }

    /**
     * Utilizes JGit to get a list of all local branches and refills
     * the model's list of local branches
     */
    // synchronized for localBranchesTyped
    public synchronized void updateLocalBranches() {
        try {
            List<Ref> getBranchesCall = new Git(this.repoHelper.getRepo()).branchList().call();

            this.localBranchesTyped.clear();
            for (Ref ref : getBranchesCall) {
                this.localBranchesTyped.add(new LocalBranchHelper(ref, this.repoHelper));
            }
        } catch (Exception e) {
            throw new ExceptionAdapter(e);
        }
    }

    /**
     * Utilizes JGit to get a list of all remote branches and refills
     * the model's list of remote branches
     */
    // synchronized for remoteBranchesTyped
    public synchronized void updateRemoteBranches() {
        try {
            List<Ref> getBranchesCall = new Git(this.repoHelper.getRepo())
                    .branchList()
                    .setListMode(ListBranchCommand.ListMode.REMOTE)
                    .call();

            // Rebuild the remote branches list from scratch.
            this.remoteBranchesTyped.clear();
            for (Ref ref : getBranchesCall) {
                // Listing the remote branches also grabs HEAD, which isn't a branch we want
                if (!ref.getName().equals("HEAD")) {
                    this.remoteBranchesTyped.add(new RemoteBranchHelper(ref, this.repoHelper));
                }
            }
        } catch (Exception e) {
            throw new ExceptionAdapter(e);
        }
    }

    /**
     * Updates the current branch by checking the repository for which
     * branch is currently checked out
     */
    // synchronized for currentBranch safety and localBranchesTyped
    public synchronized void refreshCurrentBranch() {
        try {
            String currentBranchRefString = this.repoHelper.getRepo().getFullBranch();

            for (LocalBranchHelper branch : this.localBranchesTyped) {
                if (branch.getRefPathString().equals(currentBranchRefString)) {
                    this.currentBranch = branch;
                    return;
                }
            }

            this.currentBranch = new LocalBranchHelper(currentBranchRefString, this.repoHelper);
        } catch (Exception e) {
            throw new ExceptionAdapter(e);
        }
    }

    /**
     * Refreshes all the head ids for all local and remote branches
     */
    public void refreshHeadIds() {
        refreshHeadIdsType(BranchType.LOCAL);
        refreshHeadIdsType(BranchType.REMOTE);
    }

    /**
     * Refreshes the head IDs for all the branches of a given type
     * @param type the type of branches to refresh
     */
    // synchronized for localBranchesTyped and remoteBranchesTyped
    public synchronized void refreshHeadIdsType(BranchType type) {
        List<? extends BranchHelper> listToRefresh = (type == BranchType.LOCAL) ? this.localBranchesTyped : this.remoteBranchesTyped;
        for (BranchHelper branch : listToRefresh)
            if (branch.getCommit() == null) {
                try {
                    branch.getHeadId();
                } catch (IOException e) {
                    logger.error("IOException getting local branches");
                    logger.debug(e.getStackTrace());
                    e.printStackTrace();
                }
            }
    }

    /**
     * Creates a local branch and tracks it
     * @param remoteBranchHelper the remote branch to track
     * @return the localBranchHelper that was added
     * @throws RefAlreadyExistsException
     * @throws GitAPIException
     * @throws IOException
     */
    // Synchronized for localBranchesTyped
    public synchronized LocalBranchHelper trackRemoteBranch(RemoteBranchHelper remoteBranchHelper) throws GitAPIException, IOException {
        LocalBranchHelper tracker = this.createLocalTrackingBranchForRemote(remoteBranchHelper);
        this.localBranchesTyped.add(tracker);
        return tracker;
    }

    /**
     * Helper method that creates a local branch tracking a remote branch.
     *
     * @param remoteBranchHelper the remote branch to be tracked.
     * @return the LocalBranchHelper of the local branch tracking the given remote branch.
     * @throws GitAPIException
     * @throws IOException
     */
    // Synchronized for localBranchesTyped
    private synchronized LocalBranchHelper createLocalTrackingBranchForRemote(RemoteBranchHelper remoteBranchHelper) throws GitAPIException, IOException {
        String localBranchName=this.repoHelper.getRepo().shortenRemoteBranchName(remoteBranchHelper.getRefPathString());
        Ref trackingBranchRef = new Git(this.repoHelper.getRepo()).branchCreate().
                setName(localBranchName).
                setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK).
                setStartPoint(remoteBranchHelper.getRefPathString()).
                call();
        LocalBranchHelper newHelper = new LocalBranchHelper(trackingBranchRef, this.repoHelper);
        this.localBranchesTyped.add(newHelper);
        return newHelper;
    }

    /**
     * Creates a new local branch using git.
     *
     * @param branchName the name of the new branch.
     * @return the new local branch's LocalBranchHelper.
     * @throws GitAPIException
     * @throws IOException
     */
    // Synchronized for localBranchesTyped
    public synchronized LocalBranchHelper createNewLocalBranch(String branchName) throws GitAPIException, IOException {
        Git git = new Git(this.repoHelper.getRepo());
        Ref newBranch = git.branchCreate().setName(branchName).call();
        LocalBranchHelper newLocalBranchHelper = new LocalBranchHelper(newBranch, this.repoHelper);
        this.localBranchesTyped.add(newLocalBranchHelper);

        git.close();
        return newLocalBranchHelper;
    }

    /**
     * Deletes a local branch, but will throw exceptions if the branch is not merged
     *
     * @param localBranchToDelete the branch helper of the branch to delete
     * @throws NotMergedException
     * @throws CannotDeleteCurrentBranchException
     * @throws GitAPIException
     */
    // Synchronized for localBranchesTyped
    public synchronized void deleteLocalBranch(LocalBranchHelper localBranchToDelete)
            throws GitAPIException {
        Git git = new Git(this.repoHelper.getRepo());
        git.branchDelete().setBranchNames(localBranchToDelete.getRefPathString()).call();
        this.localBranchesTyped.remove(localBranchToDelete);
        git.close();
    }


    /**
     * Force deletes a branch, even if it is not merged in
     *
     * @param branchToDelete the branch helper of the branch to delete
     */
    // Synchronized for localBranchesTyped
    public synchronized void forceDeleteLocalBranch(LocalBranchHelper branchToDelete) throws CannotDeleteCurrentBranchException, GitAPIException {
        Git git = new Git(this.repoHelper.getRepo());
        git.branchDelete().setForce(true).setBranchNames(branchToDelete.getRefPathString()).call();
        this.localBranchesTyped.remove(branchToDelete);
        git.close();
    }

    /**
     * Deletes a remote branch. Essentially a 'git push <remote> :<remote branch name>'
     *
     * @param branchHelper the remote branch to delete
     * @return the status of the push to remote to delete
     * @throws GitAPIException
     */
    public RemoteRefUpdate.Status deleteRemoteBranch(RemoteBranchHelper branchHelper) throws GitAPIException, IOException {
        PushCommand pushCommand = new Git(this.repoHelper.getRepo()).push();
        // We're deleting the branch on a remote, so there it shows up as refs/heads/<branchname>
        // instead of what it shows up on local: refs/<remote>/<branchname>, so we manually enter
        // this thing in here
        pushCommand.setRemote("origin").add(":refs/heads/"+branchHelper.parseBranchName());
        this.repoHelper.wrapAuthentication(pushCommand);

        // Update the remote branches in case it worked
        updateRemoteBranches();

        boolean succeeded=false;
        for (PushResult result : pushCommand.call()) {
            for (RemoteRefUpdate refUpdate : result.getRemoteUpdates()) {
                return refUpdate.getStatus();
            }
        }
        return null;
    }

    /**
     * Merges the current branch with the selected branch
     *
     * @param branchToMergeFrom the branch to merge into the current branch
     * @return merge result, used in determining the notification in BranchCheckoutController
     * @throws GitAPIException
     * @throws IOException
     */
    public MergeResult mergeWithBranch(BranchHelper branchToMergeFrom) throws GitAPIException, IOException {
        Git git = new Git(this.repoHelper.getRepo());

        MergeCommand merge = git.merge();
        merge.include(this.repoHelper.getRepo().resolve(branchToMergeFrom.getRefPathString()));

        MergeResult mergeResult = merge.call();

        String current = getCurrentBranch().getRefName();
        HashMap<String, String> results = new HashMap<>();
        results.put("mergedBranch", branchToMergeFrom.getRefName());
        results.put("baseBranch", current);
        //ObjectId[] parents = mergeResult.getMergedCommits();
        results.put("baseParent", mergeResult.getMergedCommits()[0].toString());
        results.put("mergedParent", mergeResult.getMergedCommits()[1].toString());
        SessionModel.getSessionModel().addMergeResult(results);

        git.close();

        return mergeResult;
    }

    // ************************* GETTERS AND SETTERS **************************

    /**
     * Getter for the current branch in the model
     *
     * @return the branch helper for the current branch
     */
    // synchronized for currentBranch safety
    public synchronized BranchHelper getCurrentBranch() { return this.currentBranch; }

    /**
     * Getter for the current remote branch head
     * @return the head of the current remote branch (if one exists)
     * @throws IOException
     */
    public CommitHelper getCurrentRemoteBranchHead() throws IOException {
        String remoteBranch = getCurrentRemoteBranch();
        if (remoteBranch != null) {
            BranchHelper currentRemoteBranch = getBranchByName(BranchType.REMOTE, remoteBranch);
            return currentRemoteBranch.getCommit();
        }
        return null;
    }

    // synchronized for currentBranch safety
    public synchronized String getCurrentRemoteBranch() throws IOException {
        if (BranchTrackingStatus.of(this.repoHelper.getRepo(), this.currentBranch.getRefName())!=null) {
            return Repository.shortenRefName(
                    BranchTrackingStatus.of(this.repoHelper.getRepo(), this.currentBranch.getRefName())
                            .getRemoteTrackingBranch());
        }
        return null;
    }

    // synchronized for currentBranch safety
    public synchronized String getCurrentRemoteAbbrevBranch() throws IOException {
        if (BranchTrackingStatus.of(this.repoHelper.getRepo(), this.currentBranch.getRefName())!=null) {
            String name =  Repository.shortenRefName(
                    BranchTrackingStatus.of(this.repoHelper.getRepo(), this.currentBranch.getRefName())
                            .getRemoteTrackingBranch());
            if (name.length() > CellLabel.MAX_CHAR_PER_LABEL) {
                name = name.substring(0,24)+"...";
            }
            return name;
        }
        return null;
    }

    /**
     * Getter for the current branch head in the model
     *
     * @return the commit helper for the head of the current branch
     */
    // synchronized for currentBranch safety
    public synchronized CommitHelper getCurrentBranchHead() { return (this.currentBranch == null) ? null : this.currentBranch.getCommit();}

    /**
     * Getter for list of branches
     *
     * @param type the type of list of branches to return
     * @return the list of branches, either all local or remote
     */
    // Synchronized for localBranchesTyped and remoteBranchesTyped
    public List<? extends BranchHelper> getBranchListTyped(BranchType type) {
        return Collections.unmodifiableList(new ArrayList<>(
                (type==BranchType.LOCAL) ? this.localBranchesTyped : this.remoteBranchesTyped
        ));
    }

    /**
     * Getter for local branches
     * @return list of local branches with type LocalBranchHelper
     */
    // Synchronized for localBranchesTyped
    public List<LocalBranchHelper> getLocalBranchesTyped() {
        return Collections.unmodifiableList(new ArrayList<>(this.localBranchesTyped));
    }

    /**
     * Getter for remote branches
     * @return list of remote branches with type RemoteBranchHelper
     */
    // Synchronized for remoteBranchesTyped
    public List<RemoteBranchHelper> getRemoteBranchesTyped() {
        return Collections.unmodifiableList(new ArrayList<>(this.remoteBranchesTyped));
    }

    /**
     * Getter for a list of branches that is typed
     * @param type the type of list to get, either local or remote
     * @return the list of branches in the 'type' list, but with type BranchHelper
     */
    // Synchronized for localBranchesTyped and remoteBranchesTyped
    public List<BranchHelper> getBranchListUntyped(BranchType type){
        List<? extends BranchHelper> typed = (type==BranchType.LOCAL) ? this.localBranchesTyped : this.remoteBranchesTyped;
        List<BranchHelper> untyped = new ArrayList<>();
        for (BranchHelper branch : typed)
            untyped.add(branch);
        return Collections.unmodifiableList(untyped);
    }

    /**
     * Getter method for all branches
     *
     * @return a list of all remote branches and local branches
     */
    // Synchronized for localBranchesTyped
    public Set<BranchHelper> getAllBranches() {
        Set<BranchHelper> tmp = ConcurrentHashMap.newKeySet();
        tmp.addAll(this.remoteBranchesTyped);
        tmp.addAll(this.localBranchesTyped);
        return Collections.unmodifiableSet(tmp);
    }

    /**
     * Gets a branch of a certain type by name
     *
     * @param type the type of the branch
     * @param branchName the name of the branch
     * @return
     */
    // Synchronized for localBranchesTyped and remoteBranchesTyped
    public BranchHelper getBranchByName(BranchType type, String branchName) {
        List<? extends BranchHelper> branchList = type==BranchType.LOCAL ? this.localBranchesTyped : this.remoteBranchesTyped;
        for (BranchHelper branch: branchList) {
            if (branch.getRefName().equals(branchName))
                return branch;
        }
        return null;
    }

    /**
     * Checks to see if the given branch is tracked. If branch is a local
     * branch, looks to see if there is a branch in the remote branches that
     * has the same name, and vice versa. Note that the tracking is determined
     * by the config file, not just by name.
     *
     * @param branch the branch to check
     * @return true if branch is being tracked, else false
     */
    // Synchronized for localBranchesTyped
    public synchronized boolean isBranchTracked(BranchHelper branch) {
        String branchName = branch.getRefName();
        if (branch instanceof LocalBranchHelper) {
            // We can check this easily by looking at the config file, but have to first
            // check if there is an entry in the config file for LocalBranchHelper
            String merge = this.repoHelper.getRepo().getConfig().getString("branch", branchName, "merge");

            // If there is no entry in the config file for this branch, then it isn't tracked remotely
            if (merge==null) return false;

            // Otherwise there is a remote branch that the local branch is tracking remotely
            return true;
        } else {
            for (BranchHelper local : this.localBranchesTyped) {
                // Skip local branches that aren't tracked remotely, as they won't have a config entry
                if (this.repoHelper.getRepo().getConfig().getString("branch", local.getRefName(), "merge")==null) continue;

                // Otherwise, we have to check all local branches to see if they're tracking the particular remote branch
                if (Repository.shortenRefName(this.repoHelper.getRepo().getConfig().getString("branch", local.getRefName(), "merge"))
                        .equals(this.repoHelper.getRepo().shortenRemoteBranchName(branch.getRefPathString()))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Helper method to check if a branch is a current branch
     * @param branch the branch to check
     * @return true if the branch is the current branch or its remote tracking branch
     */
    // synchronized for currentBranch safety
    public synchronized boolean isBranchCurrent(BranchHelper branch) {
        if (this.currentBranch==branch)
            return true;
        if (this.currentBranch==null)
            return false;
        try {
            // If the branch is the local's remote tracking branch, it is current
            BranchTrackingStatus status = BranchTrackingStatus.of(this.repoHelper.getRepo(), this.currentBranch.getRefName());
            if (branch instanceof RemoteBranchHelper && status != null && Repository.shortenRefName(
                    status.getRemoteTrackingBranch()).equals(branch.getRefName())) {
                return true;
            }
        } catch (IOException e) {
            // Shouldn't happen here, session controller would catch this first
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Updates the heads of all local and remote branches, then returns a map of them
     * @return
     */
    public Map<CommitHelper, List<BranchHelper>> getAllBranchHeads(){
        Map<CommitHelper, List<BranchHelper>> heads = new ConcurrentHashMap<>();

        this.refreshHeadIds();

        Set<BranchHelper> branches = this.getAllBranches();

        for(BranchHelper branch : branches){
            CommitHelper head = branch.getCommit();
            if(heads.containsKey(head)){
                heads.get(head).add(branch);
            }else{
                List<BranchHelper> helpers = new ArrayList<>(branches.size());
                helpers.add(branch);
                heads.put(head, helpers);
            }
        }
        for (CommitHelper head : heads.keySet()) {
            heads.put(head, Collections.unmodifiableList(heads.get(head)));
        }
        return Collections.unmodifiableMap(heads);
    }

    /**
     * Gets a list of names of branches that have the given commit helper as their head
     * @param commit the commit helper to look at
     * @return a list of names of branches that have the given commit helper as their head
     */
    public List<String> getBranchesWithHead(CommitHelper commit) {
        List<BranchHelper> branches = getAllBranchHeads().get(commit);
        List<String> branchLabels = new LinkedList<>();
        if(branches != null) {
            branchLabels = branches.stream()
                    .map(BranchHelper::getRefName)
                    .collect(Collectors.toList());
        }
        return Collections.unmodifiableList(branchLabels);
    }

    /**
     * @return a list of the current branches, useful for the ref labels
     */
    public Set<String> getCurrentAbbrevBranches() {
        HashSet<String> branches = new HashSet<>();
        for (BranchHelper branch : getAllBranches()) {
            if (isBranchCurrent(branch))
                branches.add(branch.getAbbrevName());
        }
        return Collections.unmodifiableSet(branches);
    }


    /**
     * Type for branches, can be local or remote
     */
    public enum BranchType {
        LOCAL, REMOTE;
    }


}
