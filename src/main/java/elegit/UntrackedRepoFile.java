package main.java.elegit;

import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * A subclass of RepoFile that contains a file that Git reports as untracked.
 */
public class UntrackedRepoFile extends RepoFile {

    public UntrackedRepoFile(Path filePath, RepoHelper repo) {
        super(filePath, repo);
        diffButton.setText("UNTRACKED");
        diffButton.setId("untrackedDiffButton");
    }

    public UntrackedRepoFile(String filePathString, RepoHelper repo) {
        this(Paths.get(filePathString), repo);
    }

    /**
     * When this RepoFile is checkboxed and the user commits, we add this file to the repository.
     *
     * @throws GitAPIException if the `git add` command fails.
     */
    @Override public boolean updateFileStatusInRepo() throws GitAPIException {
        AddCommand add = new Git(this.repo.getRepo()).add().addFilepattern(this.filePath.toString());
        add.call();
        return true;
    }
}
