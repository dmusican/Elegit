package elegit;

import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.util.Callback;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;
import java.util.*;

/**
 *
 * AllFilesPanelView displays the current working tree's directory and
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
    protected List<TreeItem<RepoFile>> getTreeItems(List<RepoFile> repoFiles) {
        List<TreeItem<RepoFile>> topLevelTreeItems = new LinkedList<>();

        Map<Integer, List<RepoFile>> depthInRepoMap = new HashMap<>();
        int maxDepth = 0;
        for(RepoFile repoFile : repoFiles){
            int depthInRepo = repoFile.getLevelInRepository();

            if(depthInRepo > maxDepth) maxDepth=depthInRepo;

            if(!depthInRepoMap.containsKey(depthInRepo)){
                List<RepoFile> list = new LinkedList<>();
                list.add(repoFile);
                depthInRepoMap.put(depthInRepo, list);
            }else{
                depthInRepoMap.get(depthInRepo).add(repoFile);
            }

            if(depthInRepo==0){
                topLevelTreeItems.add(new TreeItem<>(repoFile, repoFile.diffButton));
            }
        }

        List<TreeItem<RepoFile>> itemsAtPrevDepth = topLevelTreeItems;
        for (int i = 1; i < maxDepth; i++) {
            List<RepoFile> filesAtDepth = depthInRepoMap.get(i);

            List<TreeItem<RepoFile>> itemsAtDepth = new LinkedList<>();
            for(RepoFile file : filesAtDepth){
                TreeItem<RepoFile> newItem = new TreeItem<>(file, file.diffButton);
                itemsAtDepth.add(newItem);
                for(TreeItem<RepoFile> item : itemsAtPrevDepth){
                    RepoFile prevFile = item.getValue();

                    if(file.getFilePath().getParent().equals(prevFile.getFilePath())){
                        item.getChildren().add(newItem);
                    }
                }
            }

            itemsAtPrevDepth = itemsAtDepth;
        }

        return topLevelTreeItems;
    }

    @Override
    public List<RepoFile> getFilesToDisplay() throws GitAPIException, IOException {
        return sessionModel.getAllRepoFiles();
    }
}
