package edugit;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;

/**
 * Created by makik on 6/11/15.
 */
public class MatchedScrollPane extends ScrollPane{

    private static final DoubleProperty hPos = new SimpleDoubleProperty(0.0);
    private static final DoubleProperty vPos = new SimpleDoubleProperty(0.0);

    private static int hMax = 1;

    public static final IntegerProperty NumItemsProperty = new SimpleIntegerProperty(1);

    public MatchedScrollPane(Node node){
        super(node);
        this.hvalueProperty().bindBidirectional(hPos);
        this.vvalueProperty().bindBidirectional(vPos);

        NumItemsProperty.addListener((observable, oldValue, newValue) -> hMax = Math.max(hMax,newValue.intValue()));
    }

    public void requestFocus(){}

    public static void scrollTo(double pos){
        if(pos < 0 || pos > 1){
            hPos.set(1.0);
        }else{
            double ratio = pos/hMax;
            double offset = ratio >= 0.5 ? 1.0/hMax : -1.0/hMax;
            hPos.set(ratio+offset);
        }
    }
}
