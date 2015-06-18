package edugit.treefx;

import java.util.*;

/**
 * Created by makik on 6/11/15.
 *
 * Handles the layout of cells in a TreeGraph in an appropriate tree structure
 */
public class TreeLayout{

    public static int V_SPACING = Cell.BOX_SIZE * 3 + 5;
    public static int H_SPACING = Cell.BOX_SIZE + 10;
    public static int V_PAD = 25;
    public static int H_PAD = 10;

    private static List<String> visited;
    private static List<Integer> minRowUsedInColumn;

    private static List<Cell> allCellsSortedByTime;

    /**
     * Recursively rearranges the given graph into a tree layout
     * @param g the graph to layout
     */
    public static void doTreeLayout(TreeGraph g){

        TreeGraphModel treeGraphModel = g.getTreeGraphModel();

        allCellsSortedByTime = treeGraphModel.allCells;
        allCellsSortedByTime.sort((c1, c2) -> Long.compare(c2.getTime(), c1.getTime()));

        relocateCells();
    }

    /**
     * Places all cells in the graph column by column, starting at the
     * cell furthest from the top (the oldest cell). Tracks the row at
     * which each column placed ends so as to make sure the space used
     * is as compact as possible.
     */
    private static void relocateCells(){
        visited = new ArrayList<>();
        minRowUsedInColumn = new ArrayList<>();

        int currentColumn;
        for(int i = allCellsSortedByTime.size() - 1; i >= 0; i--){
            Cell c = allCellsSortedByTime.get(i);
            if(!visited.contains(c.getCellId())){
                currentColumn = getColumnOfCell(c);
                int minHeight = relocateCellColumn(c);
                if(minRowUsedInColumn.size()-1 < currentColumn){
                    minRowUsedInColumn.add(currentColumn, minHeight);
                }else if(minHeight < minRowUsedInColumn.get(currentColumn)){
                    minRowUsedInColumn.set(currentColumn, minHeight);
                }
            }
        }
    }

    /**
     * Places the given cell into the column closest to the
     * left of the screen, and then chooses its child closest
     * to the top to place in next. Each recursive call happens
     * on only a single of c's children to allow the columns to
     * space correctly
     * @param c the cell to place
     * @return the minimum row in which a cell was placed before
     * it had no non-visited children
     */
    private static int relocateCellColumn(Cell c){
        visited.add(c.getCellId());

        int w = getColumnOfCell(c);
        int h = getRowOfCell(c);

        c.xLocationProperty.set(w);
        c.yLocationProperty.set(h);

        double x = c.xLocationProperty.get() * H_SPACING + H_PAD;
        double y = c.yLocationProperty.get() * V_SPACING + V_PAD;
        c.relocate(x, y);

        List<Cell> list = c.getCellChildren();
        list.sort((c1, c2) -> Long.compare(c2.getTime(), c1.getTime()));

        for(Cell child : list){
            if(!visited.contains(child.getCellId())){
                return relocateCellColumn(child);
            }
        }
        return h;
    }

    /**
     * Calculates the column closest to the left of the screen to place the
     * given cell based on the cell's row and the minimum heights recorded
     * for each column
     * @param c the cell to examine
     * @return the lowest indexed column in which to place c
     */
    private static int getColumnOfCell(Cell c){
        int column = 0;
        int cellRow = getRowOfCell(c);
        while(minRowUsedInColumn.size() > column && cellRow > minRowUsedInColumn.get(column)){
            column++;
        }
        return column;
    }

    /**
     * Gets the row of the given cell, with row 0 being the top of the screen
     * and the root cell of the tree being at the bottom
     * @param c the cell to examine
     * @return the row index of this cell
     */
    private static int getRowOfCell(Cell c){
        return allCellsSortedByTime.indexOf(c);
    }
}
