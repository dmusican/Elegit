package elegit.exceptions;

/**
 * An exception thrown when a file that can't be added is selected
 * to be added
 */
public class UnableToAddException extends Exception {
    public String filename;
    public UnableToAddException(String filename) {
        this.filename = filename;
    }
}
