package main.java.elegit;

import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * A wrapper class for commits to make them easier to interact with and preserves certain
 * aspects that are expensive to look up with JGit's standard RevCommit, e.g. parents,
 * children, and author.
 */
public class CommitHelper{

    // The commit this helper wraps
    RevCommit commit;
    // The author of this commit
    PersonIdent author;

    // The parents and children of this commit
    ParentCommitHelper parents;
    List<CommitHelper> children;

    // The branches for which this commit is a head
    Map<String, BranchHelper> branchesAsHead;

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
        this.parents = new ParentCommitHelper(this, null, null);
        this.branchesAsHead = new HashMap<>();
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
     * @return the unique identifying string for this commit
     */
    public String getId(){
        return CommitTreeModel.getId(this);
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
     * @return the email of the author of this commit
     */
    public String getAuthorEmail(){
        return author.getEmailAddress();
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
     * Sets the parents of this commit to be the two given commits. 0, 1, or 2
     * of the parameters can be null for the corresponding number of parents
     * @param parent1 the first parent
     * @param parent2 the second parent
     */
    public void setParents(CommitHelper parent1, CommitHelper parent2){
        this.parents = new ParentCommitHelper(this, parent1, parent2);
    }

    /**
     * Add an additional parent to this commit. If parent is null, this method
     * does effectively nothing
     * @param parent the commit to add
     */
    public void addParent(CommitHelper parent){
        if(parents == null){
            this.parents = new ParentCommitHelper(this, parent, null);
        }else{
            this.parents.addParent(parent);
        }
    }

    /**
     * @return the number of parents this commit has
     */
    public int getParentCount(){
        return parents.count();
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

    /**
     * Notifies this commit that it is the head of the given branch
     * @param branch the branch for which this commit is the head
     */
    public void setAsHead(BranchHelper branch){
        branchesAsHead.put(branch.getRefPathString(), branch);
    }

    /**
     * Notifies this commit that it is no longer the head of the given branch
     * @param branch the branch for which this commit is no longer the head
     */
    public void removeAsHead(BranchHelper branch){
        branchesAsHead.remove(branch.getRefPathString());
    }

    /**
     * @return all branches for which this commit is the head
     */
    public List<BranchHelper> getBranchesAsHead(){
        return new LinkedList<>(branchesAsHead.values());
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
     * @return the list of tags that reference this commit
     */
    public List<TagHelper> getTags() {
        if (this.tags == null)
            this.tags = new ArrayList<>();
        return this.tags;
    }

    /**
     * @return the commit object for this helper
     */
    public RevCommit getCommit() {
        return this.commit;
    }

    /**
     * A helper class for the parents of a commit. Holds 0-2 commits that
     * can be parents of the same commit
     */
    private class ParentCommitHelper{

        private CommitHelper child,mom,dad;

        /**
         * Sets mom and dad to be the parents of child
         * @param child the child commit
         * @param mom the first parent commit
         * @param dad the second parent commit
         */
        public ParentCommitHelper(CommitHelper child, CommitHelper mom, CommitHelper dad){
            this.mom = mom;
            this.dad = dad;
            this.setChild(child);
        }

        /**
         * @return the number of parent commits associated with this object
         */
        public int count(){
            int count = 0;
            if(mom != null) count++;
            if(dad != null) count++;
            return count;
        }

        /**
         * @return the stored parent commits in list form
         */
        public List<CommitHelper> toList(){
            List<CommitHelper> list = new ArrayList<>(2);
            if(mom != null) list.add(mom);
            if(dad != null) list.add(dad);
            return list;
        }

        /**
         * Adds the given child to the children of each parent commit
         * @param child the child associated with this object
         */
        private void setChild(CommitHelper child){
            this.child = child;
            if(this.mom != null){
                this.mom.addChild(child);
            }
            if(this.dad != null){
                this.dad.addChild(child);
            }
        }

        /**
         * Adds the given parent to this object and child to its children
         * @param parent the parent to add
         */
        public void addParent(CommitHelper parent){
            if(this.mom == null){
                this.mom = parent;
                this.mom.addChild(this.child);
            }else if(this.dad == null){
                this.dad = parent;
                this.dad.addChild(this.child);
            }
        }
    }
}
