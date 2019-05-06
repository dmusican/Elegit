package elegit.exceptions;

import java.util.*;

/**
 * Exception thrown when merging results in conflicting files
 */
public class ConflictingFilesException extends Exception{

    private final Map<String, int[][]> conflicts;

    public ConflictingFilesException(Map<String, int[][]> conflicts){
        this.conflicts = conflicts;
    }

    public Set<String> getConflictingFiles(){
        return Collections.unmodifiableSet(conflicts.keySet());
    }
}
