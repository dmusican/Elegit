package elegit.models;

import elegit.gui.SimpleProgressMonitor;
import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.FetchResult;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RemoteRefUpdate;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Manage JGit calls in a threadsafe manner, since JGit does not do this on its own. This class
 * maintains a collection of ThreadsafeGitManagers, each one of which is unique for a particular git path.
 * It manages locking around each set of git operations.
 *
 * According to this mailing list posting:
 * https://dev.eclipse.org/mhonarc/lists/jgit-dev/msg02125.html
 * JGit is threadsafe for manipulating the library and references, but not for the working directory. When using read
 * vs write locks, the real question is how it impacts the working directory.
 */
public class ThreadsafeGitManager {

    // Collection of ThreadSafeGitManagers
    private static final ConcurrentHashMap<String, ThreadsafeGitManager> tsGitManagers
            = new ConcurrentHashMap<>();

    // The repository object associated with this manager
    private final Repository repo;

    // The lock used to manage this particular repository.
    private final ReentrantReadWriteLock repoLock = new ReentrantReadWriteLock();

    // private so that can only be handled via static factory
    private ThreadsafeGitManager(Repository repo) {
        this.repo = repo;
    }


    private interface JGitOperation<T, E extends Throwable> {
        T call() throws E;
    }

    /**
     * Factory method. It returns the existing object if one exists for the given repo. If one does not exist,
     * it constructs it first
     *
     * @param repo
     * @return the appropriate ThreadsafeGitManager object
     */
    public static ThreadsafeGitManager get(Repository repo) {
        String dotGitDirectory = repo.getDirectory().toString();
        if (dotGitDirectory == null) {
            throw new RuntimeException("Repository is not local; Elegit does not currently support only remote repos.");
        }

        if (tsGitManagers.containsKey(dotGitDirectory)) {
            return tsGitManagers.get(dotGitDirectory);
        } else {
            ThreadsafeGitManager tsGitManager = new ThreadsafeGitManager(repo);
            tsGitManagers.put(dotGitDirectory, tsGitManager);
            return tsGitManager;
        }
    }

    /**
     * Functional interfaces for read lock, to allow using it via lambda expressions elsewhere.
     * @param jGitOperation The operation to be called
     * @param <T> The type of the Git operation
     * @return The result of the Git operation
     * @throws E
     */
    public <T, E extends Throwable> T readLock(JGitOperation<T, E> jGitOperation) throws E {
        repoLock.readLock().lock();
        try {
            return jGitOperation.call();
        } finally {
            repoLock.readLock().unlock();
        }
    }

    /**
     * Functional interfaces for write lock, to allow using it via lambda expressions elsewhere.
     * @param jGitOperation The operation to be called
     * @param <T> The type of the Git operation
     * @return The result of the Git operation
     * @throws E
     */
    public <T, E extends Throwable> T writeLock(JGitOperation<T, E> jGitOperation) throws E {
        repoLock.writeLock().lock();
        try {
            return jGitOperation.call();
        } finally {
            repoLock.writeLock().unlock();
        }
    }

    /**
     * Git add command, for a specified path. It would seem that one might be able to get away with a read lock
     * here, since add shouldn't be changing the working directory. This is poorly documented, however, and it
     * isn't a chance worth taking, so go with a write lock.
     *
     * @param filePath
     * @throws GitAPIException
     */
    public void addFilePathTest(String filePath) throws GitAPIException {
        ArrayList<String> filePaths = new ArrayList<>();
        filePaths.add(filePath);
        addFilePathTest(filePaths);
    }

    /**
     * Git add command, for a specified collection of paths. It would seem that one might be able to get away with a
     * read lock here, since add shouldn't be changing the working directory. This is poorly documented, however, and it
     * isn't a chance worth taking, so go with a write lock.
     *
     * @param filePaths
     * @throws GitAPIException
     */
    public DirCache addFilePathTest(ArrayList<String> filePaths) throws GitAPIException {
        try (Git git = new Git(repo)) {
            AddCommand addCommand = git.add();
            for (String filePath : filePaths) {
                addCommand.addFilepattern(filePath);
            }
            return writeLock(addCommand::call);
        }
    }


