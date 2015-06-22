package edugit;

import edugit.treefx.TreeGraph;
import edugit.treefx.TreeLayout;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.Group;
import javafx.scene.control.ScrollPane;

/**
 * Created by makik on 6/10/15.
 *
 * Class for the local and remote panel views that handles the drawing of a tree structure
 * from a given treeGraph.
 *
 */
public class CommitTreePanelView extends Group{

    public static int TREE_PANEL_WIDTH = 200;
    public static int TREE_PANEL_HEIGHT = 500;

    private boolean isRunning = false;
    private Task task;
    private Thread th;

    /**
     * Handles the layout and display of the treeGraph
     * @param treeGraph the graph to be displayed
     */
    public void displayTreeGraph(TreeGraph treeGraph){

        if(isRunning){
            task.cancel();
            try{
                th.join();
            }catch(InterruptedException e){
                e.printStackTrace();
            }
        }

        task = TreeLayout.getTreeLayoutTask(treeGraph);

        th = new Thread(task);
        th.setName("Graph Layout (x = "+this.getLayoutX()+")");
        th.setDaemon(true);
        th.start();
        isRunning = true;

        Platform.runLater(new Task<Void>(){
            @Override
            protected Void call(){
                try{
                    th.join();
                }catch(InterruptedException e){
                    e.printStackTrace();
                }

                TreeLayout.moveCells(treeGraph);

                ScrollPane sp = treeGraph.getScrollPane();
                sp.setPannable(true);
                sp.setPrefSize(TREE_PANEL_WIDTH, TREE_PANEL_HEIGHT);
                getChildren().clear();
                getChildren().add(sp);
                isRunning = false;
                return null;
            }
        });
    }

    public void displayEmptyView(){
        ScrollPane sp = new ScrollPane();
        sp.setPrefSize(TREE_PANEL_WIDTH, TREE_PANEL_HEIGHT);
        this.getChildren().clear();
        this.getChildren().add(sp);
    }
}
