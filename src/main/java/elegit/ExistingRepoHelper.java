package elegit;

import main.java.elegit.exceptions.NoOwnerInfoException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.IOException;
import java.nio.file.Path;

/**
 * A elegit.RepoHelper implementation for pre-existing repositories.
 */
public class ExistingRepoHelper extends RepoHelper {
    public ExistingRepoHelper(Path directoryPath, RepoOwner owner) throws IOException, NoOwnerInfoException, GitAPIException{
        super(directoryPath, owner);
    }

    /**
     * Builds a repository by searching the directory for .git files
     * and then returns the JGit Repository object.
     *
     * @return the elegit.RepoHelper's associated Repository object.
     * @throws IOException if building the repository fails.
     */
    @Override
    protected Repository obtainRepository() throws IOException {
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        return builder.findGitDir(this.localPath.toFile())
                .readEnvironment()
                .build();
    }
}
