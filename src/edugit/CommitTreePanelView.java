package edugit;

import edugit.treefx.Cell;
import edugit.treefx.TreeGraph;
import edugit.treefx.TreeLayout;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Region;

/**
 * Created by makik on 6/10/15.
 *
 * Class for the local and remote panel views that handles the drawing of a tree structure
 * from a given treeGraph.
 *
 */
public class CommitTreePanelView extends Region{

    public static int TREE_PANEL_WIDTH = 500;
    public static int TREE_PANEL_HEIGHT = (Cell.BOX_SIZE + TreeLayout.H_SPACING) * 5;

    private boolean isRunning = false;
    private Task task;
    private Thread th;

    public CommitTreePanelView(){
        super();
        this.setPrefHeight(TREE_PANEL_HEIGHT);
    }

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
        th.setName("Graph Layout (x = " + this.getLayoutX() + ")");
        th.setDaemon(true);
        th.start();
        isRunning = true;

        Task<Void> endTask = new Task<Void>(){
            @Override
            protected Void call(){
                try{
                    th.join();
                }catch(InterruptedException e){
                    e.printStackTrace();
                }
                Platform.runLater(new Task<Void>(){
                    @Override
                    protected Void call(){
                        ScrollPane sp = treeGraph.getScrollPane();
                        sp.setPannable(true);
                        sp.setHvalue(sp.getHmax());
                        getChildren().clear();
                        getChildren().add(anchorScrollPane(sp));
                        isRunning = false;
                        return null;
                    }
                });
                return null;
            }
        };
        Thread endThread = new Thread(endTask);
        endThread.setName("Layout finalization");
        endThread.setDaemon(true);
        endThread.start();
    }

    public void displayEmptyView(){
        ScrollPane sp = new ScrollPane();
        this.getChildren().clear();
        this.getChildren().add(anchorScrollPane(sp));
    }

    private Node anchorScrollPane(ScrollPane sp){
        sp.prefWidthProperty().bind(this.widthProperty());
        sp.prefHeightProperty().bind(this.heightProperty());
        return sp;
    }
}
