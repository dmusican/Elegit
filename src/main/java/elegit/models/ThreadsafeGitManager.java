package elegit.models;

import elegit.exceptions.ExceptionAdapter;
import elegit.exceptions.MissingRepoException;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.CheckoutResult;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.GitCommand;
import org.eclipse.jgit.api.RmCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
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
    public void addFilePathTest(ArrayList<String> filePaths) throws GitAPIException {
        Git git = new Git(repo);
        AddCommand addCommand = git.add();
        for (String filePath : filePaths) {
            addCommand.addFilepattern(filePath);
        }
        repoLock.writeLock().lock();
        try {
            addCommand.call();
        } finally {
            repoLock.writeLock().unlock();
            git.close();
        }
    }


    /**
     * Git checkout file command.
     *
     * @param filePath
     * @throws GitAPIException
     */
    public void checkoutFile(Path filePath) throws GitAPIException {
        Git git = new Git(repo);
        GitCommand checkoutCommand = git.checkout().addPath(filePath.toString());
        repoLock.writeLock().lock();
        try {
            checkoutCommand.call();
        } finally {
            repoLock.writeLock().unlock();
            git.close();
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
        Git git = new Git(repo);
        CheckoutCommand checkoutCommand = git.checkout().setStartPoint(startPoint);
        for (String filePath : filePaths) {
            checkoutCommand.addPath(filePath);
        }
        repoLock.writeLock().lock();
        try {
            checkoutCommand.call();
            return checkoutCommand.getResult();
        } finally {
            repoLock.writeLock().unlock();
            git.close();
        }
    }

    /**
     * Git rm command, for a specified collection of paths.
     *
     * @param filePaths
     * @throws GitAPIException
     */
    public void removeFilePaths(ArrayList<Path> filePaths) throws GitAPIException {
        Git git = new Git(repo);
        RmCommand removeCommand = git.rm();
        for (Path filePath : filePaths) {
            removeCommand.addFilepattern(filePath.toString());
        }
        repoLock.writeLock().lock();
        try {
            removeCommand.call();
        } finally {
            repoLock.writeLock().unlock();
            git.close();
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
    public RevCommit commit(String commitMessage) throws GitAPIException, MissingRepoException {

        Git git = new Git(repo);
        CommitCommand commitCommand = git.commit().setMessage(commitMessage);
        repoLock.writeLock().lock();
        try {
            RevCommit commit = commitCommand.call();
            return commit;
        } finally {
            repoLock.writeLock().lock();
            git.close();
        }
    }




    //////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Gets a list of all remotes associated with this repository. The URLs
     * correspond to the output seen by running 'git remote -v'
     *
     * @return a list of the remote URLs associated with this repository
     */
    public List<String> getLinkedRemoteRepoURLs() {
        repoLock.readLock().lock();
        try {
            Config storedConfig = repo.getConfig();
            Set<String> remotes = storedConfig.getSubsections("remote");
            ArrayList<String> urls = new ArrayList<>(remotes.size());
            for (String remote : remotes) {
                urls.add(storedConfig.getString("remote", remote, "url"));
            }
            return Collections.unmodifiableList(urls);
        } finally {
            repoLock.readLock().unlock();
        }
    }


}