package elegit;

import javafx.scene.control.Tooltip;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * A subclass of RepoFile that contains a file that Git reports as modified.
 */
public class ModifiedRepoFile extends RepoFile {

    public ModifiedRepoFile(Path filePath, RepoHelper repo) {
        super(filePath, repo);
        diffButton.setText("MODIFIED");
        diffButton.setId("modifiedDiffButton");
        Tooltip tooltip = new Tooltip("This file was modified after your most recent commit.");
        tooltip.setFont(new javafx.scene.text.Font(12));
        diffButton.setTooltip(tooltip);
        showPopover = true;
    }

    public ModifiedRepoFile(String filePathString, RepoHelper repo) {
        this(Paths.get(filePathString), repo);
    }

    /**
     * When this RepoFile is checkboxed and the user commits, we add this file to the repository.
     *
     * @throws GitAPIException if the `git add` command fails.
     */
    @Override public boolean updateFileStatusInRepo() throws GitAPIException {
        AddCommand add = new Git(this.repo.getRepo()).add().addFilepattern(this.filePath.toString());
        add.call();
        return true;
    }
}