    /**
     * Git checkout file command.
     *
     * @param filePath
     * @throws GitAPIException
     */
    public CheckoutResult checkoutFile(Path filePath) throws GitAPIException {
        try (Git git = new Git(repo)) {
            CheckoutCommand checkoutCommand = git.checkout().addPath(filePath.toString());
            return writeLock(() -> {
                checkoutCommand.call();
                return checkoutCommand.getResult();
            });
        }
    }

    /**
     * Git checkout multiple files, given a starting point commit to pull the files from.
     *
     * @param filePaths
     * @throws GitAPIException
     * @return the result of the checkout
     */
    public CheckoutResult checkoutFiles(List<String> filePaths, String startPoint) throws GitAPIException {
        try (Git git = new Git(repo)) {
            CheckoutCommand checkoutCommand = git.checkout().setStartPoint(startPoint);
            for (String filePath : filePaths) {
                checkoutCommand.addPath(filePath);
            }
            return writeLock(() -> {
                checkoutCommand.call();
                return checkoutCommand.getResult();
            });
        }
    }

    /**
     * Git rm command, for a specified collection of paths.
     *
     * @param filePaths
     * @throws GitAPIException
     */
    public DirCache removeFilePaths(ArrayList<Path> filePaths) throws GitAPIException {
        try (Git git = new Git(repo)) {
            RmCommand removeCommand = git.rm();
            for (Path filePath : filePaths) {
                removeCommand.addFilepattern(filePath.toString());
            }
            return writeLock(removeCommand::call);
        }
    }

    /**
     * Commits changes to the repository. It would seem that one might be able to get away with a read lock
     * here, since add shouldn't be changing the working directory. This is poorly documented, however, and it
     * isn't a chance worth taking, so go with a write lock.
     *
     * @param commitMessage the message for the commit.
     * @return RevCommit the commit object
     * @throws GitAPIException if the `git commit` call fails.
     */
    public RevCommit commit(String commitMessage) throws GitAPIException {
        try (Git git = new Git(repo)) {
            CommitCommand commitCommand = git.commit().setMessage(commitMessage);
            return writeLock(commitCommand::call);
        }
    }

    /**
     * Commits all changes that have been modified or delete (new files are not impacted) to the repository. See
     * comment above about whether this should be a read or write lock.
     *
     * @param commitMessage the message for the commit.
     * @return RevCommit the commit object
     * @throws GitAPIException if the `git commit -all` call fails.
     */
    public RevCommit commitAll(String commitMessage) throws GitAPIException {
        try (Git git = new Git(repo)) {
            CommitCommand commitCommand = git.commit().setMessage(commitMessage).setAll(true);
            return writeLock(commitCommand::call);
        }
    }

    // TODO: figure out if this needs locking and why locking save() won't work
    public void setUpstreamBranch(String branchName, String remote) throws IOException {
        try (Git git = new Git(repo)) {
            StoredConfig config = git.getRepository().getConfig();
            config.setString(ConfigConstants.CONFIG_BRANCH_SECTION, branchName, ConfigConstants.CONFIG_KEY_REMOTE, remote);
            config.setString(ConfigConstants.CONFIG_BRANCH_SECTION, branchName, ConfigConstants.CONFIG_KEY_MERGE,
                    Constants.R_HEADS + branchName);
            config.save();
        }
    }

    // TODO: figure out if this needs locking
    public Set<String> getRemote() {
        try (Git git = new Git(repo)) {
            StoredConfig config = git.getRepository().getConfig();
            return config.getSubsections("remote");
        }
    }

    /**
     * Reverts all of the commits listed.
     *
     * @param commits list of commits to revert
     * @throws GitAPIException if the `git revert` fails.
     */
    public void revert(List<AnyObjectId> commits) throws GitAPIException {
        try (Git git = new Git(repo)) {
            RevertCommand revertCommand = git.revert();
            for (AnyObjectId commit : commits)
                revertCommand.include(commit);
            writeLock(revertCommand::call);
        }
    }

    /**
     * Reverts the changes that happened in the given commit, stores changes in working directory if conflicting,
     * otherwise, makes a new commit
     *
     * @param objectId the objectId for the commit to revert changes for
     * @throws GitAPIException if the `git revert` fails.
     */
    public void revert(ObjectId objectId) throws GitAPIException {
        try (Git git = new Git(repo)) {
            RevertCommand revertCommand = git.revert().include(objectId);
            writeLock(revertCommand::call);
        }
    }

