package elegit;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
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
    }

    public ConflictingThenModifiedRepoFile(String filePathString, RepoHelper repo) {
        this(Paths.get(filePathString), repo);
    }

    /**
     * When this RepoFile is checkboxed and the user commits, display an alert.
     */
    @Override public boolean updateFileStatusInRepo() throws GitAPIException, IOException {
        ReentrantLock lock = new ReentrantLock();
        Condition finishedAlert = lock.newCondition();

        Platform.runLater(() -> {
            logger.warn("Notification about conflicting the modified file");
            lock.lock();
            try{
                Alert alert = new Alert(Alert.AlertType.WARNING);

                ButtonType commitButton = new ButtonType("Commit");
                ButtonType buttonTypeCancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

                alert.getButtonTypes().setAll(commitButton, buttonTypeCancel);

                alert.setResizable(true);
                alert.getDialogPane().setPrefSize(300, 200);

                alert.setTitle("Adding previously conflicting file");
                alert.setHeaderText("You are adding a conflicting file that was recently modified to the commit");
                alert.setContentText("If the file is what you want it to be, you should commit. Otherwise, modify the file accordingly.");

                Optional<ButtonType> result = alert.showAndWait();

                if(result.get() == commitButton){
                    logger.info("Chose to resolve conflicts");
                    setResultType("commit");
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
            if(resultType.equals("commit")){
                AddCommand add = new Git(this.repo.getRepo()).add().addFilepattern(this.filePath.toString());
                add.call();
                return true;
            }
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

