package elegit;

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
        diffButton.setTooltip(getToolTip("This file is missing."));
    }

    public MissingRepoFile(String filePathString, RepoHelper repo) {
        this(Paths.get(filePathString), repo);
    }

    @Override public boolean canAdd() { return false; }

    @Override public boolean canRemove() { return true; }
}
