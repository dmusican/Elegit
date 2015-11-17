package elegit;

import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * A subclass of elegit.RepoFile that contains a file that Git reports as modified.
 */
public class ModifiedRepoFile extends RepoFile {

    public ModifiedRepoFile(Path filePath, Repository repo) {
        super(filePath, repo);
        diffButton.setText("MODIFIED");
        diffButton.setId("modifiedDiffButton");
    }

    public ModifiedRepoFile(String filePathString, Repository repo) {
        this(Paths.get(filePathString), repo);
    }

    /**
     * When this elegit.RepoFile is checkboxed and the user commits, we add this file to the repository.
     *
     * @throws GitAPIException if the `git add` command fails.
     */
    @Override public boolean updateFileStatusInRepo() throws GitAPIException {
        AddCommand add = new Git(this.repo).add().addFilepattern(this.filePath.toString());
        add.call();
        return true;
    }
}
