package edugit;

import com.sun.jdi.InvocationException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.IOException;
import java.nio.file.Path;

/**
 * A RepoHelper implementation for pre-existing repositories.
 */
public class ExistingRepoHelper extends RepoHelper {
    public ExistingRepoHelper(Path directoryPath, RepoOwner owner) throws Exception {
        super(directoryPath, owner);
    }

    /**
     * Builds a repository by searching the directory for .git files
     * and then returns the JGit Repository object.
     *
     * @return the RepoHelper's associated Repository object.
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
