package elegit.models;
import java.util.ArrayList;
/**
 * Created by gorram on 6/20/18.
 * Line object that stores the line and a boolean that determines whether or not it is conflicting
 */
public class ConflictLine {
    private ArrayList<String> lines;
    // true when conflicting, false otherwise
    private boolean conflict;
    private boolean modified;

    public ConflictLine(ArrayList<String> lines, boolean conflict) {
        this.lines = lines;
        this.conflict = conflict;
        this.modified = false;
    }

    public ConflictLine(ArrayList<String> lines) {
        this.lines = lines;
        this.conflict = false;
    }
    public ConflictLine(boolean conflict) {
        this.lines = new ArrayList<>();
        this.conflict = conflict;
    }

    public void addLine(String line){
        lines.add(line);
    }
    public ArrayList<String> getLines() {
        return lines;
    }

    //public void changeLine(String changedLine) {
    //    this.line=changedLine;
    //}

    public boolean isConflicting() {
        return conflict;
    }

    public void setConflictStatus(boolean newStatus) {
        conflict = newStatus;
    }

    public boolean isModified() {
        return modified;
    }

    public void setModifiedStatus(boolean newStatus) {
        modified = newStatus;
    }
}
