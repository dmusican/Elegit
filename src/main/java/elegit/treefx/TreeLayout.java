package elegit.treefx;

import elegit.Main;
import elegit.models.CommitHelper;
import io.reactivex.Observable;
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler;
import io.reactivex.schedulers.Schedulers;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import org.apache.http.annotation.ThreadSafe;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Handles the layout of cells in a TreeGraph in an appropriate tree structure
 */
@ThreadSafe
// but critically only because of all the asserts requiring this be done only in the FX thread. Without that, it
// isn't threadsafe, not least because of the bindings that are done.
public class TreeLayout{

    static final int H_SPACING = Cell.BOX_SIZE + 10;
    static final int V_SPACING = Cell.BOX_SIZE * 3 + 5;
    static final int H_PAD = 10;
    static final int V_PAD = 25;

    static final AtomicBoolean movingCells = new AtomicBoolean();

    private final static BooleanProperty commitSortTopological = new SimpleBooleanProperty(true);


    public synchronized static void bindSorting(ReadOnlyBooleanProperty status) {
        commitSortTopological.bind(status);
    }

    private final static int CELLS_AT_A_TIME = 20;
    private final static int CELL_RENDER_TIME_DELAY = 100;

    /**
     * The task within is specifically designed to pick out 10 cells, and move them one-by-one.
     * A percentage tracker is also updated.
     * Everything in here should be super fast, with the exception of moving a cell, which calls
     * a separate method anyway, which in turn calls Platform.runLater; so it seems there's no reason
     * to do this as a separate thread. Let's try pulling that out.
     */
    public static class CellMover {
        private int currentCell;
        private final List<Cell> allCellsSortedByTime;
        private AtomicInteger percent;

        CellMover(List<Cell> allCellsSortedByTime) {
            //Main.assertNotFxThread();
            this.allCellsSortedByTime = Collections.unmodifiableList(allCellsSortedByTime);
            this.percent = new AtomicInteger(0);
            this.currentCell = 0;
            movingCells.set(true);
        }

        public boolean moveSomeCells(int startCell, Optional<CommitHelper> commitToHighlight) {
            Main.assertFxThread();
            for (int i = startCell; i < startCell + CELLS_AT_A_TIME; i++) {
                if (i > allCellsSortedByTime.size() - 1) {
                    percent.set(100);
                    return true;
                }
                ;
                Cell cellToMove = allCellsSortedByTime.get(i);
                moveCell(cellToMove);

                if (commitToHighlight.isPresent() && cellToMove.getCellId().equals(commitToHighlight.get().getName())) {
                        CommitTreeController.focusCommitInGraph(commitToHighlight.get());
                }

                // Update progress if need be
                int max = allCellsSortedByTime.size()-1;
                if (i * 100.0 / max > percent.get() && percent.get() < 100) {
                    percent.set(i * 100 / max);
                }
            }
            return true;
        }
    }


