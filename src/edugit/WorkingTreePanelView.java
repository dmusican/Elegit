package edugit;

import javafx.scene.Group;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.MenuBar;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.CheckBoxTreeCell;
import javafx.scene.layout.BorderPane;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Set;

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

//        CheckBoxTreeItem<Path> directoryLeaf = new CheckBoxTreeItem<Path>(directoryPath.getFileName());
//        CheckBoxTreeItem<Path> rootItem = this.walkThroughDirectoryToGetTreeItem(directoryPath, directoryLeaf);
//        rootItem.setExpanded(true);


        // just for convenience
        Repository repo = this.sessionModel.getCurrentRepo();

        // TODO: make these headers cleaner. Right now "Test" isn't a filePathString like it should be... it's just a string.
        CheckBoxTreeItem<RepoFile> rootItem = new CheckBoxTreeItem<RepoFile>(new UntrackedRepoFile("Git Status", repo));
        rootItem.setExpanded(true);

        // TODO: consider changing the constant switching from Path to String...
        CheckBoxTreeItem<RepoFile> untrackedFilesRoot = new CheckBoxTreeItem<RepoFile>(new UntrackedRepoFile("Untracked Files", repo));
        for (String untrackedFile : this.getUntrackedFiles()){
            UntrackedRepoFile untrackedRepoFile = new UntrackedRepoFile(untrackedFile, repo);
            CheckBoxTreeItem<RepoFile> untrackedFileLeaf = new CheckBoxTreeItem<>(untrackedRepoFile);
            untrackedFilesRoot.getChildren().add(untrackedFileLeaf);

            this.fileLeafs.add(untrackedFileLeaf);
        }

        CheckBoxTreeItem<RepoFile> missingFilesRoot = new CheckBoxTreeItem<RepoFile>(new MissingRepoFile("Missing Files", repo));
        for (String missingFile : this.getMissingFiles()){
            MissingRepoFile missingRepoFile = new MissingRepoFile(missingFile, repo);
            CheckBoxTreeItem<RepoFile> missingFileLeaf = new CheckBoxTreeItem<>(missingRepoFile);
            missingFilesRoot.getChildren().add(missingFileLeaf);

            this.fileLeafs.add(missingFileLeaf);
        }

        CheckBoxTreeItem<RepoFile> modifiedFilesRoot = new CheckBoxTreeItem<RepoFile>(new UntrackedRepoFile("Modified Files", repo));
        for (String modifiedFile : this.getModifiedFiles()){
            ModifiedRepoFile modifiedRepoFile = new ModifiedRepoFile(modifiedFile, repo);
            CheckBoxTreeItem<RepoFile> modifiedFileLeaf = new CheckBoxTreeItem<>(modifiedRepoFile);
            modifiedFilesRoot.getChildren().add(modifiedFileLeaf);

            this.fileLeafs.add(modifiedFileLeaf);
        }

        rootItem.getChildren().addAll(untrackedFilesRoot, missingFilesRoot, modifiedFilesRoot);

        // TODO: Write a custom tree cell? Somehow, show only the fileName, not the whole path.
        this.directoryTreeView = new TreeView<RepoFile>(rootItem);
        this.directoryTreeView.setCellFactory(CheckBoxTreeCell.<RepoFile>forTreeView());

        this.getChildren().clear();
        this.getChildren().add(directoryTreeView);
    }

    private Set<String> getUntrackedFiles() throws GitAPIException {
        Status status = new Git(this.sessionModel.getCurrentRepo()).status().call();
        Set<String> untrackedFiles = status.getUntracked();

        return untrackedFiles;
    }

    private Set<String> getMissingFiles() throws GitAPIException {
        Status status = new Git(this.sessionModel.getCurrentRepo()).status().call();
        Set<String> missingFiles = status.getMissing();

        return missingFiles;
    }

    private Set<String> getModifiedFiles() throws GitAPIException {
        Status status = new Git(this.sessionModel.getCurrentRepo()).status().call();
        Set<String> modifiedFiles = status.getModified();

        return modifiedFiles;
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
