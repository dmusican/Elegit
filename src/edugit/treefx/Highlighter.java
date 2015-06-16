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

    private static String selectedCellID = null;

    public static List<TreeGraphModel> trackedModels = new ArrayList<>();

    public static void handleMouseClicked(Cell cell){
        if(!isSelected(cell)){
            if(selectedCellID == null){
                selectCell(cell.getCellId(), true);
            }else{
                selectCell(selectedCellID, false);
                selectCell(cell.getCellId(), true);
            }
        }else{
            selectCell(cell.getCellId(), false);
        }
        Edge.allVisible.set(selectedCellID == null);
    }

    public static void handleMouseover(Cell cell, boolean isOverCell){
        if(!isSelected(cell)){
            List<Cell> relatives = cell.getCellParents();
            relatives.addAll(cell.getCellChildren());
            relatives.add(cell);
            for(Cell c : relatives){
                if(!isRelativeSelected(c)){
                    if(isOverCell){
                        highlightCell(c.getCellId(), HIGHLIGHT_COLORS[selectedCellID == null ? 0 : 1], false);
                    }else{
                        highlightCell(c.getCellId(), STANDARD_COLOR, false);
                    }
                }
            }
            updateCellEdges(cell.getCellId(), isOverCell);
        }
    }

    private static boolean isSelected(Cell c){
        return isSelected(c.getCellId());
    }

    private static boolean isSelected(String cellID){
        return selectedCellID != null && selectedCellID.equals(cellID);
    }

    private static void selectCell(String cellID, boolean enable){
        Color color;
        if(enable){
            color = SELECT_COLOR;
            selectedCellID = cellID;
        }else{
            color = STANDARD_COLOR;
            selectedCellID = null;
        }

        for(TreeGraphModel m : trackedModels){
            Cell cell = m.cellMap.get(cellID);
            if(cell == null) continue;

            cell.setColor(color);

            List<Cell> relatives = cell.getCellParents();
            relatives.addAll(cell.getCellChildren());
            highlightAllCells(relatives, color, false);
        }

        updateCellEdges(cellID, enable);
    }

    private static void updateCellEdges(String cellID, boolean enable){
        for(TreeGraphModel m : trackedModels){
            Cell selectedCell = m.cellMap.get(selectedCellID);
            if(selectedCell == null) continue;
            Cell cell = m.cellMap.get(cellID);
            if(cell == null) continue;

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
    }

    private static void highlightCell(String cellID, Color c, boolean highlightRelatives){
        for(TreeGraphModel m : trackedModels){
            Cell cell = m.cellMap.get(cellID);
            if(cell == null) continue;

            if(!isSelected(cellID)){
                cell.setColor(c);
            }

            if(highlightRelatives){
                List<Cell> relatives = cell.getCellParents();
                relatives.addAll(cell.getCellChildren());
                highlightAllCells(relatives, c, false);
            }
        }
    }

    private static void highlightAllCells(List<Cell> cells, Color c, boolean highlightRelatives){
        for(Cell cell : cells){
            highlightCell(cell.getCellId(), c, highlightRelatives);
        }
    }

    private static boolean isRelativeSelected(Cell cell){
        List<Cell> relatives = cell.getCellParents();
        relatives.addAll(cell.getCellChildren());
        for(Cell c : relatives){
            if(isSelected(cell)){
                return true;
            }
        }
        return false;
    }
}
