package elegit;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * A subclass of the RepoFile class that holds a reference to
 * and interacts with a file in the repository that was conflicting
 * but was then modified
 */
public class ConflictingThenModifiedRepoFile extends RepoFile {

    public ConflictingThenModifiedRepoFile(Path filePath, RepoHelper repo) {
        super(filePath, repo);
        diffButton.setText("CONFLICTING\nMODIFIED");
        diffButton.setId("conflictingThenModifiedDiffButton");
    }

    public ConflictingThenModifiedRepoFile(String filePathString, RepoHelper repo) {
        this(Paths.get(filePathString), repo);
    }
}
