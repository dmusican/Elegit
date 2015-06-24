package edugit;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;

/**
 * MatchedScrollPane instances will all share the same horizontal and vertical
 * positioning of their scroll bars. In this way, scrolling in one is equivalent
 * to scrolling in all of them.
 */
public class MatchedScrollPane extends ScrollPane{

    // All MatchedScrollPanes share a horizontal and vertical positioning
    private static final DoubleProperty hPos = new SimpleDoubleProperty(1.0);
    private static final DoubleProperty vPos = new SimpleDoubleProperty(1.0);

    private static boolean isScrollLocked = false;

    // A property used to update the number of items in the scroll panes
    public final IntegerProperty NumItemsProperty = new SimpleIntegerProperty(1);

    // The number of horizontally arranged items present in the scroll panes
    private static int numItems = 1;

    public MatchedScrollPane(Node node){
        super(node);

        this.hvalueProperty().addListener((observable, oldValue, newValue) -> {
            if(!isScrollLocked){
                hPos.setValue(newValue);
            }
        });
        hPos.addListener((observable, oldValue, newValue) -> {
            this.hvalueProperty().setValue(newValue);
        });

        this.vvalueProperty().addListener((observable, oldValue, newValue) -> {
            if(!isScrollLocked){
                vPos.setValue(newValue);
            }
        });
        vPos.addListener((observable, oldValue, newValue) -> {
            this.vvalueProperty().setValue(newValue);
        });

        NumItemsProperty.addListener((observable, oldValue, newValue) -> numItems = newValue.intValue());
    }

    /**
     * Scroll panes of this type do not accept focus
     */
    public void requestFocus(){}

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
            hPos.set(1.0);
        }else{
            double ratio = pos/numItems;
            double offset = ratio >= 0.5 ? 1.0/numItems : -1.0/numItems;
            hPos.set(ratio+offset);
        }
    }

    public static void ignoreScrolling(boolean ignore){
        isScrollLocked = ignore;
    }
}
