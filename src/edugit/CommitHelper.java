package edugit;

import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by makik on 6/12/15.
 *
 * A wrapper class for commits to make them easier to interact with and preserves certain
 * aspects that are expensive to look up with JGit's standard RevCommit, e.g. parents,
 * children, and author.
 */
public class CommitHelper{

    RevCommit commit;
    PersonIdent author;

    ParentCommitHelper parents;
    ArrayList<CommitHelper> children;

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
    public ObjectId getId(){
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
        DateFormat formatter = new SimpleDateFormat("h:mm a MMM dd yyyy");
        String dateFormatted = formatter.format(this.getWhen());
        return dateFormatted;
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
    public ArrayList<CommitHelper> getParents(){
        return parents.toList();
    }

    /**
     * Checks to see if the given commit is a child of this commit.
     *
     * NOTE: DOES NOT CHECK BEYOND THIS COMMITS CHILDREN. GRANDCHILDREN
     * AND BEYOND WILL NOT BE DETECTED
     *
     * @param commit the commit to check
     * @return true if commit is a child of this commit, otherwise false
     */
    public boolean isChild(CommitHelper commit){
        return children.contains(commit);
    }

    /**
     * @param child the new child of this commit
     */
    public void addChild(CommitHelper child){
        if(!isChild(child)){
            children.add(child);
        }
    }

    /**
     * @return the list of this commits children
     */
    public ArrayList<CommitHelper> getChildren(){
        return children;
    }

    /**
     * Testing method
     * @return a string with some information about this commit
     */
    public String getInfoString(){
        String s = this.getName();
        s = s + " - " + this.getAuthorName();
        s = s + " - " + this.getFormattedWhen();
        s = s + " - " + this.getMessage(false);

        return s;
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
        public ArrayList<CommitHelper> toList(){
            ArrayList<CommitHelper> list = new ArrayList<>(2);
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
