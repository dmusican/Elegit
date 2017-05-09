package elegit;

import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.util.Callback;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/**
 * StagedTreePanelView displays all staged files in the current repository
 * and their status
 */
public class StagedTreePanelView extends FileStructurePanelView{

    public StagedTreePanelView() {
        this.init();
    }

    /**
     * @return a factory that generates a custom tree cell that includes a context menu for each
     * item
     */
    @Override
    protected Callback<TreeView<RepoFile>, TreeCell<RepoFile>> getTreeCellFactory() {
        return arg -> new RepoFileTreeCell();
    }

    @Override
    protected TreeItem<RepoFile> getRootTreeItem(DirectoryRepoFile rootDirectory) {
        return new TreeItem<>(rootDirectory);
    }

    /**
     * Builds a tree containing all staged files in the repository, with the base
     * directory of the current repository as the root. Subsequent calls to this method
     * will update the items in place
     * @param repoFiles the files to add to the tree
     * @param root the root of the tree
     */
    @Override
    protected void addTreeItemsToRoot(List<RepoFile> repoFiles, TreeItem<RepoFile> root){

        // Track all current files to make sure they should still be displayed
        Map<TreeItem<RepoFile>, Boolean> shouldKeepChild = new HashMap<>();
        for(TreeItem<RepoFile> treeItem : root.getChildren()){
            shouldKeepChild.put(treeItem, false);
        }

        for (RepoFile repoFile : repoFiles) {
            TreeItem<RepoFile> newItem = new TreeItem<>(repoFile, null);
            boolean foundMatchingItem = false;

            for (int i = 0; i < root.getChildren().size(); i++) {
                TreeItem<RepoFile> oldItem = root.getChildren().get(i);
                if (oldItem.equals(repoFile)) {
                    // Check if the file already exists
                    foundMatchingItem = true;
                    shouldKeepChild.put(oldItem, true);
                } else if (oldItem.getValue().getFilePath().equals(repoFile.getFilePath())) {
                    // File exists but is updated
                    root.getChildren().set(i, newItem);
                    foundMatchingItem = true;
                    shouldKeepChild.put(oldItem, true);
                }
            }

            // The file wasn't being displayed, so add it
            if(!foundMatchingItem){
                root.getChildren().add(newItem);
                shouldKeepChild.put(newItem, true);
            }
        }

        // Remove all elements that shouldn't be displayed
        for(TreeItem item : shouldKeepChild.keySet()){
            if (!shouldKeepChild.get(item)) {
                root.getChildren().remove(item);
            }
        }
    }

    /**
     * @return every file in the repository (included untracked, ignored, etc)
     * @throws GitAPIException
     * @throws IOException
     */
    @Override
    public List<RepoFile> getFilesToDisplay() throws GitAPIException, IOException {
        List<RepoFile> repoFiles = new ArrayList<>();
        for (RepoFile file : sessionModel.getAllChangedRepoFiles()) {
            if (file instanceof StagedRepoFile || file instanceof StagedAndModifiedRepoFile)
                repoFiles.add(file);
        }
        return repoFiles;
    }

    /**
     * An overwritten version of TreeCell that adds a context menu to our
     * tree structure
     */
    private class RepoFileTreeCell extends TreeCell<RepoFile>{
        @Override
        protected void updateItem(RepoFile item, boolean empty){
            super.updateItem(item, empty);

            setText(getItem() == null ? "" : getItem().toString());
            setGraphic(getTreeItem() == null ? null : getTreeItem().getGraphic());

            setOnContextMenuRequested(event -> {
                if(getTreeItem() != null) getTreeItem().getValue().showContextMenu(this, event.getScreenX(), event.getScreenY());
            });
        }
    }
}
