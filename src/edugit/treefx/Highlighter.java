package edugit.treefx;

import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by makik on 6/16/15.
 */
public class Highlighter{

    public static final Color STANDARD_COLOR = Color.BLUE;
    public static final Color SELECT_COLOR = Color.DARKRED;
    public static final Color[] HIGHLIGHT_COLORS = {Color.RED, Color.MEDIUMSEAGREEN};

    private static Cell selectedCell = null;

    public static void handleMouseClicked(Cell cell){
        if(!isSelected(cell)){
            if(selectedCell == null){
                selectCell(cell, true);
            }else{
                selectCell(selectedCell, false);
                selectCell(cell, true);
            }
        }else{
            selectCell(cell, false);
        }
        Edge.allVisible.set(selectedCell == null);
    }

    public static void handleMouseover(Cell cell, boolean isOverCell){
        if(!isSelected(cell)){
            List<Cell> relatives = cell.getCellParents();
            relatives.addAll(cell.getCellChildren());
            relatives.add(cell);
            for(Cell c : relatives){
                if(!isRelativeSelected(c)){
                    if(isOverCell){
                        highlightCell(c, HIGHLIGHT_COLORS[selectedCell == null ? 0 : 1], false);
                    }else{
                        highlightCell(c, STANDARD_COLOR, false);
                    }
                }
            }
            updateCellEdges(cell, isOverCell);
        }
    }

    private static boolean isSelected(Cell c){
        return selectedCell != null && selectedCell.equals(c);
    }

    private static void selectCell(Cell cell, boolean enable){
        Color color;
        if(enable){
            color = SELECT_COLOR;
            selectedCell = cell;
        }else{
            color = STANDARD_COLOR;
            selectedCell = null;
        }
        cell.setColor(color);

        List<Cell> relatives = cell.getCellParents();
        relatives.addAll(cell.getCellChildren());
        highlightAllCells(relatives, color, false);

        updateCellEdges(cell, enable);
    }

    private static void updateCellEdges(Cell cell, boolean enable){
        List<Edge> list = new ArrayList<>(cell.edges);
        if(!enable && selectedCell != null){
            for(Edge e :selectedCell.edges){
                list.remove(e);
            }
        }
        for(Edge e : list){
            e.setHighlighted(enable);
        }
    }

    private static void highlightCell(Cell cell, Color c, boolean highlightRelatives){
        if(!isSelected(cell)){
            cell.setColor(c);
        }

        if(highlightRelatives){
            List<Cell> relatives = cell.getCellParents();
            relatives.addAll(cell.getCellChildren());
            highlightAllCells(relatives, c, false);
        }
    }

    private static void highlightAllCells(List<Cell> cells, Color c, boolean highlightRelatives){
        for(Cell child : cells){
            highlightCell(child, c, highlightRelatives);
        }
    }

    private static boolean isRelativeSelected(Cell cell){
        List<Cell> relatives = cell.getCellParents();
        relatives.addAll(cell.getCellChildren());
        for(Cell c : relatives){
            if(isSelected(c)){
                return true;
            }
        }
        return false;
    }
}
