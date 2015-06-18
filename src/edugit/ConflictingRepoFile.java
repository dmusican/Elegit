package edugit;

import javafx.scene.text.Text;
import org.eclipse.jgit.lib.Repository;

import java.nio.file.Path;

/**
 * Created by grahamearley on 6/18/15.
 */
public class ConflictingRepoFile extends RepoFile {
    public ConflictingRepoFile(String filePathString, Repository repo) {
        super(filePathString, repo);
        this.textLabel = new Text("CONFLICTING");
        textLabel.setId("conflictingText");
    }

    public ConflictingRepoFile(Path filePath, Repository repo) {
        super(filePath, repo);
        this.textLabel = new Text("CONFLICTING");
        textLabel.setId("conflictingText");
    }
}
