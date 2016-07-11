package elegit;

import elegit.exceptions.MissingRepoException;
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

    @Override public boolean canAdd() { return true; }

    @Override public boolean canRemove() { return false; }
}
