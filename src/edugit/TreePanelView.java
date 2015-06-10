package edugit;

import edugit.treefx.Graph;
import edugit.treefx.Layout;
import edugit.treefx.Model;
import edugit.treefx.RandomLayout;
import javafx.scene.Group;

/**
 * Created by makik on 6/10/15.
 *
 * Super class for the local and remote panel views that handles the common functionality,
 * namely the drawing of a tree structure
 */
public class TreePanelView extends Group{

    public TreePanelView(){
//        Canvas c = new Canvas(50,300);
//        GraphicsContext gc = c.getGraphicsContext2D();
//        gc.setFill(Color.BLUE);
//        gc.fillOval(25,25,10,10);

//        for(int i=0; i<Math.random()*10; i++){
//            Circle c = new Circle(Math.random()*50+i*5,Math.random()*300+i*40,Math.random()*40+10, Color.BLUE);
//            this.getChildren().add(c);
//        }

        Graph graph = new Graph();

        this.getChildren().add(graph.getScrollPane());

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
