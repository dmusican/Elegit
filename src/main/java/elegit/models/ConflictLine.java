package elegit.models;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by gorram on 6/20/18.
 * Line object that stores the line and a boolean that determines whether or not it is conflicting
 */
public class ConflictLine {
    private ArrayList<String> lines;
    // true when conflicting, false otherwise
    private AtomicBoolean conflict = new AtomicBoolean();
    private AtomicBoolean handled = new AtomicBoolean();
    private AtomicBoolean changed = new AtomicBoolean();

    public ConflictLine(ArrayList<String> lines, boolean conflict) {
        this.lines = lines;
        this.conflict.set(conflict);
        this.handled.set(false);
        this.changed.set(false);
    }

    public ConflictLine(ArrayList<String> lines) {
        this.lines = lines;
        this.conflict.set(false);// = false;
        this.handled.set(false);
        this.changed.set(false);
    }
    public ConflictLine(boolean conflict) {
        this.lines = new ArrayList<>();
        this.conflict.set(conflict);
        this.handled.set(false);
        this.changed.set(false);
    }

    public void addLine(String line){
        lines.add(line);
    }
    public List<String> getLines() {
        return lines;
    }


    public boolean isConflicting() {
        return conflict.get();
    }

    public void setConflictStatus(boolean newStatus) {
        conflict.set(newStatus);
    }

    public boolean isHandled() {
        return handled.get();
    }

    public void setHandledStatus(boolean newStatus) {
        handled.set(newStatus);
    }

    public boolean isChanged() {
        return changed.get();
    }

    public void setChangedStatus(boolean newStatus) {
        changed.set(newStatus);
    }
}
