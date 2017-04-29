package elegit;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * A subclass of RepoFile that contains a file that Git reports as untracked.
 */
public class UntrackedRepoFile extends RepoFile {

    UntrackedRepoFile(Path filePath, RepoHelper repo) {
        super(filePath, repo);
        diffButton.setText("UNTRACKED");
        diffButton.setId("untrackedDiffButton");
        diffButton.setTooltip(getToolTip("This file has not been added to git. Commit to add it."));
    }

    UntrackedRepoFile(String filePathString, RepoHelper repo) {
        this(Paths.get(filePathString), repo);
    }

    @Override public boolean canAdd() { return true; }

    @Override public boolean canRemove() { return false; }
}
