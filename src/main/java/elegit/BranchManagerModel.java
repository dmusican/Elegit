package elegit;

import javafx.collections.ObservableList;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;
import java.util.List;

public class BranchManagerModel {

    private List<LocalBranchHelper> localBranches;
    private List<RemoteBranchHelper> remoteBranches;
    private RepoHelper repoHelper;

    public BranchManagerModel(List<LocalBranchHelper> localBranches,
                              List<RemoteBranchHelper> remoteBranches,
                              RepoHelper repoHelper) {
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

    public void setLocalBranches(ObservableList<LocalBranchHelper>
                                         localBranches) {

        this.localBranches = localBranches;
    }

    /* if gitFetch is called, the remote branch list in branch manager should
     * update accordingly by calling this method */
    public List<RemoteBranchHelper> getUpdatedRemoteBranches()
            throws GitAPIException, IOException {

        this.remoteBranches = this.repoHelper.getListOfRemoteBranches();
        return remoteBranches;
    }
}
