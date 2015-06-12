package edugit;

import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by makik on 6/12/15.
 */
public class CommitHelper{

    static RepoHelper repoHelper;
    static ArrayList<ObjectId> parsedIds = new ArrayList<>();

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
        this.constructParents();
    }

    public CommitHelper(ObjectId id) throws IOException{
        this(parseInfo(id));
    }

    public CommitHelper(String refString) throws IOException{
        this(repoHelper.getRepo().resolve(refString));
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

    private void constructParents() throws IOException{
        if(parents == null){
            int count = this.commit.getParentCount();
            CommitHelper parent1 = null;
            CommitHelper parent2 = null;
            if(count > 0) parent1 = new CommitHelper(this.commit.getParent(0).getId());
            if(count > 1) parent2 = new CommitHelper(this.commit.getParent(1).getId());
            this.parents = new ParentCommitHelper(this, parent1, parent2);
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
        s = s + " - Parents: " + this.getParentCount();
        s = s + " - " + this.getMessage(false);

        return s;
    }

    public static RevCommit parseInfo(ObjectId id) throws IOException{
        if(parsedIds.contains(id)){
            // ??
        }
        RevWalk w = new RevWalk(repoHelper.getRepo());
        parsedIds.add(id);
        return w.parseCommit(id);
    }

    public static void setRepoHelper(RepoHelper newRepoHelper){
        repoHelper = newRepoHelper;
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
    }
}
