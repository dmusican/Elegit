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

    private static int rootHeight;
    private static int[] heightCounts;
    private static List<String> visited;

    /**
     * Recursively rearranges the given graph into a tree layout
     * @param g the graph to layout
     */
    public static void doTreeLayout(TreeGraph g){

        TreeGraphModel treeGraphModel = g.getTreeGraphModel();
        Cell rootCell = treeGraphModel.getRoot();

        relocateCell(rootCell);
    }

    /**
     * Records the root cell's height and initializes variables
     * necessary for recursion, then starts relocation of every
     * cell recursively
     * @param root the root of the tree upon which the tree is built
     */
    private static void relocateCell(Cell root){
        rootHeight = root.height;
        heightCounts = new int[rootHeight+1];
        visited = new ArrayList<>();

        relocateCell(root, rootHeight);
    }

    /**
     * Moves the given cell to its appropriate x and y position based on its
     * depth in the tree and how many cells have already been placed on that
     * depth. Gets called recursively on the children of the given cell by
     * order of their respective heights
     * @param c the cell to relocate
     * @param lastHeight the height of the previously added cell
     */
    private static void relocateCell(Cell c, int lastHeight){
        visited.add(c.getCellId());

        int h = getHeightOfCell(c);
        double x = heightCounts[h] * H_SPACING + H_PAD;
        double y = h * V_SPACING + V_PAD;
        c.relocate(x, y);

        heightCounts[h] += 1;

        for(int i = h; i < lastHeight; i++){
            if(heightCounts[i] < heightCounts[lastHeight]){
                heightCounts[i] += 1;
            }
        }

        List<Cell> list = c.getCellChildren();
        list.sort(null);

        for(Cell child : list){
            if(!visited.contains(child.getCellId())){
                relocateCell(child, h);
            }
        }
    }

    private static int getHeightOfCell(Cell c){
        return c.height;
//        long timeFromMostRecent = (minTime - c.getTimeInMillis());
//        double ratio = ((double)Math.max(1,timeFromMostRecent))/intervalSize;
//        int pos = (int)(ratio * numSteps);
//        return pos;
    }
}
