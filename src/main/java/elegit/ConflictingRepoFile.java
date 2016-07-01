package elegit;

import javafx.application.Platform;
import javafx.scene.control.*;
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
    @Override public boolean updateFileStatusInRepo() throws GitAPIException, IOException {
        ReentrantLock lock = new ReentrantLock();
        Condition finishedAlert = lock.newCondition();

        Platform.runLater(() -> {
            logger.warn("Notification about conflicting file");
            lock.lock();
            try{
                Alert alert = new Alert(Alert.AlertType.WARNING);

                ButtonType resolveButton = new ButtonType("Open Editor");
                ButtonType addButton = new ButtonType("Commit");
                ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
                ButtonType helpButton = new ButtonType("Help", ButtonBar.ButtonData.HELP);

                alert.getButtonTypes().setAll(helpButton, resolveButton, addButton, cancelButton);

                alert.setResizable(true);
                alert.getDialogPane().setPrefSize(450, 200);

                alert.setTitle("Warning: conflicting file");
                alert.setHeaderText("You're adding a conflicting file to the commit");
                alert.setContentText("You can open an editor to resolve the conflicts, or commit the changes anyways. What do you want to do?");

                ImageView img = new ImageView(new javafx.scene.image.Image("/elegit/conflict.png"));
                img.setFitHeight(40);
                img.setFitWidth(80);
                img.setPreserveRatio(true);
                alert.setGraphic(img);

                Optional<ButtonType> result = alert.showAndWait();

                if(result.get() == resolveButton){
                    logger.info("Chose to resolve conflicts");
                    setResultType("resolve");
                }else if(result.get() == addButton){
                    logger.info("Chose to add file");
                    setResultType("add");
                }else if(result.get() == helpButton) {
                    logger.info("Chose to get help");
                    setResultType("help");
                } else{
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
            }else if (resultType.equals("help")){
                System.out.println("need to add help option");
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
