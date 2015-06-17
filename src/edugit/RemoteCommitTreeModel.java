package edugit;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by makik on 6/12/15.
 *
 * Subclass of CommitTreeModel that examines remote commits
 */
public class RemoteCommitTreeModel extends CommitTreeModel{

    public RemoteCommitTreeModel(SessionModel model, CommitTreePanelView view){
        super(model, view);
    }

    @Override
    public List<CommitHelper> getCommits(){
        if(this.sessionModel.currentRepoHelper != null){
            return this.sessionModel.currentRepoHelper.getRemoteCommits();
        }
        return new ArrayList<>();
    }
}
