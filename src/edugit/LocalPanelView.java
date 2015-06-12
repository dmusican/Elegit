package edugit;

import org.eclipse.jgit.lib.Repository;

import java.io.IOException;

/**
 * Created by makik on 6/10/15.
 */
public class LocalPanelView extends TreePanelView{

    @Override
    public void drawTreeFromCurrentRepo(){
        Repository repo = this.model.currentRepoHelper.getRepo();

        try{
//            ArrayList<String> info = this.model.currentRepoHelper.getAllCommitsInfo();
//            for(String s : info){
//                System.out.println(s);
//            }

//            RevCommit head = this.model.currentRepoHelper.getCurrentHeadCommit();

            CommitHelper.setRepoHelper(this.model.currentRepoHelper);
            CommitHelper commitHelper = new CommitHelper("HEAD");
            printInfo(commitHelper);

        }catch(IOException e){
            e.printStackTrace();
        }
    }

    private void printInfo(CommitHelper commitHelper) throws IOException{
        System.out.println(commitHelper.getInfoString());
        for(int i = 0; i < commitHelper.getParentCount(); i++){
            printInfo(commitHelper.getParent(i));
        }
    }
}
