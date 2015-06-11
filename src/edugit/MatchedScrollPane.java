package edugit;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Group;
import javafx.scene.control.ScrollPane;

import java.util.ArrayList;

/**
 * Created by makik on 6/11/15.
 */
public class MatchedScrollPane extends ScrollPane{

    private static double vPos;
    private static ArrayList<MatchedScrollPane> instances = new ArrayList<>(2);

    public MatchedScrollPane(Group g){
        super(g);
        this.setVvalue(vPos);

        instances.add(this);

        vvalueProperty().addListener(new ChangeListener<Number>(){
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue){
                updateVerticalPosition(newValue.doubleValue());
            }
        });
    }

    public static void updateVerticalPosition(double newPos){
        vPos = newPos;
        for(MatchedScrollPane sp : instances){
            sp.setVvalue(vPos);
        }
    }
}
