package elegit.treefx;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;

/**
 * The commit tree scroll pane
 */
public class CommitTreeScrollPane extends ScrollPane{
    private final static double DEFAULT_SCROLL_POS = 1.0;

    // A property used to update the number of items in the scroll pane
    public final IntegerProperty NumItemsProperty = new SimpleIntegerProperty(1);

    // The number of horizontally arranged items present in the scroll panes
    private static int numItems = 1;

    private static final DoubleProperty vPos = new SimpleDoubleProperty(-1.0);

    public CommitTreeScrollPane(Node node) {
        super(node);

        vPos.addListener((observable, oldValue, newValue) -> {
            if (newValue.doubleValue() != -1) {
                // For some reason setVvalue doesn't take hold unless you
                // bash it with repetition. However, more checks are needed
                // to avoid an infinite loop.
                double value = newValue.doubleValue()>1 ? 1 : newValue.doubleValue();
                for (int i=0; i<3; i++)
                    this.vvalueProperty().setValue(value);
            }
        });

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
            vPos.setValue(DEFAULT_SCROLL_POS);
        }else{
            double ratio = pos/numItems;
            double offset = ratio >= 0.5 ? 1.0/numItems : -1.0/numItems;
            vPos.set(1-(ratio+offset));
        }
        vPos.setValue(-1.0);
    }
}
