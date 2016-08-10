package elegit.exceptions;

/**
 * Exception thrown when there are uncommited changes in the working tree
 * and one tries to switch branches or related git function
 */
public class UncommitedChangesException extends Exception {
}
