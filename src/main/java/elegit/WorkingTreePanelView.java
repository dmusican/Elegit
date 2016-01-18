package main.java.elegit;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.CheckBoxTreeCell;
import javafx.util.Callback;
import org.eclipse.jgit.api.errors.GitAPIException;
import sun.reflect.generics.tree.Tree;

import java.util.*;

/**
 *
 * WorkingTreePanelView displays the current working tree's directory and
 * lets the user mark checkboxes on files to commit. The 'commit action' to
 * be performed on a checkboxed file is determined by that file's status:
 * untracked/new, modified, or deleted.
 *
 */
public class WorkingTreePanelView extends FileStructurePanelView{

    public BooleanProperty isAnyFileSelectedProperty;

    public List<TreeItem<RepoFile>> fileLeafs;

    public WorkingTreePanelView() {
        super();
        this.fileLeafs = new LinkedList<>();
        isAnyFileSelectedProperty = new SimpleBooleanProperty(false);
    }

    @Override
    protected Callback<TreeView<RepoFile>, TreeCell<RepoFile>> getTreeCellFactory() {
        return CheckBoxTreeCell.<RepoFile>forTreeView();
    }

    @Override
    protected TreeItem<RepoFile> getRootTreeItem(DirectoryRepoFile rootDirectory) {
        return new CheckBoxTreeItem<>(rootDirectory);
    }

    @Override
    protected List<TreeItem<RepoFile>> addTreeItems(List<RepoFile> repoFiles, TreeItem<RepoFile> root) {
        fileLeafs = new LinkedList<>();

        // Helper to construct 'isAnyFileSelectedProperty'
        BooleanProperty isSelectedPropertyHelper = new SimpleBooleanProperty(false);

        // Track all current children of root to make sure they should still be displayed
        Map<TreeItem<RepoFile>, Boolean> shouldKeepChild = new HashMap<>();
        for(int i = 0; i < root.getChildren().size(); i++){
            shouldKeepChild.put(root.getChildren().get(i), false);
        }

        // Loop over every file to be shown
        for(RepoFile repoFile : repoFiles) {
            CheckBoxTreeItem<RepoFile> newItem = new CheckBoxTreeItem<>(repoFile, repoFile.diffButton);

            BooleanProperty oldHelper = isSelectedPropertyHelper;
            isSelectedPropertyHelper = new SimpleBooleanProperty();

            // Check if the file is already being displayed
            boolean foundMatchingItem = false;
            for(int i = 0; i < root.getChildren().size(); i++){
                CheckBoxTreeItem<RepoFile> oldItem = (CheckBoxTreeItem) root.getChildren().get(i);

                if(oldItem.getValue().equals(repoFile)){
                    // The given file is already present, no additional processing necessary
                    isSelectedPropertyHelper.bind(oldHelper.or(oldItem.selectedProperty()));
                    fileLeafs.add(oldItem);
                    foundMatchingItem = true;
                    shouldKeepChild.put(oldItem, true);
                    break;
                }else if(oldItem.getValue().getFilePath().equals(repoFile.getFilePath())){
                    // The file was being displayed, but its status has changed. Replace the old with the new
                    newItem.setSelected(oldItem.isSelected());
                    root.getChildren().set(i, newItem);
                    isSelectedPropertyHelper.bind(oldHelper.or(newItem.selectedProperty()));
                    fileLeafs.add(newItem);
                    foundMatchingItem = true;
                    shouldKeepChild.put(newItem, true);
                    break;
                }
            }

            // The file wasn't being displayed, so add it
            if(!foundMatchingItem){
                root.getChildren().add(newItem);
                isSelectedPropertyHelper.bind(oldHelper.or(newItem.selectedProperty()));

                fileLeafs.add(newItem);

                shouldKeepChild.put(newItem, true);
            }
        }

        // Remove all elements that shouldn't be displayed
        for(TreeItem item : shouldKeepChild.keySet()){
            if(!shouldKeepChild.get(item)){
                root.getChildren().remove(item);
            }
        }

        isAnyFileSelectedProperty.bind(isSelectedPropertyHelper);

        return fileLeafs;
    }

    @Override
    public List<RepoFile> getFilesToDisplay() throws GitAPIException{
        return sessionModel.getAllChangedRepoFiles();
    }

    /**
     * Checks through all the file leafs and finds all leafs whose checkbox is checked.
     *
     * @return an array of RepoFiles whose CheckBoxTreeItem cells are checked.
     */
    public ArrayList<RepoFile> getCheckedFilesInDirectory() {
        ArrayList<RepoFile> checkedFiles = new ArrayList<>();
        for (TreeItem fileLeaf : this.fileLeafs) {
            CheckBoxTreeItem checkBoxFile = (CheckBoxTreeItem) fileLeaf;
            if (checkBoxFile.isSelected())
                checkedFiles.add((RepoFile)fileLeaf.getValue());
        }
        return checkedFiles;
    }

    public void setAllFilesSelected(boolean selected) {
        for (TreeItem fileLeaf : fileLeafs) {
            CheckBoxTreeItem checkBoxFile = (CheckBoxTreeItem) fileLeaf;
            checkBoxFile.setSelected(selected);
        }
    }

    /**
     * @return true if any file is checked, else false
     */
    public boolean isAnyFileSelected(){
        return isAnyFileSelectedProperty.get();
    }
}
