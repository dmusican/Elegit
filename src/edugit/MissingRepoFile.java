package edugit;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.RmCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;

import java.nio.file.Path;

/**
 * Created by grahamearley on 6/12/15.
 */
public class MissingRepoFile extends RepoFile {
    public MissingRepoFile(String filePathString, Repository repo) {
        super(filePathString, repo);
    }

    public MissingRepoFile(Path filePath, Repository repo) {
        super(filePath, repo);
    }


    /*
    When the checkbox is checked for a missing file, we assume the user wants to
    remove it from the repo.
     */
    @Override public void updateFileStatusInRepo() throws GitAPIException {
        // TODO: Unify this relativization! This code is copied from the SessionModel. Do things in one place only!
        // Relativize the path to the repository, because that's the file structure JGit
        //  looks for in an 'add' command
        Path repoDirectory = this.repo.getWorkTree().toPath();
        Path relativizedPath = repoDirectory.relativize(this.filePath);

        RmCommand rm = new Git(this.repo).rm().addFilepattern(relativizedPath.toString());
        rm.call();
    }

    // TODO: Missing icon instead of text
    @Override public String toString() {
        return "MISSING:" + super.toString();
    }
}
