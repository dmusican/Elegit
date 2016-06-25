package elegit.treefx;

import javafx.application.Platform;
import javafx.concurrent.Service;
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

    public static int cells_moved = 0;

    // Service to compute the location of where all cells should go
    public static class ComputeCellPosService extends Service {
        private List<Integer> maxColumnUsedInRow;
        private List<Integer> maxColumnWantedInRow;
        private List<Cell> allCellsSortedByTime;
        private boolean isInitialSetupFinished;
        private int cellLocation;

        public ComputeCellPosService(List<Cell> allCellsSortedByTime, boolean isInitialSetupFinished) {
            this.maxColumnUsedInRow = new ArrayList<>();
            this.maxColumnWantedInRow = new ArrayList<>();
            this.maxColumnWantedInRow.add(0,allCellsSortedByTime.size());

            this.cellLocation = allCellsSortedByTime.size()-1;
            this.allCellsSortedByTime = allCellsSortedByTime;
            this.isInitialSetupFinished = isInitialSetupFinished;
        }

        public void setCellLocation(int cellLocation) { this.cellLocation = cellLocation;  }

        @Override
        protected synchronized Task createTask() {
            return new Task() {
                @Override
                protected Void call() throws Exception {
                    //cellLocation = allCellsSortedByTime.size()-1 - getCellsMoved();
                    if (cellLocation > allCellsSortedByTime.size()-1)
                        this.failed();

                    // Get cell at rightmost location not yet placed
                    Cell c = allCellsSortedByTime.get(allCellsSortedByTime.size()-1-cellLocation);

                    // Get where the cell should go based on which columns have been 'reserved'
                    int x = cellLocation;
                    int y = getRowOfCellInColumn(maxColumnWantedInRow, x);

                    // See whether or not this cell will move
                    int oldColumnLocation = c.columnLocationProperty.get();
                    int oldRowLocation = c.rowLocationProperty.get();
                    c.columnLocationProperty.set(x);
                    c.rowLocationProperty.set(y);

                    boolean hasCellMoved = oldColumnLocation >= 0 && oldRowLocation >= 0;
                    boolean willCellMove = oldColumnLocation != x || oldRowLocation != y;

                    // Update where the cell has been placed
                    if (y >= maxColumnUsedInRow.size())
                        maxColumnUsedInRow.add(x);
                    else
                        maxColumnUsedInRow.set(y, x);

                    // Update the reserved rows
                    for (int i = y+1; y< maxColumnWantedInRow.size(); y++)
                        if (maxColumnWantedInRow.get(i) == i)
                            maxColumnWantedInRow.set(i, maxColumnUsedInRow.get(i));

                    // Set the animation and use parent properties of the cell
                    c.setAnimate(isInitialSetupFinished && willCellMove);
                    c.setUseParentAsSource(!hasCellMoved);
                    // Move the cell - this will happen in a thread
                    //moveCell(c, isInitialSetupFinished && willCellMove, !hasCellMoved);

                    // Update the reserved columns in rows with the cells parents, oldest to newest
                    List<Cell> list = c.getCellParents();
                    list.sort((c1, c2) -> Long.compare(c1.getTime(), c2.getTime()));

                    // For each parent, oldest to newest, place its want of row in the highest possible
                    for(Cell parent : list){
                        int col = allCellsSortedByTime.size() -1- allCellsSortedByTime.indexOf(parent);
                        int row = getRowOfCellInColumn(maxColumnWantedInRow, col);
                        System.out.println(col+" "+row);
                        if (maxColumnWantedInRow.size() > row)
                            maxColumnWantedInRow.set(row, col);
                        else
                            maxColumnWantedInRow.add(row, col);
                    }
                    return null;
                }
            };
        }
    }


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
                ComputeCellPosService mover = new ComputeCellPosService(allCellsSortedByTime, false);
                mover.setOnFailed(event -> {
                    // Schedule the moving of all cells
                    Task moveCells = new Task() {
                        @Override
                        protected Object call() throws Exception {
                            int max = allCellsSortedByTime.size()-1;
                            int percent=0;
                            for (int i = 0; i < max; i++) {
                                moveCell(allCellsSortedByTime.get(i));
                                /*if (i>0 && 100-(max*100.0/i%100)>percent) {
                                    updateProgress(i, max);
                                    percent++;
                                }*/
                            }
                            return null;
                        }
                    };
                    Thread th = new Thread(moveCells);
                    th.setDaemon(true);
                    th.setName("Cell Mover");
                    th.start();
                    if(!isCancelled()){
                        treeGraphModel.isInitialSetupFinished = true;
                    }
                });
                mover.setOnSucceeded(event -> {
                    mover.setCellLocation(mover.cellLocation-1);
                    mover.restart();
                });
                mover.start();
                return null;
            }
        };
    }

    /**
     * Calculates the row closest to the top of the screen to place the
     * given cell based on the cell's column and the maximum heights recorded
     * for each row
     * @param maxColumnUsedInRow the map of max columns used in each row
     * @param cellCol the column the cell to examine is in
     * @return the lowest indexed row in which to place c
     */
    public static int getRowOfCellInColumn(List<Integer> maxColumnUsedInRow, int cellCol){
        int row = 0;
        while(maxColumnUsedInRow.size() > row && (cellCol > maxColumnUsedInRow.get(row))){
            row++;
        }
        return row;
    }

    /**
     * Helper method that updates the given cell's position to the coordinates corresponding
     * to its stored row and column locations
     * @param c the cell to move
     */
    public static void moveCell(Cell c){
        Platform.runLater(new Task<Void>(){
            @Override
            protected Void call(){
                boolean animate = c.getAnimate();
                boolean useParentPosAsSource = c.getUseParentAsSource();
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

}
