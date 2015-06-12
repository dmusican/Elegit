package edugit;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

/**
 * Created by grahamearley on 6/12/15.
 */
public class RepoFile {

    Path filePath;
    Repository repo;
    protected ArrayList<RepoFile> children; // Only directories will use this!


    public RepoFile(String filePathString, Repository repo) {
        this.repo = repo;
        this.filePath = Paths.get(filePathString);
        this.children = new ArrayList<>();
    }

    public RepoFile(Path filePath, Repository repo) {
        this.repo = repo;
        this.filePath = filePath;
        this.children = new ArrayList<>();
    }

    public void updateFileStatusInRepo() throws GitAPIException {
        System.out.printf("This file requires no update: %s\n", this.filePath.toString());
    }

    // The string of the file path should be just the actual file/directory name.
    @Override
    public String toString() {
        return this.filePath.getFileName().toString();
    }

    public Path getFilePath() {
        return this.filePath;
    }

    public Repository getRepo() {
        return this.repo;
    }

    public ArrayList<RepoFile> getChildren() {
        return this.children;
    }

    public void addChild() {
        System.err.println("Can't add children to this type of RepoFile.");
    }
}
