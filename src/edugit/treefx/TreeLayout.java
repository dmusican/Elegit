package edugit.treefx;

import java.util.List;

/**
 * Created by makik on 6/11/15.
 */
public class TreeLayout extends Layout{

    public static int V_SPACING = 50;
    public static int H_SPACING = 25;

    Graph graph;
    int[] depthCounts;

    public TreeLayout(Graph g){
        this.graph = g;
    }
    @Override
    public void execute(){

        Model model = graph.getModel();

        List<Cell> cells = model.getAllCells();

        Cell rootCell = model.getRoot();

        depthCounts = new int[cells.size()];

        relocate(rootCell, 0);
    }

    private void relocate(Cell c, int depth){
        double x = depthCounts[depth] * H_SPACING;
        double y = depth * V_SPACING;
        c.relocate(x,y);

        depthCounts[depth] += 1;

        for(Cell child : c.getCellChildren()){
            relocate(child, depth + 1);
        }
    }
}
