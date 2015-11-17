package elegit;

import org.eclipse.jgit.lib.Repository;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * A subclass of elegit.RepoFile that contains a file that Git is ignoring.
 */
public class IgnoredRepoFile extends RepoFile {

    public IgnoredRepoFile(Path filePath, Repository repo) {
        super(filePath, repo);
        diffButton.setText("IGNORED");
        diffButton.setId("ignoredDiffButton");
    }

    public IgnoredRepoFile(String filePathString, Repository repo) {
        this(Paths.get(filePathString), repo);
    }

    /**
     * When this elegit.RepoFile is checkboxed and the user commits, do nothing.
     */
    @Override public boolean updateFileStatusInRepo() {
        return true;
    }
}