    /**
     * Resets the given file to the version in HEAD
     *
     * @param relativePathName the relative path between the local path and the path for the file to reset, as a string
     * @throws GitAPIException if the `git reset` fails.
     */
    public void reset(String relativePathName) throws GitAPIException {
        try (Git git = new Git(repo)) {
            ResetCommand resetCommand = git.reset().addPath(relativePathName);
            writeLock(resetCommand::call);
        }
    }

    /**
     * Resets the given files to the version in HEAD
     *
     * @param localPath the local path of the repository
     * @param paths a list of the paths of the files to reset
     * @throws GitAPIException if the `git reset` fails.
     */
    public void reset(Path localPath, List<Path> paths) throws GitAPIException {
        try (Git git = new Git(repo)) {
            ResetCommand resetCommand = git.reset();
            paths.forEach(path -> resetCommand.addPath(localPath.relativize(path).toString()));
            writeLock(resetCommand::call);
        }
    }

    /**
     * Resets to the given commit with the given mode
     *
     * @param ref  the ref (commit id or branch label) to reset to
     * @param mode the mode of reset to use (hard, mixed, soft, merge, or keep)
     * @throws GitAPIException if the `git reset --hard/soft/etc.` fails.
     */
    public void reset(String ref, ResetCommand.ResetType mode) throws GitAPIException {
        try (Git git = new Git(repo)) {
            ResetCommand resetCommand = git.reset().setRef(ref).setMode(mode);
            writeLock(resetCommand::call);
        }
    }

    /**
     * Stashes the current working directory and index changes with the default message (the branch name)
     *
     * @param includeUntracked whether or not to include untracked files
     * @return RevCommit the commit object
     * @throws GitAPIException if the `git stash save` fails.
     */
    public RevCommit stashSave(boolean includeUntracked) throws GitAPIException {
        try (Git git = new Git(repo)) {
            StashCreateCommand stashCreateCommand = git.stashCreate().setIncludeUntracked(includeUntracked);
            return writeLock(stashCreateCommand::call);
        }
    }

    /**
     * Stashes the current working directory changes with the given message
     *
     * @param includeUntracked whether or not to include untracked files
     * @param wdMessage        the message used when committing working directory changes
     * @param indexMessage     the messaged used when committing the index changes
     * @return RevCommit the commit object
     * @throws GitAPIException if the `git stash save` fails.
     */
    public RevCommit stashSave(boolean includeUntracked, String wdMessage,
                               String indexMessage) throws GitAPIException {
        try (Git git = new Git(repo)) {
            StashCreateCommand stashCreateCommand = git.stashCreate().setIncludeUntracked(includeUntracked)
                    .setWorkingDirectoryMessage(wdMessage).setIndexMessage(indexMessage);
            return writeLock(stashCreateCommand::call);
        }
    }

    /**
     * Returns a list of the stashed commits
     *
     * @return stashCommitList a list of stashed commits
     * @throws GitAPIException if the `git stash list` fails.
     * @throws IOException if commit is not a fully parsed commit CommitHelper will fail and throw errors.
     */
    public List<CommitHelper> stashList() throws GitAPIException, IOException {
        try (Git git = new Git(repo)) {
            List<CommitHelper> stashCommitList = new ArrayList<>();

            for (RevCommit commit : readLock(git.stashList()::call)) {
                stashCommitList.add(new CommitHelper(commit));
            }
            return stashCommitList;
        }
    }

    /**
     * Applies the given stash to the repository, by default restores the index and untracked files.
     *
     * @param stashRef the string that corresponds to the stash to apply
     * @param force    whether or not to force apply
     * @throws GitAPIException if the `git stash apply (-f)` fails.
     */
    public void stashApply(String stashRef, boolean force) throws GitAPIException {
        try (Git git = new Git(repo)) {
            StashApplyCommand stashApplyCommand = git.stashApply().setStashRef(stashRef).ignoreRepositoryState(force);
            writeLock(stashApplyCommand::call);
        }
    }

