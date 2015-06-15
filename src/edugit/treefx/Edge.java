package edugit.treefx;

import javafx.scene.Group;
import javafx.scene.shape.Line;

/**
 * Created by makik on 6/10/15.
 *
 * Connects two cells in the TreeGraph using a DirectedLine
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

        Line startLine = new Line();
        DirectedLine endLine = new DirectedLine();

        startLine.startXProperty().bind(source.layoutXProperty().add(source.getBoundsInParent().getWidth() / 2.0));
        startLine.startYProperty().bind(source.layoutYProperty().add(source.getBoundsInParent().getHeight() / 2.0));
        startLine.endXProperty().bind(endLine.endXProperty());
        startLine.endYProperty().bind(startLine.startYProperty().subtract(TreeLayout.V_SPACING / 2.));

        endLine.startXProperty().bind(startLine.endXProperty());
        endLine.startYProperty().bind(startLine.endYProperty());
        endLine.endXProperty().bind(target.layoutXProperty().add(target.getBoundsInParent().getWidth() / 2.0));
        endLine.endYProperty().bind(target.layoutYProperty().add(target.getBoundsInParent().getHeight() / 2.0));

        getChildren().add(startLine);
        getChildren().add(endLine);

    }

    public Cell getSource() {
        return source;
    }

    public Cell getTarget() {
        return target;
    }

}
