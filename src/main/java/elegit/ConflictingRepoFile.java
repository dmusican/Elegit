package elegit;

import elegit.exceptions.MissingRepoException;
import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Dialog;
import javafx.scene.control.MenuItem;
import javafx.scene.image.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A subclass of the RepoFile class that holds a reference to
 * and interacts with a file in the repository that has conflicts
 * in git.
 */
public class ConflictingRepoFile extends RepoFile {

    private String resultType;

    static final Logger logger = LogManager.getLogger();

    public ConflictingRepoFile(Path filePath, RepoHelper repo) {
        super(filePath, repo);
        setTextIdTooltip("CONFLICTING","conflictingDiffButton",
                "This file caused a merge conflict.\nEdit the file to fix the conflict.");
        MenuItem resolveMerge = new MenuItem("Resolve conflict...");
        addToContextMenu(resolveMerge);
    }

    public ConflictingRepoFile(String filePathString, RepoHelper repo) {
        this(Paths.get(filePathString), repo);
    }

    @Override public boolean canAdd() throws GitAPIException, IOException{
        Main.assertFxThread();
        logger.warn("Notification about conflicting file");
        resultType = PopUpWindows.showCommittingConflictingFileAlert();
        switch (resultType) {
            case "resolve":
                Desktop desktop = Desktop.getDesktop();

                File workingDirectory = this.getRepo().getRepo().getWorkTree();
                File unrelativized = new File(workingDirectory, this.getFilePath().toString());

                System.out.println("before");
                desktop.open(unrelativized);
                System.out.println("after");
                break;
            case "add":
                return true;
            case "help":
                PopUpWindows.showConflictingHelpAlert();
                break;
        }
        return false;
    }

    @Override public boolean canRemove() {
        return true;
    }
}
