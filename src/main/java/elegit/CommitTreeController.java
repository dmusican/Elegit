package elegit;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import elegit.treefx.Cell;
import elegit.treefx.Highlighter;
import elegit.treefx.TreeGraphModel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * The controller class for the commit trees. Handles mouse interaction, cell selection/highlighting,
 * as well as updating the views when necessary
 */
public class CommitTreeController{

    // The list of all models controlled by this controller
    public static List<CommitTreeModel> allCommitTreeModels = new ArrayList<>();

    // The id of the currently selected cell
    private static String selectedCellID = null;

    // The session controller if this controller needs to access other models/views
    public static SessionController sessionController;

    private static ObjectProperty<String> selectedIDProperty = new SimpleObjectProperty<>();

    /**
     * Takes in the cell that was clicked on, and either selects or deselects
     * it depending on whether it had already been selected
     * @param clickedCellId the id of the cell that was clicked
     */
    public static void handleMouseClicked(String clickedCellId){
        if(clickedCellId.equals(selectedCellID)){
            resetSelection();
        }else{
            selectCommit(clickedCellId, false, false, false);
        }
    }

    /**
     * Handles mouse clicks that didn't happen on a cell. Deselects everything.
     */
    public static void handleMouseClicked(){
        resetSelection();
    }

    /**
     * Takes in the cell that was moused over, and highlights it using highlightCommit
     * @param cell the cell generated the mouseover event
     * @param isOverCell whether the mouse is entering or exiting the cell
     */
    public static void handleMouseover(Cell cell, boolean isOverCell){
        highlightCommitInGraph(cell.getCellId(), isOverCell);
    }

    /**
     * Selects the commit with the given id. Loops through all tracked CommitTreeModels and updates
     * their corresponding views.
     * @param commitID the id of the commit to select
     * @param ancestors whether to highlight the commit's parents
     * @param descendants whether to highlight the commit's children
     * @param allGenerations whether to highlight further generations than just parents/children (i.e. grandparents, grandchildren etc)
     */
    private static void selectCommitInGraph(String commitID, boolean ancestors, boolean descendants, boolean allGenerations){
        for(CommitTreeModel model : allCommitTreeModels){
            if(model.treeGraph == null) continue;
            TreeGraphModel m = model.treeGraph.treeGraphModel;

            selectCommitInGraph(commitID, m, true, ancestors, descendants, allGenerations);
        }

        selectedCellID = commitID;
        selectedIDProperty.set(commitID);

//        Edge.allVisible.set(selectedCellID == null);
    }

    /**
     * Highlight the commit with the given id in every tracked CommitTreeModel and corresponding
     * view. If the given id is selected, do nothing.
     * @param commitID the id of the commit to select
     * @param isOverCell whether to highlight or un-highlight the corresponding cells
     */
    private static void highlightCommitInGraph(String commitID, boolean isOverCell){
        for(CommitTreeModel model : allCommitTreeModels){
            if(model.treeGraph == null) continue;
            TreeGraphModel m = model.treeGraph.treeGraphModel;

            if(!isSelected(commitID)){
                Highlighter.highlightCell(commitID, selectedCellID, m, isOverCell);
                Highlighter.updateCellEdges(commitID, selectedCellID, m, isOverCell);
            }
        }
    }

    /**
     * Helper method that uses the Highlighter class to appropriately color the selected
     * commit and its edges
     * @param commitID the commit to select
     * @param model the model wherein the corresponding cell should be highlighted
     * @param enable whether to select or deselect the commit
     * @param ancestors whether to highlight the commit's parents
     * @param descendants whether to highlight the commit's children
     * @param allGenerations whether to highlight further generations than just parents/children (i.e. grandparents, grandchildren etc)
     */
    private static void selectCommitInGraph(String commitID, TreeGraphModel model, boolean enable, boolean ancestors, boolean descendants, boolean allGenerations){
        Highlighter.highlightSelectedCell(commitID, model, enable, ancestors, descendants, allGenerations);
        if(enable){
            Highlighter.updateCellEdges(commitID, commitID, model, true);
        }else{
            Highlighter.updateCellEdges(commitID, null, model, false);
        }
    }

    /**
     * Selects the cell with the given id. If there is already a commit selected,
     * deselects it first.
     * @param id the cell to select
     * @param ancestors whether to highlight the commit's parents
     * @param descendants whether to highlight the commit's children
     * @param allGenerations whether to highlight further generations than just parents/children (i.e. grandparents, grandchildren etc)
     */
    public static void selectCommit(String id, boolean ancestors, boolean descendants, boolean allGenerations){
        resetSelection();
        selectCommitInGraph(id, ancestors, descendants, allGenerations);
        sessionController.selectCommit(id);
    }

