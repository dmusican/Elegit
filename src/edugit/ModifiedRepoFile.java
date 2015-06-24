package edugit;

import javafx.scene.control.Button;
import javafx.scene.text.Text;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * A subclass of RepoFile that contains a file that Git reports as modified.
 */
public class ModifiedRepoFile extends RepoFile {

    public ModifiedRepoFile(Path filePath, Repository repo) {
        super(filePath, repo);
        this.textLabel = new Text("MODIFIED");
        textLabel.setId("modifiedText");

        diffButton.setText("MODIFIED");
    }

    public ModifiedRepoFile(String filePathString, Repository repo) {
        this(Paths.get(filePathString), repo);
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
