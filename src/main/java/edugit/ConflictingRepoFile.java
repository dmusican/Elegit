package main.java.edugit;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * A subclass of the RepoFile class that holds a reference to
 * and interacts with a file in the repository that has conflicts
 * in git.
 */
public class ConflictingRepoFile extends RepoFile {

    public ConflictingRepoFile(Path filePath, Repository repo) {
        super(filePath, repo);
        diffButton.setText("CONFLICTING");
        diffButton.setId("conflictingDiffButton");
    }

    public ConflictingRepoFile(String filePathString, Repository repo) {
        this(Paths.get(filePathString), repo);
    }

    /**
     * When this RepoFile is checkboxed and the user commits,
     * open the conflicting file in an external editor.
     */
    @Override public void updateFileStatusInRepo() throws GitAPIException {
        Alert alert = new Alert(Alert.AlertType.WARNING);

        ButtonType resolveButton = new ButtonType("Resolve conflicts in editor");
        ButtonType addButton = new ButtonType("Commit conflicting file");

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
