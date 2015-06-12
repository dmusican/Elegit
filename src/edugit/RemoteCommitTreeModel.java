package edugit;

import java.util.ArrayList;

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
    public ArrayList<CommitHelper> getCommits(){
        return new ArrayList<>();
    }
}
