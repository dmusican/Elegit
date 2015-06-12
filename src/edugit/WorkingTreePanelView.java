package edugit;

import javafx.scene.Group;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.CheckBoxTreeCell;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

/**
 * Created by makik on 6/10/15.
 */
public class WorkingTreePanelView extends Group {

    private SessionModel sessionModel;
    private ArrayList<CheckBoxTreeItem> fileLeafs;

    // The views within the view
    private TreeView<RepoFile> directoryTreeView;

    public WorkingTreePanelView() {
        this.fileLeafs = new ArrayList<>();
        this.directoryTreeView = new TreeView<RepoFile>();
        this.getChildren().add(this.directoryTreeView);
    }

    public void drawDirectoryView() throws GitAPIException {
        Path directoryPath = this.sessionModel.currentRepoHelper.getDirectory();

        DirectoryRepoFile parentDirectoryRepoFile = this.sessionModel.getParentDirectoryRepoFile();

        CheckBoxTreeItem<RepoFile> rootItem = new CheckBoxTreeItem<RepoFile>(parentDirectoryRepoFile);
        rootItem.setExpanded(true);

        rootItem = this.populateRepoFileTreeLeaf(rootItem);

        // TODO: Write a custom tree cell? Show icons for NEW, MODIFIED, or MISSING
        this.directoryTreeView = new TreeView<RepoFile>(rootItem);
        this.directoryTreeView.setCellFactory(CheckBoxTreeCell.<RepoFile>forTreeView());

        // TreeViews must all have ONE root to hold the leafs. Don't show that root
        this.directoryTreeView.setShowRoot(false);

        this.getChildren().clear();
        this.getChildren().add(directoryTreeView);
    }

    public CheckBoxTreeItem<RepoFile> populateRepoFileTreeLeaf(CheckBoxTreeItem<RepoFile> parentLeaf) {
        RepoFile parentLeafRepoFile = parentLeaf.getValue();

        // TODO: change this if statement to check if null, and make non-directory RepoFiles have that arraylist be null
        if (!parentLeafRepoFile.getChildren().isEmpty()) {
            for (RepoFile childRepoFile : parentLeafRepoFile.getChildren()) {
                CheckBoxTreeItem<RepoFile> childLeaf = new CheckBoxTreeItem<>(childRepoFile);
                if (!childRepoFile.getChildren().isEmpty()) {
                    // Populate the child leaf, then add it to the parent
                    childLeaf = populateRepoFileTreeLeaf(childLeaf);
                }
                parentLeaf.getChildren().add(childLeaf);
                this.fileLeafs.add(childLeaf);
            }
        }

        return parentLeaf;
    }



    // Currently not in use.
    // This was for displaying the whole directory structure in the WorkingTreePanelView.
    private CheckBoxTreeItem<Path> walkThroughDirectoryToGetTreeItem(Path superDirectory, CheckBoxTreeItem<Path> superDirectoryLeaf) {
        // Get the directories and subdirectories
        try {
            DirectoryStream<Path> directoryStream = Files.newDirectoryStream(superDirectory);
            for (Path path : directoryStream) {
                if (Files.isDirectory(path)) {
                    // Recurse!
                    CheckBoxTreeItem<Path> subdirectoryLeaf = new CheckBoxTreeItem<Path>(path);
                    walkThroughDirectoryToGetTreeItem(path, subdirectoryLeaf);
                    superDirectoryLeaf.getChildren().add(subdirectoryLeaf);
                }
                else {
                    // So, it's a file, not a directory.
                    CheckBoxTreeItem<Path> fileLeaf = new CheckBoxTreeItem<Path>(path);
                    superDirectoryLeaf.getChildren().add(fileLeaf);
                    this.fileLeafs.add(fileLeaf);
                }
            }
            directoryStream.close(); // Have to close this to prevent overflow!
        } catch (IOException ex) {}

        return superDirectoryLeaf;
    }

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
