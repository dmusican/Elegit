package elegit;

import com.jcraft.jsch.UserInfo;
import elegit.exceptions.CancelledAuthorizationException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.IOException;
import java.nio.file.Path;

/**
 * A RepoHelper implementation for pre-existing repositories.
 */
public class ExistingRepoHelper extends RepoHelper {
    public ExistingRepoHelper(Path directoryPath) throws IOException, GitAPIException, CancelledAuthorizationException{
        super(directoryPath);
        repo = obtainRepository();
        setup();
    }

    ExistingRepoHelper(Path directoryPath, UserInfo userInfo) throws IOException, GitAPIException,
            CancelledAuthorizationException{
        super(directoryPath, userInfo);
        repo = obtainRepository();
        setup();
    }

    /**
     * Builds a repository by searching the directory for .git files
     * and then returns the JGit Repository object.
     *
     * @return the RepoHelper's associated Repository object.
     * @throws IOException if building the repository fails.
     */
    protected Repository obtainRepository() throws IOException {
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        return builder.findGitDir(this.localPath.toFile())
                .readEnvironment()
                .build();
    }
}
