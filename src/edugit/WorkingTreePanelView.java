package edugit;

import javafx.scene.Group;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.MenuBar;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.CheckBoxTreeCell;
import javafx.scene.layout.BorderPane;

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
    private ArrayList<CheckBoxTreeItem> fileLeafs;

    // The views within the view
    private TreeView<Path> directoryTreeView;

    public WorkingTreePanelView() {
        this.fileLeafs = new ArrayList<>();
        this.directoryTreeView = new TreeView<Path>();
        this.getChildren().add(this.directoryTreeView);
    }

    public void drawDirectoryView() {
        Path directoryPath = this.sessionModel.currentRepoHelper.getDirectory();

        // example-based:
        // http://www.adam-bien.com/roller/abien/entry/listing_directory_contents_with_jdk

        CheckBoxTreeItem<Path> directoryLeaf = new CheckBoxTreeItem<Path>(directoryPath.getFileName());
        CheckBoxTreeItem<Path> rootItem = this.walkThroughDirectoryToGetTreeItem(directoryPath, directoryLeaf);
        rootItem.setExpanded(true);

        // TODO: Write a custom tree cell? Somehow, show only the fileName, not the whole path.
        this.directoryTreeView = new TreeView<Path>(rootItem);
        this.directoryTreeView.setCellFactory(CheckBoxTreeCell.<Path>forTreeView());

        this.getChildren().clear();
        this.getChildren().add(directoryTreeView);
    }

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

    public ArrayList<Path> getCheckedFilesInDirectory() {
        ArrayList<Path> checkedFiles = new ArrayList<>();
        for (CheckBoxTreeItem fileLeaf : this.fileLeafs) {
            if (fileLeaf.isSelected())
                checkedFiles.add((Path)fileLeaf.getValue());
        }
        return checkedFiles;
    }

    public void setSessionModel(SessionModel sessionModel) {
        this.sessionModel = sessionModel;
    }

}
