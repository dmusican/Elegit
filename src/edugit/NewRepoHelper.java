package edugit;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;

import java.io.File;

/**
 * Created by grahamearley on 6/10/15.
 */
public class NewRepoHelper extends RepoHelper {
    public NewRepoHelper(File directoryPath, String ownerToken) throws Exception {
        super(directoryPath, ownerToken);
    }

    @Override
    protected Repository obtainRepository() throws GitAPIException {
        // create the directory
        Git git = Git.init().setDirectory(this.localPath).call();
        git.close();
        return git.getRepository();
    }
}