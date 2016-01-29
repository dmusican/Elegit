package main.java.elegit;

import javafx.scene.control.TreeItem;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;
import java.nio.file.Path;
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

    private Map<Path, TreeItem<RepoFile>> itemMap;

    public AllFilesPanelView() {
        super();
    }

    @Override
    public void init(){
        this.itemMap = new HashMap<>();
        super.init();
    }

    @Override
    protected TreeItem<RepoFile> getRootTreeItem(DirectoryRepoFile rootDirectory) {
        return new TreeItem<>(rootDirectory);
    }

    @Override
    protected void addTreeItemsToRoot(List<RepoFile> repoFiles, TreeItem<RepoFile> root){
        // To ensure files are added after their parents, sort the given files into lists
        // based on their respective depths in the file structure
        Map<Integer, List<RepoFile>> filesAtDepthMap = new HashMap<>();
        int maxDepth = 0;
        for(RepoFile repoFile : repoFiles){
            int depthInRepo = repoFile.getLevelInRepository();

            if(depthInRepo > maxDepth) maxDepth=depthInRepo;

            if(!filesAtDepthMap.containsKey(depthInRepo)){
                List<RepoFile> list = new LinkedList<>();
                list.add(repoFile);
                filesAtDepthMap.put(depthInRepo, list);
            }else{
                filesAtDepthMap.get(depthInRepo).add(repoFile);
            }
        }

        // Track all currently displayed files to make sure they are still valid
        Map<TreeItem<RepoFile>, Boolean> shouldKeepItem = new HashMap<>();
        for(Path p : itemMap.keySet()){
            shouldKeepItem.put(itemMap.get(p), false);
        }

        // Loop through files at each depth
        for(int i = 0; i < maxDepth + 1; i++) {
            List<RepoFile> filesAtDepth = filesAtDepthMap.get(i);
            for (RepoFile repoFile : filesAtDepth) {
                Path pathToFile = repoFile.getFilePath();
                TreeItem<RepoFile> newItem = new TreeItem<>(repoFile, repoFile.diffButton);

                // Check if there is already a record of this file
                if (itemMap.containsKey(pathToFile)) {
                    TreeItem<RepoFile> oldItem = itemMap.get(pathToFile);

                    if (oldItem.getValue().equals(repoFile)) {
                        // The given file is already present, no additional processing needed
                        shouldKeepItem.put(oldItem, true);
                    } else {
                        // The file is displayed, but needs its status updated. Replace the old with the new
                        newItem.setExpanded(oldItem.isExpanded());
                        newItem.getChildren().setAll(oldItem.getChildren());
                        TreeItem<RepoFile> parent = oldItem.getParent();
                        parent.getChildren().set(parent.getChildren().indexOf(oldItem), newItem);
                        shouldKeepItem.put(newItem, true);
                        itemMap.put(pathToFile, newItem);
                    }
                } else {
                    // The given file wasn't present, so need to add it
                    Path pathToParent = pathToFile.getParent();
                    boolean foundParent = false;
                    // Make sure this new item is properly inserted as a child to its parent
                    while (pathToParent != null && !root.getValue().getFilePath().equals(pathToParent)) {
                        if (itemMap.containsKey(pathToParent)) {
                            itemMap.get(pathToParent).getChildren().add(newItem);
                            foundParent = true;
                            break;
                        }
                        pathToParent = pathToParent.getParent();
                    }
                    // If no parent is found, we can assume it belongs to the root
                    if (!foundParent){
                        root.getChildren().add(newItem);
                    }
                    itemMap.put(pathToFile, newItem);
                    shouldKeepItem.put(newItem, true);
                }
            }
        }

        // Remove all elements that shouldn't be displayed
        for (TreeItem<RepoFile> item : shouldKeepItem.keySet()) {
            if (!shouldKeepItem.get(item)) {
                if(item.getParent() != null)
                    item.getParent().getChildren().remove(item);
                itemMap.remove(item.getValue().getFilePath());
            }
        }
    }

    @Override
    public List<RepoFile> getFilesToDisplay() throws GitAPIException, IOException {
        return sessionModel.getAllRepoFiles();
    }
}
