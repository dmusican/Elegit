package elegit;

import elegit.controllers.CommitController;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
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
// TODO: Make sure threadsafe
public class CommitHelper{

    // The commit this helper wraps. It appears that RevCommit is immutable.
    private final RevCommit commit;

    // The parents and children of this commit
    private final ArrayList<CommitHelper> parents;



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
        this.children = new ArrayList<>();
        this.parents = new ArrayList<>();
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
    // TODO: Make this class immutable by making the method below return a new CommitHelper, with an updated list of parents.
    // Can speed this up by allowing a version of addParent that takes a list of parents, and does it at once.
    public void addParent(CommitHelper parent){
        if(!parents.contains(parent)) {
            parents.add(parent);
        }
    }

    public List<String> getParentNames() {
        ArrayList<String> parentNames = new ArrayList<>();
        for (CommitHelper commit : parents) {
            parentNames.add(commit.getName());
        }
        return Collections.unmodifiableList(parentNames);
    }
    /**
     * @return the parents of this commit in an ArrayList
     */
    // TODO: Make class immutable by returning an unmodifiable copy of parents; also make sure parents can't get changed after construction
    public List<CommitHelper> getParents(){
        return parents;
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
    private boolean isChild(CommitHelper commit, int depth){
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
    void addTag(TagHelper t) {
        if (this.tags == null)
            this.tags = new ArrayList<>();
        this.tags.add(t);
    }

    /**
     * @param s a TagHelper that will be deleted
     */
    void removeTag(String s) {
        for (TagHelper tag: this.tags) {
            if (tag.getRefName().equals(s)) {
                this.tags.remove(tag);
                return;
            }
        }
    }

    List<String> getTagNames() {
        if (this.tags == null)
            this.tags = new ArrayList<>();
        ArrayList<String> tagNames = new ArrayList<>();
        for (TagHelper tag: this.tags) {
            tagNames.add(tag.getRefName());
        }
        return tagNames;
    }

    boolean hasTag(String tagName) { return this.getTagNames().contains(tagName); }

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
