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
        Path dirpath = Paths.get(System.getProperty("user.home")+"/Documents"); //this.sessionModel.openRepoHelper.getDirectory().toString();

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
        // Get the directories and subdirectories
        try {
            DirectoryStream<Path> directoryStream = Files.newDirectoryStream(superDirectory);
            for (Path path : directoryStream) {
                if (Files.isDirectory(path)) {
                    // Recurse!
                    TreeItem<Path> subdirectoryTreeItem = new TreeItem<Path>(path.getFileName());
                    walkThroughDirectoryToGetTreeItem(path, subdirectoryTreeItem);
                    superDirectoryTreeItem.getChildren().add(subdirectoryTreeItem);
                }
                else {
                    // So, it's just a file
                    TreeItem<Path> fileTreeItem = new TreeItem<Path>(path.getFileName());
                    superDirectoryTreeItem.getChildren().add(fileTreeItem);
                }
            }
            directoryStream.close();
        } catch (IOException ex) {}

        return superDirectoryTreeItem;
    }

    public void setSessionModel(SessionModel sessionModel) {
        this.sessionModel = sessionModel;
    }

}
