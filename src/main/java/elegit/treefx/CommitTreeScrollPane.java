package elegit.treefx;

import elegit.Main;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import org.apache.http.annotation.ThreadSafe;

/**
 * The commit tree scroll pane
 */
@ThreadSafe
// but critically only because of all the asserts requiring this be done only in the FX thread. Without that, it
// isn't threadsafe, not least because of the bindings / listeners that are done.
public class CommitTreeScrollPane extends ScrollPane {

    private final static double DEFAULT_SCROLL_POS = 1.0;

    // A property used to update the number of items in the scroll pane
    final IntegerProperty NumItemsProperty = new SimpleIntegerProperty(1);

    // The number of horizontally arranged items present in the scroll panes
    private static int numItems = 1;

    CommitTreeScrollPane(Node node) {
        super(node);
        //super();
        Main.assertFxThread();
        setId("Elegit scroll pane");

        NumItemsProperty.addListener((observable, oldValue, newValue) -> numItems = newValue.intValue());
    }

    /**
     * Brings the item at the given position into focus of the scroll pane.
     * The passed in position should be smaller than numItems, which corresponds
     * to the number of items present in the scroll pane.
     *
     * In other words, scrolling to the 5th item in a MatchedScrollPane with
     * 10 items is accomplished by passing in 5 to this method
     * @param pos the horizontal position to scroll to when compared as a ratio
     *            to numItems
     */
    public static void scrollTo(double pos){
        Main.assertFxThread();

        // The below-code is necessary to get the scroll pane layout to kick in before scrolling it to the right
        // position. The reason is explained here, after a fashion, where it talks about the need for a Group:
        // https://docs.oracle.com/javase/8/javafx/api/javafx/scene/control/ScrollPane.html
        // Wrapping the cell pane in a group, like this:
        // scrollPane = new CommitTreeScrollPane(new Group(cellLayer));
        // ... in TreeGraph.java works great and avoids the hack below, but kills performance dramatically.
        CommitTreeModel.getCommitTreeModel().getTreeGraph().getScrollPane().applyCss();
        CommitTreeModel.getCommitTreeModel().getTreeGraph().getScrollPane().layout();

        ScrollPane scrollPane = CommitTreeModel.getCommitTreeModel().getTreeGraph().getScrollPane();
        double pixelPosition = pos* TreeLayout.V_SPACING;
        double paneSize =  scrollPane.getContent().getBoundsInParent().getHeight();
        double value = pixelPosition / (paneSize-TreeLayout.V_PAD-TreeLayout.V_SPACING);

        CommitTreeModel.getCommitTreeModel().getTreeGraph().getScrollPane().setVvalue(value);
    }
}
