package main.java.elegit;

import javafx.collections.ObservableList;

import java.util.List;

/**
 * Created by grahamearley on 7/7/15.
 */
public class BranchManagerModel {

    private List<LocalBranchHelper> localBranches;
    private List<RemoteBranchHelper> remoteBranches;
    private RepoHelper repoHelper;

    public BranchManagerModel(List<LocalBranchHelper> localBranches, List<RemoteBranchHelper> remoteBranches, RepoHelper repoHelper) {
        this.localBranches = localBranches;
        this.remoteBranches = remoteBranches;
        this.repoHelper = repoHelper;
    }

    public List<LocalBranchHelper> getLocalBranches() {
        return this.localBranches;
    }

    public List<RemoteBranchHelper> getRemoteBranches() {
        return this.remoteBranches;
    }

    public void setLocalBranches(ObservableList<LocalBranchHelper> localBranches) {
        this.localBranches = localBranches;
    }
}
