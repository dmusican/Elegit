package elegit.gui;

import java.util.function.IntFunction;

import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.shape.Polygon;

import org.reactfx.value.Val;

/**
 * Created by grenche on 6/21/18.
 * Creates a pointer, gives it a style class, and makes it so it is only visible when it is pointing at the current line
 * AKA current conflict (unless the user clicks elsewhere)
 * Very similar to example given by Tomas Mikula as an answer to a StackOverflow question about adding break points to
 * CodeAreas.
 */
public class ConflictLinePointer implements IntFunction<Node> {
    private final ObservableValue<Integer> conflictingLine;

    public ConflictLinePointer(ObservableValue<Integer> conflictingLine) {
        this.conflictingLine = conflictingLine;
    }

    @Override
    public Node apply(int lineNumber) {
        Polygon pointer = new Polygon(0, 0, 10, 5, 0, 10);

        pointer.getStyleClass().add("conflict-pointer");

        // Only show the pointer when the code area is focused on that conflict
        Val<Boolean> visible = Val.map(
                conflictingLine,
                cl -> cl == lineNumber);
        pointer.visibleProperty().bind(visible.conditionOnShowing(pointer));

        return pointer;
    }
}
