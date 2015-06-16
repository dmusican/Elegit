package edugit;

import javafx.scene.text.Text;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.RmCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;

import java.nio.file.Path;

/**
 * A subclass of RepoFile that contains a file that Git reports as missing.
 */
public class MissingRepoFile extends RepoFile {

    public MissingRepoFile(String filePathString, Repository repo) {
        super(filePathString, repo);
        this.textLabel = new Text("MISSING");
        textLabel.setId("missingText");
    }

    public MissingRepoFile(Path filePath, Repository repo) {
        super(filePath, repo);
        this.textLabel = new Text("MISSING");
        textLabel.setId("missingText");
    }

    /**
     * When this RepoFile is checkboxed and the user commits, we remove this file from the repository.
     *
     * @throws GitAPIException if the `git rm` command fails.
     */
    @Override public void updateFileStatusInRepo() throws GitAPIException {
        RmCommand rm = new Git(this.repo).rm().addFilepattern(this.filePath.toString());
        rm.call();
    }
}
