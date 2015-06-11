package edugit;

import org.eclipse.jgit.lib.Repository;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by makik on 6/10/15.
 */
public class LocalPanelView extends TreePanelView{

    @Override
    public void drawTreeFromCurrentRepo(){
        Repository repo = this.model.currentRepoHelper.getRepo();

        try{
            ArrayList<String> messages = this.model.currentRepoHelper.getAllCommitMessages();
            for(String s : messages){
                System.out.println(s);
            }
        }catch(IOException e){
            e.printStackTrace();
        }
    }
}
