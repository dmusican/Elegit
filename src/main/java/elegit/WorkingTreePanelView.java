package main.java.elegit;

import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.TreeItem;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.util.ArrayList;
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
    private BooleanBinding isAnyFileSelectedHelperBinding;

    public WorkingTreePanelView() {
        super();
        isAnyFileSelectedProperty = new SimpleBooleanProperty(false);
    }

    @Override
    protected TreeItem<RepoFile> getRootTreeItem(DirectoryRepoFile rootDirectory) {
        isAnyFileSelectedProperty.unbind();
        return new CheckBoxTreeItem<>(rootDirectory);
    }

    @Override
    protected TreeItem<RepoFile> getTreeItem(RepoFile repoFile) {
        CheckBoxTreeItem<RepoFile> file = new CheckBoxTreeItem<>(repoFile, repoFile.diffButton);

        isAnyFileSelectedHelperBinding = (isAnyFileSelectedHelperBinding ==null) ? file.selectedProperty().or(new SimpleBooleanProperty(false)) : isAnyFileSelectedHelperBinding.or(file.selectedProperty());

        isAnyFileSelectedProperty.unbind();
        isAnyFileSelectedProperty.bind(isAnyFileSelectedHelperBinding);

        return file;
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
