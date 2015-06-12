package edugit;

/**
 * Created by makik on 6/10/15.
 */
public class LocalPanelView extends TreePanelView{

    @Override
    public void drawTreeFromCurrentRepo(){
        RepoHelper repoHelper = this.model.currentRepoHelper;

        this.addCommitsToTree(repoHelper.getLocalCommits());

        this.displayTreeGraph();
    }
}
