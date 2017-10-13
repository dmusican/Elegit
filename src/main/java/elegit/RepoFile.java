package elegit;

import elegit.controllers.SessionController;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.text.Font;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.controlsfx.control.PopOver;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

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
 * This class is a view; it does lots of work involving buttons, etc. It should only be run from the Java FX thread.
 *
 */
public class RepoFile implements Comparable<RepoFile> {

    private final Path filePath;
    private final RepoHelper repo;
    private final Button diffButton;
    private final PopOver diffPopover;
    private final ContextMenu contextMenu;

    protected static final Logger logger = LogManager.getLogger();

    public RepoFile(Path filePath, RepoHelper repo) {
        // This completely wires up a diffButton that eventually gets passed off to someone who will run it on
        // the FX thread, but none of its code actually gets run here
        //////////////Main.assertFxThread();
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
        SessionController controller = CommitTreeController.sessionController;
        checkoutItem.setOnAction(event -> {
            controller.handleCheckoutButton(filePath);
        });

        this.contextMenu.getItems().addAll(addToIgnoreItem, checkoutItem);
    }

    protected Button initialDiffButton() {
        // Creates a button, that will get clicked someday on the FX thread, but isn't actually getting clicked here
        ///////////Main.assertFxThread();
        return new Button("UNCHANGED");
    }

    protected void addDiffPopover() {
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
        return diffButton;

    }

    public void setTextIdTooltip(String buttonText, String id, String tooltipText) {
        diffButton.setText(buttonText);
        diffButton.setId(id);
        Tooltip tooltip = new Tooltip(tooltipText);
        tooltip.setFont(new Font(10));
        diffButton.setTooltip(tooltip);
    }

    public void addToContextMenu(MenuItem menuItem) {
        contextMenu.getItems().add(menuItem);
    }

    /**
     *
     * @return whether or not this file can be added (staged)
     */
    public boolean canAdd() throws GitAPIException, IOException {
        // Model
        ///////Main.assertFxThread();
        return false;
    }

    public boolean canRemove() {
        // Model
        ///////Main.assertFxThread();
        Main.assertFxThread();
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
        ///////Main.assertFxThread();
        return this.filePath.getFileName().toString();
    }

    public Path getFilePath() {
        //////////////////Main.assertFxThread();
        return this.filePath;
    }

    public RepoHelper getRepo() {
        // Doesn't explicitly need to run on FX thread
        ////Main.assertFxThread();
        return this.repo;
    }

    public int getLevelInRepository(){
        ////// THere are no graphics at all in this method, and it can easily run wherever it wants
        ///Main.assertFxThread();
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
        /// This is all model-style work; it's merely checking paths for equality
        //Main.assertFxThread();
        if(o != null && o.getClass().equals(this.getClass())){
            RepoFile other = (RepoFile) o;
            return this.filePath.equals(other.filePath) && other.getRepo().getLocalPath().equals(getRepo().getLocalPath());
        }
        return false;
    }

    @Override
    public int compareTo(RepoFile other) {
        ///////////////Main.assertFxThread();
        return this.toString().compareToIgnoreCase(other.toString());
    }
}
