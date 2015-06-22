package edugit.treefx;

import javafx.application.Platform;
import javafx.concurrent.Task;

import java.util.ArrayList;
import java.util.List;

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

    /**
     * Returns a task that will take care of laying out the given
     * graph into a tree. Uses a combination of recursion and
     * iteration to pack cells as far left as possible with each
     * cell being arranged vertically based on time
     * @param g the graph to layout
     * @return a task that, when executed, does the layout
     */
    public static Task getTreeLayoutTask(TreeGraph g){

        return new Task<Void>(){

            private List<String> visited;
            private List<Integer> minRowUsedInColumn;

            private List<Cell> allCellsSortedByTime;

            @Override
            protected Void call() throws Exception{
                TreeGraphModel treeGraphModel = g.getTreeGraphModel();

                allCellsSortedByTime = treeGraphModel.allCells;
                allCellsSortedByTime.sort((c1, c2) -> Long.compare(c2.getTime(), c1.getTime()));

                relocateCells();

                treeGraphModel.isInitialSetupFinished = true;

                return null;
            }

            /**
             * Places all cells in the graph column by column, starting at the
             * cell furthest from the top (the oldest cell). Tracks the row at
             * which each column placed ends so as to make sure the space used
             * is as compact as possible.
             */
            private void relocateCells(){
                visited = new ArrayList<>();
                minRowUsedInColumn = new ArrayList<>();

                for(int i = allCellsSortedByTime.size() - 1; i >= 0; i--){
                    if(isCancelled()) return;
                    Cell c = allCellsSortedByTime.get(i);
                    if(!visited.contains(c.getCellId())){
                        int minRow = relocateCellAndChildColumn(c);
                        int columnOfMinRow = getColumnOfCellInRow(minRow);
                        if(minRowUsedInColumn.size()-1 < columnOfMinRow){
                            minRowUsedInColumn.add(columnOfMinRow, minRow);
                        }else if(minRow < minRowUsedInColumn.get(columnOfMinRow)){
                            minRowUsedInColumn.set(columnOfMinRow, minRow);
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
            private int relocateCellAndChildColumn(Cell c){
                if(isCancelled()) return -1;
                visited.add(c.getCellId());

                int h = getRowOfCell(c);
                int w = getColumnOfCellInRow(h);

                c.columnLocationProperty.set(w);
                c.rowLocationProperty.set(h);

                Platform.runLater(new Task<Void>(){
                    @Override
                    protected Void call(){
                        double x = c.columnLocationProperty.get() * H_SPACING + H_PAD;
                        double y = c.rowLocationProperty.get() * V_SPACING + V_PAD;
                        c.moveTo(x, y, false);
                        return null;
                    }
                });

                List<Cell> list = c.getCellChildren();
                list.sort((c1, c2) -> Long.compare(c2.getTime(), c1.getTime()));

                for(Cell child : list){
                    if(!visited.contains(child.getCellId())){
                        return relocateCellAndChildColumn(child);
                    }
                }
                return h;
            }

            /**
             * Calculates the column closest to the left of the screen to place the
             * given cell based on the cell's row and the minimum heights recorded
             * for each column
             * @param cellRow the row the cell to examine is in
             * @return the lowest indexed column in which to place c
             */
            private int getColumnOfCellInRow(int cellRow){
                int column = 0;
                while(minRowUsedInColumn.size() > column && (cellRow > minRowUsedInColumn.get(column))){
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
            private int getRowOfCell(Cell c){
                return allCellsSortedByTime.indexOf(c);
            }
        };
    }
}
