package elegit;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.RmCommand;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * A subclass of RepoFile that contains a file that Git reports as missing.
 */
public class MissingRepoFile extends RepoFile {

    public MissingRepoFile(Path filePath, RepoHelper repo) {
        super(filePath, repo);
        diffButton.setText("MISSING");
        diffButton.setId("missingDiffButton");
    }

    public MissingRepoFile(String filePathString, RepoHelper repo) {
        this(Paths.get(filePathString), repo);
    }

    /**
     * When this RepoFile is checkboxed and the user commits, we remove this file from the repository.
     *
     * @throws GitAPIException if the `git rm` command fails.
     */
    @Override public boolean updateFileStatusInRepo() throws GitAPIException {
        RmCommand rm = new Git(this.repo.getRepo()).rm().addFilepattern(this.filePath.toString());
        rm.call();
        return true;
    }
}
