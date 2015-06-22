package edugit;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Group;
import javafx.scene.control.ScrollPane;

/**
 * Created by makik on 6/11/15.
 */
public class MatchedScrollPane extends ScrollPane{

    private static final DoubleProperty hPos = new SimpleDoubleProperty(0.0);
    private static final DoubleProperty vPos = new SimpleDoubleProperty(0.0);

    public MatchedScrollPane(Group g){
        super(g);
        this.hvalueProperty().bindBidirectional(hPos);
        this.vvalueProperty().bindBidirectional(vPos);
    }

    public void requestFocus(){}

    public static void scrollTo(double pos){
        hPos.set(pos);
    }
}
