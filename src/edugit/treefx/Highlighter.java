package edugit.treefx;

import edugit.MatchedScrollPane;
import javafx.animation.*;
import javafx.scene.paint.Color;
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

    // Color constants
    public static final Color STANDARD_COLOR = Color.BLUE;
    public static final Color SELECT_COLOR = Color.DARKRED;
    public static final Color[] HIGHLIGHT_COLORS = {Color.RED, Color.MEDIUMSEAGREEN};
    public static final Color EMPHASIZE_COLOR = Color.FORESTGREEN;

    private static final List<String> blockedCellIDs = new ArrayList<>();
    private static final Map<Cell, Color> cellColors = new HashMap<>();

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
            highlightCell(cell, SELECT_COLOR);
            highlightAllRelatives(cellID, model, HIGHLIGHT_COLORS[0]);
        }else{
            highlightCell(cell, STANDARD_COLOR);
            highlightAllRelatives(cellID, model, STANDARD_COLOR);
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
     * @param color the color to highlight the relatives
     */
    private static void highlightAllRelatives(String cellID, TreeGraphModel model, Color color){
        highlightAllCells(model.getRelatives(cellID), color);
    }

    /**
     * Helper method to highlight every relative of a cell with the given color, unless the
     * relative is a neighbor to the given cell to avoid
     * @param cellID the cell whose relatives will be highlighted
     * @param neighborID the cell whose neighbors will not be highlighted, even if they are a relative of the given cell
     * @param model the model wherein these cells are found
     * @param color the color to highlight the valid relatives
     */
    private static void highlightAllRelativesWithoutNeighbor(String cellID, String neighborID, TreeGraphModel model, Color color){
        List<Cell> relatives = model.getRelatives(cellID);
        List<Cell> relativesToHighlight = new ArrayList<>();
        for(Cell c : relatives){
            if(!c.getCellId().equals(neighborID) && !model.isNeighbor(c.getCellId(), neighborID)){
                relativesToHighlight.add(c);
            }
        }
        highlightAllCells(relativesToHighlight, color);
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

        Color color;
        if(enable){
            if(selectedCellID == null){
                color = HIGHLIGHT_COLORS[0];
            }else{
                color = HIGHLIGHT_COLORS[1];
            }
        }else{
            color = STANDARD_COLOR;
        }

        if(!cellID.equals(selectedCellID) && !model.isNeighbor(cellID, selectedCellID)){
            highlightCell(cell, color);
        }

        highlightAllRelativesWithoutNeighbor(cellID, selectedCellID, model, color);
    }

    /**
     * Helper method that sets the color of all cells in the given list to be
     * the given color
     * @param cells the cells to color
     * @param c the color
     */
    private static void highlightAllCells(List<Cell> cells, Color c){
        for(Cell cell : cells){
            highlightCell(cell, c);
        }
    }

    /**
     * Helper method to set the color of a cell
     * @param cell the cell to color
     * @param c the color
     */
    private static void highlightCell(Cell cell, Color c){
        cellColors.put(cell, c);
        if(blockedCellIDs.contains(cell.getCellId())) return;
        cell.setColor(c);
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

        Shape s = (Shape) c.view;

        ScaleTransition sct = new ScaleTransition(Duration.millis(625), s);
        sct.setByX(0.3f);
        sct.setByY(0.3f);
        sct.setCycleCount(6);
        sct.setAutoReverse(true);

        SequentialTransition sqt;

        if(c instanceof InvisibleCell){
            FillTransition st1 = new FillTransition(Duration.millis(1000), s, Color.TRANSPARENT, Highlighter.EMPHASIZE_COLOR);
            st1.setCycleCount(1);

            FillTransition st2 = new FillTransition(Duration.millis(1500), s, Highlighter.EMPHASIZE_COLOR, Color.TRANSPARENT);
            st2.setCycleCount(1);

            sqt = new SequentialTransition(st1, new PauseTransition(Duration.millis(2500)), st2);
            sqt.setCycleCount(1);

        }else{
            FillTransition ft1 = new FillTransition(Duration.millis(1000), s, Highlighter.STANDARD_COLOR, Highlighter.EMPHASIZE_COLOR);
            ft1.setCycleCount(1);

            FillTransition ft2 = new FillTransition(Duration.millis(1500), s, Highlighter.EMPHASIZE_COLOR, Highlighter.STANDARD_COLOR);
            ft2.setCycleCount(1);

            sqt = new SequentialTransition(ft1, new PauseTransition(Duration.millis(2500)), ft2);
            sqt.setCycleCount(1);
        }

        ParallelTransition pt = new ParallelTransition(sqt, sct);
        pt.play();

        pt.setOnFinished(event -> endEmphasisOnCell(c));
    }

    private static void endEmphasisOnCell(Cell c){
        blockedCellIDs.remove(c.getCellId());
        highlightCell(c, cellColors.getOrDefault(c, STANDARD_COLOR));
    }
}
