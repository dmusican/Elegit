package elegit.treefx;

import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.scene.shape.Shape;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class provides static methods for highlighting and animating cells in a tree graph
 */
public class Highlighter{

    // Cells that are currently blocked from being highlighted
    private static final List<String> blockedCellIDs = new ArrayList<>();
    // A map from each known cell to its state
    private static final Map<Cell, CellState> cellStates = new HashMap<>();

    /**
     * Highlights the cell corresponding to the given id in the given model, as well as
     * its relatives (depending on parameters), with either the standard color or the
     * constants SELECT_COLOR and first HIGHLIGHT_COLOR, respectively
     * @param cellID the selected cell to highlight
     * @param model the model wherein the cell is found
     * @param enable whether to highlight the cell or return it to standard
     * @param ancestors whether to highlight the cell's parents
     * @param descendants whether to highlight the cell's children
     * @param allGenerations whether to highlight further generations than just parents/children (i.e. grandparents, grandchildren etc)
     */
    public static void highlightCellAndRelatives(String cellID, TreeGraphModel model, boolean enable,
                                                 boolean ancestors, boolean descendants, boolean allGenerations){
        Cell cell = model.cellMap.get(cellID);
        if(cell == null) return;
        if(enable){
            highlightCell(cell, CellState.SELECTED, true);
            if(ancestors){
                highlightCellParents(cell, CellState.HIGHLIGHTED1, allGenerations, new ArrayList<>());
            }
            if(descendants){
                highlightCellChildren(cell, CellState.HIGHLIGHTED1, allGenerations, new ArrayList<>());
            }
        }else{
            highlightCell(cell, CellState.STANDARD, true);
            if(ancestors){

                highlightCellParents(cell, CellState.STANDARD, allGenerations, new ArrayList<>());
            }
            if(descendants){
                highlightCellChildren(cell, CellState.STANDARD, allGenerations, new ArrayList<>());
            }
        }
    }

    /**
     * Recursively highlights all of cell's parents. Note that the initial cell itself is not highlighted
     * @param cell the cell whose parents should be highlighted
     * @param state the new state for the cell's parents
     * @param recurse whether to recurse or not
     * @param visited a list of all visited cells so far
     */
    private static void highlightCellParents(Cell cell, CellState state, boolean recurse, List<Cell> visited){
        for(Cell parent : cell.getCellParents()){
            if(visited.contains(parent)) continue;
            visited.add(parent);
            highlightCell(parent, state, true);
            if(recurse) highlightCellParents(parent, state, true, visited);
        }
    }

    /**
     * Recursively highlights all of cell's children. Note that the initial cell itself is not highlighted
     * @param cell the cell whose children should be highlighted
     * @param state the new state for the cell's children
     * @param recurse whether to recurse or not
     * @param visited a list of all visited cells so far
     */
    private static void highlightCellChildren(Cell cell, CellState state, boolean recurse, List<Cell> visited){
        for(Cell child : cell.getCellChildren()){
            if(visited.contains(child)) continue;
            visited.add(child);
            highlightCell(child, state, true);
            if(recurse) highlightCellChildren(child, state, true, visited);
        }
    }

    /**
     * Takes care of ensuring the edges surrounding highlighted and selected cells are correctly
     * flagged as visible
     * TODO: examine how we want to do edge highlighting
     * @param cellID the cell whose edges this method will examine
     * @param selectedCellID the currently selected cell, if any
     * @param model the model wherein these cells are found
     * @param enable whether to flag these edges as visible or not
     */
    public static void updateCellEdges(String cellID, String selectedCellID, TreeGraphModel model, boolean enable){
        Cell cell = model.cellMap.get(cellID);
        if(cell == null) return;
        Cell selectedCell = model.cellMap.get(selectedCellID);

        List<Edge> list = new ArrayList<>(cell.edges);
        if(!enable && selectedCellID != null){
            for(Edge e : selectedCell.edges){
                list.remove(e);
            }
        }
        for(Edge e : list){
            e.setHighlighted(enable);
        }
    }

