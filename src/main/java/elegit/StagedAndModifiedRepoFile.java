package elegit;

import javafx.scene.control.Tooltip;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * A subclass of RepoFile that contains a file that Git is ignoring.
 */
public class StagedAndModifiedRepoFile extends RepoFile {

    public StagedAndModifiedRepoFile(Path filePath, RepoHelper repo) {
        super(filePath, repo);
        diffButton.setText("STAGED\nMODIFIED");
        diffButton.setId("stagedModifiedDiffButton");
        Tooltip tooltip = new Tooltip("This file has a version stored in your git index\n and is ready to commit.");
        tooltip.setFont(new javafx.scene.text.Font(12));
        diffButton.setTooltip(tooltip);
    }

    public StagedAndModifiedRepoFile(String filePathString, RepoHelper repo) {
        this(Paths.get(filePathString), repo);
    }

    @Override public boolean canAdd() {
        return true;
    }

    @Override public boolean canRemove() {
        return true;
    }
}
