package main.java.edugit.treefx;

import javafx.animation.ScaleTransition;
import javafx.scene.shape.Shape;
import javafx.util.Duration;
import main.java.edugit.MatchedScrollPane;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class provides static methods for highlighting and animating cells in a tree graph
 */
public class Highlighter{

    private static final List<String> blockedCellIDs = new ArrayList<>();
    private static final Map<Cell, CellState> cellStates = new HashMap<>();

    /**
     * Highlights the cell corresponding to the given id in the given model, as well as
     * its relatives, with either the standard color or the constants SELECT_COLOR and
     * first HIGHLIGHT_COLOR, respectively
     * @param cellID the selected cell to highlight
     * @param model the model wherein the cell is found
     * @param enable whether to highlight the cell or return it to standard
     */
    public static void highlightSelectedCell(String cellID, TreeGraphModel model, boolean enable){
        Cell cell = model.cellMap.get(cellID);
        if(cell == null) return;
        if(enable){
            highlightCell(cell, CellState.SELECTED);
            highlightAllRelatives(cellID, model, CellState.HIGHLIGHTED1);
        }else{
            highlightCell(cell, CellState.STANDARD);
            highlightAllRelatives(cellID, model, CellState.STANDARD);
        }
    }

    /**
     * Takes care of ensuring the edges surrounding highlighted and selected cells are correctly
     * flagged as visible
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
     * Helper method to highlight every relative of a cell with the given color
     * @param cellID the cell whose relatives will be highlighted
     * @param model the model wherein the cell is found
     * @param state the new state of each relative
     */
    private static void highlightAllRelatives(String cellID, TreeGraphModel model, CellState state){
        highlightAllCells(model.getRelatives(cellID), state);
    }

    /**
     * Helper method to highlight every relative of a cell with the given color, unless the
     * relative is a neighbor to the given cell to avoid
     * @param cellID the cell whose relatives will be highlighted
     * @param neighborID the cell whose neighbors will not be highlighted, even if they are a relative of the given cell
     * @param model the model wherein these cells are found
     * @param state the new state for the valid relatives
     */
    private static void highlightAllRelativesWithoutNeighbor(String cellID, String neighborID, TreeGraphModel model, CellState state){
        List<Cell> relatives = model.getRelatives(cellID);
        List<Cell> relativesToHighlight = new ArrayList<>();
        for(Cell c : relatives){
            if(!c.getCellId().equals(neighborID) && !model.isNeighbor(c.getCellId(), neighborID)){
                relativesToHighlight.add(c);
            }
        }
        highlightAllCells(relativesToHighlight, state);
    }

    /**
     * Highlights the cell corresponding to the given id in the given model, as well as
     * its relatives, with either the standard color, the first HIGHLIGHT_COLOR constant,
     * or the second HIGHLIGHT_COLOR constant. When highlighting, if there is a selected cell
     * the second highlight color will be used. Otherwise, the first highlight color is chosen
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

        if(!cellID.equals(selectedCellID) && !model.isNeighbor(cellID, selectedCellID)){
            highlightCell(cell, state);
        }

        highlightAllRelativesWithoutNeighbor(cellID, selectedCellID, model, state);
    }

    /**
     * Helper method that sets the color of all cells in the given list to be
     * the given color
     * @param cells the cells to color
     * @param state the new state for the cell
     */
    private static void highlightAllCells(List<Cell> cells, CellState state){
        for(Cell cell : cells){
            highlightCell(cell, state);
        }
    }

    /**
     * Helper method to set the color of a cell
     * @param cell the cell to color
     * @param state the new state of the cell
     */
    private static void highlightCell(Cell cell, CellState state){
        cellStates.put(cell, state);
        if(blockedCellIDs.contains(cell.getCellId())) return;
        cell.setCellState(state);
    }

    /**
     * First requests the focus of the MatchedScrollPanes and then
     * performs an animation on the given cell in order to emphasize it.
     * Currently, the animation is a pulsing size and color change
     * @param c the cell to emphasize
     */
    public static void emphasizeCell(Cell c){
        if(!blockedCellIDs.contains(c.getCellId())){
            blockedCellIDs.add(c.getCellId());
        }

        MatchedScrollPane.scrollTo(c.columnLocationProperty.doubleValue() + 1);
        c.setCellState(CellState.EMPHASIZED);

        Shape s = (Shape) c.view;

        ScaleTransition sct = new ScaleTransition(Duration.millis(625), s);
        sct.setByX(0.3f);
        sct.setByY(0.3f);
        sct.setCycleCount(6);
        sct.setAutoReverse(true);

        c.view.setScaleX(1.0);
        c.view.setScaleY(1.0);

        sct.play();

        sct.setOnFinished(event -> endEmphasisOnCell(c));
    }

    private static void endEmphasisOnCell(Cell c){
        blockedCellIDs.remove(c.getCellId());
        highlightCell(c, cellStates.getOrDefault(c, CellState.STANDARD));
    }
}
