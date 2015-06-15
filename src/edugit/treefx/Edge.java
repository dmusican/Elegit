package edugit.treefx;

import javafx.beans.binding.DoubleBinding;
import javafx.scene.Group;

/**
 * Created by makik on 6/10/15.
 *
 * Connects two cells in the TreeGraph using a DirectedPath
 */
public class Edge extends Group {

    protected Cell source;
    protected Cell target;

    /**
     * Constructs a directed line between the source and target cells and binds
     * properties to handle relocation smoothly
     * @param source the source (parent) cell
     * @param target the target (child) cell
     */
    public Edge(Cell source, Cell target) {

        this.source = source;
        this.target = target;

        DoubleBinding startX = source.layoutXProperty().add(source.getBoundsInParent().getWidth() / 2.0);
        DoubleBinding startY = source.layoutYProperty().add(source.getBoundsInParent().getHeight() / 2.0);

        DoubleBinding endX = target.layoutXProperty().add(target.getBoundsInParent().getWidth() / 2.0);
        DoubleBinding endY = target.layoutYProperty().add(target.getBoundsInParent().getHeight());

        DirectedPath path = new DirectedPath(startX, startY, endX, startY.subtract(TreeLayout.V_SPACING / 2.), endX, endY);

        getChildren().add(path);

    }

    public Cell getSource() {
        return source;
    }

    public Cell getTarget() {
        return target;
    }

}
