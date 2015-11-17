package elegit;

import javafx.application.Platform;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.CheckBoxTreeCell;
import javafx.scene.layout.Region;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public abstract class FileStructurePanelView extends Region{

    // fileLeafs stores all 'leafs' in the directory TreeView:
    private TreeView<RepoFile> directoryTreeView;
    public ArrayList<TreeItem<RepoFile>> fileLeafs;
    public SessionModel sessionModel;

    public FileStructurePanelView() {
        this.fileLeafs = new ArrayList<>();
        this.directoryTreeView = new TreeView<>();

        this.directoryTreeView.prefHeightProperty().bind(this.heightProperty());
        this.getChildren().add(this.directoryTreeView);
    }

    /**
     * Draws the directory TreeView by getting the parent directory's elegit.RepoFile,
     * populating it with the files it contains, and adding it to the display.
     *
     * FIXME: this method resets the users selections if they've checked any boxes (low priority)
     *
     * @throws GitAPIException if the elegit.SessionModel can't get the ParentDirectoryRepoFile.
     */
    public void drawDirectoryView() throws GitAPIException{

        if(this.sessionModel.getCurrentRepoHelper() == null) return;
        DirectoryRepoFile rootDirectory = new DirectoryRepoFile("", this.sessionModel.getCurrentRepo());

        fileLeafs = new ArrayList<>(fileLeafs.size());

        TreeItem<RepoFile> rootItem = this.getRootTreeItem(rootDirectory);
        rootItem.setExpanded(true);

        for(RepoFile changedRepoFile : this.getFilesToDisplay()){
            TreeItem<RepoFile> leaf = this.getTreeItem(changedRepoFile);
            rootItem.getChildren().add(leaf);
            this.fileLeafs.add(leaf);
        }

        this.directoryTreeView = new TreeView<>(rootItem);
        this.directoryTreeView.setCellFactory(CheckBoxTreeCell.<RepoFile>forTreeView());

        // TreeViews must all have ONE root to hold the leafs. Don't show that root:
        this.directoryTreeView.setShowRoot(false);

        this.directoryTreeView.prefHeightProperty().bind(this.heightProperty());

        Platform.runLater(() -> {
            this.getChildren().clear();
            this.getChildren().add(directoryTreeView);
        });
    }

    protected abstract TreeItem<RepoFile> getRootTreeItem(DirectoryRepoFile rootDirectory);

    protected abstract TreeItem<RepoFile> getTreeItem(RepoFile repoFile);

    protected abstract List<RepoFile> getFilesToDisplay() throws GitAPIException;

    public void setSessionModel(SessionModel sessionModel) {
        this.sessionModel = sessionModel;
    }
}
