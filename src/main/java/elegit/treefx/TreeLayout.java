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


    /**
     * Mover service to go through moving the cells. Services are scheduled really well, so we
     * like using them for big repetitive things like this.
     */
    public static class MoveCellService extends Service {
        private int currentCell, percent, max;
        private List<Cell> allCellsSortedByTime;

        public MoveCellService (List<Cell> allCellsSortedByTime) {
            this.allCellsSortedByTime = allCellsSortedByTime;
            this.max = allCellsSortedByTime.size()-1;
            this.percent = 0;
        }

        public void setCurrentCell(int currentCell) { this.currentCell = currentCell; }

        protected synchronized Task createTask() {
            return new Task() {
                @Override
                protected Object call() throws Exception {
                    moveCell(allCellsSortedByTime.get(currentCell));

                    // Update progress if need be
                    if (100-(max*100.0/currentCell%100)>percent) {
                        updateProgress(currentCell, max);
                        percent++;
                    }
                    return null;
                }
            };
        }
    }


    // Service to compute the location of where all cells should go
    public static class ComputeCellPosService extends Service {
        private List<Integer> maxColumnUsedInRow;
        private List<Integer> maxColumnWantedInRow;
        private List<Cell> allCellsSortedByTime;
        private boolean isInitialSetupFinished;
        private int cellLocation;
        private List<Integer> movedCells;

        public ComputeCellPosService(List<Cell> allCellsSortedByTime, boolean isInitialSetupFinished) {
            this.maxColumnUsedInRow = new ArrayList<>();
            this.maxColumnWantedInRow = new ArrayList<>();

            this.cellLocation = allCellsSortedByTime.size()-1;
            this.allCellsSortedByTime = allCellsSortedByTime;
            this.isInitialSetupFinished = isInitialSetupFinished;

            this.movedCells = new ArrayList<>();
        }

        public void setCellLocation(int cellLocation) { this.cellLocation = cellLocation;  }

        @Override
        protected synchronized Task createTask() {
            return new Task() {
                @Override
                protected Void call() throws Exception {
                    if (cellLocation < 0)
                        this.cancelled();
                    if (movedCells.contains(cellLocation))
                        return null;

                    // Get cell at rightmost location not yet placed
                    Cell c = allCellsSortedByTime.get(allCellsSortedByTime.size()-1-cellLocation);

                    setCellPosition(c, cellLocation, getRowOfCellInColumn(maxColumnUsedInRow, cellLocation));

                    // Update the reserved columns in rows with the cells parents, oldest to newest
                    List<Cell> list = c.getCellParents();
                    list.sort((c1, c2) -> Long.compare(c1.getTime(), c2.getTime()));

                    // For each parent, oldest to newest, place it in the highest row possible recursively
                    for(Cell parent : list){
                        int col = allCellsSortedByTime.size() -1- allCellsSortedByTime.indexOf(parent);
                        int row = getRowOfCellInColumn(maxColumnWantedInRow, col);
                        if (maxColumnWantedInRow.size() > row)
                            maxColumnWantedInRow.set(row, col);
                        else
                            maxColumnWantedInRow.add(row, col);
                    }
                    return null;
                }
            };
        }

        private void setCellPosition(Cell c, int x, int y) {
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
            for (int i = y+1; i< maxColumnWantedInRow.size(); i++) {
                if (maxColumnWantedInRow.get(i) == i)
                    maxColumnWantedInRow.set(i, maxColumnUsedInRow.get(i));
            }

            // Set the animation and use parent properties of the cell
            c.setAnimate(isInitialSetupFinished && willCellMove);
            c.setUseParentAsSource(!hasCellMoved);

            this.movedCells.add(x);
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
            private List<Integer> maxColUsedInRow;
            private boolean isInitialSetupFinished;

            /**
             * Extracts the TreeGraphModel, sorts its cells by time, then relocates
             * every cell. When complete, updates the model if necessary to show
             * it has been through the layout process at least once already
             */
            @Override
            protected Void call() throws Exception{
                TreeGraphModel treeGraphModel = g.treeGraphModel;
                isInitialSetupFinished = treeGraphModel.isInitialSetupFinished;

                allCellsSortedByTime = treeGraphModel.allCells;
                sortListOfCells();

                maxColUsedInRow = new ArrayList<>();

                // Spin off thread to compute locations of the cells
                Thread thread = new Thread(new Task<Void>() {
                    @Override
                    protected Void call() throws Exception {
                        // Set the move cell service to start once the computation of cells finishes
                        this.setOnSucceeded(event -> {
                            MoveCellService mover = new MoveCellService(allCellsSortedByTime);
                            mover.setOnSucceeded(event1 -> {
                                mover.setCurrentCell(mover.currentCell-1);
                                mover.restart();
                            });
                            mover.setOnCancelled(event1 -> {
                                treeGraphModel.isInitialSetupFinished = true;
                            });
                            mover.start();
                        });


                        // Compute the positions of cells recursively
                        for (int i=allCellsSortedByTime.size()-1; i>=0; i--) {
                            // Tell the cell to move
                            computeCellPosition(i);
                        }


                        return null;
                    }
                });
                thread.setDaemon(true);
                thread.setName("Cell Position Computing");
                thread.start();
                return null;
            }


            /**
             * Helper method to sort the list of cells
             */
            private void sortListOfCells() {
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
            }


            /**
             * Helper method that computes the cell position for a given cell and its parents (oldest to newest), recursively
             * @param cellPosition position of cell to compute position for
             */
            public void computeCellPosition(int cellPosition) {

                // Get cell at rightmost location not yet placed
                Cell c = allCellsSortedByTime.get(allCellsSortedByTime.size()-1-cellPosition);

                setCellPosition(c, cellPosition, getRowOfCellInColumn(maxColUsedInRow, cellPosition));

                // Update the reserved columns in rows with the cells parents, oldest to newest
                List<Cell> list = c.getCellParents();
                list.sort((c1, c2) -> Long.compare(c1.getTime(), c2.getTime()));

                // For each parent, oldest to newest, place it in the highest row possible recursively
                for(Cell parent : list){
                    computeCellPosition(allCellsSortedByTime.indexOf(parent));
                }
            }

            public void setCellPosition(Cell c, int x, int y) {
                // See whether or not this cell will move
                int oldColumnLocation = c.columnLocationProperty.get();
                int oldRowLocation = c.rowLocationProperty.get();
                c.columnLocationProperty.set(x);
                c.rowLocationProperty.set(y);

                boolean hasCellMoved = oldColumnLocation >= 0 && oldRowLocation >= 0;
                boolean willCellMove = oldColumnLocation != x || oldRowLocation != y;

                // Update where the cell has been placed
                if (y >= maxColUsedInRow.size())
                    maxColUsedInRow.add(x);
                else
                    maxColUsedInRow.set(y, x);

                // Set the animation and use parent properties of the cell
                c.setAnimate(isInitialSetupFinished && willCellMove);
                c.setUseParentAsSource(!hasCellMoved);

                this.movedCells.add(x);
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
    private static int getRowOfCellInColumn(List<Integer> maxColumnUsedInRow, int cellCol){
        int row = 0;
        System.out.println(maxColumnUsedInRow+" "+cellCol);
        while(maxColumnUsedInRow.size() > row && (cellCol > maxColumnUsedInRow.get(row))){
            row++;
        }
        System.out.println(" "+row+" "+cellCol);
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
