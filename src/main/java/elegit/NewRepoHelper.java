package elegit;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;

import java.nio.file.Path;

/**
 * A RepoHelper implementation for newly instantiated repositories in an empty folder.
 */
public class NewRepoHelper extends RepoHelper {
    public NewRepoHelper(Path directoryPath, String remoteURL, RepoOwner owner) throws Exception {
        super(directoryPath, remoteURL, owner);
    }

    /**
     * Creates a new Git repository in the given directory and returns the
     * JGit Repository object.
     *
     * @return the RepoHelper's associated Repository object.
     * @throws GitAPIException if the repository initialization fails.
     */
    @Override
    protected Repository obtainRepository() throws GitAPIException {
        // create the directory
        Git git = Git.init().setDirectory(this.localPath.toFile()).call();
        git.close();
        return git.getRepository();
    }
}