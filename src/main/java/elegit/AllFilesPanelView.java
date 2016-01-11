package main.java.elegit;

import javafx.scene.control.TreeItem;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * AllFilesPanelView displays all files in the current repository,
 * whether tracked or otherwise, as well as their status. It does so
 * in a hierarchical manner
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
        for (int i = 1; i < maxDepth+1; i++) {
            List<RepoFile> filesAtDepth = depthInRepoMap.get(i);

            List<TreeItem<RepoFile>> itemsAtDepth = new LinkedList<>();
            for(RepoFile file : filesAtDepth){
                TreeItem<RepoFile> newItem = new TreeItem<>(file, file.diffButton);
                itemsAtDepth.add(newItem);

                for(TreeItem<RepoFile> prevItem : itemsAtPrevDepth){
                    RepoFile prevFile = prevItem.getValue();

                    if(file.getFilePath().getParent().equals(prevFile.getFilePath())){
                        prevItem.getChildren().add(newItem);
                        break;
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
