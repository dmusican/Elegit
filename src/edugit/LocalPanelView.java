package edugit;

import edugit.treefx.Layout;
import edugit.treefx.TreeGraph;
import edugit.treefx.TreeGraphModel;
import edugit.treefx.TreeLayout;
import javafx.scene.control.ScrollPane;
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

            beginAddCommitsToTree(commitHelper);

            Layout layout = new TreeLayout(treeGraph);
            layout.execute();

        }catch(IOException e){
            e.printStackTrace();
        }
    }

    private void beginAddCommitsToTree(CommitHelper commitHelper){
        TreeGraphModel graphModel = new TreeGraphModel(commitHelper.getName()+" "+commitHelper.getMessage(false));
        treeGraph = new TreeGraph(graphModel);

        ScrollPane sp = treeGraph.getScrollPane();
        sp.setPannable(true);
        sp.setPrefSize(200, 600);
        this.getChildren().add(sp);

        treeGraph.beginUpdate();
        for(CommitHelper next : commitHelper.getParents()){
            this.addCommitsToTree(next, commitHelper, graphModel);
        }
        treeGraph.endUpdate();
    }

    private void addCommitsToTree(CommitHelper commitHelper, CommitHelper parent, TreeGraphModel graphModel){
        graphModel.addCell(commitHelper.getName()+" "+commitHelper.getMessage(false),parent.getName()+" "+parent.getMessage(false));
        for(CommitHelper next : commitHelper.getParents()){
            addCommitsToTree(next, commitHelper, graphModel);
        }
    }

    private void printInfo(CommitHelper commitHelper) throws IOException{
        System.out.println(commitHelper.getInfoString());
        for(int i = 0; i < commitHelper.getParentCount(); i++){
            printInfo(commitHelper.getParent(i));
        }
    }
}
