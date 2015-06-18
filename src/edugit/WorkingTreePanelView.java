package edugit;

import javafx.scene.Group;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.CheckBoxTreeCell;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;

/**
 * Git for Education, 2015
 *
 * WorkingTreePanelView displays the current working tree's directory and
 * lets the user mark checkboxes on files to commit. The 'commit action' to
 * be performed on a checkboxed file is determined by that file's status:
 * untracked/new, modified, or deleted.
 *
 */
public class WorkingTreePanelView extends Group {

    // fileLeafs stores all 'leafs' in the directory TreeView:
    private ArrayList<CheckBoxTreeItem> fileLeafs;
    private TreeView<RepoFile> directoryTreeView;
    private SessionModel sessionModel;

    public WorkingTreePanelView() {
        this.fileLeafs = new ArrayList<>();
        this.directoryTreeView = new TreeView<RepoFile>();
        this.getChildren().add(this.directoryTreeView);
    }

    /**
     * Draws the directory TreeView by getting the parent directory's RepoFile,
     * populating it with the files it contains, and adding it to the display.
     *
     * @throws GitAPIException if the SessionModel can't get the ParentDirectoryRepoFile.
     * @throws IOException if populating the parentDirectoryRepoFile fails.
     */
    public void drawDirectoryView() throws GitAPIException, IOException {
        Path directoryPath = this.sessionModel.getCurrentRepoHelper().getDirectory();

        // NOTE: performance stuff with recursion
        // #old: This is commented out since we're no longer loading the whole directory.
//        DirectoryRepoFile parentDirectoryRepoFile = this.sessionModel.getParentDirectoryRepoFile();

        DirectoryRepoFile rootDirectory = new DirectoryRepoFile("", this.sessionModel.getCurrentRepo());

        CheckBoxTreeItem<RepoFile> rootItem = new CheckBoxTreeItem<RepoFile>(rootDirectory);
        rootItem.setExpanded(true);

//        rootItem = this.populateRepoFileTreeLeaf(rootItem);

        for (RepoFile changedRepoFile : this.sessionModel.getAllChangedRepoFiles()) {
            CheckBoxTreeItem<RepoFile> leaf = new CheckBoxTreeItem<>(changedRepoFile, changedRepoFile.textLabel);
            rootItem.getChildren().add(leaf);
            this.fileLeafs.add(leaf);
        }

        this.directoryTreeView = new TreeView<RepoFile>(rootItem);
        this.directoryTreeView.setCellFactory(CheckBoxTreeCell.<RepoFile>forTreeView());

        // TreeViews must all have ONE root to hold the leafs. Don't show that root:
        this.directoryTreeView.setShowRoot(false);

        this.getChildren().clear();
        this.getChildren().add(directoryTreeView);
    }

    /**
     * Adds children to a directory's CheckBoxTreeItem by checking the CheckBoxTreeItem's inner RepoFile
     * for children and then making CheckBoxTreeItems for those children and populating them recursively
     * using this method.
     *
     * #old: This is unused since we're no longer loading the whole directory.
     *
     * @param parentLeaf A RepoFile's CheckBoxTreeItem to be populated with its children.
     * @return the populated parent leaf.
     *
     */
    public CheckBoxTreeItem<RepoFile> populateRepoFileTreeLeaf(CheckBoxTreeItem<RepoFile> parentLeaf) {
        RepoFile parentLeafRepoFile = parentLeaf.getValue();

        // Check for nullness, since non-directory RepoFiles will a null-valued
        // children list.
        if (parentLeafRepoFile.getChildren() != null) {
            for (RepoFile childRepoFile : parentLeafRepoFile.getChildren()) {
                CheckBoxTreeItem<RepoFile> childLeaf = new CheckBoxTreeItem<>(childRepoFile);
                if (childRepoFile.getChildren() != null) {
                    // Recursively populate the child leaf, then add it to the parent
                    childLeaf = populateRepoFileTreeLeaf(childLeaf);
                }
                parentLeaf.getChildren().add(childLeaf);
                // Store each leaf that we're adding to this directory tree:
                this.fileLeafs.add(childLeaf);
            }
        }
        return parentLeaf;
    }

    /**
     * Checks through all the file leafs and finds all leafs whose checkbox is checked.
     *
     * @return an array of RepoFiles whose CheckBoxTreeItem cells are checked.
     */
    public ArrayList<RepoFile> getCheckedFilesInDirectory() {
        ArrayList<RepoFile> checkedFiles = new ArrayList<>();
        for (CheckBoxTreeItem fileLeaf : this.fileLeafs) {
            if (fileLeaf.isSelected())
                checkedFiles.add((RepoFile)fileLeaf.getValue());
        }
        return checkedFiles;
    }

    public void setSessionModel(SessionModel sessionModel) {
        this.sessionModel = sessionModel;
    }

}
