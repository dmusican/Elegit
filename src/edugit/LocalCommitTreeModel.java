package edugit;

import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;
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
        this.view.setName("Local commit tree");
    }

    @Override
    protected List<CommitHelper> getAllCommits(){
        if(this.sessionModel.currentRepoHelper != null){
            return this.sessionModel.currentRepoHelper.getLocalCommits();
        }
        return new ArrayList<>();
    }

    @Override
    protected List<CommitHelper> getNewCommits() throws GitAPIException, IOException{
        if(this.sessionModel.currentRepoHelper != null){
            return this.sessionModel.currentRepoHelper.getNewLocalCommits();
        }
        return new ArrayList<>();
    }
}
