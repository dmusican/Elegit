package edugit;

import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Subclass of CommitTreeModel that examines remote commits
 */
public class RemoteCommitTreeModel extends CommitTreeModel{

    public RemoteCommitTreeModel(SessionModel model, CommitTreePanelView view){
        super(model, view);
        this.view.setName("Remote commit tree");
    }

    @Override
    protected List<CommitHelper> getAllCommits(){
        if(this.sessionModel.currentRepoHelper != null){
            return this.sessionModel.currentRepoHelper.getRemoteCommits();
        }
        return new ArrayList<>();
    }

    @Override
    protected List<CommitHelper> getNewCommits() throws GitAPIException, IOException{
        if(this.sessionModel.currentRepoHelper != null){
            return this.sessionModel.currentRepoHelper.getNewRemoteCommits();
        }
        return new ArrayList<>();
    }
}
