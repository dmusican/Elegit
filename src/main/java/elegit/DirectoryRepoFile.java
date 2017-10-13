package elegit;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

/**
 * A subclass of RepoFile that contains a directory within the repository.
 * This subclass is different from its parent in that it can hold children.
 */
public class DirectoryRepoFile extends RepoFile {

    private Boolean showFullPath;

    public DirectoryRepoFile(Path filePath, RepoHelper repo) {
        super(filePath, repo);
        this.diffButton = null;
        this.showFullPath = false;
    }

    public DirectoryRepoFile(String filePathString, RepoHelper repo) {
        this(Paths.get(filePathString), repo);
    }

    @Override
    public String toString() {
        return this.showFullPath ? this.getFilePath().toString() : this.getFilePath().getFileName().toString();
    }

    @Override public boolean canAdd() {
        return true;
    }

    @Override public boolean canRemove() {
        return true;
    }

    public void setShowFullPath(Boolean b) { this.showFullPath = b; }
}
