package edugit;

import java.util.ArrayList;

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
    public ArrayList<CommitHelper> getCommits(){
        if(this.model.currentRepoHelper != null){
            return this.model.currentRepoHelper.getLocalCommits();
        }
        return new ArrayList<>();
    }
}
