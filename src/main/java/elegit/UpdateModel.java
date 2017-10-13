package elegit;

import elegit.models.BranchHelper;
import elegit.models.CommitHelper;
import elegit.models.TagHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Class to hold updates for a commit tree model
 */
public class UpdateModel {
    private List<CommitHelper> commitsToAdd, commitsToRemove, commitsToUpdate;
    private List<BranchHelper> branchesToUpdate;
    private List<TagHelper> tagsToUpdate;

    /**
     * Constructor without params, initializes lists
     */
    public UpdateModel() {
        this.commitsToAdd = new ArrayList<>();
        this.commitsToRemove = new ArrayList<>();
        this.commitsToUpdate = new ArrayList<>();
        this.branchesToUpdate = new ArrayList<>();
        this.tagsToUpdate = new ArrayList<>();
    }

    /**
     * Method to see if there are changes in the update model
     * @return if there are any changes that need to be processed
     */
    public boolean hasChanges() {
        return this.commitsToAdd.size()+this.commitsToRemove.size()+this.commitsToUpdate.size()
                +this.branchesToUpdate.size()+this.tagsToUpdate.size()>0;
    }

    public void addCommit(CommitHelper commitHelper) { this.commitsToAdd.add(commitHelper); }
    public void removeCommit(CommitHelper commitHelper) { this.commitsToRemove.add(commitHelper); }
    void updateCommits(List<CommitHelper> commits) { this.commitsToUpdate.addAll(commits); }
    void addBranch(BranchHelper branch) { this.branchesToUpdate.add(branch); }
    void addTag(TagHelper tag) { this.tagsToUpdate.add(tag); }


    // ********************* GETTERS AND SETTERS ************************

    List<CommitHelper> getCommitsToAdd() { return this.commitsToAdd; }
    List<CommitHelper> getCommitsToRemove() { return this.commitsToRemove; }
    List<CommitHelper> getCommitsToUpdate() { return this.commitsToUpdate; }

    void setCommitsToAdd(List<CommitHelper> commitsToAdd) { this.commitsToAdd = commitsToAdd; }
    void setCommitsToRemove(List<CommitHelper> commitsToRemove) { this.commitsToRemove = commitsToRemove; }
    void setCommitsToUpdate(List<CommitHelper> commitsToUpdate) { this.commitsToUpdate = commitsToUpdate; }

    public List<BranchHelper> getBranchesToUpdate() { return this.branchesToUpdate; }

    public void setBranchesToUpdate(List<BranchHelper> branchesToUpdate) { this.branchesToUpdate = branchesToUpdate; }
}