    /**
     * Deselects the currently selected commit, if there is one
     */
    public static void resetSelection(){
        if(selectedCellID != null){
            Highlighter.resetAll();
            selectedCellID = null;
            selectedIDProperty.set(null);
        }
        sessionController.clearSelectedCommit();
    }

    /**
     * Checks to see if the given id is currently selected
     * @param cellID the id to check
     * @return true if it is selected, false otherwise
     */
    private static boolean isSelected(String cellID){
        return selectedCellID != null && selectedCellID.equals(cellID);
    }

    /**
     * Initializes the view corresponding to the given CommitTreeModel. Updates
     * all tracked CommitTreeModels with branch heads and missing commits,
     * but does not update their view
     * @param commitTreeModel the model whose view should be updated
     */
    public static void init(CommitTreeModel commitTreeModel){
        RepoHelper repo = commitTreeModel.sessionModel.getCurrentRepoHelper();

        List<String> commitIDs = repo.getAllCommitIDs();
        for(CommitTreeModel model : allCommitTreeModels){
            if(model.treeGraph != null){
                for(String id : commitIDs){
                    if(!model.containsID(id)){
                        model.addInvisibleCommit(id);
                    }
                }
                model.treeGraph.update();
            }
        }

        setBranchHeads(commitTreeModel, repo);

        commitTreeModel.view.displayTreeGraph(commitTreeModel.treeGraph, commitTreeModel.sessionModel
                .getCurrentRepoHelper().getBranchModel().getCurrentBranchHead());
    }

    /**
     * Updates the views corresponding to all tracked CommitTreeModels after updating them
     * with branch heads and any missing commits
     * @param repo the repo from which the list of all commits is pulled
     */
    public static void update(RepoHelper repo) throws IOException{
        List<String> commitIDs = repo.getAllCommitIDs();
        for(CommitTreeModel model : allCommitTreeModels){
            if(model.treeGraph != null){
                for(String id : commitIDs){
                    if(!model.containsID(id)){
                        model.addInvisibleCommit(id);
                    }
                }

                model.treeGraph.update();
                model.view.displayTreeGraph(model.treeGraph, model.sessionModel
                        .getCurrentRepoHelper().getBranchModel().getCurrentBranchHead());

                setBranchHeads(model, repo);
            }
        }
    }

    /**
     * Uses the Highlighter class to emphasize and scroll to the cell corresponding
     * to the given commit in every view corresponding to a tracked CommitTreeModel
     * @param commit the commit to focus
     */
    public static void focusCommitInGraph(CommitHelper commit){
        if(commit == null) return;

        for(CommitTreeModel model : allCommitTreeModels){
            if(model.treeGraph != null && model.treeGraph.treeGraphModel.containsID(commit.getId())){
                Cell c = model.treeGraph.treeGraphModel.cellMap.get(commit.getId());
                Highlighter.emphasizeCell(c);
            }
        }
    }

    /**
     * Uses the Highlighter class to emphasize and scroll to the cell corresponding
     * to the cell with the given ID in every view corresponding to a tracked CommitTreeModel
     * @param commitID the ID of the commit to focus
     */
    public static void focusCommitInGraph(String commitID){
        if(commitID == null) return;
        for(CommitTreeModel model : allCommitTreeModels){
            if(model.treeGraph != null && model.treeGraph.treeGraphModel.containsID(commitID)){
                Cell c = model.treeGraph.treeGraphModel.cellMap.get(commitID);
                Highlighter.emphasizeCell(c);
            }
        }
    }

    /**
     * Loops through the branches and sets the cells that are branch heads to have the
     * correct shape (untracked=circle, tracked=traingle)
     * @param model: the commit tree model to set the branch heads for
     * @return true if the model has branches, false if not
     */
    public static boolean setBranchHeads(CommitTreeModel model, RepoHelper repo) {
        List<BranchHelper> modelBranches = repo.getBranchModel().getAllBranches();
        if(modelBranches == null) return false;
        model.resetBranchHeads(true);
        for(BranchHelper branch : modelBranches){
            if(!model.sessionModel.getCurrentRepoHelper().getBranchModel().isBranchTracked(branch)){
                model.setCommitAsBranchHead(branch, false);
            }else{
                model.setCommitAsBranchHead(branch, true);
            }
        }
        return true;
    }

    public static ObjectProperty<String> selectedIDProperty(){
        return selectedIDProperty;
    }
}
