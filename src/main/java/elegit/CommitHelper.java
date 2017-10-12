package elegit;

import org.apache.http.annotation.GuardedBy;
import org.apache.http.annotation.ThreadSafe;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * A wrapper class for commits to make them easier to interact with and preserves certain
 * aspects that are expensive to look up with JGit's standard RevCommit, e.g. parents,
 * children, and author.
 */
@ThreadSafe
public class CommitHelper{

    // The commit this helper wraps. It appears that RevCommit is immutable.
    private final RevCommit commit;

    // The parents and children of this commit
    @GuardedBy("this") private final ArrayList<CommitHelper> parents;
    @GuardedBy("this") private final List<TagHelper> tags;

    /**
     * Constructs a helper for the given commit. Note that if c is not a fully parsed commit
     * this constructor will fail and throw errors. Using one of the other constructors will
     * parse the info.
     *
     * @param c a fully parsed commit
     */
    public CommitHelper(RevCommit c) throws IOException{
        this.commit = c;
        this.parents = new ArrayList<>();
        this.tags = new ArrayList<>();
    }

    /**
     * @return the unique name of the commit (the hash)
     */
    public String getName(){
        return commit.getName();
    }

    /**
     * @return the unique ObjectId of the commit
     */
    public ObjectId getObjectId(){
        return this.commit.getId();
    }

    /**
     * @param fullMessage whether to return the full or abbreviated commit message
     * @return the commit message
     */
    public String getMessage(boolean fullMessage){
        if(fullMessage){
            return commit.getFullMessage();
        }else{
            return commit.getShortMessage();
        }
    }

    /**
     * @return the name of the author of this commit
     */
    public String getAuthorName(){
        return commit.getAuthorIdent().getName();
    }

    /**
     * @return the date object corresponding to the time of this commit
     */
    public Date getWhen(){
        return new Date(commit.getAuthorIdent().getWhen().getTime());
    }

    /**
     * @return the formatted date string corresponding to the time of this commit
     */
    public String getFormattedWhen(){
        DateFormat formatter = new SimpleDateFormat("MMM dd yyyy, h:mm a");
        return formatter.format(this.getWhen());
    }

    /**
     * Add an additional parent to this commit. If parent is null, this method
     * does effectively nothing
     * @param parent the commit to add
     */
    public synchronized void addParent(CommitHelper parent){
        if(!parents.contains(parent)) {
            parents.add(parent);
        }
    }

    public synchronized List<String> getParentNames() {
        ArrayList<String> parentNames = new ArrayList<>();
        for (CommitHelper commit : parents) {
            parentNames.add(commit.getName());
        }
        return Collections.unmodifiableList(parentNames);
    }
    /**
     * @return the parents of this commit in an ArrayList
     */
    public synchronized List<CommitHelper> getParents(){
        return Collections.unmodifiableList(parents);
    }

    public synchronized boolean parentsContains(CommitHelper parentCommitHelper) {
        return parents.contains(parentCommitHelper);
    }

    public synchronized int numParents() {
        return parents.size();
    }

    @Override
    public String toString(){
        String s = this.getAuthorName();
        s = s + " \t" + this.getFormattedWhen();
        s = s + " \t" + this.getName();
        s = s + " \t" + this.getMessage(false);
        return s;
    }

    /**
     * @param t a TagHelper for a tag that references this commit
     */
    public synchronized void addTag(TagHelper t) {
        this.tags.add(t);
    }

    /**
     * @param s a TagHelper that will be deleted
     */
    public synchronized void removeTag(String s) {
        for (TagHelper tag: this.tags) {
            if (tag.getRefName().equals(s)) {
                this.tags.remove(tag);
                return;
            }
        }
    }

    public synchronized boolean hasTag(String tagName) {
        for (TagHelper tag: this.tags) {
            if (tag.getRefName().equals(tagName))
                return true;
        }
        return false;
    }

    /**
     * @return the commit object for this helper
     */
    public RevCommit getCommit() {
        return this.commit;
    }

    @Override
    public int hashCode(){
        return this.commit.hashCode();
    }

    @Override
    public boolean equals(Object other){
        return (other instanceof CommitHelper) && this.commit.equals(((CommitHelper) other).getCommit());
    }
}
