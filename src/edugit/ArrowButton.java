package edugit;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.Button;

/**
 * Created by makik on 6/25/15.
 */
public class ArrowButton extends Button{

    public enum ArrowDirection{
        LEFT, RIGHT, UP, DOWN
    }

    public ObjectProperty<ArrowDirection> arrowDirection;

    public ArrowButton(){
        super();
        this.setSkin(new ArrowButtonSkin(this));
        arrowDirection = new SimpleObjectProperty<>();
        arrowDirection.addListener((observable, oldValue, newValue) -> ((ArrowButtonSkin)this.getSkin()).direction = newValue);
    }

    public ArrowDirection getArrowDirection(){
        return arrowDirection.get();
    }

    public ObjectProperty<ArrowDirection> arrowDirectionProperty(){
        return arrowDirection;
    }

    public void setArrowDirection(ArrowDirection arrowDir){
        this.arrowDirection.set(arrowDir);
    }
}
