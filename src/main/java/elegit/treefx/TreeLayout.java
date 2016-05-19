package elegit.treefx;

import javafx.application.Platform;
import javafx.concurrent.Task;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles the layout of cells in a TreeGraph in an appropriate tree structure
 */
public class TreeLayout{

    public static int V_SPACING = Cell.BOX_SIZE + 10;
    public static int H_SPACING = Cell.BOX_SIZE * 3 + 5;
    public static int V_PAD = 10;
    public static int H_PAD = 25;

    /**
     * Returns a task that will take care of laying out the given
     * graph into a tree. Uses a combination of recursion and
     * iteration to pack cells as far up as possible with each
     * cell being arranged horizontally based on time
     * @param g the graph to layout
     * @return a task that, when executed, does the layout of g
     */
    public static Task getTreeLayoutTask(TreeGraph g){

        return new Task<Void>(){

            private List<String> visited;
            private List<Integer> maxColumnUsedInRow;

            private List<Cell> allCellsSortedByTime;

            /**
             * Extracts the TreeGraphModel, sorts its cells by time, then relocates
             * every cell. When complete, updates the model if necessary to show
             * it has been through the layout process at least once already
             */
            @Override
            protected Void call() throws Exception{
                TreeGraphModel treeGraphModel = g.treeGraphModel;

                allCellsSortedByTime = treeGraphModel.allCells;
                allCellsSortedByTime.sort((c1, c2) -> {
                    int i = Long.compare(c2.getTime(), c1.getTime());
                    if(i == 0){
                        if(c2.getCellChildren().contains(c1)){
                            return -1;
                        }else if(c1.getCellChildren().contains(c2)){
                            return 1;
                        }
                    }
                    return i;
                });

                relocateCells(treeGraphModel.isInitialSetupFinished);
                if(!isCancelled()){
                    treeGraphModel.isInitialSetupFinished = true;
                }
                return null;
            }

            /**
             * Places all cells in the graph row by row, starting at the
             * cell furthest to the left (the oldest cell). Tracks the column at
             * which each row placed ends so as to make sure the space used
             * is as compact as possible.
             */
            private void relocateCells(boolean isInitialSetupFinished){
                visited = new ArrayList<>();
                maxColumnUsedInRow = new ArrayList<>();

                for(int i = allCellsSortedByTime.size() - 1; i >= 0; i--){
                    if(isCancelled()) return;
                    Cell c = allCellsSortedByTime.get(i);
                    if(!visited.contains(c.getCellId())){
                        int maxCol = relocateCellAndChildRow(c, isInitialSetupFinished);
                        updateMaxColumnArray(maxCol);
                    }
                }
            }

            /**
             * Places the given cell into the row closest to the
             * top of the screen, and then chooses its child furthest
             * to the right to place in next. Each recursive call happens
             * on only a single of c's children to allow the rows to
             * space correctly
             * @param c the cell to place
             * @return the maximum column in which a cell was placed before
             * it had no non-visited children
             */
            private int relocateCellAndChildRow(Cell c, boolean animateRelocatedCells){
                if(isCancelled()) return -1;
                visited.add(c.getCellId());

                int x = getColumnOfCell(c);
                int y = getRowOfCellInColumn(x);

                int oldColumnLocation = c.columnLocationProperty.get();
                int oldRowLocation = c.rowLocationProperty.get();

                c.columnLocationProperty.set(x);
                c.rowLocationProperty.set(y);

                boolean hasCellMoved = oldColumnLocation >= 0 && oldRowLocation >= 0;
                boolean willCellMove = oldColumnLocation != x || oldRowLocation != y;

                moveCell(c, animateRelocatedCells && willCellMove, !hasCellMoved);

                List<Cell> list = c.getCellChildren();
                list.sort((c1, c2) -> Long.compare(c2.getTime(), c1.getTime()));

                for(Cell child : list){
                    if(!visited.contains(child.getCellId())){
                        return relocateCellAndChildRow(child, animateRelocatedCells);
                    }
                }
                return x;
            }

            /**
             * Helper method that updates the given cell's position to the coordinates corresponding
             * to its stored row and column locations
             * @param c the cell to move
             * @param animate whether the given cell should have an animation towards its new position
             * @param useParentPosAsSource whether the given cell should move to its first parent before
             *                             being animated, to prevent animations starting from off screen
             */
            private void moveCell(Cell c, boolean animate, boolean useParentPosAsSource){
                Platform.runLater(new Task<Void>(){
                    @Override
                    protected Void call(){
                        if(animate && useParentPosAsSource && c.getCellParents().size()>0){
                            double px = c.getCellParents().get(0).columnLocationProperty.get() * H_SPACING + H_PAD;
                            double py = c.getCellParents().get(0).rowLocationProperty.get() * V_SPACING + V_PAD;
                            c.moveTo(px, py, false, false);
                        }

                        double x = c.columnLocationProperty.get() * H_SPACING + H_PAD;
                        double y = c.rowLocationProperty.get() * V_SPACING + V_PAD;
                        c.moveTo(x, y, animate, animate && useParentPosAsSource);
                        return null;
                    }
                });
            }

            /**
             * Updates the array holding the max column used in every row
             * @param column the column to with
             */
            private void updateMaxColumnArray(int column){
                int rowOfColumn = getRowOfCellInColumn(column);
                if(maxColumnUsedInRow.size()-1 < rowOfColumn){
                    maxColumnUsedInRow.add(rowOfColumn, column);
                }else if(column > maxColumnUsedInRow.get(rowOfColumn)){
                    maxColumnUsedInRow.set(rowOfColumn, column);
                }
            }

            /**
             * Calculates the row closest to the top of the screen to place the
             * given cell based on the cell's column and the maximum heights recorded
             * for each row
             * @param cellCol the column the cell to examine is in
             * @return the lowest indexed row in which to place c
             */
            private int getRowOfCellInColumn(int cellCol){
                int row = 0;
                while(maxColumnUsedInRow.size() > row && (cellCol < maxColumnUsedInRow.get(row))){
                    row++;
                }
                return row;
            }

            /**
             * Gets the column of the given cell, with column 0 being the right of the screen
             * and the root cell of the tree being at the bottom
             * @param c the cell to examine
             * @return the column index of this cell
             */
            private int getColumnOfCell(Cell c){
                return allCellsSortedByTime.size() - 1 - allCellsSortedByTime.indexOf(c);
            }
        };
    }
}
