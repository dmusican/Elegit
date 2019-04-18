package elegit.models;

import elegit.models.CommitHelper;
import elegit.models.RefHelper;
import net.jcip.annotations.ThreadSafe;
import org.eclipse.jgit.revwalk.RevTag;

/**
 * A wrapper class for annotated tags to make them easier to interact with and preserves
 * certain aspects that are expensive to look up with JGit's standard RevTag, e.g. author,
 * tagMessage, etc.
 */
@ThreadSafe
public class TagHelper extends RefHelper {

    // THe name of this ref, e.g. 'master' or 'tag1'
    private final String refName;

    public TagHelper(RevTag t, CommitHelper c) {
        // Synchronized here operations on the tag object might not be threadsafe
        synchronized(t) {
            this.refName = t.getTagName();
            this.commit.set(c);
        }
    }

    public TagHelper (String name, CommitHelper c) {
        this.refName = name;
        this.commit.set(c);
    }

    /**
     * @return the name of the ref
     */
    @Override
    public String getRefName() {
        return this.refName;
    }


    /**
     * @param c the commit helper this tag is associated with
     */
    public void setCommit(CommitHelper c) {
        this.commit.set(c);
    }

    public String getCommitId() {
        return this.commit.get().getName();
    }

}
