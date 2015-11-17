package elegit;

import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.CheckBoxTreeCell;
import javafx.util.Callback;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * WorkingTreePanelView displays the current working tree's directory and
 * lets the user mark checkboxes on files to commit. The 'commit action' to
 * be performed on a checkboxed file is determined by that file's status:
 * untracked/new, modified, or deleted.
 *
 */
public class WorkingTreePanelView extends FileStructurePanelView{

    public BooleanProperty isAnyFileSelectedProperty;

    public List<TreeItem<RepoFile>> fileLeafs;

    public WorkingTreePanelView() {
        super();
        this.fileLeafs = new LinkedList<>();
        isAnyFileSelectedProperty = new SimpleBooleanProperty(false);
    }

    @Override
    protected Callback<TreeView<RepoFile>, TreeCell<RepoFile>> getTreeCellFactory() {
        return CheckBoxTreeCell.<RepoFile>forTreeView();
    }

    @Override
    protected TreeItem<RepoFile> getRootTreeItem(DirectoryRepoFile rootDirectory) {
        return new CheckBoxTreeItem<>(rootDirectory);
    }

    @Override
    protected List<TreeItem<RepoFile>> getTreeItems(List<RepoFile> repoFiles) {
        fileLeafs = new LinkedList<>();

        BooleanProperty temp = new SimpleBooleanProperty(false);

        for(RepoFile repoFile : repoFiles) {
            CheckBoxTreeItem<RepoFile> file = new CheckBoxTreeItem<>(repoFile, repoFile.diffButton);

            BooleanProperty oldTemp = temp;
            temp = new SimpleBooleanProperty();
            temp.bind(oldTemp.or(file.selectedProperty()));

            fileLeafs.add(file);
        }

        isAnyFileSelectedProperty.bind(temp);

        return fileLeafs;
    }

    @Override
    public List<RepoFile> getFilesToDisplay() throws GitAPIException{
        return sessionModel.getAllChangedRepoFiles();
    }

    /**
     * Checks through all the file leafs and finds all leafs whose checkbox is checked.
     *
     * @return an array of RepoFiles whose CheckBoxTreeItem cells are checked.
     */
    public ArrayList<RepoFile> getCheckedFilesInDirectory() {
        ArrayList<RepoFile> checkedFiles = new ArrayList<>();
        for (TreeItem fileLeaf : this.fileLeafs) {
            CheckBoxTreeItem checkBoxFile = (CheckBoxTreeItem) fileLeaf;
            if (checkBoxFile.isSelected())
                checkedFiles.add((RepoFile)fileLeaf.getValue());
        }
        return checkedFiles;
    }

    public void setAllFilesSelected(boolean selected) {
        for (TreeItem fileLeaf : fileLeafs) {
            CheckBoxTreeItem checkBoxFile = (CheckBoxTreeItem) fileLeaf;
            checkBoxFile.setSelected(selected);
        }
    }

    /**
     * @return true if any file is checked, else false
     */
    public boolean isAnyFileSelected(){
        return isAnyFileSelectedProperty.get();
    }
}
