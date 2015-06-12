package edugit;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;

import java.nio.file.Path;
import java.util.ArrayList;

/**
 * Created by grahamearley on 6/12/15.
 */
public class DirectoryRepoFile extends RepoFile {

    public DirectoryRepoFile(String filePathString, Repository repo) {
        super(filePathString, repo);
    }

    public DirectoryRepoFile(Path filePath, Repository repo) {
        super(filePath, repo);
    }

    public void addChildFile(RepoFile repoFile) {
        this.children.add(repoFile);
    }

    @Override
    public ArrayList<RepoFile> getChildren() {
        return children;
    }
}
