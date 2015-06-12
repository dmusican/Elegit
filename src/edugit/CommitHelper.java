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
 */
public class CommitHelper{

    static ArrayList<ObjectId> parsedIds = new ArrayList<>();

    RepoHelper repoHelper;
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
    public CommitHelper(RevCommit c, RepoHelper repoHelper) throws IOException{
        this.repoHelper = repoHelper;
        this.commit = c;
        this.author = c.getAuthorIdent();
        this.children = new ArrayList<>();
        this.parents = new ParentCommitHelper(this, null, null);
    }

    public CommitHelper(ObjectId id, RepoHelper repoHelper) throws IOException{
        this(repoHelper.parseRawCommit(id), repoHelper);
    }

    public CommitHelper(String refString, RepoHelper repoHelper) throws IOException{
        this(repoHelper.getRepo().resolve(refString), repoHelper);
    }

    public String getName(){
        return commit.getName();
    }

    public ObjectId getId(){
        return this.commit.getId();
    }

    public String getMessage(boolean fullMessage){
        if(fullMessage){
            return commit.getFullMessage();
        }else{
            return commit.getShortMessage();
        }
    }

    public String getAuthorName(){
        return author.getName();
    }

    public String getAuthorEmail(){
        return author.getEmailAddress();
    }

    public Date getWhen(){
        return author.getWhen();
    }

    public void setParents(CommitHelper parent1, CommitHelper parent2){
        this.parents = new ParentCommitHelper(this, parent1, parent2);
    }

    public void addParent(CommitHelper parent){
        if(parents == null){
            this.parents = new ParentCommitHelper(this, parent, null);
        }else{
            this.parents.addParent(this, parent);
        }
    }

    public int getParentCount(){
        return parents.count();
    }

    public ArrayList<CommitHelper> getParents(){
        return parents.toList();
    }

    public CommitHelper getParent(int index){
        return parents.toList().get(index);
    }

    public boolean isChild(CommitHelper commit){
        return children.contains(commit);
    }

    public void addChild(CommitHelper child){
        if(!isChild(child)){
            children.add(child);
        }
    }

    public CommitHelper getChild(int index){
        return children.get(index);
    }

    public ArrayList<CommitHelper> getChildren(){
        return children;
    }

    public String getInfoString(){
        DateFormat formatter = new SimpleDateFormat("h:mm a MMM dd yyyy");

        String dateFormatted = formatter.format(this.getWhen());

        String s = this.getName();
        s = s + " - " + this.getAuthorName();
        s = s + " - " + dateFormatted;
        s = s + " - " + this.getMessage(false);

        return s;
    }

    private class ParentCommitHelper{

        private CommitHelper mom,dad;

        public ParentCommitHelper(CommitHelper child, CommitHelper mom, CommitHelper dad){
            this.mom = mom;
            this.dad = dad;
            this.setChild(child);
        }

        public int count(){
            int count = 0;
            if(mom != null) count++;
            if(dad != null) count++;
            return count;
        }

        public ArrayList<CommitHelper> toList(){
            ArrayList<CommitHelper> list = new ArrayList<>(2);
            if(mom != null) list.add(mom);
            if(dad != null) list.add(dad);
            return list;
        }

        public void setChild(CommitHelper child){
            if(this.mom != null){
                this.mom.addChild(child);
            }
            if(this.dad != null){
                this.dad.addChild(child);
            }
        }

        public void addParent(CommitHelper child, CommitHelper parent){
            if(this.mom == null){
                this.mom = parent;
                this.mom.addChild(child);
            }else if(this.dad == null){
                this.dad = parent;
                this.dad.addChild(child);
            }
        }
    }
}
