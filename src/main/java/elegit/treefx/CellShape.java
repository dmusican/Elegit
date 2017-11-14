package elegit.treefx;

import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import org.apache.http.annotation.ThreadSafe;

/**
 * Enum for the different shapes a cell can take on
 */
@ThreadSafe
public enum CellShape{
    SQUARE,
    CIRCLE,
    TRIANGLE_UP,
    TRIANGLE_DOWN,
    TRIANGLE_RIGHT,
    TRIANGLE_LEFT;

    /**
     * Gets a shape based on the type of a cell
     * @return the JavaFX object corresponding to the shape
     */
    public Shape getNewFxShapeObject(){
        switch(this){
            case CIRCLE:
                return new Circle(Cell.BOX_SIZE / 2., Cell.BOX_SIZE / 2., Cell.BOX_SIZE / 2.);
            case TRIANGLE_UP:
                return new Polygon(0, Cell.BOX_SIZE, Cell.BOX_SIZE, Cell.BOX_SIZE, Cell.BOX_SIZE / 2., 0);
            case TRIANGLE_DOWN:
                return new Polygon(0, 0, Cell.BOX_SIZE, 0, Cell.BOX_SIZE / 2., Cell.BOX_SIZE);
            case TRIANGLE_RIGHT:
                return new Polygon(0, 0, Cell.BOX_SIZE, Cell.BOX_SIZE / 2., 0, Cell.BOX_SIZE);
            case TRIANGLE_LEFT:
                return new Polygon(0, Cell.BOX_SIZE / 2., Cell.BOX_SIZE, 0, Cell.BOX_SIZE, Cell.BOX_SIZE);
            case SQUARE:
            default:
                return new Rectangle(Cell.BOX_SIZE, Cell.BOX_SIZE);

        }
    }

}
