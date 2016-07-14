package elegit;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import elegit.treefx.Cell;
import elegit.treefx.TreeGraph;
import elegit.treefx.TreeLayout;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

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
    private Background bg;

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
        Text loading = new Text("Computing commit tree graph...");
        loading.setFont(new Font(17));
        loading.setFill(Color.DODGERBLUE);
        computingCommitTree = new StackPane(loading);
        computingCommitTree.setLayoutX(10);
        computingCommitTree.setLayoutY(10);
        bg = new Background(new BackgroundFill(Color.WHITESMOKE, CornerRadii.EMPTY, Insets.EMPTY));
        computingCommitTree.setBackground(bg);
    }

    /**
     * Helper method to initialize the commit tree scroll panes
     * @param treeGraph TreeGraph
     */
    private void initCommitTreeScrollPanes(TreeGraph treeGraph, boolean showLoadingText) {
        ScrollPane sp = treeGraph.getScrollPane();
        sp.setOnMouseClicked(event -> CommitTreeController.handleMouseClicked());
        getChildren().clear();
        getChildren().add(anchorScrollPane(sp));
        getChildren().add(computingCommitTree);
        if(showLoadingText) {
            computingCommitTree.setVisible(true);
            computingCommitTree.setBackground(bg);
        }
        isLayoutThreadRunning = false;
    }

    /**
     * Handles the layout and display of the treeGraph. Creates a thread
     * in which to execute the TreeLayoutTask, and a thread that waits
     * for the layout to finish and then updates the view
     * @param treeGraph the graph to be displayed
     */
    public synchronized void displayTreeGraph(TreeGraph treeGraph, CommitHelper commitToFocusOnLoad, boolean showLoadingText){
        if (Platform.isFxApplicationThread()) {
            initCommitTreeScrollPanes(treeGraph, showLoadingText);
        }else {
            Platform.runLater(() -> initCommitTreeScrollPanes(treeGraph, showLoadingText));
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
                    computingCommitTree.setVisible(false);
                    computingCommitTree.setBackground(null);
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
