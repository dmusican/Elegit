package elegit.treefx;

import elegit.Main;
import elegit.SessionController;
import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.scene.Group;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.text.Text;

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
    public static boolean movingCells;


    /**
     * Mover service to go through moving the cells. Services are scheduled really well, so we
     * like using them for big repetitive things like this.
     */
    public static class MoveCellService extends Service {
        private int currentCell, max;
        private List<Cell> allCellsSortedByTime;
        private IntegerProperty percent;

        public MoveCellService (List<Cell> allCellsSortedByTime) {
            this.allCellsSortedByTime = allCellsSortedByTime;
            this.max = allCellsSortedByTime.size()-1;
            this.percent = new SimpleIntegerProperty(0);
            this.currentCell = 0;
            movingCells = true;
        }

        public void setCurrentCell(int currentCell) { this.currentCell = currentCell; }

        protected synchronized Task createTask() {
            return new Task() {
                @Override
                protected Object call() throws Exception {
                    for (int i=currentCell; i<currentCell+10; i++) {
                        if (i > allCellsSortedByTime.size() - 1) {
                            percent.set(100);
                            this.cancelled();
                        }
                        moveCell(allCellsSortedByTime.get(i));

                        // Update progress if need be
                        if (i * 100.0 / max > percent.get()) {
                            percent.set(i*100/max);
                        }
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
            private List<Integer> maxColUsedInRow;
            private List<Integer> movedCells;
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

                // Initialize variables
                maxColUsedInRow = new ArrayList<>();
                movedCells = new ArrayList<>();

                // Compute the positions of cells recursively
                for (int i=allCellsSortedByTime.size()-1; i>=0; i--) {
                    computeCellPosition(i);
                }
                // Once all cell's positions have been set, move them in a service
                MoveCellService mover = new MoveCellService(allCellsSortedByTime);

                //********************* Loading Bar Start *********************
                Pane cellLayer = g.getCellLayerPane();
                ScrollPane sp = g.getScrollPane();
                ProgressBar progressBar = new ProgressBar();
                Text loadingCommits = new Text("Loading commits ");
                HBox loading = new HBox(loadingCommits, progressBar);
                loading.setSpacing(5);
                loading.setLayoutX(10);
                loading.setLayoutY(10);
                cellLayer.getChildren().add(loading);

                mover.percent.addListener(((observable, oldValue, newValue) -> {
                    if((int)newValue == 100) {
                        loading.setVisible(false);
                    }
                }));
                //********************** Loading Bar End **********************

                mover.setOnSucceeded(event1 -> {
                    if (!Main.isAppClosed && movingCells) {
                        mover.setCurrentCell(mover.currentCell + 10);
                        progressBar.setProgress(mover.percent.get() / 100.0);
                        mover.restart();
                    }else {
                        mover.cancel();
                    }
                });

                mover.setOnCancelled(event1 -> treeGraphModel.isInitialSetupFinished = true);

                mover.reset();
                mover.start();
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
            private void computeCellPosition(int cellPosition) {
                // Don't try to compute a new position if the cell has already been moved
                if (movedCells.contains(cellPosition))
                    return;

                // Get cell at the inputted position
                Cell c = allCellsSortedByTime.get(allCellsSortedByTime.size()-1-cellPosition);

                setCellPosition(c, cellPosition, getRowOfCellInColumn(maxColUsedInRow, cellPosition));

                // Update the reserved columns in rows with the cells parents, oldest to newest
                List<Cell> list = c.getCellParents();
                list.sort((c1, c2) -> Long.compare(c1.getTime(), c2.getTime()));

                // For each parent, oldest to newest, place it in the highest row possible recursively
                for(Cell parent : list){
                    if (parent.getTime()>c.getTime() || allCellsSortedByTime.indexOf(parent)<0) break;
                    computeCellPosition(allCellsSortedByTime.size()-1-allCellsSortedByTime.indexOf(parent));
                    break;
                }
            }

            private void setCellPosition(Cell c, int x, int y) {
                // See whether or not this cell will move
                int oldColumnLocation = c.columnLocationProperty.get();
                int oldRowLocation = c.rowLocationProperty.get();
                c.columnLocationProperty.set(allCellsSortedByTime.size()-1-x);
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
                    c.moveTo(py, px, false, false);
                }

                double x = c.columnLocationProperty.get() * H_SPACING + H_PAD;
                double y = c.rowLocationProperty.get() * V_SPACING + V_PAD;
                c.moveTo(y, x, animate, animate && useParentPosAsSource);
                return null;
            }
        });
    }

    public static synchronized void stopMovingCells(){
        movingCells = false;
    }

}
