package elegit.exceptions;

/**
 * An exception thrown when a file that can't be added is selected
 * to be added
 */
public class UnableToAddException extends Exception {
    private final String filename;

    public UnableToAddException(String filename) {
        this.filename = filename;
    }

    public String getFilename() {
        return filename;
    }
}
