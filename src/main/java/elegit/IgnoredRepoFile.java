package elegit;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * A subclass of RepoFile that contains a file that Git is ignoring.
 */
public class IgnoredRepoFile extends RepoFile {

    private IgnoredRepoFile(Path filePath, RepoHelper repo) {
        super(filePath, repo);
        setTextIdTooltip(
                "IGNORED",
                "ignoredDiffButton",
                "This file is being ignored because it's in your .gitignore.\n" +
                        "Remove it from your .gitignore if you want to add it to git");
    }

    IgnoredRepoFile(String filePathString, RepoHelper repo) {
        this(Paths.get(filePathString), repo);
    }

    @Override public boolean canAdd() { return false; }

    @Override public boolean canRemove() { return false; }
}
