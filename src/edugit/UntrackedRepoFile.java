package edugit;

import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;

import java.nio.file.Path;

/**
 * Created by grahamearley on 6/12/15.
 */
public class UntrackedRepoFile extends RepoFile {
    public UntrackedRepoFile(String filePathString, Repository repo) {
        super(filePathString, repo);
    }

    public UntrackedRepoFile(Path filePath, Repository repo) {
        super(filePath, repo);
    }

    /*
    When the checkbox is checked for an untracked file, we assume the user wants to
    add it to the repo.
     */
    @Override public void updateFileStatusInRepo() throws GitAPIException {
        // TODO: Unify this relativization! This code is copied from the SessionModel. Do things in one place only!
        // Relativize the path to the repository, because that's the file structure JGit
        //  looks for in an 'add' command
        Path repoDirectory = this.repo.getWorkTree().toPath();
        Path relativizedPath = repoDirectory.relativize(this.filePath);

        AddCommand add = new Git(this.repo).add().addFilepattern(relativizedPath.toString());
        add.call();
    }

    // TODO: Untracked icon instead of text
    @Override public String toString() {
        return "UNTRACKED:" + super.toString();
    }
}
