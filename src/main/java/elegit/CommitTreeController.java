package elegit;

import elegit.controllers.SessionController;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import elegit.treefx.Cell;
import elegit.treefx.Highlighter;
import elegit.treefx.TreeGraphModel;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The controller class for the commit trees. Handles mouse interaction, cell selection/highlighting,
 * as well as updating the views when necessary
 */
public class CommitTreeController{

    // The list of all models controlled by this controller
    public static List<CommitTreeModel> allCommitTreeModels = new ArrayList<>();

    // The list of selected cells
    private static List<String> selectedCellIds = new ArrayList<>();

    // The session controller if this controller needs to access other models/views
    public static SessionController sessionController;

    private static ObjectProperty<String> selectedIDProperty = new SimpleObjectProperty<>();
    static Property<Boolean> multipleNotSelectedProperty = new SimpleBooleanProperty(true);

    /**
     * Takes in the cell that was clicked on, and either selects or deselects
     * it depending on whether it had already been selected
     * @param clickedCellId the id of the cell that was clicked
     */
    public static void handleMouseClicked(String clickedCellId){
        if(selectedCellIds.size()==1 && clickedCellId.equals(selectedCellIds.get(0))){
            resetSelection();
        } else if (selectedCellIds.size()==0) {
            selectCommit(clickedCellId, false, false, false);
        } else {
            resetSelection();
            selectCommit(clickedCellId, false, false, false);
        }
    }

    /**
     * Takes in a cell that was clicked while holding shift, and
     * selects or deselects is based on whether or not it was in the
     * selected group
     * @param cell the cell that was clicked with shift down
     */
    public static void handleMouseClickedShift(Cell cell) {
        if (selectedCellIds.contains(cell.getCellId())) {
            if (isSelected(cell.getCellId())) {
                selectedIDProperty.set(null);
                sessionController.clearSelectedCommit();
            }
            Highlighter.resetCell(cell);
            selectedCellIds.remove(cell.getCellId());
            multipleNotSelectedProperty.setValue(selectedCellIds.size()<2);
        } else if (selectedCellIds.size() == 0) {
            selectCommit(cell.getCellId(), false, false, false);
        } else {
            selectCommitInGraph(cell.getCellId(), false, false, false);
        }
    }

    /**
     * Getter method for all selected cells
     * @return the list of selected cells
     */
    static List<String> getSelectedIds() {
        return selectedCellIds;
    }

    /**
     * Handles mouse clicks that didn't happen on a cell. Deselects everything.
     */
    static void handleMouseClicked(){
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

        selectedCellIds.add(commitID);
        selectedIDProperty.set(commitID);
        multipleNotSelectedProperty.setValue(selectedCellIds.size()<2);

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

            if(selectedCellIds.size()>0 && !isSelected(commitID)){
                Highlighter.highlightCell(commitID, selectedCellIds.get(0), m, isOverCell);
                Highlighter.updateCellEdges(commitID, selectedCellIds.get(0), m, isOverCell);
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
        if(selectedCellIds.size() > 0){
            Highlighter.resetAll();
            selectedCellIds.clear();
            selectedIDProperty.set(null);
            multipleNotSelectedProperty.setValue(true);
        }
        sessionController.clearSelectedCommit();
    }

    /**
     * Checks to see if the given id is currently selected
     * @param cellID the id to check
     * @return true if it is selected, false otherwise
     */
    private static boolean isSelected(String cellID){
        return selectedCellIds.size()==1 && selectedCellIds.get(0).equals(cellID);
    }

    /**
     * Initializes the view corresponding to the given CommitTreeModel. Updates
     * all tracked CommitTreeModels with branch heads and missing commits,
     * but does not update their view
     * @param commitTreeModel the model whose view should be updated
     */
    public static void init(CommitTreeModel commitTreeModel){
        RepoHelper repo = commitTreeModel.sessionModel.getCurrentRepoHelper();

        commitTreeModel.treeGraph.update();

        setBranchHeads(commitTreeModel, repo);

        commitTreeModel.view.displayTreeGraph(commitTreeModel.treeGraph, commitTreeModel.sessionModel
                .getCurrentRepoHelper().getBranchModel().getCurrentBranchHead());
    }

    /**
     * Updates the view corresponding to the given CommitTreeModel. Updates
     * all tracked CommitTreeModels with branch heads and missing commits,
     * but does not update their view
     * @param commitTreeModel the model whose view should be updated
     */
    public static void update(CommitTreeModel commitTreeModel){
        RepoHelper repo = commitTreeModel.sessionModel.getCurrentRepoHelper();

        commitTreeModel.treeGraph.update();

        commitTreeModel.view.displayTreeGraph(commitTreeModel.treeGraph, commitTreeModel.sessionModel
                .getCurrentRepoHelper().getBranchModel().getCurrentBranchHead());

        setBranchHeads(commitTreeModel, repo);
    }

    /**
     * Uses the Highlighter class to emphasize and scroll to the cell corresponding
     * to the given commit in every view corresponding to a tracked CommitTreeModel
     * @param commit the commit to focus
     */
    public static void focusCommitInGraph(CommitHelper commit){
        assert(Platform.isFxApplicationThread());
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
        try {
            repo.getBranchModel().updateAllBranches();
        } catch (IOException | GitAPIException e) {
            // This shouldn't happen once the repo is loaded and going
        }
        Map<CommitHelper, List<BranchHelper>> headIds = repo.getBranchModel().getAllBranchHeads();
        if(headIds == null) return false;
        model.resetBranchHeads();
        boolean isTracked;
        for(CommitHelper head : headIds.keySet()){
            isTracked = false;
            for (BranchHelper branch : headIds.get(head)) {
                if (repo.getBranchModel().isBranchTracked(branch))
                    isTracked = true;
            }
            model.setCommitAsBranchHead(head, isTracked);
        }
        model.updateAllRefLabels();
        return true;
    }

    public static ObjectProperty<String> selectedIDProperty(){
        return selectedIDProperty;
    }

    /**
     * @return the commit tree model for the current session
     */
    public static CommitTreeModel getCommitTreeModel() {
        return CommitTreeController.allCommitTreeModels.get(0);
    }
}
