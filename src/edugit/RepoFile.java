package edugit;

import javafx.scene.text.Text;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

/**
 *
 * The RepoFile contains a file in the repo and stores any children the file may have
 * (i.e. if it's a directory).
 *
 * For now, each RepoFile instance also contains a copy of the Repository that the file belongs
 * to, because the *currently open* repo may change when we support multiple repositories.
 * TODO: determine if including a Repository instance is necessary.
 *
 * This broad class is extended by more specific types of files you may find in a repository, such as:
 *      - DirectoryRepoFile
 *      - ModifiedRepoFile
 *      - UntrackedRepoFile
 *      - MissingRepoFile.
 *
 * The plain RepoFile class represents an untouched file in the repository that is thus
 * unaffected by commits.
 *
 */
public class RepoFile {

    Path filePath;
    Repository repo;
    protected ArrayList<RepoFile> children; // Only directories will use this!

    Text textLabel;

    public RepoFile(String filePathString, Repository repo) {
        this.repo = repo;
        this.filePath = Paths.get(filePathString);
        this.textLabel = new Text("");
    }

    public RepoFile(Path filePath, Repository repo) {
        this.repo = repo;
        this.filePath = filePath;
        this.textLabel = new Text("");
    }

    /**
     * Performs that 'commit action' for the file when it is checkboxed and
     * the user makes a commit. This method is typically overridden
     * by RepoFile subclasses.
     *
     * In the case of plain RepoFiles, no action is required.
     *
     * @throws GitAPIException if an interaction with Git fails (only applies to subclasses).
     */
    public void updateFileStatusInRepo() throws GitAPIException {
        System.out.printf("This file requires no update: %s\n", this.filePath.toString());
    }

    /**
     * The toString method is overridden because the files are *displayed*
     * by only their name, not the whole path.
     *
     * This is particularly helpful in the WorkingTreePanelView, where
     * the TreeView's leafs contain RepoFiles and presents them by their
     * string representation. Instead of flooding the user with a long directory
     * string, this displays the only info the user really cares about: the file name.
     *
     * @return the RepoFile's file name.
     */
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
        // Files with no children will return null, since this ArrayList was never instantiated.
        return this.children;
    }

    public void addChild(RepoFile repoFile) {
        // Files with no children can't have children added to them!
        System.err.println("Can't add children to this type of RepoFile.");
    }
}
