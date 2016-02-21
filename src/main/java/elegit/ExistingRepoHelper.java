package main.java.elegit;

import main.java.elegit.exceptions.CancelledAuthorizationException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.IOException;
import java.nio.file.Path;

/**
 * A RepoHelper implementation for pre-existing repositories.
 */
public class ExistingRepoHelper extends RepoHelper {
    public ExistingRepoHelper(Path directoryPath, String username, UsernamePasswordCredentialsProvider ownerAuth) throws IOException, GitAPIException, CancelledAuthorizationException{
        super(directoryPath, username, ownerAuth);
    }

    /**
     * Builds a repository by searching the directory for .git files
     * and then returns the JGit Repository object.
     *
     * @return the RepoHelper's associated Repository object.
     * @throws IOException if building the repository fails.
     * @param ownerAuth
     */
    @Override
    protected Repository obtainRepository(UsernamePasswordCredentialsProvider ownerAuth) throws IOException {
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        return builder.findGitDir(this.localPath.toFile())
                .readEnvironment()
                .build();
    }
}
