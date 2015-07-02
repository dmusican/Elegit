package main.java.edugit;

import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
    protected List<CommitHelper> getNewCommits(RepoHelper repoHelper) throws GitAPIException, IOException{
        return repoHelper.getNewLocalCommits(this.branchMap);
    }

    @Override
    protected List<BranchHelper> getAllBranches(RepoHelper repoHelper){
        return new ArrayList<>(repoHelper.getLocalBranches());
    }
}
