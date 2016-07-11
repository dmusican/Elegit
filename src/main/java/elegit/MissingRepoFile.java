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

    /**
     * When this RepoFile is checkboxed and the user commits, we remove this file from the repository.
     *
     * @throws GitAPIException if the `git rm` command fails.
     */
    @Override public boolean updateFileStatusInRepo() throws GitAPIException, MissingRepoException {
        this.repo.removeFilePath(this.filePath);
        return true;
    }
}
