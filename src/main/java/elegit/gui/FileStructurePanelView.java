package elegit.gui;

import elegit.Main;
import elegit.models.SessionModel;
import elegit.repofile.DirectoryRepoFile;
import elegit.repofile.RepoFile;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.Region;
import javafx.util.Callback;
import net.jcip.annotations.ThreadSafe;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;
import java.util.List;

/**
 * Super class for the panels that display different files in the repository
 * in a tree structure
 *
 * This is a view and controller merged together
 */
@ThreadSafe
// because of all the assert statements I have throughout. This is a view class, and at least for now,
// all methods must run on the FX thread. This class loses threadsafeness if any of that is changed.
public abstract class FileStructurePanelView extends Region{

    private TreeView<RepoFile> directoryTreeView;
    private TreeItem<RepoFile> treeRoot;

    /**
     * Builds a new tree view using the methods to set up a cell
     * factory and add items to the tree.
     */
    public void init(){
        // DRM: May not definitively need to be on FX thread, but putting it there for now to get architecture in shape
        Main.assertFxThread();
        this.directoryTreeView = new TreeView<>();
        this.directoryTreeView.setCellFactory(this.getTreeCellFactory());

        DirectoryRepoFile rootDirectory = new DirectoryRepoFile("", SessionModel.getSessionModel().getCurrentRepoHelper());
        this.treeRoot = this.getRootTreeItem(rootDirectory);
        this.treeRoot.setExpanded(true);

        this.directoryTreeView.setRoot(this.treeRoot);

        // TreeViews must all have ONE root to hold the leafs. Don't show that root:
        this.directoryTreeView.setShowRoot(false);

        this.getChildren().clear();
        this.getChildren().add(directoryTreeView);
    }

    /**
     * Draws the directory TreeView by getting the parent directory's RepoFile,
     * populating it with the files it contains, and adding it to the display.
     *
     * @throws GitAPIException if the SessionModel can't get the ParentDirectoryRepoFile.
     */
    public void drawDirectoryView() throws GitAPIException, IOException {
        // DRM: This is likely slow, and I may want to think about how to push some of this off on threads. For now,
        // however, I've got to back up and get this straight on the FX thread so I can get the architecture in shape.
        Main.assertFxThread();
        if(SessionModel.getSessionModel().getCurrentRepoHelper() == null) return;

        if(this.treeRoot.getValue().getRepo() == null || !this.treeRoot.getValue().getRepo().equals(SessionModel.getSessionModel().getCurrentRepoHelper())) {
            this.init();
        }
        directoryTreeView.setCellFactory(this.getTreeCellFactory());
        List<RepoFile> filesToShow = this.getFilesToDisplay();
        this.addTreeItemsToRoot(filesToShow, this.treeRoot);
    }

    public void resetFileStructurePanelView() {
        this.directoryTreeView = new TreeView<>();
        this.directoryTreeView.setCellFactory(this.getTreeCellFactory());
        this.getChildren().clear();
        this.getChildren().add(directoryTreeView);
    }

    /**
     * @return the cell factory for this tree view. Defaults to null, which means the default
     * factory will be used
     */
    protected Callback<TreeView<RepoFile>,TreeCell<RepoFile>> getTreeCellFactory(){
        return null;
    }

    /**
     * Puts the given RepoFiles under the given root of the tree
     * @param repoFiles the files to add to the tree
     * @param root the root of the tree
     */
    protected abstract void addTreeItemsToRoot(List<RepoFile> repoFiles, TreeItem<RepoFile> root);

    /**
     * @param rootDirectory RepoFile corresponding to the root of the repository
     * @return a TreeItem that will server as the root of this TreeView
     */
    protected abstract TreeItem<RepoFile> getRootTreeItem(DirectoryRepoFile rootDirectory);

    /**
     * @return the list of all files to display
     * @throws GitAPIException
     * @throws IOException
     */
    protected abstract List<RepoFile> getFilesToDisplay() throws GitAPIException, IOException;

    public void showDebugOutput() {
        System.out.println("What's in the tree? ");
        for (TreeItem item : treeRoot.getChildren()) {
            System.out.println(item);
        }
    }


}
