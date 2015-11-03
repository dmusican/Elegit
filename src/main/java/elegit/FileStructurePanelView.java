package main.java.elegit;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.CheckBoxTreeCell;
import javafx.scene.layout.Region;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class FileStructurePanelView extends Region{

    // fileLeafs stores all 'leafs' in the directory TreeView:
    private ArrayList<CheckBoxTreeItem<RepoFile>> fileLeafs;
    private TreeView<RepoFile> directoryTreeView;
    public SessionModel sessionModel;

    public BooleanProperty isAnyFileSelectedProperty;

    public FileStructurePanelView() {
        this.fileLeafs = new ArrayList<>();
        this.directoryTreeView = new TreeView<>();
        isAnyFileSelectedProperty = new SimpleBooleanProperty(false);

        this.directoryTreeView.prefHeightProperty().bind(this.heightProperty());
        this.getChildren().add(this.directoryTreeView);
    }

    public void setAllFilesSelected(boolean selected) {
        for (CheckBoxTreeItem fileCell : this.fileLeafs) {
            fileCell.setSelected(selected);
        }
    }

    /**
     * Draws the directory TreeView by getting the parent directory's RepoFile,
     * populating it with the files it contains, and adding it to the display.
     *
     * FIXME: this method resets the users selections if they've checked any boxes (low priority)
     *
     * @throws GitAPIException if the SessionModel can't get the ParentDirectoryRepoFile.
     */
    public void drawDirectoryView() throws GitAPIException{

        if(this.sessionModel.getCurrentRepoHelper() == null) return;
        DirectoryRepoFile rootDirectory = new DirectoryRepoFile("", this.sessionModel.getCurrentRepo());

        fileLeafs = new ArrayList<>(fileLeafs.size());

        CheckBoxTreeItem<RepoFile> rootItem = new CheckBoxTreeItem<RepoFile>(rootDirectory);
        rootItem.setExpanded(true);

        isAnyFileSelectedProperty.unbind();
        BooleanProperty temp = new SimpleBooleanProperty(false);
        for(RepoFile changedRepoFile : this.getFilesToDisplay()){
            CheckBoxTreeItem<RepoFile> leaf = new CheckBoxTreeItem<>(changedRepoFile, changedRepoFile.diffButton);
            rootItem.getChildren().add(leaf);
            this.fileLeafs.add(leaf);
            BooleanProperty oldTemp = temp;
            temp = new SimpleBooleanProperty();
            temp.bind(oldTemp.or(leaf.selectedProperty()));
        }
        isAnyFileSelectedProperty.bind(temp);

        this.directoryTreeView = new TreeView<>(rootItem);
        this.directoryTreeView.setCellFactory(CheckBoxTreeCell.<RepoFile>forTreeView());

        // TreeViews must all have ONE root to hold the leafs. Don't show that root:
        this.directoryTreeView.setShowRoot(false);

        this.directoryTreeView.prefHeightProperty().bind(this.heightProperty());

        Platform.runLater(() -> {
            this.getChildren().clear();
            this.getChildren().add(directoryTreeView);
        });
    }

    public List<RepoFile> getFilesToDisplay() throws GitAPIException{
        return sessionModel.getAllRepoFiles();
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

    /**
     * @return true if any file is checked, else false
     */
    public boolean isAnyFileSelected(){
        return isAnyFileSelectedProperty.get();
    }
}