    /**
     * Deletes all the stashed commits
     *
     * @return the value of the stash reference after the drop occurs
     * @throws GitAPIException if the `git stash clear` fails.
     */
    public ObjectId stashClear() throws GitAPIException {
        try (Git git = new Git(repo)) {
            return writeLock(git.stashDrop().setAll(true)::call);
        }
    }

    /**
     * Deletes a single stashed reference
     *
     * @param stashRef the stash reference int to drop (0-based)
     * @return the value of the value of the stashed reference
     * @throws GitAPIException
     */
    public ObjectId stashClear(int stashRef) throws GitAPIException {
        try (Git git = new Git(repo)) {
            return writeLock(git.stashDrop().setStashRef(stashRef)::call);
        }
    }

    public Collection<Ref> getRefsFromRemote(LsRemoteCommand command) throws GitAPIException {
        return readLock(command::call);
    }

    // TODO: figure out if this locking is correct
    public PushCommand prepareToPushCurrentBranch(String remote, String branchToPushRefPathString) {
        try (Git git = new Git(repo)) {
            PushCommand push = git.push().setRemote(remote).add(branchToPushRefPathString);

            writeLock(() -> {
                // TODO: Make this a real progress monitor
                ProgressMonitor progress = new SimpleProgressMonitor();
                return push.setProgressMonitor(progress);
            });

            return push;
        }
    }

    public PushCommand prepareToPushAll(PushCommand push) {
        return writeLock(() -> {
            ProgressMonitor progress = new SimpleProgressMonitor();
            return push.setProgressMonitor(progress);
        });
    }

    public Iterable<PushResult> pushTags(PushCommand push) throws GitAPIException {
        return writeLock(() -> {
            ProgressMonitor progress = new SimpleProgressMonitor();
            push.setProgressMonitor(progress);

            return push.setPushTags().call();
        });
    }

