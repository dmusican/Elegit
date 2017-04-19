package elegit;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * A subclass of RepoFile that contains a file that Git reports as untracked.
 */
public class LabelRepoFile extends RepoFile {

    public LabelRepoFile(Path filePath, RepoHelper repo) {
        super(filePath, repo);
        diffButton.setText("LABEL");
        diffButton.setTooltip(getToolTip("This is the root directory for the below files."));
    }

    public LabelRepoFile(String filePathString, RepoHelper repo) {
        this(Paths.get(filePathString), repo);
    }

    @Override public boolean canAdd() { return false; }

    @Override public boolean canRemove() { return false; }

    @Override
    public String toString() {
        return this.filePath.toString();
    }
}
