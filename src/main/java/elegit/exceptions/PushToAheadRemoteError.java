package elegit.exceptions;

/**
 * An error thrown when a push doesn't go through because
 * Remote is ahead of Local.
 */
public class PushToAheadRemoteError extends Exception {

    private final boolean allRefsRejected;

    public PushToAheadRemoteError(boolean allRefsRejected){
        this.allRefsRejected = allRefsRejected;
    }

    public boolean isAllRefsRejected(){
        return allRefsRejected;
    }
}
