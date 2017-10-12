package elegit;

import elegit.models.CommitHelper;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevTag;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;

/**
 * A wrapper class for annotated tags to make them easier to interact with and preserves
 * certain aspects that are expensive to look up with JGit's standard RevTag, e.g. author,
 * tagMessage, etc.
 */
public class TagHelper extends RefHelper{

    // The tag this helper wraps
    private RevTag tag;
    // The author of this commit
    private PersonIdent author;

    // The short message of the tag
    private String shortMessage;
    // The full message of the tag
    private String fullMessage;

    // Whether the tag for this helper is lightweight or annotated
    private boolean isAnnotated;

    TagHelper(RevTag t, CommitHelper c) {
        this.tag = t;
        this.author = t.getTaggerIdent();
        this.refName = t.getTagName();
        this.shortMessage = t.getShortMessage();
        this.fullMessage = t.getFullMessage();
        this.isAnnotated = true;
        this.commit = c;
    }

    TagHelper (String name, CommitHelper c) {
        this.refName = name;
        this.commit = c;
        this.isAnnotated = false;
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

    public String getCommitId() {
        return this.commit.getName();
    }

    public boolean isAnnotated() { return this.isAnnotated; }

    boolean presentDeleteDialog() {
        //Create the dialog
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Delete Tag");
        dialog.setHeaderText("Are you sure you want to delete tag "+ refName +"?");

        ButtonType confirm = new ButtonType("Yes", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(confirm, ButtonType.CANCEL);

        dialog.setResultConverter(dialogButton -> dialogButton == confirm);

        Optional<Boolean> result = dialog.showAndWait();

        return result.orElse(false);
    }
}
