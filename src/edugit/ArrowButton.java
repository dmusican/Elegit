package edugit;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.Button;

/**
 * Created by makik on 6/25/15.
 */
public class ArrowButton extends Button{

    private BooleanProperty arrowUp, arrowRight, arrowDown, arrowLeft;

    public ArrowButton(){
        super();
        this.setSkin(new ArrowButtonSkin(this));

        arrowUp = new SimpleBooleanProperty(false);
        arrowRight = new SimpleBooleanProperty(false);
        arrowDown = new SimpleBooleanProperty(false);
        arrowLeft = new SimpleBooleanProperty(false);

        arrowUp.addListener((observable, oldValue, newValue) -> ((ArrowButtonSkin) this.getSkin()).up = newValue);
        arrowRight.addListener((observable, oldValue, newValue) -> ((ArrowButtonSkin) this.getSkin()).right = newValue);
        arrowDown.addListener((observable, oldValue, newValue) -> ((ArrowButtonSkin) this.getSkin()).down = newValue);
        arrowLeft.addListener((observable, oldValue, newValue) -> ((ArrowButtonSkin) this.getSkin()).left = newValue);
    }

    public boolean getArrowUp(){
        return arrowUp.get();
    }

    public BooleanProperty arrowUpProperty(){
        return arrowUp;
    }

    public void setArrowUp(boolean arrowUp){
        this.arrowUp.set(arrowUp);
    }

    public boolean getArrowRight(){
        return arrowRight.get();
    }

    public BooleanProperty arrowRightProperty(){
        return arrowRight;
    }

    public void setArrowRight(boolean arrowRight){
        this.arrowRight.set(arrowRight);
    }

    public boolean getArrowDown(){
        return arrowDown.get();
    }

    public BooleanProperty arrowDownProperty(){
        return arrowDown;
    }

    public void setArrowDown(boolean arrowDown){
        this.arrowDown.set(arrowDown);
    }

    public boolean getArrowLeft(){
        return arrowLeft.get();
    }

    public BooleanProperty arrowLeftProperty(){
        return arrowLeft;
    }

    public void setArrowLeft(boolean arrowLeft){
        this.arrowLeft.set(arrowLeft);
    }
}
