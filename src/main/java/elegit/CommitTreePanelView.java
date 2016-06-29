package elegit;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Region;
import elegit.treefx.Cell;
import elegit.treefx.TreeGraph;
import elegit.treefx.TreeLayout;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Class for the local and remote panel views that handles the drawing of a tree structure
 * from a given treeGraph.
 *
 */
public class CommitTreePanelView extends Region{

    // Constants for panel size
    public static int TREE_PANEL_WIDTH = 500;
    public static int TREE_PANEL_HEIGHT = (Cell.BOX_SIZE + TreeLayout.H_SPACING) * 5;

    // Thread information
    private boolean isLayoutThreadRunning = false;
    private Task task;
    private Thread th;
    private String name;

    private StackPane computingCommitTree;
    private Text loading;

    /**
     * Constructs a new view for the commit tree
     */
    public CommitTreePanelView(){
        super();
        this.setPrefHeight(TREE_PANEL_HEIGHT);

        initLoadingText();
    }

    /**
     * Helper method to initialize loading text
     */
    private void initLoadingText() {
        loading = new Text("Computing commit tree graph...");
        loading.setFont(new Font(15));
        loading.setFill(Color.DODGERBLUE);
        VBox vBox = new VBox(loading);
        computingCommitTree = new StackPane(vBox);
        computingCommitTree.setLayoutX(170);
        computingCommitTree.setLayoutY(120);
    }

    /**
     * Helper method to initialize the commit tree scroll panes
     * @param treeGraph TreeGraph
     */
    private void initCommitTreeScrollPanes(TreeGraph treeGraph) {
        MatchedScrollPane.ignoreScrolling(true);
        ScrollPane sp = treeGraph.getScrollPane();

        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("SS");
        System.out.println(sdf.format(cal.getTime()));

        sp.setId(sdf.format(cal.getTime()));

        sp.setOnMouseClicked(event -> CommitTreeController.handleMouseClicked());
        getChildren().clear();
        getChildren().add(anchorScrollPane(sp));
        getChildren().add(computingCommitTree);
        MatchedScrollPane.ignoreScrolling(false);
    }

    /**
     * Handles the layout and display of the treeGraph. Creates a thread
     * in which to execute the TreeLayoutTask, and a thread that waits
     * for the layout to finish and then updates the view
     * @param treeGraph the graph to be displayed
     */
    public synchronized void displayTreeGraph(TreeGraph treeGraph, CommitHelper commitToFocusOnLoad){
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("SS");
        System.out.println("displayTreeGraph(): " + sdf.format(cal.getTime()));

        if (Platform.isFxApplicationThread()) {
            initCommitTreeScrollPanes(treeGraph);
        }else {
            Platform.runLater(() -> initCommitTreeScrollPanes(treeGraph));
        }

        if(isLayoutThreadRunning){
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
        isLayoutThreadRunning = true;

        Task<Void> endTask = new Task<Void>(){
            @Override
            protected Void call(){
                try {
                    th.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Platform.runLater(() -> {
                    isLayoutThreadRunning = false;
                    loading.setVisible(false);
                    CommitTreeController.focusCommitInGraph(commitToFocusOnLoad);
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
