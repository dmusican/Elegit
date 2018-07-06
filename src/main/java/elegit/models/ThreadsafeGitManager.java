package elegit.models;

import elegit.exceptions.ExceptionAdapter;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.GitCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;

import java.nio.file.Path;
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
     * @param filePath
     * @throws GitAPIException
     */
    public void addFilePathTest(String filePath) throws GitAPIException {
        Git git = new Git(repo);
        GitCommand addCommand = git.add().addFilepattern(filePath);
        repoLock.writeLock().lock();
        try {
            addCommand.call();
        } finally {
            repoLock.writeLock().unlock();
        }
        git.close();
    }



}
