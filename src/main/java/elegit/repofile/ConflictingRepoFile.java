package elegit.repofile;

import elegit.Main;
import elegit.controllers.ConflictManagementToolController;
import elegit.controllers.SessionController;
import elegit.gui.GitIgnoreEditor;
import elegit.gui.PopUpWindows;
import elegit.models.RepoHelper;
import elegit.treefx.CommitTreeController;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import javafx.scene.control.MenuItem;
import net.jcip.annotations.ThreadSafe;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.awt.*;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

/**
 * A subclass of the RepoFile class that holds a reference to
 * and interacts with a file in the repository that has conflicts
 * in git.
 * This class is a view, controller, and model all mixed in one. That said. the model aspects are minimal, and the
 * view is mostly just a button and a context menu. Most notably, because most of the code is view oriented, ALL OF IT
 * should only be run from the JavaFX thread. In principle, a handful of method could be run elsewhere, but they're
 * fast anyway and not much would be gained; and most of this is FX work that should be done on the thread.
 *
 */
@ThreadSafe
// but only threadsafe because of the asserts on the FX thread nearly everywhere. No guarantees if any of those go;
// this needs to be thought through
public class ConflictingRepoFile extends RepoFile {

    private String resultType;
    private SessionController controller;

    static final Logger logger = LogManager.getLogger();

    public ConflictingRepoFile(Path filePath, RepoHelper repo) {
        super(filePath, repo);
        controller = CommitTreeController.getSessionController();
        setTextIdTooltip("CONFLICTING","conflictingDiffButton",
                "This file caused a merge conflict.\nEdit the file to fix the conflict.");
        MenuItem resolveMerge = new MenuItem("Resolve conflict...");
        resolveMerge.setId("resolveConflicts");
        addToContextMenu(resolveMerge);

        // Open conflict management tool
        resolveMerge.setOnAction(event -> controller.handleOpenConflictManagementTool(this.getRepo().
                getLocalPath().toString(), this.getFilePath().toString()));
    }

    public ConflictingRepoFile(String filePathString, RepoHelper repo) {
        this(Paths.get(filePathString), repo);
    }

    @Override public boolean canAdd() throws GitAPIException, IOException{
        Main.assertFxThread();
        logger.warn("Notification about conflicting file");
        resultType = PopUpWindows.showCommittingConflictingFileAlert();
        switch (resultType) {
            case "tool":
                // Open conflict management tool
                controller.handleOpenConflictManagementTool(this.getRepo().getLocalPath().toString(),
                        this.getFilePath().toString());
                break;
            case "editor":
                Desktop desktop = Desktop.getDesktop();

                File workingDirectory = this.getRepo().getRepo().getWorkTree();
                File unrelativized = new File(workingDirectory, this.getFilePath().toString());

                // Desktop.open can't be run on the FX thread, apparently. I tried it and it hung;
                // I found some SO postings that confirmed that
                Observable.just(unrelativized)
                        .subscribeOn(Schedulers.io())
                        .subscribe(desktop::open, Throwable::printStackTrace);
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
