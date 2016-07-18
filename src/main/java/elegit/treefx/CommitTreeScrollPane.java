package elegit.treefx;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;

/**
 * Created by connellyj on 7/14/16.
 *
 * The commit tree scroll pane
 */
public class CommitTreeScrollPane extends ScrollPane{

    // A property used to update the number of items in the scroll pane
    public final IntegerProperty NumItemsProperty = new SimpleIntegerProperty(1);

    // The number of horizontally arranged items present in the scroll panes
    private static int numItems = 1;

    private static final DoubleProperty vPos = new SimpleDoubleProperty(0.0);

    public CommitTreeScrollPane(Node node) {
        super(node);

        vPos.addListener(((observable, oldValue, newValue) -> this.vvalueProperty().setValue(newValue)));

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
        if(pos < 0 || pos > numItems){
            vPos.set(1.0);
        }else{
            double ratio = 1-pos/numItems;
            double offset = ratio >= 0.5 ? 1.0/numItems : -1.0/numItems;
            vPos.set(ratio+offset);
        }
    }
}
