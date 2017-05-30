package elegit;

import elegit.controllers.SessionController;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.TreeItem;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;
import java.util.*;

/**
 * StagedTreePanelView displays all staged files in the current repository
 * and their status
 */
public class StagedTreePanelView extends FileStructurePanelView{

    private WorkingTreePanelView workingTreePanel;

    public StagedTreePanelView() {
        this.init();
    }

    @Override
    public void init(){
        isAnyFileSelectedProperty = new SimpleBooleanProperty(false);

        // Used to disable/enable add and remove buttons
        isAnyFileSelectedProperty.addListener(((observable, oldValue, newValue) -> SessionController.anyIndexFileChecked.set(newValue)));
        super.init();
    }

    @Override
    protected TreeItem<RepoFile> getRootTreeItem(DirectoryRepoFile rootDirectory) {
        return new TreeItem<>(rootDirectory);
    }

    /**
     * Builds a tree containing all staged files in the repository, with the base
     * directory of the current repository as the root. Subsequent calls to this method
     * will update the items in place
     * @param updatedRepoFiles the files to add to the tree
     * @param root the root of the tree
     */
    @Override
    protected void addTreeItemsToRoot(List<RepoFile> updatedRepoFiles, TreeItem<RepoFile> root){

        displayedFiles = root.getChildren();

        // Helper to construct 'isAnyFileSelectedProperty'
        BooleanProperty isSelectedPropertyHelper = new SimpleBooleanProperty(false);

        // Track all current files to make sure they should still be displayed
        Map<TreeItem<RepoFile>, Boolean> shouldKeepChild = new HashMap<>();
        for(TreeItem<RepoFile> treeItem : displayedFiles){
            shouldKeepChild.put(treeItem, false);
        }

        // Loop over every file to be shown
        for (RepoFile repoFile : updatedRepoFiles) {
            CheckBoxTreeItem<RepoFile> newItem = new CheckBoxTreeItem<>(repoFile, null);
            newItem.addEventHandler(CheckBoxTreeItem.checkBoxSelectionChangedEvent(), (CheckBoxTreeItem.TreeModificationEvent<RepoFile> e) -> {
                if (e.getTreeItem().isSelected() && workingTreePanel != null) {
                    workingTreePanel.setAllFilesSelected(false);
                }
            });

            BooleanProperty oldHelper = isSelectedPropertyHelper;
            isSelectedPropertyHelper = new SimpleBooleanProperty();

            // Check if the file is already being displayed
            boolean foundMatchingItem = false;
            for (int i = 0; i < displayedFiles.size(); i++) {
                CheckBoxTreeItem<RepoFile> oldItem = (CheckBoxTreeItem<RepoFile>) displayedFiles.get(i);

                if (oldItem.getValue().getFilePath().equals(repoFile.getFilePath())) {
                    if (oldItem.getValue().equals(repoFile)) {
                        isSelectedPropertyHelper.bind(oldHelper.or(oldItem.selectedProperty()));
                    } else {
                        // File exists but is updated
                        displayedFiles.set(i, newItem);
                        isSelectedPropertyHelper.bind(oldHelper.or(newItem.selectedProperty()));
                    }
                    foundMatchingItem = true;
                    shouldKeepChild.put(oldItem, true);
                    break;
                }
            }

            // The file wasn't being displayed, so add it
            if(!foundMatchingItem){
                isSelectedPropertyHelper.bind(oldHelper.or(newItem.selectedProperty()));
                displayedFiles.add(newItem);
                shouldKeepChild.put(newItem, true);
            }
        }

        // Remove all elements that shouldn't be displayed
        for(TreeItem item : shouldKeepChild.keySet()){
            if (!shouldKeepChild.get(item)) {
                displayedFiles.remove(item);
            }
        }

        isAnyFileSelectedProperty.bind(isSelectedPropertyHelper);
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
     * Sets all displayed items to have the given selected status
     * @param selected true to check every box, false to uncheck every box
     */
    public void setAllFilesSelected(boolean selected) {
        for (TreeItem fileLeaf : displayedFiles) {
            CheckBoxTreeItem checkBoxFile = (CheckBoxTreeItem) fileLeaf;
            checkBoxFile.setSelected(selected);
        }
    }

    public void setWorkingTreePanel(WorkingTreePanelView workingTreePanel) {
        this.workingTreePanel = workingTreePanel;
    }

}
