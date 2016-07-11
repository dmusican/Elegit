package elegit;

import elegit.exceptions.MissingRepoException;
import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Dialog;
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
        diffButton.setText("CONFLICTING");
        diffButton.setId("conflictingDiffButton");
        Tooltip tooltip = new Tooltip("This file caused a merge conflict.\nEdit the file to fix the conflict.");
        tooltip.setFont(new javafx.scene.text.Font(12));
        diffButton.setTooltip(tooltip);
    }

    public ConflictingRepoFile(String filePathString, RepoHelper repo) {
        this(Paths.get(filePathString), repo);
    }

    /**
     * When this RepoFile is checkboxed and the user commits, display an alert.
     */
    @Override public boolean updateFileStatusInRepo() throws GitAPIException, IOException, MissingRepoException {
        ReentrantLock lock = new ReentrantLock();
        Condition finishedAlert = lock.newCondition();

        Platform.runLater(() -> {
            logger.warn("Notification about conflicting file");
            lock.lock();
            try{
                resultType = PopUpWindows.showCommittingConflictingFileAlert();
                finishedAlert.signal();
            }finally{
                lock.unlock();
            }
        });

        lock.lock();
        try{
            finishedAlert.await();
            switch (resultType) {
                case "resolve":
                    Desktop desktop = Desktop.getDesktop();

                    File workingDirectory = this.repo.getRepo().getWorkTree();
                    File unrelativized = new File(workingDirectory, this.filePath.toString());

                    desktop.open(unrelativized);
                    break;
                case "add":
                    this.repo.add(this.filePath.toString());
                    return true;
                case "help":
                    PopUpWindows.showConflictingHelpAlert();
                    break;
            }
        }catch(InterruptedException ignored){
        }finally{
            lock.unlock();
        }
        return false;
    }
}
