package elegit;

import elegit.models.CommitHelper;
import elegit.treefx.CellLabel;

/**
 * Parent class for branch and tag helpers
 */
// TODO: Make sure threadsafe
public abstract class RefHelper {
    // The commit that this ref points to
    CommitHelper commit;

    /**
     * @return the commit that this ref points to, or null if it hasn't been set
     */
    public CommitHelper getCommit(){
        return commit;
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
