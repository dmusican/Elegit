package elegit.treefx;

import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;

/**
 * Enum for the different shapes a cell can take on
 */
public enum CellShape{
    SQUARE,
    CIRCLE,
    TRIANGLE_UP,
    TRIANGLE_DOWN,
    TRIANGLE_RIGHT,
    TRIANGLE_LEFT;

    /**
     * Gets a shape based on the type of a cell
     * @param type the type of the cell (local, remote, or both)
     * @return the shape of the cell, just the basic shape for local or remote and a concentric circle for both
     */
    public Shape getType(Cell.CellType type) {
        Shape toReturn;
        switch (type) {
            case LOCAL:
                toReturn = get();
                break;
            case REMOTE:
                toReturn = get();
                break;
            case BOTH:
                toReturn = Shape.union(getInside(Cell.BOX_INSIDE), Shape.subtract(get(), getInside(Cell.BOX_INSET)));
                break;
            default:
                toReturn = null;
                break;
        }
        return toReturn;
    }

    /**
     * @return the JavaFX object corresponding to the shape
     */
    public Shape get(){
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

    /**
     * @param inset the inset of the inner shape to get
     * @return the JavaFX object corresponding to the interior to the shape
     */
    public Shape getInside(int inset) {
        int extraInset = inset;
        if (inset==Cell.BOX_INSIDE)
            extraInset += 1;
        switch(this){
            case CIRCLE:
                return new Circle(Cell.BOX_SIZE / 2., Cell.BOX_SIZE / 2., Cell.BOX_SIZE / 2. - inset);
            case TRIANGLE_UP:
                return new Polygon(extraInset, Cell.BOX_SIZE-inset, Cell.BOX_SIZE-extraInset, Cell.BOX_SIZE-inset,
                        Cell.BOX_SIZE / 2., extraInset+1);
            case TRIANGLE_DOWN:
                return new Polygon(extraInset, inset, Cell.BOX_SIZE-extraInset, inset,
                        Cell.BOX_SIZE / 2., Cell.BOX_SIZE-extraInset);
            case TRIANGLE_RIGHT:
                return new Polygon(inset, extraInset, Cell.BOX_SIZE-extraInset, Cell.BOX_SIZE / 2.,
                        inset, Cell.BOX_SIZE-extraInset);
            case TRIANGLE_LEFT:
                return new Polygon(extraInset, Cell.BOX_SIZE / 2., Cell.BOX_SIZE-inset, extraInset,
                        Cell.BOX_SIZE-inset, Cell.BOX_SIZE-extraInset);
            case SQUARE:
            default:
                return new Rectangle(inset, inset, Cell.BOX_SIZE-(2*inset), Cell.BOX_SIZE-(2*inset));

        }
    }
}
