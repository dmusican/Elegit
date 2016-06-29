package elegit;

import java.util.ArrayList;
import java.util.List;

/**
 * Class to hold updates for a commit tree model
 */
public class UpdateModel {
    public List<CommitHelper> commitsToAdd, commitsToRemove;
    // TODO: add branches and tags

    /**
     * Constructor without params, initializes lists
     */
    public UpdateModel() {
        this.commitsToAdd = new ArrayList<>();
        this.commitsToRemove = new ArrayList<>();
    }

    /**
     * Constructor with params
     * @param commitsToAdd the list of commits to add
     * @param commitsToRemove
     */
    public UpdateModel(List<CommitHelper> commitsToAdd, List<CommitHelper> commitsToRemove) {
        this.commitsToAdd = commitsToAdd;
        this.commitsToRemove = commitsToRemove;
    }

    public void addCommit(CommitHelper commitHelper) { this.commitsToAdd.add(commitHelper); }
    public void removeCommit(CommitHelper commitHelper) { this.commitsToRemove.add(commitHelper); }

    public List<CommitHelper> getCommitsToAdd() { return this.commitsToAdd; }
    public List<CommitHelper> getCommitsToRemove() { return this.commitsToRemove; }

    public void setCommitsToAdd(List<CommitHelper> commitsToAdd) { this.commitsToAdd = commitsToAdd; }
    public void setCommitsToRemove(List<CommitHelper> commitsToRemove) { this.commitsToRemove = commitsToRemove; }
}
