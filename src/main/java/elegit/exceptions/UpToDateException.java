package elegit.exceptions;

/**
 * Exception thrown when trying to merge, but the local
 * branch is already up to date with the remote
 */
public class UpToDateException extends Exception {
}
