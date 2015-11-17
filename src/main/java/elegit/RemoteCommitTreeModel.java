package elegit;

import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Subclass of elegit.CommitTreeModel that examines remote commits
 */
public class RemoteCommitTreeModel extends CommitTreeModel{

    public RemoteCommitTreeModel(SessionModel model, CommitTreePanelView view){
        super(model, view);
        this.view.setName("Remote commit tree");
    }

    @Override
    protected List<CommitHelper> getAllCommits(RepoHelper repoHelper) {
        return repoHelper.getRemoteCommits();
    }

    @Override
    protected List<CommitHelper> getNewCommits(RepoHelper repoHelper, Map<String, BranchHelper> oldBranches) throws GitAPIException, IOException{
        return repoHelper.getNewRemoteCommits(oldBranches);
    }

    @Override
    protected List<BranchHelper> getAllBranches(RepoHelper repoHelper){
        return repoHelper.getRemoteBranches();
    }
}
