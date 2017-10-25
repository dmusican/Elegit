package elegit.gui;

import elegit.Main;
import elegit.models.SessionModel;
import elegit.controllers.SessionController;
import elegit.repofile.DirectoryRepoFile;
import elegit.repofile.RepoFile;
import elegit.repofile.StagedAndModifiedRepoFile;
import elegit.repofile.StagedRepoFile;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.CheckBoxTreeCell;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.util.Callback;
import org.apache.http.annotation.ThreadSafe;
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
@ThreadSafe
// because of all the assert statements I have throughout. This is a view class, and at least for now,
// all methods must run on the FX thread. This class loses threadsafeness if any of that is changed.
// MOREOVER, there is extensive use of bindings and listeners here, which must happen on the FX thread.
public class WorkingTreePanelView extends FileStructurePanelView{

    private final BooleanProperty isAnyFileSelectedProperty = new SimpleBooleanProperty(false);
    private final List<TreeItem<RepoFile>> displayedFiles = new LinkedList<>();

    private CheckBoxTreeItem<RepoFile> checkBox;

    public WorkingTreePanelView() {
        Main.assertFxThread();

        // Used to disable/enable add and remove buttons
        isAnyFileSelectedProperty.addListener(((observable, oldValue, newValue) -> SessionController.anythingCheckedProperty().set(newValue)));

        init();
    }

    @Override
    public void init() {
        Main.assertFxThread();
        displayedFiles.clear();
        super.init();
    }
    /**
     * @return the cell from CheckBoxTreeCell's implementation of a TreeView factory, with
     * an added context menu for the given RepoFile
     */
    @Override
    protected Callback<TreeView<RepoFile>, TreeCell<RepoFile>> getTreeCellFactory() {
        return arg -> {
            TreeCell<RepoFile> cell = CheckBoxTreeCell.<RepoFile>forTreeView().call(arg);
            cell.setOnContextMenuRequested(event -> {
                if(cell.getTreeItem()!= null) cell.getTreeItem().getValue().showContextMenu(cell, event.getScreenX(), event.getScreenY());
            });
            return cell;
        };
    }

    @Override
    protected TreeItem<RepoFile> getRootTreeItem(DirectoryRepoFile rootDirectory) {
        Main.assertFxThread();
        TreeItem<RepoFile> item = new CheckBoxTreeItem<>(rootDirectory);
        initCheckBox();
        item.getChildren().add(checkBox);
        return item;
    }

    private void initCheckBox() {
        Main.assertFxThread();
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
        Main.assertFxThread();
        newItem.setSelected(oldItem.isSelected());
        List<TreeItem<RepoFile>> directoryFiles = oldItem.getParent().getChildren();
        directoryFiles.set(directoryFiles.indexOf(oldItem), newItem);
        displayedFiles.set(index, newItem);
    }

    // Adds a new repo file to its proper place in the tree
    private void addNewRepoFile(RepoFile repoFile, CheckBoxTreeItem<RepoFile> newItem, TreeItem<RepoFile> root) {
        Main.assertFxThread();
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
                DirectoryRepoFile parent = new DirectoryRepoFile(pathToParent, SessionModel.getSessionModel().getCurrentRepoHelper());
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
        Main.assertFxThread();

        System.out.println("Getting started ");
        for (TreeItem item : root.getChildren()) {
            System.out.println(item);
        }

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
        for(RepoFile repoFile : updatedRepoFiles) {
            CheckBoxTreeItem<RepoFile> newItem = new CheckBoxTreeItem<>(repoFile, repoFile.getDiffButton());

            BooleanProperty oldHelper = isSelectedPropertyHelper;
            isSelectedPropertyHelper = new SimpleBooleanProperty();

            // Check if the file is already being displayed
            boolean foundMatchingItem = false;
            for(int i = 0; i < displayedFiles.size(); i++) {
                CheckBoxTreeItem<RepoFile> oldItem = (CheckBoxTreeItem<RepoFile>) displayedFiles.get(i);

                boolean fileChangedStatus = oldItem.getValue().getFilePath().equals(repoFile.getFilePath());
                if(oldItem.getValue().equals(repoFile) || fileChangedStatus) {
                    // The given file is already present
                    if (fileChangedStatus) {
                        updateRepoFile(oldItem, newItem, i);
                        isSelectedPropertyHelper.bind(oldHelper.or(newItem.selectedProperty()));
                    } else {
                        isSelectedPropertyHelper.bind(oldHelper.or(oldItem.selectedProperty()));
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

        System.out.println("And again ");
        for (TreeItem item : root.getChildren()) {
            System.out.println(item);
        }

        // Remove all elements that shouldn't be displayed
        for(TreeItem item : shouldKeepChild.keySet()){
            if(!shouldKeepChild.get(item)){
                // Test if it is a file in the root level
                if(!root.getChildren().remove(item)) {
                    // The file to remove must be a grandchild of the root
                    // Loop through all the directory parents until we have removed the item
                    TreeItem<RepoFile> directoryRepoFile = root.getChildren().get(1);
                    System.out.println("Trying to remove... " + item);
                    System.out.println("directoryRepoFile = " + directoryRepoFile);
                    while (!directoryRepoFile.getChildren().remove(item)) {
                        directoryRepoFile = directoryRepoFile.nextSibling();
                        System.out.println("directoryRepoFile = " + directoryRepoFile);
                    }

                    // If we have removed all files in a directory, remove the directory
                    if(directoryRepoFile.getChildren().isEmpty()) {
                        System.out.println(item);
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
        Main.assertFxThread();
        return SessionModel.getSessionModel().getAllChangedRepoFiles();
    }

    /**
     * Checks through all the files and finds all whose checkbox is checked.
     *
     * @return an array of RepoFiles whose CheckBoxTreeItem cells are checked.
     */
    public ArrayList<RepoFile> getCheckedFilesInDirectory() {
        Main.assertFxThread();
        ArrayList<RepoFile> checkedFiles = new ArrayList<>();
        for (TreeItem fileLeaf : this.displayedFiles) {
            CheckBoxTreeItem checkBoxFile = (CheckBoxTreeItem) fileLeaf;
            if (checkBoxFile.isSelected())
                checkedFiles.add((RepoFile)fileLeaf.getValue());
        }
        return checkedFiles;
    }

    /**
     * Sets all displayed items to have the given selected status
     * @param selected true to check every box, false to uncheck every box
     */
    public void setAllFilesSelected(boolean selected) {
        Main.assertFxThread();
        for (TreeItem fileLeaf : displayedFiles) {
            CheckBoxTreeItem checkBoxFile = (CheckBoxTreeItem) fileLeaf;
            checkBoxFile.setSelected(selected);
        }
    }

    /**
     * @return true if any file is checked, else false
     */
    public boolean isAnyFileSelected(){
        Main.assertFxThread();
        return isAnyFileSelectedProperty.get();
    }

    /**
     * @return true if any file has been staged, else false
     */
    public boolean isAnyFileStaged() {
        Main.assertFxThread();
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
        Main.assertFxThread();
        for (RepoFile treeItem : getCheckedFilesInDirectory()) {
            if (treeItem instanceof StagedRepoFile)
                return true;
        }
        return false;
    }

    /**
     *
     * @return true if the select all box is checked
     */
    public boolean isSelectAllChecked() {
        Main.assertFxThread();
        return checkBox.isSelected();
    }

}