    /*
     * Takes care of laying out the given
     * graph into a tree. Uses a combination of recursion and
     * iteration to pack cells as far up as possible with each
     * cell being arranged horizontally based on time
     */
            /*
             * Extracts the TreeGraphModel, sorts its cells by time, then relocates
             * every cell. When complete, updates the model if necessary to show
             * it has been through the layout process at least once already
             */
    public static Observable<Boolean> doTreeLayout(TreeGraph g, CommitHelper commitToHighlight) {
        // TreeGraph has to live on FX thread, and this calls methods on it, so this should start there too.
        // Spin off slow parts as I need to, perhaps. Be careful, lots of code below involves bindings, etc, that
        // certainly must stay.
        Main.assertFxThread();
        try {
            TreeGraphModel treeGraphModel = g.treeGraphModel;

            // This is a new ArrayList, not a pointer to the one inside the model, so it can be safely changed here
            // (not the cells within, of course)
            List<Cell> allCells = treeGraphModel.getAllCells();
            if (commitSortTopological.get())
                topologicalSortListOfCells(allCells);
            else
                sortListOfCells(allCells);

            // Initialize variables
            Map<Integer,Integer> minRowUsedInCol = new ConcurrentHashMap<>();
            Set<Integer> movedCells = ConcurrentHashMap.newKeySet();

            boolean treeLayoutDoneAtLeastOnce = treeGraphModel.checkAndFlipTreeLayoutDoneAtLeastOnce();

            // Compute the positions of cells recursively
            for (int i = 0; i < allCells.size(); i++) {
                computeCellPosition(allCells, minRowUsedInCol, movedCells,
                        treeLayoutDoneAtLeastOnce, i);
            }

            // Once all cell's positions have been set, move them in a service
            CellMover mover = new CellMover(allCells);

            //********************* Loading Bar Start *********************
            Pane cellLayer = g.getCellLayerPane();
            ScrollPane sp = g.getScrollPane();
            SimpleDoubleProperty viewportY = new SimpleDoubleProperty(0);
            SimpleDoubleProperty viewportX = new SimpleDoubleProperty(0);
            ProgressBar progressBar = new ProgressBar();

            Text loadingCommits = new Text("Loading commits ");
            loadingCommits.setFont(new Font(14));
            VBox loading = new VBox(loadingCommits, progressBar);
//            loading.setPickOnBounds(false);
//            loading.setAlignment(Pos.CENTER);
//            loading.setSpacing(5);
//            loading.layoutYProperty().bind(viewportY);
//            loading.layoutXProperty().bind(viewportX);
            //loading.setRotationAxis(Rotate.X_AXIS);
//            loading.setRotate(180);
            // TODO: Put loading bar back in. It's out because it's not appearing anyway without threads currently,...
            // and also because it was messing up the top portion of the screen where the commits were supposed to go.
            //cellLayer.getChildren().add(loading);

//            sp.vvalueProperty().addListener(((observable, oldValue, newValue) -> {
//                viewportY.set(cellLayer.getLayoutBounds().getHeight()-((double) newValue * cellLayer.getLayoutBounds().getHeight() +
//                        (0.5 - (double) newValue) * sp.getViewportBounds().getHeight()));
//            }));
//
//            sp.viewportBoundsProperty().addListener(((observable, oldValue, newValue) -> {
//                viewportX.set(sp.getViewportBounds().getWidth() - loading.getWidth() - 35);
//                viewportY.set(cellLayer.getLayoutBounds().getHeight()
//                        - (sp.getVvalue() * cellLayer.getLayoutBounds().getHeight()
//                        + (0.5 - sp.getVvalue()) * sp.getViewportBounds().getHeight()));
//            }));
            //********************** Loading Bar End **********************

            final Optional<CommitHelper> commitToHighlightOrNot;
            if (treeLayoutDoneAtLeastOnce)
                commitToHighlightOrNot = Optional.empty();
            else
                commitToHighlightOrNot = Optional.of(commitToHighlight);

            return Observable.intervalRange(0, (int)(Math.ceil(allCells.size()/(double)CELLS_AT_A_TIME)),
                    0, CELL_RENDER_TIME_DELAY, TimeUnit.MILLISECONDS,
                    Schedulers.io())

                    .observeOn(JavaFxScheduler.platform())
                    .map(cellNumber -> mover.moveSomeCells(cellNumber.intValue()*CELLS_AT_A_TIME,
                            commitToHighlightOrNot));

//            while (!Main.isAppClosed.get() && movingCells.get() && mover.currentCell < allCells.size()) {
//                mover.moveSomeCells();
//                mover.setCurrentCell(mover.currentCell + CELLS_AT_A_TIME);
//                //progressBar.setProgress(mover.percent.get() / 100.0);
//            }

            //loadingCommits.setVisible(false);
            //progressBar.setVisible(false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Helper method that computes the cell position for a given cell and its parents (oldest to newest), recursively
     * @param cellIndex position of cell to compute position for
     */
    private static void computeCellPosition(List<Cell> allCells, Map<Integer,Integer> minRowUsedInCol,
                                            Set<Integer> movedCells, boolean isInitialSetupFinished,
                                            int cellIndex) {
        // This method calls setCellPosition, which critically needs to happen on the FX thread. It also interacts
        // directly with cells. Perhaps other portions of this could be spun off, but for now, keeping it here.
        Main.assertFxThread();

        boolean done = false;

        while (!done) {
            // Don't try to compute a new position if the cell has already been moved
            int ycoord = cellIndex; //allCells.size() - 1 - cellIndex;
            if (movedCells.contains(ycoord))
                return;

            // Get cell at the inputted position
            Cell c = allCells.get(cellIndex);

            int xcoord = getXCoordFromYCoord(minRowUsedInCol, ycoord);
            setCellPosition(c, minRowUsedInCol, movedCells, isInitialSetupFinished, xcoord, ycoord);

            // Update the reserved columns in rows with the cells parents, oldest to newest
            List<Cell> list = new ArrayList<>(c.getCellParents());
            list.sort(Comparator.comparingLong(Cell::getTime));

            // For each parent, oldest to newest, place it in the highest row possible recursively
            for (Cell parent : list) {
                if (parent.getTime() > c.getTime() || allCells.indexOf(parent) < 0) {
                    done = true;
                    break;
                }
                cellIndex = allCells.indexOf(parent);
                break;
            }
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
    private static void setCellPosition(Cell c, Map<Integer, Integer> minRowUsedInCol, Set<Integer> movedCells,
                                        boolean isInitialSetupFinished, int x, int y) {
        // This must run on the FX thread, since it uses properties that FX thread values will automatically be
        // seeing.
        Main.assertFxThread();

        // See whether or not this cell will move
        int oldColumnLocation = c.columnLocationProperty.get();
        int oldRowLocation = c.rowLocationProperty.get();

        c.columnLocationProperty.set(x);
        c.rowLocationProperty.set(y);

        boolean hasCellMoved = oldColumnLocation >= 0 && oldRowLocation >= 0;
        boolean willCellMove = oldColumnLocation != x || oldRowLocation != y;

        // Update where the cell has been placed
        minRowUsedInCol.put(x,y);

        // Set the animation and use parent properties of the cell
        c.setAnimate(isInitialSetupFinished && willCellMove);
        c.setUseParentAsSource(!hasCellMoved);

        movedCells.add(y);
    }

    /**
     * Helper method to sort the list of cells
     */
    public static void sortListOfCells(List<Cell> cellsToSort) {
        // For now, leaving this on the FX thread. Could possibly be spun off, but be careful about the Cell
        // accesses within.
        Main.assertFxThread();
        cellsToSort.sort((c1, c2) -> {
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
     * Helper method to sort the list of cells. Does a topological sort, so that cells which depend on others appear
     * first. Parents should appear later. Ties are broken by time, so that later commits appear more towards
     * the beginning (top) of the list.
     *
     * Uses Kahn's algorithm.
     */
    public static void topologicalSortListOfCells(List<Cell> cellsToSort) {
        // For now, leaving this on the FX thread. Could possibly be spun off, but be careful about the Cell
        // accesses within.
        Main.assertFxThread();

        Map<String,Integer> visitCount = new HashMap<>();

        // Queue to maintain which nodes are available next for exploring. Done as a priority queue so that the one
        // with the most recent is done first.
        Comparator<Cell> comparator =  (cell1, cell2) -> Long.compare((cell2.getTime()), cell1.getTime());

        PriorityQueue<Cell> pq = new PriorityQueue<>(10, comparator);

        // Initialize priority queue to be those nodes with no children
        for (Cell cell: cellsToSort) {
            if (cell.getCellChildren().size() == 0)
                pq.add(cell);
        }

        int originalSize = cellsToSort.size();
        cellsToSort.clear();

        // Inspired by https://en.wikipedia.org/wiki/Topological_sorting
        while (!pq.isEmpty()) {
            Cell current = pq.poll();
            cellsToSort.add(current);
            for (Cell parent : current.getCellParents()) {
                String parentId = parent.getCellId();
                visitCount.put(parentId, 1 + visitCount.getOrDefault(parentId, 0));
                int maxPossibleVisits = parent.getCellChildren().size();
                if (visitCount.get(parentId) == maxPossibleVisits) {
                    pq.add(parent);
                }
            }
        }

        assert(originalSize==cellsToSort.size());
    }


    /**
     * Calculates the column closest to the left of the screen to place the
     * given cell based on the cell's row and the heights of each column so far
     * @param minRowUsedInCol the map of max rows used in each column
     * @param ycoord the row the cell to examine is in
     * @return the lowest indexed row in which to place c
     */
    private static int getXCoordFromYCoord(Map<Integer, Integer> minRowUsedInCol, int ycoord){
        //Main.assertNotFxThread();
        int xcoord = 0;
        while(minRowUsedInCol.containsKey(xcoord) && (minRowUsedInCol.get(xcoord) > ycoord)){
            xcoord++;
        }
        return xcoord;
    }

    /**
     * Helper method that updates the given cell's position to the coordinates corresponding
     * to its stored row and column locations
     * @param c the cell to move
     */
    static void moveCell(Cell c){
//        Platform.runLater(() -> {
        Main.assertFxThread();
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
        //});
    }

    // This can get called from either a worker thread or from the FX thread. It's simply a trigger to tell it
    // to stop moving cells around if there's a thread running that's doing this. Critical that the method is
    // synchronized, as it ensures that the variable is not hit by more than one thread at a time.
    public static synchronized void stopMovingCells(){
        movingCells.set(false);
    }

}
