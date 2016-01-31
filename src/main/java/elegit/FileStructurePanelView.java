package main.java.elegit;

import javafx.application.Platform;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.Region;
import javafx.util.Callback;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;
import java.util.List;

/**
 *
 */
public abstract class FileStructurePanelView extends Region{

    private TreeView<RepoFile> directoryTreeView;
    private TreeItem<RepoFile> treeRoot;

    public SessionModel sessionModel;

    public FileStructurePanelView() {
        this.init();
        this.getChildren().add(this.directoryTreeView);
    }

    public void init(){
        this.directoryTreeView = new TreeView<>();
        this.directoryTreeView.setCellFactory(this.getTreeCellFactory());

        if(this.sessionModel != null) {
            DirectoryRepoFile rootDirectory = new DirectoryRepoFile("", this.sessionModel.getCurrentRepo());
            this.treeRoot = this.getRootTreeItem(rootDirectory);
            this.treeRoot.setExpanded(true);

            this.directoryTreeView.setRoot(this.treeRoot);
        }

        // TreeViews must all have ONE root to hold the leafs. Don't show that root:
        this.directoryTreeView.setShowRoot(false);

        this.directoryTreeView.prefHeightProperty().bind(this.heightProperty());

        Platform.runLater(() -> {
            this.getChildren().clear();
            this.getChildren().add(directoryTreeView);
        });
    }

    /**
     * Draws the directory TreeView by getting the parent directory's RepoFile,
     * populating it with the files it contains, and adding it to the display.
     *
     * FIXME: this method resets the users selections if they've checked any boxes (low priority) and scrolls them to the top (higher priority)
     *
     * @throws GitAPIException if the SessionModel can't get the ParentDirectoryRepoFile.
     */
    public void drawDirectoryView() throws GitAPIException, IOException {
        if(this.sessionModel.getCurrentRepoHelper() == null) return;

        if(this.treeRoot == null || !this.treeRoot.getValue().getRepo().equals(this.sessionModel.getCurrentRepo())) {
            this.init();
        }

        List<RepoFile> filesToShow = this.getFilesToDisplay();
        if (this.treeRoot.isLeaf()) {
            this.addTreeItemsToRoot(filesToShow, this.treeRoot);
        }
    }

    protected Callback<TreeView<RepoFile>,TreeCell<RepoFile>> getTreeCellFactory(){
        return null;
    }

    protected abstract void addTreeItemsToRoot(List<RepoFile> repoFiles, TreeItem<RepoFile> root);

    protected abstract TreeItem<RepoFile> getRootTreeItem(DirectoryRepoFile rootDirectory);

    protected abstract List<RepoFile> getFilesToDisplay() throws GitAPIException, IOException;

    public void setSessionModel(SessionModel sessionModel) {
        this.sessionModel = sessionModel;
    }
}