    /**
     * Highlights the cell corresponding to the given id in the given model, with either the
     * standard color, the first HIGHLIGHT_COLOR constant, or the second HIGHLIGHT_COLOR constant.
     * When highlighting, if there is a selected cell the second highlight color will be used.
     * Otherwise, the first highlight color is chosen
     * @param cellID the cell to highlight
     * @param selectedCellID the currently selected cell, if any
     * @param model the model wherein these cells are found
     * @param enable whether to highlight this cell or return it to the standard color
     */
    public static void highlightCell(String cellID, String selectedCellID, TreeGraphModel model, boolean enable){
        Cell cell = model.cellMap.get(cellID);
        if(cell == null) return;

        CellState state;
        if(enable){
            if(selectedCellID == null){
                state = CellState.HIGHLIGHTED1;
            }else{
                state = CellState.HIGHLIGHTED2;
            }
        }else{
            state = CellState.STANDARD;
        }

        if(!cellID.equals(selectedCellID)){
            highlightCell(cell, state, false);
        }
    }

    /**
     * Helper method to set the state of a cell. Note that if a cell had its state
     * changed TO a non-persistent state, FROM a persistent non-standard state, attempting
     * to put the cell back into the standard state will instead place it back into its
     * previous persistent state.
     * @param cell the cell to change states
     * @param state the new state of the cell
     * @param persistent whether the new state completely overwrites the old (i.e.
     *                   does the new state get put in the map or will the previous
     *                   state stay in memory)
     */
    private static void highlightCell(Cell cell, CellState state, boolean persistent){
        if(persistent) cellStates.put(cell, state);

        if(blockedCellIDs.contains(cell.getCellId())) return;

        if(state == CellState.STANDARD){
            if(cellStates.containsKey(cell)){
                cell.setCellState(cellStates.get(cell));
                return;
            }
        }
        cell.setCellState(state);
    }

    /**
     * Resets all cell's to have the standard state. Also clears the cellStates
     * map as it is redundant with everything reset.
     */
    public static void resetAll(){
        for(Cell cell : cellStates.keySet()){
            cell.setCellState(CellState.STANDARD);
        }
        cellStates.clear();
    }

    /**
     * Resets a single cell to the stand state
     * @param cell the cell to reset to standard state
     */
    public static void resetCell(Cell cell) {
        cell.setCellState(CellState.STANDARD);
        cellStates.remove(cell);
    }

    /**
     * First requests the focus of the MatchedScrollPanes, then
     * sets the state of the given cell to emphasized, and fianlly
     * performs an animation on it. Currently, the animation is
     * a repeated scaling.
     * Cells being emphasized are blocked from other highlighting.
     * @param c the cell to emphasize
     */
    public static void emphasizeCell(Cell c){
        assert Platform.isFxApplicationThread();
        if(!blockedCellIDs.contains(c.getCellId())){
            blockedCellIDs.add(c.getCellId());
        }

        CommitTreeScrollPane.scrollTo(c.rowLocationProperty.doubleValue());
        c.setCellState(CellState.EMPHASIZED);

        Shape s = c.view;

        ScaleTransition sct = new ScaleTransition(Duration.millis(425), s);
        sct.setByX(0.3f);
        sct.setByY(0.3f);
        sct.setCycleCount(6);
        sct.setAutoReverse(true);

        c.view.setScaleX(1.0);
        c.view.setScaleY(1.0);

        sct.play();

        sct.setOnFinished(event -> endEmphasisOnCell(c));
    }

    /**
     * Unblocks the given cell and sets its state to standard
     * @param c the cell to end emphasis on
     */
    private static void endEmphasisOnCell(Cell c){
        blockedCellIDs.remove(c.getCellId());
        highlightCell(c, cellStates.getOrDefault(c, CellState.STANDARD), true);
    }
}
