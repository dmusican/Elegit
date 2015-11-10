package main.java.elegit;

import javafx.scene.control.TreeItem;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.util.List;

/**
 *
 * WorkingTreePanelView displays the current working tree's directory and
 * lets the user mark checkboxes on files to commit. The 'commit action' to
 * be performed on a checkboxed file is determined by that file's status:
 * untracked/new, modified, or deleted.
 *
 */
public class AllFilesPanelView extends FileStructurePanelView{

    public AllFilesPanelView() {
        super();
    }

    @Override
    protected TreeItem<RepoFile> getRootTreeItem(DirectoryRepoFile rootDirectory) {
        return new TreeItem<>(rootDirectory);
    }

    @Override
    protected TreeItem<RepoFile> getTreeItem(RepoFile repoFile) {
        return new TreeItem<>(repoFile, repoFile.diffButton);
    }

    @Override
    public List<RepoFile> getFilesToDisplay() throws GitAPIException{
        return sessionModel.getAllRepoFiles();
    }
}
