package elegit;

import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * A wrapper class for commits to make them easier to interact with and preserves certain
 * aspects that are expensive to look up with JGit's standard RevCommit, e.g. parents,
 * children, and author.
 */
// TODO: Make sure threadsafe
public class CommitHelper{

    // The commit this helper wraps
    RevCommit commit;
    // The author of this commit
    PersonIdent author;

    // The parents and children of this commit
    ParentCommitHelper parents;
    List<CommitHelper> children;

    // The short and full message of this commit
    String shortMessage;
    String fullMessage;

    List<TagHelper> tags;

    /**
     * Constructs a helper for the given commit. Note that if c is not a fully parsed commit
     * this constructor will fail and throw errors. Using one of the other constructors will
     * parse the info.
     *
     * @param c a fully parsed commit
     */
    public CommitHelper(RevCommit c) throws IOException{
        this.commit = c;
        this.author = c.getAuthorIdent();
        this.children = new ArrayList<>();
        this.parents = new ParentCommitHelper(this);
        this.fullMessage = c.getFullMessage();
        this.shortMessage = c.getShortMessage();
    }

    /**
     * Constructs a helper after parsing the commit corresponding to the given ObjectId
     * @param id the id of the commit to parse and wrap
     * @param repoHelper the repoHelper with the repository necessary to parse the commit
     * @throws IOException
     */
    public CommitHelper(ObjectId id, RepoHelper repoHelper) throws IOException{
        this(repoHelper.parseRawCommit(id));
    }

    /**
     * Constructs a helper from a reference string by getting its corresponding id
     * and then parsing the corresponding commit
     * @param refString the string corresponding to some commit
     * @param repoHelper the repoHelper with the repository necessary to parse the reference
     *                   and commit
     * @throws IOException
     */
    public CommitHelper(String refString, RepoHelper repoHelper) throws IOException{
        this(repoHelper.getRepo().resolve(refString), repoHelper);
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
            return this.fullMessage;
        }else{
            return this.shortMessage;
        }
    }

    /**
     * @return the name of the author of this commit
     */
    public String getAuthorName(){
        return author.getName();
    }

    /**
     * @return the date object corresponding to the time of this commit
     */
    public Date getWhen(){
        return author.getWhen();
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
    public void addParent(CommitHelper parent){
        if(parents == null){
            this.parents = new ParentCommitHelper(this, parent);
        }else {
            this.parents.addParent(parent);
        }
    }

    /**
     * @return the parents of this commit in an ArrayList
     */
    public List<CommitHelper> getParents(){
        return parents.toList();
    }

    /**
     * Checks to see if the given commit has this commit as an ancestor,
     * up to the given number of generations.
     *
     * Entering zero or a negative number will search all descendants
     *
     * @param commit the commit to check
     * @param depth how many generations down to check
     * @return true if commit is a child of this commit, otherwise false
     */
    public boolean isChild(CommitHelper commit, int depth){
        depth--;
        if(children.contains(commit)) return true;
        else if(depth != 0){
            for(CommitHelper child : children){
                if(child.isChild(commit, depth)){
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * @param child the new child of this commit
     */
    public void addChild(CommitHelper child){
        if(!isChild(child, 1)){
            children.add(child);
        }
    }

    /**
     * @return the list of this commits children
     */
    public List<CommitHelper> getChildren(){
        return children;
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
    public void addTag(TagHelper t) {
        if (this.tags == null)
            this.tags = new ArrayList<>();
        this.tags.add(t);
    }

    /**
     * @param s a TagHelper that will be deleted
     */
    public void removeTag(String s) {
        for (TagHelper tag: this.tags) {
            if (tag.getRefName().equals(s)) {
                this.tags.remove(tag);
                return;
            }
        }
    }

    /**
     * @return the list of tags that reference this commit
     */
    public List<TagHelper> getTags() {
        if (this.tags == null)
            this.tags = new ArrayList<>();
        return this.tags;
    }

    public List<String> getTagNames() {
        if (this.tags == null)
            this.tags = new ArrayList<>();
        ArrayList<String> tagNames = new ArrayList<>();
        for (TagHelper tag: this.tags) {
            tagNames.add(tag.getRefName());
        }
        return tagNames;
    }

    public boolean hasTags() {
        return this.tags.size()!=0;
    }

    public boolean hasTag(String tagName) { return this.getTagNames().contains(tagName); }

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

    /**
     * A helper class for the parents of a commit. Any number of commits that
     * can be parents of the same commit
     */
    private class ParentCommitHelper{

        private CommitHelper child;
        private ArrayList<CommitHelper> parents;

        /**
         * Sets parent to be the parent of child
         * @param child the child commit
         * @param parent the first parent commit
         */
        public ParentCommitHelper(CommitHelper child, CommitHelper parent){
            parents = new ArrayList<>();
            parents.add(parent);
            this.setChild(child);
        }

        /**
         * Sets the child as the child, no parents yet
         * @param child CommitHelper
         */
        public ParentCommitHelper(CommitHelper child){
            parents = new ArrayList<>();
            this.setChild(child);
        }

        /**
         * @return the number of parent commits associated with this object
         */
        public int count(){
            return parents.size();
        }

        /**
         * @return the stored parent commits in list form
         */
        public List<CommitHelper> toList(){
            return parents;
        }

        /**
         * Adds the given child to the children of each parent commit
         * @param child the child associated with this object
         */
        private void setChild(CommitHelper child){
            this.child = child;
            for(CommitHelper parent : parents) {
                if(parent != null) {
                    parent.addChild(child);
                }
            }
        }

        /**
         * Adds the given parent to this object and child to its children
         * @param parent the parent to add
         */
        public void addParent(CommitHelper parent){
            if(!parents.contains(parent)) {
                parent.addChild(this.child);
                parents.add(parent);
            }
        }
    }
}
