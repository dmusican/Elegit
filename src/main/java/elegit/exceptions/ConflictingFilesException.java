package main.java.elegit.exceptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Exception thrown when merging results in conflicting files
 */
public class ConflictingFilesException extends Exception{

    private final Map<String, int[][]> conflicts;

    public ConflictingFilesException(Map<String, int[][]> conflicts){
        this.conflicts = conflicts;
    }

    public Map<String, int[][]> getConflicts(){
        return conflicts;
    }

    public List<String> getConflictingFiles(){
        return new ArrayList<>(conflicts.keySet());
    }
}
