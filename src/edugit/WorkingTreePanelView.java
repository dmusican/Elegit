package edugit;

import javafx.event.EventHandler;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.CheckBoxTreeCell;
import javafx.scene.layout.Region;
import org.eclipse.jgit.api.errors.GitAPIException;

import javax.swing.plaf.basic.BasicComboBoxUI;
import java.awt.event.KeyEvent;
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
public class WorkingTreePanelView extends Region{

    // fileLeafs stores all 'leafs' in the directory TreeView:
    private ArrayList<CheckBoxTreeItem> fileLeafs;
    private TreeView<RepoFile> directoryTreeView;
    private SessionModel sessionModel;

    public WorkingTreePanelView() {
        this.fileLeafs = new ArrayList<>();
        this.directoryTreeView = new TreeView<>();

        this.directoryTreeView.prefHeightProperty().bind(this.heightProperty());
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
        if(this.sessionModel.getCurrentRepoHelper() == null) return;

        Path directoryPath = this.sessionModel.getCurrentRepoHelper().getDirectory();

        DirectoryRepoFile rootDirectory = new DirectoryRepoFile("", this.sessionModel.getCurrentRepo());

        CheckBoxTreeItem<RepoFile> rootItem = new CheckBoxTreeItem<RepoFile>(rootDirectory);
        rootItem.setExpanded(true);

        for (RepoFile changedRepoFile : this.sessionModel.getAllChangedRepoFiles()) {
            CheckBoxTreeItem<RepoFile> leaf = new CheckBoxTreeItem<>(changedRepoFile, changedRepoFile.diffButton);
            rootItem.getChildren().add(leaf);
            this.fileLeafs.add(leaf);
        }

        this.directoryTreeView = new TreeView<RepoFile>(rootItem);
        this.directoryTreeView.setCellFactory(CheckBoxTreeCell.<RepoFile>forTreeView());

        // TreeViews must all have ONE root to hold the leafs. Don't show that root:
        this.directoryTreeView.setShowRoot(false);

        this.directoryTreeView.prefHeightProperty().bind(this.heightProperty());
        this.getChildren().clear();
        this.getChildren().add(this.directoryTreeView);
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
