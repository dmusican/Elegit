package edugit;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by grahamearley on 6/12/15.
 */
public abstract class RepoFile {

    Path filePath;
    Repository repo;

    public RepoFile(String filePathString, Repository repo) {
        this.filePath = Paths.get(filePathString);
        this.repo = repo;
    }

    // How should Git handle changes in the file? -rm? -add?
    // That's up to the subclasses!
    public abstract void updateFileStatusInRepo() throws GitAPIException;

    // The string of the file path should be just the actual file/directory name.
    @Override
    public String toString() {
        return this.filePath.getFileName().toString();
    }

}
