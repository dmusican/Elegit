package elegit;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * A subclass of RepoFile that contains a file that Git reports as missing.
 *
 * This class is a view; it does lots of work involving buttons, etc. It should only be run from the Java FX thread.
 */
public class MissingRepoFile extends RepoFile {

    MissingRepoFile(Path filePath, RepoHelper repo) {
        super(filePath, repo);
        Main.assertFxThread();
        setTextIdTooltip("MISSING", "missingDiffButton", "This file is missing.");
    }

    MissingRepoFile(String filePathString, RepoHelper repo) {
        this(Paths.get(filePathString), repo);
        Main.assertFxThread();
    }

    @Override public boolean canAdd() {
        Main.assertFxThread();
        return false;
    }

    @Override public boolean canRemove() {
        Main.assertFxThread();
        return true;
    }
}
