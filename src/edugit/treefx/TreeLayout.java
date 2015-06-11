package edugit.treefx;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by makik on 6/11/15.
 */
public class TreeLayout extends Layout{

    public static int V_SPACING = 50;
    public static int H_SPACING = 25;
    public static int V_PAD = 25;
    public static int H_PAD = 10;

    TreeGraph treeGraph;
    int rootHeight;
    int[] depthCounts;
    List<String> visited;

    public TreeLayout(TreeGraph g){
        this.treeGraph = g;
    }
    @Override
    public void execute(){
        TreeGraphModel treeGraphModel = treeGraph.getTreeGraphModel();
        Cell rootCell = treeGraphModel.getRoot();

        relocateCell(rootCell);
    }

    private void relocateCell(Cell root){
        rootHeight = root.height;
        depthCounts = new int[rootHeight+1];
        visited = new ArrayList<>();

        relocateCell(root, 0);
    }

    private void relocateCell(Cell c, int depth){
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
