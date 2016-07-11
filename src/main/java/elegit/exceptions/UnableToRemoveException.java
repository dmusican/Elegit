package elegit.exceptions;

/**
 * An exception thrown when a file that can't be removed is selected
 * to be added
 */
public class UnableToRemoveException extends Exception {
    public String filename;
    public UnableToRemoveException(String filename) {
        this.filename = filename;
    }
}
