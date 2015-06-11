package edugit;

import edugit.treefx.Graph;
import javafx.scene.Group;

/**
 * Created by makik on 6/10/15.
 *
 * Super class for the local and remote panel views that handles the common functionality,
 * namely the drawing of a tree structure
 */
public class TreePanelView extends Group{

    Graph graph;

    public TreePanelView(){
//        Canvas c = new Canvas(50,300);
//        GraphicsContext gc = c.getGraphicsContext2D();
//        gc.setFill(Color.BLUE);
//        gc.fillOval(25,25,10,10);

//        for(int i=0; i<Math.random()*10; i++){
//            Circle c = new Circle(Math.random()*50+i*5,Math.random()*300+i*40,Math.random()*40+10, Color.BLUE);
//            this.getChildren().add(c);
//        }
    }
}
