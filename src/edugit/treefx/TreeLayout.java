package edugit.treefx;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by makik on 6/11/15.
 *
 * Handles the layout of cells in a TreeGraph in an appropriate tree structure
 */
public class TreeLayout{

    public static int V_SPACING = 50;
    public static int H_SPACING = 25;
    public static int V_PAD = 25;
    public static int H_PAD = 10;

    private static int rootHeight;
    private static int[] depthCounts;
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
        depthCounts = new int[rootHeight+1];
        visited = new ArrayList<>();

        relocateCell(root, 0);
    }

    /**
     * Moves the given cell to its appropriate x and y position based on its
     * depth in the tree and how many cells have already been placed on that
     * depth. Gets called recursively on the children of the given cell by
     * order of their respective heights
     * @param c the cell to relocate
     * @param depth the current depth of the cell
     */
    private static void relocateCell(Cell c, int depth){
        visited.add(c.getCellId());
        double x = (depthCounts[depth]) * H_SPACING + H_PAD;
        double y = (rootHeight - depth) * V_SPACING + V_PAD;
        c.relocate(x, y);

        depthCounts[depth] += 1;

        List<Cell> list = c.getCellChildren();
        list.sort(null);

        for(Cell child : list){
            if(!visited.contains(child.getCellId())){
                relocateCell(child, depth + 1);
            }
        }
    }
}
