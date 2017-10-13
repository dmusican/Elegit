package elegit;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * A subclass of RepoFile that contains a file that Git is ignoring.
 */
public class StagedRepoFile extends RepoFile {

    StagedRepoFile(Path filePath, RepoHelper repo) {
        super(filePath, repo);
        setTextIdTooltip("STAGED","stagedDiffButton",
        "This file has a version stored in your git index\nand is ready to commit.");
    }

    StagedRepoFile(String filePathString, RepoHelper repo) {
        this(Paths.get(filePathString), repo);
    }

    @Override public boolean canAdd() {
        return false;
    }

    @Override public boolean canRemove() {
        return true;
    }
}
