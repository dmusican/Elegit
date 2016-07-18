package elegit.treefx;

import elegit.Main;
import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.transform.Rotate;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles the layout of cells in a TreeGraph in an appropriate tree structure
 */
public class TreeLayout{

    public static int H_SPACING = Cell.BOX_SIZE + 10;
    public static int V_SPACING = Cell.BOX_SIZE * 3 + 5;
    public static int H_PAD = 10;
    public static int V_PAD = 25;
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
                    // Try/catch is just in for debugging purposes, left because any
                    // errors here are very hard to find without it
                    try {
                        for (int i = currentCell; i < currentCell + 10; i++) {
                            if (i > allCellsSortedByTime.size() - 1) {
                                percent.set(100);
                                return null;
                            }
                            moveCell(allCellsSortedByTime.get(i));

                            // Update progress if need be
                            if (i * 100.0 / max > percent.get() && percent.get() < 100) {
                                percent.set(i * 100 / max);
                            }
                        }
                        this.succeeded();
                    }catch (Exception e) {
                        e.printStackTrace();
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
            private List<Integer> minRowUsedInCol;
            private List<Integer> movedCells;
            private boolean isInitialSetupFinished;

            /**
             * Extracts the TreeGraphModel, sorts its cells by time, then relocates
             * every cell. When complete, updates the model if necessary to show
             * it has been through the layout process at least once already
             */
            @Override
            protected Void call() throws Exception{
                try {
                    TreeGraphModel treeGraphModel = g.treeGraphModel;
                    isInitialSetupFinished = treeGraphModel.isInitialSetupFinished;

                    allCellsSortedByTime = treeGraphModel.allCells;
                    sortListOfCells();

                    // Initialize variables
                    minRowUsedInCol = new ArrayList<>();
                    movedCells = new ArrayList<>();

                    // Compute the positions of cells recursively
                    for (int i = allCellsSortedByTime.size() - 1; i >= 0; i--) {
                        computeCellPosition(i);
                    }
                    // Once all cell's positions have been set, move them in a service
                    MoveCellService mover = new MoveCellService(allCellsSortedByTime);

                    //********************* Loading Bar Start *********************
                    Pane cellLayer = g.getCellLayerPane();
                    ScrollPane sp = g.getScrollPane();
                    SimpleDoubleProperty viewportY = new SimpleDoubleProperty(0);
                    SimpleDoubleProperty viewportX = new SimpleDoubleProperty(0);
                    ProgressBar progressBar = new ProgressBar();

                    Text loadingCommits = new Text("Loading commits ");
                    loadingCommits.setFont(new Font(14));
                    VBox loading = new VBox(loadingCommits, progressBar);
                    loading.setAlignment(Pos.CENTER);
                    loading.setSpacing(5);
                    loading.layoutYProperty().bind(viewportY);
                    loading.layoutXProperty().bind(viewportX);
                    loading.setRotationAxis(Rotate.X_AXIS);
                    loading.setRotate(180);
                    if (Platform.isFxApplicationThread()) {
                        cellLayer.getChildren().add(loading);
                    } else {
                        Platform.runLater(() -> cellLayer.getChildren().add(loading));
                    }

                    sp.vvalueProperty().addListener(((observable, oldValue, newValue) -> {
                        viewportY.set(cellLayer.getLayoutBounds().getMaxY()-((double) newValue * cellLayer.getLayoutBounds().getMaxY() +
                                (0.5 - (double) newValue) * sp.getViewportBounds().getHeight()));
                    }));

                    sp.viewportBoundsProperty().addListener(((observable, oldValue, newValue) -> {
                        viewportX.set(sp.getViewportBounds().getWidth() - loading.getWidth() - 35);
                        viewportY.set(cellLayer.getLayoutBounds().getMaxY()
                                - (sp.getVvalue() * cellLayer.getLayoutBounds().getMaxY()
                                + (0.5 - sp.getVvalue()) * sp.getViewportBounds().getHeight()));
                    }));

                    mover.percent.addListener(((observable, oldValue, newValue) -> {
                        if ((int) newValue == 100) {
                            loading.setVisible(false);
                        }
                    }));
                    //********************** Loading Bar End **********************

                    mover.setOnSucceeded(event1 -> {
                        if (!Main.isAppClosed && movingCells && mover.currentCell < allCellsSortedByTime.size() - 1) {
                            mover.setCurrentCell(mover.currentCell + 10);
                            progressBar.setProgress(mover.percent.get() / 100.0);
                            mover.restart();
                        } else {
                            treeGraphModel.isInitialSetupFinished = true;
                        }
                    });

                    mover.reset();
                    mover.start();
                } catch (Exception e) {
                    e.printStackTrace();
                }
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

                setCellPosition(c, getColumnOfCellInRow(minRowUsedInCol, cellPosition), cellPosition);

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

            /**
             * Helper method to set the position of a cell and update various
             * parameters for the cell
             *
             * @param c the cell to set the position of
             * @param x the new column of the cell
             * @param y the new row of the cell
             */
            private void setCellPosition(Cell c, int x, int y) {
                // See whether or not this cell will move
                int oldColumnLocation = c.columnLocationProperty.get();
                int oldRowLocation = c.rowLocationProperty.get();

                c.columnLocationProperty.set(x);
                c.rowLocationProperty.set(y);

                boolean hasCellMoved = oldColumnLocation >= 0 && oldRowLocation >= 0;
                boolean willCellMove = oldColumnLocation != x || oldRowLocation != y;

                // Update where the cell has been placed
                if (x >= minRowUsedInCol.size())
                    minRowUsedInCol.add(y);
                else
                    minRowUsedInCol.set(x, y);

                // Set the animation and use parent properties of the cell
                c.setAnimate(isInitialSetupFinished && willCellMove);
                c.setUseParentAsSource(!hasCellMoved);

                this.movedCells.add(y);
            }
        };
    }

    /**
     * Calculates the column closest to the left of the screen to place the
     * given cell based on the cell's row and the heights of each column so far
     * @param minRowUsedInCol the map of max rows used in each column
     * @param cellRow the row the cell to examine is in
     * @return the lowest indexed row in which to place c
     */
    private static int getColumnOfCellInRow(List<Integer> minRowUsedInCol, int cellRow){
        int col = 0;
        while(minRowUsedInCol.size() > col && (cellRow > minRowUsedInCol.get(col))){
            col++;
        }
        return col;
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

    public static synchronized void stopMovingCells(){
        movingCells = false;
    }

}
