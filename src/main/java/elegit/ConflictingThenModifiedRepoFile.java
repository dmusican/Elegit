package elegit;

import elegit.exceptions.MissingRepoException;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Tooltip;
import javafx.scene.text.*;
import javafx.scene.text.Font;
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
 * and interacts with a file in the repository that was conflicting
 * but was then modified after the user was informed of the conflict
 */
public class ConflictingThenModifiedRepoFile extends RepoFile {

    private String resultType;

    public ConflictingThenModifiedRepoFile(Path filePath, RepoHelper repo) {
        super(filePath, repo);
        diffButton.setText("CONFLICTING\nMODIFIED");
        diffButton.setId("conflictingThenModifiedDiffButton");
        Tooltip tooltip = new Tooltip("This file was conflicting, but was recently modified.\nCommit if the changes are finalized.");
        tooltip.setFont(new Font(12));
        diffButton.setTooltip(tooltip);
    }

    public ConflictingThenModifiedRepoFile(String filePathString, RepoHelper repo) {
        this(Paths.get(filePathString), repo);
    }

    /**
     * When this RepoFile is checkboxed and the user commits, display an alert.
     */
    @Override public boolean updateFileStatusInRepo() throws GitAPIException, IOException, MissingRepoException {
        ReentrantLock lock = new ReentrantLock();
        Condition finishedAlert = lock.newCondition();

        Platform.runLater(() -> {
            logger.warn("Notification about conflicting the modified file");
            lock.lock();
            try{
                resultType = PopUpWindows.showCommittingConflictingThenModifiedFileAlert();
                finishedAlert.signal();
            }finally{
                lock.unlock();
            }
        });

        lock.lock();
        try{
            finishedAlert.await();
            if(resultType.equals("commit")){
                this.repo.addFilePath(this.filePath);
                return true;
            }
        }catch(InterruptedException ignored){
        }finally{
            lock.unlock();
        }
        return false;
    }
}

