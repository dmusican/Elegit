package edugit.treefx;

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
        double x = heightCounts[c.height] * H_SPACING + H_PAD;
        double y = c.height * V_SPACING + V_PAD;
        c.relocate(x, y);

        heightCounts[c.height] += 1;

//        for(int i = c.height; i < lastHeight; i++){
//            System.out.println("Is it useful?");
//            if(heightCounts[i] <= heightCounts[lastHeight]){
//                System.out.println("Yes");
//                heightCounts[i] += 1;
//            }
//        }

        List<Cell> list = c.getCellChildren();
        list.sort(null);

        for(Cell child : list){
            if(!visited.contains(child.getCellId())){
                relocateCell(child, c.height);
            }
        }
    }
}
