package edugit;

import edugit.treefx.Cell;
import edugit.treefx.TreeLayout;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;

/**
 * Created by makik on 6/11/15.
 */
public class MatchedScrollPane extends ScrollPane{

    private static final DoubleProperty hPos = new SimpleDoubleProperty(0.0);
    private static final DoubleProperty vPos = new SimpleDoubleProperty(0.0);

    private static double w = CommitTreePanelView.TREE_PANEL_WIDTH;
    private static double hMax = 1.0;

    public MatchedScrollPane(Node node){
        super(node);
        this.hvalueProperty().bindBidirectional(hPos);
        this.vvalueProperty().bindBidirectional(vPos);

        this.widthProperty().addListener((observable, oldValue, newValue) -> w = newValue.doubleValue());
        this.hmaxProperty().addListener((observable, oldValue, newValue) -> hMax = newValue.doubleValue());
    }

    public void requestFocus(){}

    public static void scrollTo(double pos){
        if(pos < 0){
            hPos.set(hMax);
        }else{
            hPos.set(pos + (w / 2.0) / (Cell.BOX_SIZE + TreeLayout.H_SPACING));
        }
    }
}
