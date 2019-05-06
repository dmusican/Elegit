package elegit.exceptions;

/**
 * An exception thrown when a file that can't be removed is selected
 * to be added
 */
public class UnableToRemoveException extends Exception {
    private final String filename;

    public UnableToRemoveException(String filename) {
        this.filename = filename;
    }

    public String getFilename() {
        return filename;
    }
}
