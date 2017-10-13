package elegit;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * A subclass of RepoFile that contains a file that Git reports as modified.
 */
public class ModifiedRepoFile extends RepoFile {

    ModifiedRepoFile(Path filePath, RepoHelper repo) {
        super(filePath, repo);
        diffButton.setText("MODIFIED");
        diffButton.setId("modifiedDiffButton");
        diffButton.setTooltip(getToolTip("This file was modified after your most recent commit."));
    }

    ModifiedRepoFile(String filePathString, RepoHelper repo) {
        this(Paths.get(filePathString), repo);
    }

    @Override
    protected boolean initialShowPopoverSetting() {
        return true;
    }

    @Override public boolean canAdd() { return true; }

    @Override public boolean canRemove() { return false; }
}
