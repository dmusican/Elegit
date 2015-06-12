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

    public CommitHelper(RevCommit c) throws IOException{
        this.commit = c;
        this.author = c.getAuthorIdent();
    }

    public CommitHelper(ObjectId id) throws IOException{
        this.commit = parseInfo(id);
        this.author = commit.getAuthorIdent();
    }

    public CommitHelper(String refString) throws IOException{
        this(repoHelper.getRepo().resolve(refString));
    }

    public String getName(){
        return commit.getName();
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

    public int getParentCount(){
        if(commit.getParents() == null){
            try{
                this.commit = CommitHelper.parseInfo(commit.getId());
            }catch(Exception e){
                e.printStackTrace();
                return -1;
            }
        }
        return commit.getParentCount();
    }

    public ArrayList<CommitHelper> getParents() throws IOException{
        ArrayList<CommitHelper> parentList = new ArrayList<>();
        for(int i = 0; i < this.getParentCount(); i++){
            CommitHelper commitHelper = this.getParent(i);
            if(commitHelper != null){
                parentList.add(commitHelper);
            }
        }
        return parentList;
    }

    public CommitHelper getParent(int index) throws IOException{
        if(this.getParentCount() > 0){
            RevCommit parent = commit.getParent(index);
            if(parent.getParents() == null){
                return new CommitHelper(parent.getId());
            }else{
                System.out.println("I don't think this will ever happen, but if it does I want to know");
                return new CommitHelper(parent);
            }
        }
        return null;
    }

    public String getInfoString(){
        DateFormat formatter = new SimpleDateFormat("h:mm a MMM dd yyyy");

        String dateFormatted = formatter.format(this.getWhen());

        String s = this.getName();
        s = s + " - " + this.getName();
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
}
