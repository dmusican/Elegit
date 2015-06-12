package edugit;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.RmCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;

/**
 * Created by grahamearley on 6/12/15.
 */
public class MissingRepoFile extends RepoFile {
    public MissingRepoFile(String filePathString, Repository repo) {
        super(filePathString, repo);
    }

    @Override
    public void updateFileStatusInRepo() throws GitAPIException {
        RmCommand rm = new Git(this.repo).rm().addFilepattern(this.filePath.toString());
        rm.call();
    }
}
