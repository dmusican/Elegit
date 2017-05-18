package elegit;

import javafx.application.Platform;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.CheckBoxTreeCell;
import javafx.scene.layout.Region;
import javafx.util.Callback;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;
import java.util.List;

/**
 * Super class for the panels that display different files in the repository
 * in a tree structure
 */
public abstract class FileStructurePanelView extends Region{

    private TreeView<RepoFile> directoryTreeView;
    private TreeItem<RepoFile> treeRoot;

    public SessionModel sessionModel;

    /**
     * Simple constructor that calls init()
     */
    public FileStructurePanelView() {
        this.init();

        // This line isn't necessary for displaying the tree, but it does prevent the tabs from getting
        // cut off and displaying a drop down arrow to flip between them.
        // TODO: Figure out why and fix it
        this.getChildren().clear();
        this.getChildren().add(this.directoryTreeView);
    }

    /**
     * Builds a new tree view using the abstract methods to set up a cell
     * factory and add items to the tree.
     */
    public void init(){
        this.directoryTreeView = new TreeView<>();
        this.directoryTreeView.setCellFactory(this.getTreeCellFactory());

        if(this.sessionModel != null) {
            DirectoryRepoFile rootDirectory = new DirectoryRepoFile("", this.sessionModel.getCurrentRepoHelper());
            this.treeRoot = this.getRootTreeItem(rootDirectory);
            this.treeRoot.setExpanded(true);

            this.directoryTreeView.setRoot(this.treeRoot);
        }

        // TreeViews must all have ONE root to hold the leafs. Don't show that root:
        this.directoryTreeView.setShowRoot(false);

        Platform.runLater(() -> {
            this.getChildren().clear();
            this.getChildren().add(directoryTreeView);
        });
    }

    /**
     * Draws the directory TreeView by getting the parent directory's RepoFile,
     * populating it with the files it contains, and adding it to the display.
     *
     * @throws GitAPIException if the SessionModel can't get the ParentDirectoryRepoFile.
     */
    public void drawDirectoryView() throws GitAPIException, IOException {
        if(this.sessionModel.getCurrentRepoHelper() == null) return;

        if(this.treeRoot == null || !this.treeRoot.getValue().getRepo().equals(this.sessionModel.getCurrentRepoHelper())) {
            this.init();
        }

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
     * @return the cell from CheckBoxTreeCell's implementation of a TreeView factory, with
     * an added context menu for the given RepoFile
     */
    protected Callback<TreeView<RepoFile>, TreeCell<RepoFile>> getTreeCellFactory() {
        return arg -> {
            TreeCell<RepoFile> cell = CheckBoxTreeCell.<RepoFile>forTreeView().call(arg);

            cell.setOnContextMenuRequested(event -> {
                if(cell.getTreeItem()!= null)
                    cell.getTreeItem().getValue().showContextMenu(cell, event.getScreenX(), event.getScreenY());
            });
            cell.setOnMouseClicked(event -> {
                if(cell.getTreeItem()!= null) {
                    CheckBoxTreeItem checkBoxFile = (CheckBoxTreeItem) cell.getTreeItem();
                    checkBoxFile.setSelected(!checkBoxFile.isSelected());
                }
            });
            return cell;
        };
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

    /**
     * Set the SessionModel for this view
     * @param sessionModel the model
     */
    public void setSessionModel(SessionModel sessionModel) {
        this.sessionModel = sessionModel;
    }
}
