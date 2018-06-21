package elegit.gui;

import java.util.function.IntFunction;

import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.shape.Rectangle;

import org.reactfx.value.Val;

/**
 * Created by grenche on 6/21/18.
 * Created a line that will be above an belowa ny chunk of code that is conflicting
 */
public class ConflictLineSection implements IntFunction<Node> {
    private final ObservableValue<Integer> conflictingLine;
    private final double height;

    public ConflictLineSection(ObservableValue<Integer> conflictingLine, double height) {
        this.conflictingLine = conflictingLine;
        this.height = height;
    }

    @Override
    public Node apply(int lineNumber) {
        Rectangle rectangle = new Rectangle(0, lineNumber, 4, height * 15);
        rectangle.getStyleClass().add("conflict-line");

        Val<Boolean> visible = Val.map(
                conflictingLine,
                cl -> cl == lineNumber);

        rectangle.visibleProperty().bind(visible.conditionOnShowing(rectangle));

        return rectangle;
    }
}
