package elegit.models;

/**
 * Created by gorram on 6/20/18.
 */
public class ConflictLine {
    private String line;
    private boolean status;

    public ConflictLine(String line, boolean status){
        this.line=line;
        this.status=status;
    }
    public ConflictLine(String line){
        this.line=line;
        this.status=false;
    }
    public String getLine(){
        return line;
    }
    public boolean isConflicting(){
        return status;
    }
    public void setConflictStatus(boolean newStatus){
        status=newStatus;
    }
}
