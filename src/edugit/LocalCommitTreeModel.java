package edugit;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by makik on 6/12/15.
 *
 * Subclass of CommitTreeModel that examines local commits
 */
public class LocalCommitTreeModel extends CommitTreeModel{

    public LocalCommitTreeModel(SessionModel model, CommitTreePanelView view){
        super(model, view);
    }

    @Override
    protected List<CommitHelper> getCommits(){
        if(this.sessionModel.currentRepoHelper != null){
            return this.sessionModel.currentRepoHelper.getLocalCommits();
        }
        return new ArrayList<>();
    }
}
