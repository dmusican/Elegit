package elegit;

import org.eclipse.jgit.lib.Repository;

import java.nio.file.Path;
import java.util.ArrayList;

/**
 * A subclass of elegit.RepoFile that contains a directory within the repository.
 * This subclass is different from its parent in that it can hold children.
 */
public class DirectoryRepoFile extends RepoFile {

    public DirectoryRepoFile(String filePathString, Repository repo) {
        super(filePathString, repo);
        this.diffButton = null;
        this.children = new ArrayList<>();
    }

    public DirectoryRepoFile(Path filePath, Repository repo) {
        super(filePath, repo);
        this.diffButton = null;
        this.children = new ArrayList<>();
    }

    @Override
    public void addChild(RepoFile repoFile) {
        this.children.add(repoFile);
    }

    @Override
    public ArrayList<RepoFile> getChildren() {
        return children;
    }
}
