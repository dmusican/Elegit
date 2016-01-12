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

    // fileLeafs stores all 'leafs' in the directory TreeView:
    private TreeView<RepoFile> directoryTreeView;
    public SessionModel sessionModel;

    public FileStructurePanelView() {
        this.directoryTreeView = new TreeView<>();

        this.directoryTreeView.prefHeightProperty().bind(this.heightProperty());
        this.getChildren().add(this.directoryTreeView);
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
        DirectoryRepoFile rootDirectory = new DirectoryRepoFile("", this.sessionModel.getCurrentRepo());

        TreeItem<RepoFile> rootItem = this.getRootTreeItem(rootDirectory);
        rootItem.setExpanded(true);

        List<RepoFile> filesToShow = this.getFilesToDisplay();
        List<TreeItem<RepoFile>> treeItemsToShow = this.getTreeItems(filesToShow);

        for(TreeItem<RepoFile> treeItem : treeItemsToShow){
            rootItem.getChildren().add(treeItem);
        }

        this.directoryTreeView = new TreeView<>(rootItem);
        this.directoryTreeView.setCellFactory(this.getTreeCellFactory());

        // TreeViews must all have ONE root to hold the leafs. Don't show that root:
        this.directoryTreeView.setShowRoot(false);

        this.directoryTreeView.prefHeightProperty().bind(this.heightProperty());

        Platform.runLater(() -> {
            this.getChildren().clear();
            this.getChildren().add(directoryTreeView);
        });
    }

    protected Callback<TreeView<RepoFile>,TreeCell<RepoFile>> getTreeCellFactory(){
        return null;
    }

    protected abstract List<TreeItem<RepoFile>> getTreeItems(List<RepoFile> repoFiles);

    protected abstract TreeItem<RepoFile> getRootTreeItem(DirectoryRepoFile rootDirectory);

    protected abstract List<RepoFile> getFilesToDisplay() throws GitAPIException, IOException;

    public void setSessionModel(SessionModel sessionModel) {
        this.sessionModel = sessionModel;
    }
}
