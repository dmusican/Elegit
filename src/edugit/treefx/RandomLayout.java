package edugit.treefx;

import java.util.List;
import java.util.Random;

/**
 * Created by makik on 6/10/15.
 */
public class RandomLayout extends Layout {

    Graph graph;

    Random rnd = new Random();

    public RandomLayout(Graph graph) {

        this.graph = graph;

    }

    public void execute() {

        List<Cell> cells = graph.getModel().getAllCells();

        for (Cell cell : cells) {

            double x = rnd.nextDouble() * 200;
            double y = rnd.nextDouble() * 700;

            cell.relocate(x, y);

        }

    }

}
