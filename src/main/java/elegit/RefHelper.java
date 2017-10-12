package elegit;

import elegit.models.CommitHelper;
import elegit.treefx.CellLabel;

/**
 * Parent class for branch and tag helpers
 */
// TODO: Make sure threadsafe
public class RefHelper {
    // THe name of this ref, e.g. 'master' or 'tag1'
    String refName;
    // The commit that this ref points to
    CommitHelper commit;

    /**
     * @return the name of the ref
     */
    public String getRefName() { return this.refName; }


    /**
     * @return the commit that this ref points to, or null if it hasn't been set
     */
    public CommitHelper getCommit(){
        return commit;
    }


    /**
     * @return the name of the ref, or an abbreviated version if it's too long
     */
    public String getAbbrevName() {
        if (this.refName.length()> CellLabel.MAX_CHAR_PER_LABEL)
            return this.refName.substring(0, 24) + "...";
        return this.refName;
    }
}
