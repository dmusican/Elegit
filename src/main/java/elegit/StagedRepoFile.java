package elegit;

import javafx.scene.control.Tooltip;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * A subclass of RepoFile that contains a file that Git is ignoring.
 */
public class StagedRepoFile extends RepoFile {

    public StagedRepoFile(Path filePath, RepoHelper repo) {
        super(filePath, repo);
        diffButton.setText("STAGED");
        diffButton.setId("stagedDiffButton");
        Tooltip tooltip = new Tooltip("This file has a version stored in your git index\n and is ready to commit.");
        tooltip.setFont(new javafx.scene.text.Font(12));
        diffButton.setTooltip(tooltip);
    }

    public StagedRepoFile(String filePathString, RepoHelper repo) {
        this(Paths.get(filePathString), repo);
    }

    /**
     * When this RepoFile is checkboxed and the user commits, do nothing.
     */
    @Override public boolean updateFileStatusInRepo() {
        return true;
    }

    @Override public boolean canAdd() {
        return false;
    }

    @Override public boolean canRemove() {
        return true;
    }
}
