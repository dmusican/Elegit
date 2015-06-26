package main.java.edugit;

import main.java.edugit.treefx.Cell;
import main.java.edugit.treefx.TreeGraph;
import main.java.edugit.treefx.TreeLayout;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Region;

/**
 * Class for the local and remote panel views that handles the drawing of a tree structure
 * from a given treeGraph.
 *
 */
public class CommitTreePanelView extends Region{

    // Constants for size
    public static int TREE_PANEL_WIDTH = 500;
    public static int TREE_PANEL_HEIGHT = (Cell.BOX_SIZE + TreeLayout.H_SPACING) * 5;

    // Thread information
    private boolean isRunning = false;
    private Task task;
    private Thread th;
    private String name;

    public CommitTreePanelView(){
        super();
        this.setPrefHeight(TREE_PANEL_HEIGHT);
    }

    /**
     * Handles the layout and display of the treeGraph. Creates a thread
     * in which to execute the TreeLayoutTask, and a thread that waits
     * for the layout to finish and then updates the view
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
        th.setName("Graph Layout: "+this.name);
        th.setDaemon(true);
        th.start();
        isRunning = true;

        Task<Void> endTask = new Task<Void>(){
            @Override
            protected Void call(){
                MatchedScrollPane.ignoreScrolling(true);
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
                        getChildren().clear();
                        getChildren().add(anchorScrollPane(sp));
                        isRunning = false;

                        MatchedScrollPane.ignoreScrolling(false);
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

    /**
     * Displays an empty scroll pane
     */
    public void displayEmptyView(){
        ScrollPane sp = new ScrollPane();
        this.getChildren().clear();
        this.getChildren().add(anchorScrollPane(sp));
    }

    /**
     * Anchors the width and height of the scroll pane to the width and height of
     * the view to ensure the scroll pane expands appropriately on resize
     * @param sp the scrollpane to anchor
     * @return the passed in scrollpane after being anchored
     */
    private Node anchorScrollPane(ScrollPane sp){
        sp.prefWidthProperty().bind(this.widthProperty());
        sp.prefHeightProperty().bind(this.heightProperty());
        return sp;
    }

    /**
     * Sets the name of this view, which appears in the Threads spawned by it
     * @param name the name
     */
    public void setName(String name){
        this.name = name;
    }
}
