package edugit.treefx;

import java.util.List;
import java.util.Random;

/**
 * Created by makik on 6/10/15.
 */
public class RandomLayout extends Layout {

    TreeGraph treeGraph;

    Random rnd = new Random();

    public RandomLayout(TreeGraph treeGraph) {

        this.treeGraph = treeGraph;

    }

    public void execute() {

        List<Cell> cells = treeGraph.getTreeGraphModel().getAllCells();

        for (Cell cell : cells) {

            double x = rnd.nextDouble() * 200;
            double y = rnd.nextDouble() * 2000;

            cell.relocate(x, y);

        }

    }

}
