package edugit;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.text.Text;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

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

    /**
     * When this RepoFile is checkboxed and the user commits,
     * open the conflicting file in an external editor.
     *
     */
    @Override public void updateFileStatusInRepo() throws GitAPIException {
        Alert alert = new Alert(Alert.AlertType.WARNING);

        ButtonType resolveButton = new ButtonType("Resolve conflicts in editor");
        ButtonType addButton = new ButtonType("Add and commit conflicting file");

        alert.getButtonTypes().setAll(addButton, resolveButton);

        alert.setTitle("Adding conflicted file");
        alert.setHeaderText("You're adding a conflicted file to the commit");
        alert.setContentText("Make sure to resolve to conflicts first! After resolving them, you can add the " +
                "previously conflicting file to the commit. What do you want to do?");

        Optional<ButtonType> result = alert.showAndWait();

        if (result.get() == resolveButton){
            Desktop desktop = Desktop.getDesktop();

            File workingDirectory = this.repo.getWorkTree();
            File unrelativized = new File(workingDirectory, this.filePath.toString());

            try {
                desktop.open(unrelativized);
            } catch (IOException e) {
                // Todo: put exception into method signature (after debugging)
                e.printStackTrace();
            }
        } else if (result.get() == addButton) {
            AddCommand add = new Git(this.repo).add().addFilepattern(this.filePath.toString());
            add.call();
        } else {
            // User cancelled the dialog
        }
        // TODO? add option for further commit help for first-timers? (like a manual page)
    }
}
