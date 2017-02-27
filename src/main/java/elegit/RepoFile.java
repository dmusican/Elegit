package elegit;

import elegit.exceptions.MissingRepoException;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.text.Font;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.controlsfx.control.PopOver;
import org.eclipse.jgit.api.Git;
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
 */
public class RepoFile implements Comparable<RepoFile> {

    Path filePath;
    RepoHelper repo;
    static final Logger logger = LogManager.getLogger();
    protected ArrayList<RepoFile> children; // Only directories will use this!

    Button diffButton;

    boolean showPopover;
    PopOver diffPopover;

    ContextMenu contextMenu;

    public RepoFile(Path filePath, RepoHelper repo) {
        this.repo = repo;

        if(filePath.isAbsolute()){
            this.filePath = repo.getLocalPath().relativize(filePath);
        } else {
            this.filePath = filePath;
        }

        showPopover = false;

        this.diffButton = new Button("UNCHANGED");
        this.diffButton.getStyleClass().add("diffButton");

        this.diffPopover = new PopOver();

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

    public RepoFile(String filePathString, RepoHelper repo) {
        this(Paths.get(filePathString), repo);
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
        return this.filePath.getFileName().toString();
    }

    public Path getFilePath() {
        return this.filePath;
    }

    public RepoHelper getRepo() {
        return this.repo;
    }

    public int getLevelInRepository(){
        int depth = 0;
        Path p = this.filePath.getParent();
        while(p!=null){
            depth++;
            p = p.getParent();
        }
        return depth;
    }

    public ArrayList<RepoFile> getChildren() {
        // Files with no children will return null, since this ArrayList was never instantiated.
        return this.children;
    }

    public void addChild(RepoFile repoFile) {
        // Files with no children can't have children added to them!
        System.err.println("Can't add children to this type of RepoFile.");
    }

    public void showDiffPopover(Node owner) throws IOException, GitAPIException {
        if(showPopover) {
            contextMenu.hide();

            DiffHelper diffHelper = new DiffHelper(this.filePath, this.repo);
            this.diffPopover.setContentNode(diffHelper.getDiffScrollPane());
            this.diffPopover.setTitle("File Diffs");
            this.diffPopover.show(owner);
        }
    }

    public void showContextMenu(Node owner, double x, double y){
        this.diffPopover.hide();
        this.contextMenu.show(owner, x, y);
    }

    protected Tooltip getToolTip(String message) {
        Tooltip tooltip = new Tooltip(message);
        tooltip.setFont(new Font(10));
        return tooltip;
    }

    public boolean equals(Object o){
        if(o != null && o.getClass().equals(this.getClass())){
            RepoFile other = (RepoFile) o;
            return this.filePath.equals(other.filePath) && other.getRepo().getLocalPath().equals(getRepo().getLocalPath());
        }
        return false;
    }

    @Override
    public int compareTo(RepoFile other) {
        return this.toString().compareToIgnoreCase(other.toString());
    }
}
