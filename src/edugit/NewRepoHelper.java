package edugit;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;

import java.nio.file.Path;

/**
 * A RepoHelper for newly instantiated repositories in an empty folder
 */
public class NewRepoHelper extends RepoHelper {
    public NewRepoHelper(Path directoryPath, String ownerToken) throws Exception {
        super(directoryPath, ownerToken);
    }

    @Override
    protected Repository obtainRepository() throws GitAPIException {
        // create the directory
        Git git = Git.init().setDirectory(this.localPath.toFile()).call();
        git.close();
        return git.getRepository();
    }
}