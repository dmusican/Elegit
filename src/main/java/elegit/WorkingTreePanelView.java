package elegit;

import elegit.controllers.SessionController;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.TreeItem;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.nio.file.Path;
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

    private CheckBoxTreeItem<RepoFile> checkBox;

    public WorkingTreePanelView() {
        this.init();
    }

    @Override
    public void init(){
        isAnyFileSelectedProperty = new SimpleBooleanProperty(false);

        // Used to disable/enable add and remove buttons
        isAnyFileSelectedProperty.addListener(((observable, oldValue, newValue) -> SessionController.anythingChecked.set(newValue)));
        super.init();
    }

    @Override
    protected TreeItem<RepoFile> getRootTreeItem(DirectoryRepoFile rootDirectory) {
        TreeItem<RepoFile> item = new CheckBoxTreeItem<>(rootDirectory);
        initCheckBox();
        item.getChildren().add(checkBox);
        return item;
    }

    private void initCheckBox() {
        Text txt = new Text("select all");
        txt.setFont(new Font(10));
        checkBox = new CheckBoxTreeItem<>(null, txt);
        checkBox.selectedProperty().addListener(((observable, oldValue, newValue) -> {
            if(newValue) {
                setAllFilesSelected(true);
                txt.setText("deselect all");
            } else {
                setAllFilesSelected(false);
                txt.setText("select all");
            }
        }));
    }

    // Replaces the old file with the updated file in both the tree structure and displayedFiles list
    private void updateRepoFile(CheckBoxTreeItem<RepoFile> oldItem, CheckBoxTreeItem<RepoFile> newItem, int index) {
        newItem.setSelected(oldItem.isSelected());
        List<TreeItem<RepoFile>> directoryFiles = oldItem.getParent().getChildren();
        directoryFiles.set(directoryFiles.indexOf(oldItem), newItem);
        displayedFiles.set(index, newItem);
    }

    // Adds a new repo file to its proper place in the tree
    private void addNewRepoFile(RepoFile repoFile, CheckBoxTreeItem<RepoFile> newItem, TreeItem<RepoFile> root) {
        Path pathToParent = repoFile.getFilePath().getParent();
        if (pathToParent != null) {
            // Check if the file should be added to an existing directory
            CheckBoxTreeItem<RepoFile> parentDirectory = null;
            for (TreeItem<RepoFile> directory : root.getChildren()) {
                if (!directory.equals(checkBox) && directory.getValue().toString().equals(pathToParent.toString())) {
                    parentDirectory = (CheckBoxTreeItem<RepoFile>) directory;
                    break;
                }
            }
            if (parentDirectory == null) {
                // Create a new directory and add it to the root
                DirectoryRepoFile parent = new DirectoryRepoFile(pathToParent, this.sessionModel.getCurrentRepoHelper());
                parent.setShowFullPath(true);
                parentDirectory = new CheckBoxTreeItem<>(parent);
                parentDirectory.setExpanded(true);
                root.getChildren().add(parentDirectory);
            }
            parentDirectory.getChildren().add(newItem);
        } else {
            root.getChildren().add(newItem);
        }
    }

    /**
     * Adds all tracked files in the repository with an updated status and displays them
     * all as top-level items.
     * @param updatedRepoFiles the files to add to the tree
     * @param root the root of the tree
     */
    @Override
    protected void addTreeItemsToRoot(List<RepoFile> updatedRepoFiles, TreeItem<RepoFile> root) {
        // Re-adds the checkbox if it had been removed
        if(root.getChildren().isEmpty()) {
            root.getChildren().add(checkBox);
        }

        // Helper to construct 'isAnyFileSelectedProperty'
        BooleanProperty isSelectedPropertyHelper = new SimpleBooleanProperty(false);

        // Track all current files to make sure they should still be displayed
        Map<TreeItem<RepoFile>, Boolean> shouldKeepChild = new HashMap<>();
        for(TreeItem<RepoFile> treeItem : displayedFiles){
            shouldKeepChild.put(treeItem, false);
        }

        // Loop over every file to be shown
        for (RepoFile repoFile : updatedRepoFiles) {
            CheckBoxTreeItem<RepoFile> newItem = new CheckBoxTreeItem<>(repoFile, repoFile.diffButton);
            newItem.addEventHandler(CheckBoxTreeItem.checkBoxSelectionChangedEvent(), (CheckBoxTreeItem.TreeModificationEvent<RepoFile> e) -> {
                if (e.getTreeItem().isSelected()) {
                    indexPanel.setAllFilesSelected(false);
                    allFilesPanel.setAllFilesSelected(false);
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
                        // File has changed status
                        updateRepoFile(oldItem, newItem, i);
                        isSelectedPropertyHelper.bind(oldHelper.or(newItem.selectedProperty()));
                    }
                    foundMatchingItem = true;
                    shouldKeepChild.put(oldItem, true);
                    break;
                }
            }

            // The file wasn't being displayed, so add it
            if(!foundMatchingItem){
                addNewRepoFile(repoFile, newItem, root);
                isSelectedPropertyHelper.bind(oldHelper.or(newItem.selectedProperty()));
                displayedFiles.add(newItem);
                shouldKeepChild.put(newItem, true);
            }
        }

        // Remove all elements that shouldn't be displayed
        for(TreeItem item : shouldKeepChild.keySet()){
            if(!shouldKeepChild.get(item)){
                // Test if it is a file in the root level
                if(!root.getChildren().remove(item)) {
                    // The file to remove must be a grandchild of the root
                    // Loop through all the directory parents until we have removed the item
                    TreeItem<RepoFile> directoryRepoFile = root.getChildren().get(1);
                    while (!directoryRepoFile.getChildren().remove(item)) {
                        directoryRepoFile = directoryRepoFile.nextSibling();
                    }

                    // If we have removed all files in a directory, remove the directory
                    if(directoryRepoFile.getChildren().isEmpty()) {
                        root.getChildren().remove(directoryRepoFile);
                    }
                }
                displayedFiles.remove(item);
            }
        }

        // Hides the 'select/deselect all' checkbox if there are no files shown
        if(root.getChildren().size() < 2) {
            root.getChildren().remove(checkBox);
        }

        isAnyFileSelectedProperty.bind(isSelectedPropertyHelper);

    }

    /**
     * @return all tracked files with an updated status
     * @throws GitAPIException
     */
    @Override
    public List<RepoFile> getFilesToDisplay() throws GitAPIException{
        return sessionModel.getAllChangedRepoFiles();
    }

    /**
     * @return true if any file has been staged, else false
     */
    public boolean isAnyFileStaged() {
        for (TreeItem<RepoFile> treeItem: displayedFiles) {
            if (treeItem.getValue() instanceof StagedAndModifiedRepoFile || treeItem.getValue() instanceof StagedRepoFile)
                return true;
        }
        return false;
    }

    /**
     * @return true if any file that is selected is a staged file
     */
    public boolean isAnyFileStagedSelected() {
        for (RepoFile treeItem : getCheckedFilesInDirectory()) {
            if (treeItem instanceof StagedRepoFile)
                return true;
        }
        return false;
    }
}
