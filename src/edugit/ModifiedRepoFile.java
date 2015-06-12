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

    /*
    When the checkbox is checked for a modified file, we assume the user wants to
    add the changed file to the repo.
     */
    @Override public void updateFileStatusInRepo() throws GitAPIException {
        AddCommand add = new Git(this.repo).add().addFilepattern(this.filePath.toString());
        add.call();
    }
}
