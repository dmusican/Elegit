package elegit;

import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Subclass of CommitTreeModel that examines local commits
 */
public class LocalCommitTreeModel extends CommitTreeModel{

    public LocalCommitTreeModel(SessionModel model, CommitTreePanelView view){
        super(model, view);
        this.view.setName("Local commit tree");
    }

    @Override
    protected List<CommitHelper> getAllCommits(RepoHelper repoHelper) {
        return repoHelper.getLocalCommits();
    }

    @Override
    protected List<CommitHelper> getNewCommits(RepoHelper repoHelper, Map<String, BranchHelper> oldBranches) throws GitAPIException, IOException{
        return repoHelper.getNewLocalCommits(oldBranches);
    }

    @Override
    protected List<BranchHelper> getAllBranches(RepoHelper repoHelper){
        return repoHelper.getLocalBranches();
    }
}
