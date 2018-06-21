package elegit.models;

/**
 * Created by gorram on 6/20/18.
 * Line object that stores the line and a boolean that determines whether or not it is conflicting
 */
public class ConflictLine {
    private String line;
    // true when conflicting, false otherwise
    private boolean conflict;
    private boolean modified;

    public ConflictLine(String line, boolean conflict) {
        this.line = line;
        this.conflict = conflict;
        this.modified = false;
    }

    public ConflictLine(String line) {
        this.line = line;
        this.conflict = false;
    }

    public String getLine() {
        return line;
    }

    public void changeLine(String changedLine) {
        this.line=changedLine;
    }

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
