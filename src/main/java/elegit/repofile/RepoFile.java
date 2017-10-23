package elegit.repofile;

import elegit.*;
import elegit.controllers.SessionController;
import elegit.models.RepoHelper;
import elegit.treefx.CommitTreeController;
import gui.GitIgnoreEditor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.text.Font;
import org.apache.http.annotation.ThreadSafe;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.controlsfx.control.PopOver;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;
import java.nio.file.Path;

/**
 *
 * The RepoFile contains a file in the repo and stores any children the file may have
 * (i.e. if it's a directory).
 *
 * For now, each RepoFile instance also contains a copy of the Repository that the file belongs
 * to, because the *currently open* repo may change when we support multiple repositories.
 *
 * This broad class is extended by more specific types of files you may find in a repository, such as:
 *      - DirectoryRepoFile
 *      - ModifiedRepoFile
 *      - UntrackedRepoFile
 *      - MissingRepoFile.
 *
 * The plain RepoFile class represents an untouched file in the repository that is thus
 * unaffected by commits.
 *
 * This class is a view, controller, and model all mixed in one. That said. the model aspects are minimal, and the
 * view is mostly just a button and a context menu. Most notably, because most of the code is view oriented, ALL OF IT
 * should only be run from the JavaFX thread. In principle, a handful of method could be run elsewhere, but they're
 * fast anyway and not much would be gained; and most of this is FX work that should be done on the thread.
 *
 */
@ThreadSafe
// but only threadsafe because of the asserts on the FX thread nearly everywhere. No guarantees if any of those go;
// this needs to be thought through
public class RepoFile implements Comparable<RepoFile> {

    private final Path filePath;
    private final RepoHelper repo;
    private final Button diffButton;
    private final PopOver diffPopover;
    private final ContextMenu contextMenu;

    protected static final Logger logger = LogManager.getLogger();

    public RepoFile(Path filePath, RepoHelper repo) {
        Main.assertFxThread();
        this.repo = repo;

        if(filePath.isAbsolute()){
            this.filePath = repo.getLocalPath().relativize(filePath);
        } else {
            this.filePath = filePath;
        }

        this.diffPopover = new PopOver();
        this.diffButton = initialDiffButton();

        if (this.diffButton != null) {
            this.diffButton.getStyleClass().add("diffButton");
        }

        this.contextMenu = new ContextMenu();

        MenuItem addToIgnoreItem = new MenuItem("Add to .gitignore...");
        addToIgnoreItem.setOnAction(event -> GitIgnoreEditor.show(this.repo, this.filePath));

        MenuItem checkoutItem = new MenuItem("Checkout...");
        SessionController controller = CommitTreeController.getSessionController();
        checkoutItem.setOnAction(event -> {
            controller.handleCheckoutButton(filePath);
        });

        this.contextMenu.getItems().addAll(addToIgnoreItem, checkoutItem);
    }

    protected Button initialDiffButton() {
        Main.assertFxThread();
        return new Button("UNCHANGED");
    }

    void addDiffPopover() {
        if (this.diffButton != null) {
            this.diffButton.setOnAction(e -> {
                try {
                    this.showDiffPopover(this.diffButton);
                } catch (IOException e1) {
                    logger.error("IOException in creating repo file");
                    logger.debug(e1.getStackTrace());
                    e1.printStackTrace();
                } catch (GitAPIException e1) {
                    logger.error("GitAPIException in creating repo file");
                    logger.debug(e1.getStackTrace());
                    e1.printStackTrace();
                }
            });
        }
    }

    public Button getDiffButton() {
        Main.assertFxThread();
        return diffButton;

    }

    public void setTextIdTooltip(String buttonText, String id, String tooltipText) {
        Main.assertFxThread();
        diffButton.setText(buttonText);
        diffButton.setId(id);
        Tooltip tooltip = new Tooltip(tooltipText);
        tooltip.setFont(new Font(10));
        diffButton.setTooltip(tooltip);
    }

    public void addToContextMenu(MenuItem menuItem) {
        Main.assertFxThread();
        contextMenu.getItems().add(menuItem);
    }

    /**
     *
     * @return whether or not this file can be added (staged)
     */
    public boolean canAdd() throws GitAPIException, IOException {
        return false;
    }

    public boolean canRemove() {
        return true;
    }

    /**
     * The files are *displayed* by only their location relative
     * to the repo, not the whole path.
     *
     * This is particularly helpful in the WorkingTreePanelView, where
     * the TreeView's leafs contain RepoFiles and presents them by their
     * string representation. Instead of flooding the user with a long directory
     * string, this displays the only info the user really cares about: the file name
     * and parent directories.
     *
     * @return the RepoFile's file name.
     */
    @Override
    public String toString() {
        // filePath is potentially share memory; could work hard to isolate it, but it's so fast; just insist
        // it runs on the FX thread
        Main.assertFxThread();
        return this.filePath.getFileName().toString();
    }

    public Path getFilePath() {
        // filePath is potentially share memory; could work hard to isolate it, but it's so fast; just insist
        // it runs on the FX thread
        Main.assertFxThread();
        return this.filePath;
    }

    public RepoHelper getRepo() {
        // repo is potentially share memory; could work hard to isolate it, but it's so fast; just insist
        // it runs on the FX thread
        Main.assertFxThread();
        return this.repo;
    }

    public int getLevelInRepository(){
        // filePath is potentially share memory; could work hard to isolate it, but it's so fast; just insist
        // it runs on the FX thread
        Main.assertFxThread();
        int depth = 0;
        Path p = this.filePath.getParent();
        while(p!=null){
            depth++;
            p = p.getParent();
        }
        return depth;
    }

    public void showDiffPopover(Node owner) throws IOException, GitAPIException {
        Main.assertFxThread();
            contextMenu.hide();

            DiffHelper diffHelper = new DiffHelper(this.filePath, this.repo);
            this.diffPopover.setContentNode(diffHelper.getDiffScrollPane());
            this.diffPopover.setTitle("File Diffs");
            this.diffPopover.show(owner);
    }

    public void showContextMenu(Node owner, double x, double y){
        Main.assertFxThread();
        this.diffPopover.hide();
        this.contextMenu.show(owner, x, y);
    }

    public boolean equals(Object o){
        // filePath is potentially share memory; could work hard to isolate it, but it's so fast; just insist
        // it runs on the FX thread
        Main.assertFxThread();
        if(o != null && o.getClass().equals(this.getClass())){
            RepoFile other = (RepoFile) o;
            return this.filePath.equals(other.filePath) && other.getRepo().getLocalPath().equals(getRepo().getLocalPath());
        }
        return false;
    }

    @Override
    public int compareTo(RepoFile other) {
        // does toString require shared memory? Not sure. For now, simply require must run on FX thread
        // to be safe. If need otherwise, what the heck is default toString doing, and why are we using it?
        Main.assertFxThread();
        return this.toString().compareToIgnoreCase(other.toString());
    }
}
