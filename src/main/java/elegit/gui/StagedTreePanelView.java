package elegit.gui;

import elegit.Main;
import elegit.models.SessionModel;
import elegit.repofile.DirectoryRepoFile;
import elegit.repofile.RepoFile;
import elegit.repofile.StagedAndModifiedRepoFile;
import elegit.repofile.StagedRepoFile;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.util.Callback;
import net.jcip.annotations.ThreadSafe;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;
import java.util.*;

/**
 * StagedTreePanelView displays all staged files in the current repository
 * and their status
 */

@ThreadSafe
// because of all the assert statements I have throughout. This is a view class, and at least for now,
// all methods must run on the FX thread. This class loses threadsafeness if any of that is changed.
public class StagedTreePanelView extends FileStructurePanelView{

    public StagedTreePanelView() {
        super.init();
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
        Main.assertFxThread();

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
    public List<RepoFile> getFilesToDisplay() throws GitAPIException {
        Main.assertFxThread();
        List<RepoFile> repoFiles = new ArrayList<>();
        for (RepoFile file : SessionModel.getSessionModel().getAllChangedRepoFiles()) {
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
            Main.assertFxThread();
            super.updateItem(item, empty);

            setText(getItem() == null ? "" : getItem().toString());
            setGraphic(getTreeItem() == null ? null : getTreeItem().getGraphic());

            setOnContextMenuRequested(event -> {
                if(getTreeItem() != null) getTreeItem().getValue().showContextMenu(this, event.getScreenX(), event.getScreenY());
            });
        }
    }
}
