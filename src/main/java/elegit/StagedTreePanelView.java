package elegit;

import com.sun.tools.javac.comp.Check;
import elegit.controllers.SessionController;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.TreeCell;
import javafx.scene.control.cell.CheckBoxTreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.util.Callback;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;
import java.util.*;

/**
 * StagedTreePanelView displays all staged files in the current repository
 * and their status
 */
public class StagedTreePanelView extends FileStructurePanelView{

    public BooleanProperty isAnyFileSelectedProperty;

    private List<TreeItem<RepoFile>> displayedFiles;

    private WorkingTreePanelView workingTreePanel;

    public StagedTreePanelView() {
        this.init();
    }

    @Override
    public void init(){
        this.displayedFiles = new LinkedList<>();
        isAnyFileSelectedProperty = new SimpleBooleanProperty(false);

        // Used to disable/enable add and remove buttons
        isAnyFileSelectedProperty.addListener(((observable, oldValue, newValue) -> SessionController.anyIndexFileChecked.set(newValue)));
        super.init();
    }

    /**
     * @return a factory that generates a custom tree cell that includes a context menu for each
     * item
     */
    @Override
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
//        return arg -> new RepoFileTreeCell();
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
                if (e.getTreeItem().isSelected()) {
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
     * An overwritten version of TreeCell that adds a context menu to our
     * tree structure
     */
    private class RepoFileTreeCell extends CheckBoxTreeCell<RepoFile>{
        @Override
        public void updateItem(RepoFile item, boolean empty){
            super.updateItem(item, empty);

            setText(getItem() == null ? "" : getItem().toString());
            setGraphic(getTreeItem() == null ? null : getTreeItem().getGraphic());

            setOnContextMenuRequested(event -> {
                if(getTreeItem() != null) getTreeItem().getValue().showContextMenu(this, event.getScreenX(), event.getScreenY());
            });
        }
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