    public FetchResult fetch(FetchCommand fetch) throws GitAPIException {
        // The JGit docs say that if setCheckFetchedObjects
        //  is set to true, objects received will be checked for validity.
        //  Not sure what that means, but sounds good so I'm doing it...
        fetch.setCheckFetchedObjects(true);

        return writeLock(() -> {
            // ProgressMonitor progress = new TextProgressMonitor(new PrintWriter(System.out));
            ProgressMonitor progress = new SimpleProgressMonitor();
            fetch.setProgressMonitor(progress);

            return fetch.call();
        });
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Gets a list of all remotes associated with this repository. The URLs
     * correspond to the output seen by running 'git remote -v'
     *
     * @return a list of the remote URLs associated with this repository
     */
    public List<String> getLinkedRemoteRepoURLs() throws GitAPIException {
        return readLock(() -> {
            Config storedConfig = repo.getConfig();
            Set<String> remotes = storedConfig.getSubsections("remote");
            ArrayList<String> urls = new ArrayList<>(remotes.size());
            for (String remote : remotes) {
                urls.add(storedConfig.getString("remote", remote, "url"));
            }
            return Collections.unmodifiableList(urls);
        });
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Gets the status of the repository.
     *
     * @return the status of the repository
     * @throws GitAPIException if the `git status` calls fail.
     */
    public Status getStatus() throws GitAPIException {
        try (Git git = new Git(repo)) {
            return readLock(git.status()::call);
        }
    }

    /**
     * Returns the set of untracked files that Git reports.
     *
     * @return a set of untracked filenames in the working directory.
     */
    public Set<String> getUntracked(Status status) {
        return readLock(status::getUntracked);
    }

    /**
     * Returns the set of ignored files that Git reports.
     *
     * @return a set of ignored filenames in the working directory.
     */
    public Set<String> getIgnoredNotInIndex(Status status) {
        return readLock(status::getIgnoredNotInIndex);
    }

    /**
     * Returns the set of conflicting files that Git reports.
     *
     * @return a set of conflicting filenames in the working directory.
     */
    public Set<String> getConflicting(Status status) {
        return readLock(status::getConflicting);
    }

    /**
     * Returns the set of missing files that Git reports.
     *
     * @return a set of missing filenames in the working directory.
     */
    public Set<String> getMissing(Status status) {
        return readLock(status::getMissing);
    }

    /**
     * Returns the set of modified files that Git reports. Modified files differ between the disk and the index
     *
     * @return a set of modified filenames in the working directory.
     */
    public Set<String> getModified(Status status) {
        return readLock(status::getModified);
    }

    /**
     * Returns the set of changed files that Git reports.
     *
     * @return a set of modified filenames in the working directory.
     */
    public Set<String> getChanged(Status status) {
        return readLock(status::getChanged);
    }

    /**
     * Returns the set of added files that Git reports.
     *
     * @return a set of modified filenames in the working directory.
     */
    public Set<String> getAdded(Status status) {
        return readLock(status::getAdded);
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Creates a local branch tracking a remote branch.
     *
     * @param localBranchName the name of the local branch.
     * @param branchRefPathName the name of the remote branch to be tracked.
     * @return the reference to the new branch
     * @throws GitAPIException if the `git branch --track` fails.
     */
    public Ref getTrackingBranchRef(String localBranchName, String branchRefPathName) throws GitAPIException {
        try (Git git = new Git(repo)) {
            return writeLock(git.branchCreate().
                        setName(localBranchName).
                        setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK).
                        setStartPoint(branchRefPathName)::
                        call);
        }
    }

    /**
     * Creates a new local branch.
     *
     * @param branchName the name of the new branch
     * @return the reference to the new branch
     * @throws GitAPIException if the `git branch` fails.
     */
    public Ref getNewBranch(String branchName) throws GitAPIException {
        try (Git git = new Git(repo)) {
            return writeLock(git.branchCreate().setName(branchName)::call);
        }
    }

    /**
     * Deletes a local branch.
     *
     * @param branchName the name of the branch to delete
     * @throws GitAPIException if the `git branch -d` fails.
     */
    public void deleteBranch(String branchName) throws GitAPIException {
        try (Git git = new Git(repo)) {
            writeLock(git.branchDelete().setBranchNames(branchName)::call);
        }
    }

    /**
     * Force deletes a branch, even if it is not merged in
     *
     * @param branchName the name of the branch to delete
     */
    public void forceDeleteBranch(String branchName) throws GitAPIException {
        try (Git git = new Git(repo)) {
            writeLock(git.branchDelete().setForce(true).setBranchNames(branchName)::call);
        }
    }

    public RemoteRefUpdate.Status deleteRemoteBranch(PushCommand pushCommand) throws GitAPIException {
        for (PushResult result : writeLock(pushCommand::call)) {
            for (RemoteRefUpdate refUpdate : result.getRemoteUpdates()) {
                return refUpdate.getStatus();
            }
        }
        return null;
    }

    /**
     * Merges the current branch with the selected branch
     *
     * @param branchToMergeFromRefPathString the ref path of the branch to merge into the current branch as a string
     * @return merge result, used in determining the notification in BranchCheckoutController
     * @throws GitAPIException if the `git merge` fails.
     * @throws IOException
     */
    public MergeResult mergeWithBranch(String branchToMergeFromRefPathString) throws IOException, GitAPIException {
        try (Git git = new Git(repo)) {
            MergeCommand merge = git.merge();
            merge.include(repo.resolve(branchToMergeFromRefPathString));

            return writeLock(merge::call);
        }
    }

    /**
     * Gets a list of all local branches
     */
    public List<Ref> getLocalBranches() throws GitAPIException {
        try (Git git = new Git(repo)) {
           return readLock(git.branchList()::call);
        }
    }

    /**
     * Gets a list of all remote branches
     */
    public List<Ref> getRemoteBranches() throws GitAPIException {
        try (Git git = new Git(repo)) {
            return readLock(git.branchList()
                    .setListMode(ListBranchCommand.ListMode.REMOTE)::
                    call);
        }
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////

    public Ref getTag(String tagName, RevCommit commit) throws GitAPIException {
        try (Git git = new Git(repo)) {
            return writeLock(git.tag().setName(tagName).setObjectId(commit).setAnnotated(false)::call);
        }
    }

    /**
     *
     * @throws GitAPIException if `git tag -d` fails.
     */
    public void deleteTag(String tagToRemoveRefName) throws GitAPIException {
        try (Git git = new Git(repo)) {
            git.tagDelete().setTags(tagToRemoveRefName).call();
        }
    }
}
