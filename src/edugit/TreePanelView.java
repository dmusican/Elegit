package edugit;

import edugit.treefx.*;
import javafx.scene.Group;
import javafx.scene.control.ScrollPane;

/**
 * Created by makik on 6/10/15.
 *
 * Super class for the local and remote panel views that handles the common functionality,
 * namely the drawing of a tree structure
 */
public class TreePanelView extends Group{

    Graph graph;

    public TreePanelView(){

        Model model = new Model("root");

        graph = new Graph(model);

        ScrollPane sp = graph.getScrollPane();
        sp.setPannable(true);
        sp.setPrefSize(200, 600);
        this.getChildren().add(sp);

        graph.beginUpdate();

        model.addCell("A");
        model.addCell("B", "root");
        model.addCell("C");
        model.addCell("D", "A");
        model.addCell("E");
        model.addCell("F","D");

        model.addCell("G");
        model.addCell("H");

        model.addCell("I","G");
        model.addCell("J");
        model.addCell("K","I");

        model.addCell("L","I");
        model.addCell("M");
        model.addCell("N","M","K");
        model.addCell("O");

        model.addCell("P","root");
        model.addCell("Q");

        model.addCell("R","J");
        model.addCell("S");
        model.addCell("T");

        model.addCell("U","O","T");
        model.addCell("V");

        graph.endUpdate();

        Layout layout = new TreeLayout(graph);
        layout.execute();
    }
}
