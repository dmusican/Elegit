package elegit.models;

import elegit.models.CommitHelper;
import elegit.treefx.CellLabel;
import org.apache.http.annotation.ThreadSafe;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Parent class for branch and tag helpers
 */
@ThreadSafe
public abstract class RefHelper {

    // The commit that this ref points to
    protected final AtomicReference<CommitHelper> commit;

    public RefHelper() {
        commit = new AtomicReference<>();
    }

    /**
     * @return the commit that this ref points to, or null if it hasn't been set
     */
    public CommitHelper getCommit(){
        return commit.get();
    }

    /**
     * @return the name of the ref
     */
    public abstract String getRefName();

    /**
     * @return the name of the ref, or an abbreviated version if it's too long
     */
    public String getAbbrevName() {
        String name = getRefName();
        if (name.length()> CellLabel.MAX_CHAR_PER_LABEL)
            name = name.substring(0, 24) + "...";
        return name;
    }
}
