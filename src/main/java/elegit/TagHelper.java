package main.java.elegit;

import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevTag;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * A wrapper class for annotated tags to make them easier to interact with and preserves
 * certain aspects that are expensive to look up with JGit's standard RevTag, e.g. author,
 * tagMessage, etc.
 */
public class TagHelper{

    // The tag this helper wraps
    RevTag tag;
    // The author of this commit
    PersonIdent author;

    // The commit that this tag points to
    CommitHelper commit;

    // The name of this tag
    String tagName;
    // The short message of the tag
    String shortMessage;
    // The full message of the tag
    String fullMessage;

    // Whether the tag for this helper is lightweight or annotated
    boolean isAnnotated;

    public TagHelper(RevTag t, CommitHelper c) {
        this.tag = t;
        this.author = t.getTaggerIdent();
        this.tagName = t.getTagName();
        this.shortMessage = t.getShortMessage();
        this.fullMessage = t.getFullMessage();
        this.isAnnotated = true;
        this.commit = c;
    }

    public TagHelper (String name, CommitHelper c) {
        this.tagName = name;
        this.commit = c;
        this.isAnnotated = false;
    }

    /**
     * @return the name of the tag
     */
    public String getName(){
        return tag.getName();
    }

    /**
     * @return the unique ObjectId of the tag
     */
    public ObjectId getObjectId(){
        return this.tag.getId();
    }

    public int getType() { return this.tag.getType(); }

    /**
     * @param fullMessage whether to return the full or abbreviated tag message
     * @return the tag message
     */
    public String getMessage(boolean fullMessage){
        if(fullMessage){
            return this.fullMessage;
        }else{
            return this.shortMessage;
        }
    }

    /**
     * @return the name of the author of this tag
     */
    public String getAuthorName(){
        return author.getName();
    }

    /**
     * @return the email of the author of this tag
     */
    public String getAuthorEmail(){
        return author.getEmailAddress();
    }

    /**
     * @return the date object corresponding to the time of this tag
     */
    public Date getWhen(){
        return author.getWhen();
    }

    /**
     * @return the formatted date string corresponding to the time of this tag
     */
    public String getFormattedWhen(){
        DateFormat formatter = new SimpleDateFormat("MMM dd yyyy, h:mm a");
        return formatter.format(this.getWhen());
    }

    /**
     * @param c the commit helper this tag is associated with
     */
    public void setCommit(CommitHelper c) {
        this.commit = c;
    }

    public boolean isAnnotated() { return this.isAnnotated; }
}
