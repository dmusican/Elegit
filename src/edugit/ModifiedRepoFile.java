package edugit;

import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;

/**
 * Created by grahamearley on 6/12/15.
 */
public class ModifiedRepoFile extends RepoFile {
    public ModifiedRepoFile(String filePathString, Repository repo) {
        super(filePathString, repo);
    }

    @Override
    public void updateFileStatusInRepo() throws GitAPIException {
        AddCommand add = new Git(this.repo).add().addFilepattern(this.filePath.toString());
        add.call();
    }
}
