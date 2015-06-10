package edugit;

import javafx.scene.Group;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

/**
 * Created by makik on 6/10/15.
 */
public class WorkingTreePanelView extends Group {

    private SessionModel sessionModel;

    // The views within the view
    private TreeView directoryView;
    private TextField commitMessageBox;

    public WorkingTreePanelView() {
        // The displays should always be initialized
        this.initializeDisplays();
    }

    public void initializeDisplays() {
        this.drawDirectoryView();
        this.drawCommitMessageBox();
    }

    private void drawDirectoryView() {
        Path dirpath = Paths.get(System.getProperty("user.home")); //this.sessionModel.openRepoHelper.getDirectory().toString();

        // example-based:
        // http://www.adam-bien.com/roller/abien/entry/listing_directory_contents_with_jdk

        TreeItem<Path> dirTreeItem = new TreeItem<Path>(dirpath.getFileName());
        TreeItem<Path> rootItem = this.walkThroughDirectoryToGetTreeItem(dirpath, dirTreeItem);
        rootItem.setExpanded(true);

        this.directoryView = new TreeView<Path>(rootItem);
        this.getChildren().add(directoryView);

    }

    private void drawCommitMessageBox() {

    }

    private TreeItem<Path> walkThroughDirectoryToGetTreeItem(Path superDirectory, TreeItem<Path> superDirectoryTreeItem) {
        ArrayList<Path> filesInDirectory = new ArrayList<>();
        ArrayList<Path> subdirectoriesInDirectory = new ArrayList<>();

        // Get the directories and subdirectories
        try {
            DirectoryStream<Path> directoryStream = Files.newDirectoryStream(superDirectory);
            for (Path path : directoryStream) {
                if (Files.isDirectory(path))
                    subdirectoriesInDirectory.add(path);
                else
                    filesInDirectory.add(path);
            }
        } catch (IOException ex) {}

        // Add the directory's files as children of the directory tree item
        for (Path file : filesInDirectory) {
            TreeItem<Path> fileTreeItem = new TreeItem<Path>(file.getFileName());
            superDirectoryTreeItem.getChildren().add(fileTreeItem);
        }

        // Recurse through each subdirectory and populate their tree items
        for (Path subdirectory : subdirectoriesInDirectory) {
            TreeItem<Path> subdirectoryTreeItem = new TreeItem<Path>(subdirectory.getFileName());
            walkThroughDirectoryToGetTreeItem(subdirectory, subdirectoryTreeItem);
            superDirectoryTreeItem.getChildren().add(subdirectoryTreeItem);
        }

        return superDirectoryTreeItem;
    }

    public void setSessionModel(SessionModel sessionModel) {
        this.sessionModel = sessionModel;
    }

}
