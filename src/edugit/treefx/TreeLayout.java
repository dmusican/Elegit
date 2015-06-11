package edugit.treefx;

import java.util.List;

/**
 * Created by makik on 6/11/15.
 */
public class TreeLayout extends Layout{

    public static int V_SPACING = 50;
    public static int H_SPACING = 25;
    public static int V_PAD = 25;
    public static int H_PAD = 10;

    Graph graph;
    int[] depthCounts;
    int rootHeight;

    public TreeLayout(Graph g){
        this.graph = g;
    }
    @Override
    public void execute(){

        Model model = graph.getModel();

        List<Cell> cells = model.getAllCells();

        Cell rootCell = model.getRoot();

        depthCounts = new int[cells.size()];
        rootHeight = rootCell.height + 1;

        relocate(rootCell, 0);
    }

    private void relocate(Cell c, int depth){
        double x = (depthCounts[depth]) * H_SPACING + H_PAD;
        double y = (rootHeight - depth) * V_SPACING + V_PAD;
        c.relocate(x,y);

        depthCounts[depth] += 1;

        List<Cell> list = c.getCellChildren();
        list.sort(null);

        for(Cell child : list){
            relocate(child, depth + 1);
        }
    }
}
