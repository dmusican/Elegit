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

    // THe name of this ref, e.g. 'master' or 'tag1'
    private final String refName;


    // The tag this helper wraps
    private RevTag tag;
    // The author of this commit
    private PersonIdent author;

    TagHelper(RevTag t, CommitHelper c) {
        this.tag = t;
        this.author = t.getTaggerIdent();
        this.refName = t.getTagName();
        this.commit = c;
    }

    TagHelper (String name, CommitHelper c) {
        this.refName = name;
        this.commit = c;
    }

    /**
     * @return the name of the ref
     */
    @Override
    public String getRefName() {
        return this.refName;
    }



    public int getType() { return this.tag.getType(); }

    /**
     * @return the date object corresponding to the time of this tag
     */
    public Date getWhen(){
        return author.getWhen();
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
