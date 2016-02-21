package main.java.elegit;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.nio.file.Path;

/**
 * A RepoHelper implementation for newly instantiated repositories in an empty folder.
 */
public class NewRepoHelper extends RepoHelper {
    public NewRepoHelper(Path directoryPath, String remoteURL, UsernamePasswordCredentialsProvider ownerAuth) throws Exception {
        super(directoryPath, remoteURL, ownerAuth);
    }

    /**
     * Creates a new Git repository in the given directory and returns the
     * JGit Repository object.
     *
     * @return the RepoHelper's associated Repository object.
     * @throws GitAPIException if the repository initialization fails.
     * @param ownerAuth
     */
    @Override
    protected Repository obtainRepository(UsernamePasswordCredentialsProvider ownerAuth) throws GitAPIException {
        // create the directory
        Git git = Git.init().setDirectory(this.localPath.toFile()).call();
        git.close();
        return git.getRepository();
    }
}