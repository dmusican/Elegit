package edugit;

import javafx.scene.text.Text;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;

import java.nio.file.Path;

/**
 * A subclass of RepoFile that contains a file that Git reports as untracked.
 */
public class UntrackedRepoFile extends RepoFile {
    public UntrackedRepoFile(String filePathString, Repository repo) {
        super(filePathString, repo);
        this.textLabel = new Text("UNTRACKED");
        textLabel.setId("untrackedText");
    }

    public UntrackedRepoFile(Path filePath, Repository repo) {
        super(filePath, repo);
        this.textLabel = new Text("UNTRACKED");
        textLabel.setId("untrackedText");

        // TODO: Have one *parent* constructor, so you don't have to have duplicate code here..
    }

    /**
     * When this RepoFile is checkboxed and the user commits, we add this file to the repository.
     *
     * @throws GitAPIException if the `git add` command fails.
     */
    @Override public void updateFileStatusInRepo() throws GitAPIException {
        AddCommand add = new Git(this.repo).add().addFilepattern(this.filePath.toString());
        add.call();
    }
}
