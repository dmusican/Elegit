package edugit;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Skin;
import javafx.scene.paint.Paint;
import javafx.scene.shape.*;

/**
 * Created by makik on 6/25/15.
 */
public class ArrowButtonSkin extends Group implements Skin<Button>{

    static final double ARROW_TIP_WIDTH = 4;
    static final double ARROW_TIP_HEIGHT = 10;
    static final double ARROW_OFFSET = 0.3;

    private static final int UP = 0;
    private static final int DOWN = 1;
    private static final int LEFT = 2;
    private static final int RIGHT = 3;

    Button button;
    boolean up,down,left,right;

    public ArrowButtonSkin(Button button){
        this.button = button;

        this.setOnMouseClicked(event -> button.getOnAction().handle(new ActionEvent(button, event.getTarget())));

        this.button.textProperty().addListener((observable, oldValue, newValue) -> draw());
    }

    private void draw(){
        Label text = new Label(button.getText());
        text.getStyleClass().setAll(button.getStyleClass());
        text.setId(button.getId());

        Path path = new Path();
        text.backgroundProperty().addListener((observable, oldValue, newValue) -> {
            Paint color = newValue.getFills().get(0).getFill();
            path.setFill(color);
            path.setStroke(color);
        });

        button.getStyleClass().clear();
        getChildren().setAll(path, text);

        Platform.runLater(() -> {
            double labelWidth = text.getBoundsInLocal().getWidth();
            double labelHeight = text.getBoundsInLocal().getHeight();
            text.setTranslateX(button.getInsets().getLeft());
            text.setTranslateY(button.getInsets().getTop());

            // Create arrow button line path elements
            MoveTo startPoint = new MoveTo();
            double width = labelWidth+button.getInsets().getLeft();
            double height = labelHeight+button.getInsets().getBottom()+button.getInsets().getTop();
            startPoint.setX(0);
            startPoint.setY(0);
            path.getElements().add(startPoint);

            if(up){
                QuadCurveTo[] curves = getArrowSide(width, height, UP);
                path.getElements().add(curves[0]);
                path.getElements().add(curves[1]);
            }else{
                HLineTo topLine = new HLineTo(width);
                path.getElements().add(topLine);
            }

            if(right){
                QuadCurveTo[] curves = getArrowSide(width, height, RIGHT);
                path.getElements().add(curves[0]);
                path.getElements().add(curves[1]);
            }else{
                VLineTo rightLine = new VLineTo(height);
                path.getElements().add(rightLine);
            }

            if(down){
                QuadCurveTo[] curves = getArrowSide(width, height, DOWN);
                path.getElements().add(curves[0]);
                path.getElements().add(curves[1]);
            }else{
                HLineTo bottomLine = new HLineTo(0);
                path.getElements().add(bottomLine);
            }

            if(left){
                QuadCurveTo[] curves = getArrowSide(width, height, LEFT);
                path.getElements().add(curves[0]);
                path.getElements().add(curves[1]);
            }else{
                VLineTo leftLine = new VLineTo(0);
                path.getElements().add(leftLine);
            }
        });
    }

    private QuadCurveTo[] getArrowSide(double width, double height, int direction){
        QuadCurveTo[] curve = new QuadCurveTo[2];
        curve[0] = new QuadCurveTo();
        curve[1] = new QuadCurveTo();
        double controlX, controlY, x, y;

        switch(direction){
            case LEFT:
                // Bottom curve
                controlX = -ARROW_TIP_WIDTH;
                controlY = height;
                x = -ARROW_TIP_HEIGHT;
                y = height / 2;
                curve[0].setX(x);
                curve[0].setY(y);
                curve[0].setControlX(controlX);
                curve[0].setControlY(controlY);

                // Top curve
                controlX = -ARROW_TIP_WIDTH;
                controlY = 0;
                x = 0;
                y = 0;
                curve[1].setX(x);
                curve[1].setY(y);
                curve[1].setControlX(controlX);
                curve[1].setControlY(controlY);
                break;
            case RIGHT:
                // Top curve
                controlX = width + ARROW_TIP_WIDTH;
                controlY = 0;
                x = width + ARROW_TIP_HEIGHT;
                y = height / 2;
                curve[0].setX(x);
                curve[0].setY(y);
                curve[0].setControlX(controlX);
                curve[0].setControlY(controlY);

                // Bottom curve
                controlX = width + ARROW_TIP_WIDTH;
                controlY = height;
                x = width;
                y = height;
                curve[1].setX(x);
                curve[1].setY(y);
                curve[1].setControlX(controlX);
                curve[1].setControlY(controlY);
                break;
            case UP:
                // Left curve
                controlX = 0;
                controlY = -ARROW_TIP_WIDTH;
                x = width / 2;
                y = -ARROW_TIP_HEIGHT;
                curve[0].setX(x);
                curve[0].setY(y);
                curve[0].setControlX(controlX);
                curve[0].setControlY(controlY);

                // Right curve
                controlX = width;
                controlY = -ARROW_TIP_WIDTH;
                x = width;
                y = 0;
                curve[1].setX(x);
                curve[1].setY(y);
                curve[1].setControlX(controlX);
                curve[1].setControlY(controlY);
                break;
            case DOWN:
                // Right curve
                controlX = width;
                controlY = height + ARROW_TIP_WIDTH;
                x = width / 2;
                y = height + ARROW_TIP_HEIGHT;
                curve[0].setX(x);
                curve[0].setY(y);
                curve[0].setControlX(controlX);
                curve[0].setControlY(controlY);

                // Left curve
                controlX = 0;
                controlY = height + ARROW_TIP_WIDTH;
                x = 0;
                y = height;
                curve[1].setX(x);
                curve[1].setY(y);
                curve[1].setControlX(controlX);
                curve[1].setControlY(controlY);
                break;
        }
        return curve;
    }

    @Override
    public Button getSkinnable(){
        return button;
    }

    @Override
    public Node getNode(){
        return this;
    }

    @Override
    public void dispose(){
    }
}
