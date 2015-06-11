package edugit;

import edugit.treefx.Graph;
import edugit.treefx.Layout;
import edugit.treefx.Model;
import edugit.treefx.RandomLayout;
import javafx.scene.control.ScrollPane;

/**
 * Created by makik on 6/10/15.
 */
public class LocalPanelView extends TreePanelView{

    public LocalPanelView(){
        super();
        Graph graph = new Graph();

        ScrollPane sp = graph.getScrollPane();
        sp.setPrefSize(200,600);
        sp.setPannable(true);
        this.getChildren().add(sp);

        Model model = graph.getModel();

        graph.beginUpdate();

        model.addCell("Cell A");
        model.addCell("Cell B");
        model.addCell("Cell C");
        model.addCell("Cell D");
        model.addCell("Cell E");
        model.addCell("Cell F");
        model.addCell("Cell G");

        model.addEdge("Cell A", "Cell B");
        model.addEdge("Cell A", "Cell C");
        model.addEdge("Cell B", "Cell C");
        model.addEdge("Cell C", "Cell D");
        model.addEdge("Cell B", "Cell E");
        model.addEdge("Cell D", "Cell F");
        model.addEdge("Cell D", "Cell G");

        graph.endUpdate();

        Layout layout = new RandomLayout(graph);
        layout.execute();
    }
}
