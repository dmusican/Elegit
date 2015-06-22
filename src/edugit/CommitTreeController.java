package edugit;

import edugit.treefx.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by makik on 6/17/15.
 */
public class CommitTreeController{

    public static List<CommitTreeModel> allCommitTreeModels = new ArrayList<>();
    private static String selectedCellID = null;

    public static void handleMouseClicked(Cell cell){
        selectCommit(cell.getCellId());
    }

    public static void handleMouseover(Cell cell, boolean isOverCell){
        highlightCommit(cell.getCellId(), isOverCell);
    }

    public static void selectCommit(String commitID){
        boolean isDeselecting = commitID.equals(selectedCellID);

        for(CommitTreeModel model : allCommitTreeModels){
            if(model.treeGraph == null) continue;
            TreeGraphModel m = model.treeGraph.getTreeGraphModel();

            if(selectedCellID == null){
                selectCommit(commitID, m, true);
            }else{
                selectCommit(selectedCellID, m, false);
                if(!isDeselecting){
                    selectCommit(commitID, m, true);
                }
            }
        }
        if(isDeselecting){
            selectedCellID = null;
        }else{
            selectedCellID = commitID;
        }
        Edge.allVisible.set(selectedCellID == null);
    }

    public static void highlightCommit(String commitID, boolean isOverCell){
        for(CommitTreeModel model : allCommitTreeModels){
            if(model.treeGraph == null) continue;
            TreeGraphModel m = model.treeGraph.getTreeGraphModel();

            if(!isSelected(commitID)){
                Highlighter.highlightCell(commitID, selectedCellID, m, isOverCell);
                Highlighter.updateCellEdges(commitID, selectedCellID, m, isOverCell);
            }
        }
    }

    private static void selectCommit(String commitID, TreeGraphModel model, boolean enable){
        Highlighter.highlightSelectedCell(commitID, model, enable);
        if(enable){
            Highlighter.updateCellEdges(commitID, commitID, model, enable);
        }else{
            Highlighter.updateCellEdges(commitID, null, model, enable);
        }
    }

    public static void resetSelection(){
        if(selectedCellID != null){
            selectCommit(selectedCellID);
        }
    }

    private static boolean isSelected(String cellID){
        return selectedCellID != null && selectedCellID.equals(cellID);
    }

    public static void update(RepoHelper repo){
        List<String> commitIDs = repo.getAllCommitIDs();
        for(CommitTreeModel model : allCommitTreeModels){
            if(model.treeGraph != null){
                for(String id : commitIDs){
                    if(!model.containsID(id)){
                        model.addInvisibleCommit(id);
                    }
                }
                model.treeGraph.update();
                model.view.displayTreeGraph(model.treeGraph);
            }
        }
    }
}
