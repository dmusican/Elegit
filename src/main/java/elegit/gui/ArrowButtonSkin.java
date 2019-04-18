package elegit.gui;

import elegit.Main;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Skin;
import javafx.scene.input.MouseButton;
import javafx.scene.paint.Paint;
import javafx.scene.shape.*;

/**
 * Creates an arrow shape for a button based on the button's parameters.
 * Uses a label and a path with a fill to emulate the button's layout
 * and coloring. Supports arrows pointing in 0 to all 4 directions
 */
public class ArrowButtonSkin extends Group implements Skin<Button>{

    // Constants for arrow shape/size
    private static final double ARROW_BASE_HEIGHT = 4;
    private static final double ARROW_TIP_HEIGHT = 10;
    private static final double ARROW_BASE_WIDTH_RATIO = 0.7;

    private enum ArrowDirection {UP, DOWN, LEFT, RIGHT}

    // The underlying button that's getting skinned
    private final ArrowButton button;

    /**
     * Constructs a skin for the given button. Pulls the necessary
     * properties from the button and constructs the Path and
     * Label that make up the new button skin
     * @param button the button being skinned
     */
    public ArrowButtonSkin(ArrowButton button){
        Main.assertFxThread();
        this.button = button;

        this.setOnMouseClicked(event -> {
            if(event.getButton() == MouseButton.PRIMARY){
                button.getOnAction().handle(new ActionEvent(button, event.getTarget()));
            }
        });

        this.button.textProperty().addListener((observable, oldValue, newValue) -> draw());
    }

    /**
     * Helper method called whenever the button is updated. Sets the text,
     * style class, and css id to match that of the button. Also constructs
     * the path that outlines the skin and fills it with the appropriate
     * color.
     */
    private void draw(){
        Main.assertFxThread();
        Label text = new Label(button.getText());
        text.getStyleClass().setAll(button.getStyleClass());
        text.setId(button.getId());

        Path path = new Path();
        Paint color = Paint.valueOf("#52B3D9");
        path.setFill(color);
        path.setStroke(color);

        button.getStyleClass().clear();
        getChildren().setAll(path, text);

        // This is here because getBoundsInLocal doesn't work until the window is laid out, which happens later.
        // https://stackoverflow.com/questions/26152642/get-the-height-of-a-node-in-javafx-generate-a-layout-pass
        // Perhaps this could be fixed with calls to applyCss() and layout(), but this works fine, and doesn't do
        // anything otherwise dangerous. The code starts on the FX thread, and posts more code on the Fx thread to
        // run later.
        Platform.runLater(() -> {
            double labelWidth = text.getBoundsInLocal().getWidth();
            double labelHeight = text.getBoundsInLocal().getHeight();
            text.setTranslateX(button.getInsets().getLeft());
            text.setTranslateY(button.getInsets().getTop());

            // Create arrow button line path elements
            MoveTo startPoint = new MoveTo();
            double width = labelWidth + button.getInsets().getLeft();
            double height = labelHeight + button.getInsets().getBottom() + button.getInsets().getTop();
            startPoint.setX(0);
            startPoint.setY(0);
            path.getElements().add(startPoint);

            if(button.getArrowUp()){
                PathElement[] curves = getArrowSide(width, height, ArrowDirection.UP);
                path.getElements().add(curves[0]);
                path.getElements().add(curves[1]);
            }else{
                HLineTo topLine = new HLineTo(width);
                path.getElements().add(topLine);
            }

            if(button.getArrowRight()){
                PathElement[] curves = getArrowSide(width, height, ArrowDirection.RIGHT);
                path.getElements().add(curves[0]);
                path.getElements().add(curves[1]);
            }else{
                VLineTo rightLine = new VLineTo(height);
                path.getElements().add(rightLine);
            }

            if(button.getArrowDown()){
                PathElement[] curves = getArrowSide(width, height, ArrowDirection.DOWN);
                path.getElements().add(curves[0]);
                path.getElements().add(curves[1]);
            }else{
                HLineTo bottomLine = new HLineTo(0);
                path.getElements().add(bottomLine);
            }

            if(button.getArrowLeft()){
                PathElement[] curves = getArrowSide(width, height, ArrowDirection.LEFT);
                path.getElements().add(curves[0]);
                path.getElements().add(curves[1]);
            }else{
                VLineTo leftLine = new VLineTo(0);
                path.getElements().add(leftLine);
            }
        });
    }


    /**
     * Helper method that returns the two Path elements that make up an arrow pointing in the given
     * direction. Constructs straight lines.
     * @param width width of the button
     * @param height height of the button
     * @param direction direction to point the arrow
     * @return two path elements, one for each side of the arrow (clockwise order)
     */
    private PathElement[] getArrowSide(double width, double height, ArrowDirection direction){
        LineTo[] lines = new LineTo[2];
        lines[0] = new LineTo();
        lines[1] = new LineTo();
        switch(direction){
            case LEFT:
                lines[0].setX(-ARROW_TIP_HEIGHT);
                lines[0].setY(height / 2.);
                lines[1].setX(0);
                lines[1].setY(0);
                break;
            case RIGHT:
                lines[0].setX(width + ARROW_TIP_HEIGHT);
                lines[0].setY(height / 2.);
                lines[1].setX(width);
                lines[1].setY(height);
                break;
            case UP:
                lines[0].setX(width / 2.);
                lines[0].setY(-ARROW_TIP_HEIGHT);
                lines[1].setX(width);
                lines[1].setY(0);
                break;
            case DOWN:
                lines[0].setX(width / 2.);
                lines[0].setY(height + ARROW_TIP_HEIGHT);
                lines[1].setX(0);
                lines[1].setY(height);
                break;
        }
        return lines;
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
