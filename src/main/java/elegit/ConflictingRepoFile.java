package elegit;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Tooltip;
import javafx.scene.text.*;
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
    @Override public boolean updateFileStatusInRepo() throws GitAPIException, IOException {
        ReentrantLock lock = new ReentrantLock();
        Condition finishedAlert = lock.newCondition();

        Platform.runLater(() -> {
            logger.warn("Notification about conflicting file");
            lock.lock();
            try{
                Alert alert = new Alert(Alert.AlertType.WARNING);

                ButtonType resolveButton = new ButtonType("Resolve conflicts in editor");
                ButtonType addButton = new ButtonType("Commit conflicting file");
                ButtonType buttonTypeCancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

                alert.getButtonTypes().setAll(addButton, resolveButton, buttonTypeCancel);

                alert.setResizable(true);
                alert.getDialogPane().setPrefSize(600, 200);

                alert.setTitle("Adding conflicted file");
                alert.setHeaderText("You're adding a conflicted file to the commit");
                alert.setContentText("Make sure to resolve to conflicts first! After resolving them, you can add the " +
                        "previously conflicting file to the commit. What do you want to do?");

                Optional<ButtonType> result = alert.showAndWait();

                if(result.get() == resolveButton){
                    logger.info("Chose to resolve conflicts");
                    setResultType("resolve");
                }else if(result.get() == addButton){
                    logger.info("Chose to add file");
                    setResultType("add");
                }else{
                    // User cancelled the dialog
                    logger.info("Cancelled dialog");
                    setResultType("cancel");
                }

                finishedAlert.signal();
            }finally{
                lock.unlock();
            }
        });

        lock.lock();
        try{
            finishedAlert.await();
            if(resultType.equals("resolve")){
                Desktop desktop = Desktop.getDesktop();

                File workingDirectory = this.repo.getRepo().getWorkTree();
                File unrelativized = new File(workingDirectory, this.filePath.toString());

                desktop.open(unrelativized);
            }else if(resultType.equals("add")){
                AddCommand add = new Git(this.repo.getRepo()).add().addFilepattern(this.filePath.toString());
                add.call();
                return true;
            }else{
                // User cancelled the dialog
            }
            // TODO? add option for further commit help for first-timers? (like a manual page)
        }catch(InterruptedException ignored){
        }finally{
            lock.unlock();
        }
        return false;
    }

    private void setResultType(String s){
        resultType = s;
    }
}
