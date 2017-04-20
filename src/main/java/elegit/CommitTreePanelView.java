package elegit;

import elegit.treefx.TreeGraph;
import elegit.treefx.TreeLayout;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Region;

/**
 * Class for the local and remote panel views that handles the drawing of a tree structure
 * from a given treeGraph.
 *
 */
public class CommitTreePanelView extends Region{

    // Thread information
    public boolean isLayoutThreadRunning = false;
    private Task task;
    private Thread treeLayoutThread;
    private String name;

    /**
     * Constructs a new view for the commit tree
     */
    public CommitTreePanelView(){
        super();
        this.setMinHeight(0);

        // Various other portions of the project test to see if treeLayoutThread is alive. If it is alive, that
        // means that a layout is being worked on, and other portions of the code decide to hold off and not do
        // things. By initializing treeLayoutThread to a thread which does nothing, and running it right away,
        // it advances quickly to the terminated / dead state, which will free up other things to run.
        treeLayoutThread = new Thread();
        treeLayoutThread.setDaemon(true);
        treeLayoutThread.start();
    }

    /**
     * Helper method to initialize the commit tree scroll panes
     * @param treeGraph TreeGraph
     */
    private void initCommitTreeScrollPanes(TreeGraph treeGraph) {
        ScrollPane sp = treeGraph.getScrollPane();
        sp.setOnMouseClicked(event -> CommitTreeController.handleMouseClicked());
        getChildren().clear();
        getChildren().add(anchorScrollPane(sp));
        isLayoutThreadRunning = false;
    }

    /**
     * Handles the layout and display of the treeGraph. Creates a thread
     * in which to execute the TreeLayoutTask, and a thread that waits
     * for the layout to finish and then updates the view
     * @param treeGraph the graph to be displayed
     */
    public synchronized void displayTreeGraph(TreeGraph treeGraph, CommitHelper commitToFocusOnLoad){
        initCommitTreeScrollPanes(treeGraph);

        // Note to me.
        // task.cancel doesn't work because the task doesn't check to see if it's cancelled.
        // (see documentation, it's a collaborative effort).
        // I just swapped treeLayoutThread to isAlive, but because we spin off a zillion threads to do the layout,
        // we need to check if it's the last thread that's done.
        // This is bad and needs to be redesigned.
        if(treeLayoutThread.isAlive()){
            task.cancel();
            try{
                treeLayoutThread.join();
            }catch(InterruptedException e){
                e.printStackTrace();
            }
        }

        task = TreeLayout.getTreeLayoutTask(treeGraph);

        treeLayoutThread = new Thread(task);
        treeLayoutThread.setName("Tree Layout: "+this.name);
        treeLayoutThread.setDaemon(true);
        treeLayoutThread.start();
        isLayoutThreadRunning = true;

        Task<Void> endTask = new Task<Void>(){
            @Override
            protected Void call(){
                try {
                    treeLayoutThread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    isLayoutThreadRunning = false;
                }
//                Platform.runLater(() -> {
//                    CommitTreeController.focusCommitInGraph(commitToFocusOnLoad);
//                });
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
        Platform.runLater(() -> {
            ScrollPane sp = new ScrollPane();
            this.getChildren().clear();
            this.getChildren().add(anchorScrollPane(sp));
        });
    }

    /**
     * Anchors the width and height of the scroll pane to the width and height of
     * the view to ensure the scroll pane expands appropriately on resize
     * @param sp the scrollpane to anchor
     * @return the passed in scrollpane after being anchored
     */
    private ScrollPane anchorScrollPane(ScrollPane sp){
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

    public String getName() {
        return this.name;
    }
}
