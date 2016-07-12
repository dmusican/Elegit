package elegit;

import elegit.exceptions.MissingRepoException;
import javafx.scene.control.Tooltip;
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
        Tooltip tooltip = new Tooltip("This file is missing.");
        tooltip.setFont(new javafx.scene.text.Font(12));
        diffButton.setTooltip(tooltip);
    }

    public MissingRepoFile(String filePathString, RepoHelper repo) {
        this(Paths.get(filePathString), repo);
    }

    @Override public boolean canAdd() { return false; }

    @Override public boolean canRemove() { return true; }
}
