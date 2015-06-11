package edugit;

import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;

import java.util.Map;

/**
 * Created by makik on 6/10/15.
 */
public class LocalPanelView extends TreePanelView{

    @Override
    public void drawTreeFromCurrentRepo(){
        Repository repo = this.model.currentRepoHelper.getRepo();

        Map<String, Ref> map = repo.getAllRefs();
        System.out.println(map);
    }
}
