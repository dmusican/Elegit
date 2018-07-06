package elegit.models;

import elegit.Main;
import elegit.exceptions.ExceptionAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.api.DiffCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;


/**
 * Created by grenche on 6/19/18.
 * Model for the conflict management tool. Parses the documents.
 */
public class ConflictManagementModel {
    private static final Logger logger = LogManager.getLogger();

    public ArrayList<ArrayList> parseConflicts(String fileName, String filePathWithoutFileName, ArrayList<String> base, ArrayList<String> merged){
        Main.assertFxThread();
        String path = filePathWithoutFileName + File.separator + fileName;
        ArrayList<ConflictLine> left = new ArrayList<>();
        ArrayList<ConflictLine> middle = new ArrayList<>();
        ArrayList<ConflictLine> right = new ArrayList<>();
        ConflictLine leftConflict = new ConflictLine(false);
        ConflictLine middleConflict = new ConflictLine(false);
        ConflictLine rightConflict = new ConflictLine(false);
        try {
            BufferedReader reader = new BufferedReader(new FileReader(path));

            String line = reader.readLine();
            int leftCounter=0;
            int rightCounter=0;
            while(line!=null) {
                //System.out.println(" "+leftCounter+ " "+ rightCounter+" "+line+" "+base.get(leftCounter)+" "+merged.get(rightCounter));
                if(line.contains("<<<<<<<")) {
                    //line is the start of a conflict
                    left.add(leftConflict);
                    middle.add(middleConflict);
                    right.add(rightConflict);
                    line = reader.readLine();
                    leftConflict= new ConflictLine(true);
                    middleConflict= new ConflictLine(true);
                    rightConflict= new ConflictLine(true);
                    //left side of the conflict
                    while(!line.contains("=======")){
                        leftCounter++;
                        leftConflict.addLine(line);
                        line = reader.readLine();
                    }
                    line=reader.readLine();
                    //right side of the conflict
                    while(!line.contains(">>>>>>>")){
                        rightCounter++;
                        rightConflict.addLine(line);
                        line = reader.readLine();
                    }
                    left.add(leftConflict);
                    middle.add(middleConflict);
                    right.add(rightConflict);
                    leftConflict= new ConflictLine(false);
                    middleConflict= new ConflictLine(false);
                    rightConflict= new ConflictLine(false);
                    line = reader.readLine();
                } else if (changed(line, base, leftCounter, merged, rightCounter)){
                    //line is part of a changed segment
                    if(!middleConflict.isChanged()) {
                        //if this is the first line of the change, switch to a new ConflictLine
                        left.add(leftConflict);
                        middle.add(middleConflict);
                        right.add(rightConflict);
                        leftConflict = new ConflictLine(false);
                        middleConflict = new ConflictLine(false);
                        rightConflict = new ConflictLine(false);
                        leftConflict.setChangedStatus(true);
                        middleConflict.setChangedStatus(true);
                        rightConflict.setChangedStatus(true);
                    }
                    middleConflict.addLine(line);
                    //detect whether the change comes from left or right
                    if(leftCounter >= base.size()){
                        rightConflict.addLine(line);
                        rightCounter++;
                    } else if(line.equals(base.get(leftCounter)) || rightCounter >= merged.size()){
                        leftConflict.addLine(line);
                        leftCounter++;
                    } else if(line.equals(merged.get(rightCounter))){
                        rightConflict.addLine(line);
                        rightCounter++;
                    }
                    line = reader.readLine();
                } else {
                    //line is the same across both originals
                    if (middleConflict.isChanged()){
                        left.add(leftConflict);
                        middle.add(middleConflict);
                        right.add(rightConflict);
                        leftConflict = new ConflictLine(false);
                        middleConflict = new ConflictLine(false);
                        rightConflict = new ConflictLine(false);
                    }
                    middleConflict.addLine(line);
                    leftConflict.addLine(line);
                    rightConflict.addLine(line);
                    rightCounter++;
                    leftCounter++;
                    line = reader.readLine();
                }
            }
            left.add(leftConflict);
            middle.add(middleConflict);
            right.add(rightConflict);
        } catch (Exception e){
            e.printStackTrace();
            throw new ExceptionAdapter(e);
        }
        ArrayList<ArrayList> list = new ArrayList<>();
        list.add(left);
        list.add(middle);
        list.add(right);
        return list;
    }
    private CanonicalTreeParser getTreeId(ObjectId commitId, Repository repository){
        try {
            RevWalk revWalk = new RevWalk(repository);
            RevTree revTree = revWalk.parseCommit(commitId).getTree();
            ObjectId objectId = revTree.getId();
            ObjectReader reader = repository.newObjectReader();
            return new CanonicalTreeParser(null, reader, objectId);

        } catch (IOException e) {
            throw new ExceptionAdapter(e);
        }
    }

    private ArrayList<String> makeDiff(ObjectId baseId, ObjectId mergedId, String fileName){
        try {
            Repository repository = SessionModel.getSessionModel().getCurrentRepoHelper().getRepo();
            CanonicalTreeParser baseTree = getTreeId(baseId, repository);
            CanonicalTreeParser mergedTree = getTreeId(mergedId, repository);
            Git git = new Git(repository);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            git.diff()
                    .setOldTree(baseTree)
                    .setNewTree(mergedTree)
                    .setOutputStream(outputStream)
                    .setPathFilter(PathFilter.create(fileName))
                    .call();

            //using toString only works if the bytes were encrypted using ascii;
            //this is probably fine since they were made just now via jgit
            return new ArrayList<>(Arrays.asList(outputStream.toString().split("\n")));

        } catch (GitAPIException e){
            e.printStackTrace();
            throw new ExceptionAdapter(e);
        }
    }

    public void secondaryParse(ObjectId baseId, ObjectId mergedId, String fileName, String filePathWithoutFileName){
        ArrayList<String> original = makeDiff(baseId, mergedId, fileName);
        String path = filePathWithoutFileName + File.separator + fileName;
        System.out.println(original);
        try {
            BufferedReader reader = new BufferedReader(new FileReader(path));
            ArrayList<ConflictLine> left = new ArrayList<>();
            ArrayList<ConflictLine> middle = new ArrayList<>();
            ArrayList<ConflictLine> right = new ArrayList<>();
            ConflictLine leftConflict = new ConflictLine(false);
            ConflictLine middleConflict = new ConflictLine(false);
            ConflictLine rightConflict = new ConflictLine(false);
            //String line = reader.readLine();
            boolean leftChanged = false;
            boolean rightChanged = false;
            for(int i = 5 ; i < original.size(); i++){
                String line = original.get(i);
                Character initial = line.charAt(0);
                line = line.substring(1);
                //- = left, +=right
                if (initial == '+') {
                    rightChanged = true;
                    if (!middleConflict.isChanged() && !middleConflict.isConflicting()) {
                        left.add(leftConflict);
                        middle.add(middleConflict);
                        right.add(rightConflict);
                        leftConflict = new ConflictLine(false);
                        middleConflict = new ConflictLine(false);
                        rightConflict = new ConflictLine(false);
                        leftConflict.setChangedStatus(true);
                        middleConflict.setChangedStatus(true);
                        rightConflict.setChangedStatus(true);
                    }
                    if(leftChanged){
                        leftConflict.setChangedStatus(false);
                        middleConflict.setChangedStatus(false);
                        rightConflict.setChangedStatus(false);
                        leftConflict.setConflictStatus(true);
                        middleConflict.setConflictStatus(true);
                        rightConflict.setConflictStatus(true);
                    }
                    middleConflict.addLine(line);
                    rightConflict.addLine(line);
                } else if (initial == '-') {
                    leftChanged = true;
                    if (!middleConflict.isChanged() && !middleConflict.isConflicting()) {
                        left.add(leftConflict);
                        middle.add(middleConflict);
                        right.add(rightConflict);
                        leftConflict = new ConflictLine(false);
                        middleConflict = new ConflictLine(false);
                        rightConflict = new ConflictLine(false);
                        leftConflict.setChangedStatus(true);
                        middleConflict.setChangedStatus(true);
                        rightConflict.setChangedStatus(true);
                    }
                    if(rightChanged){
                        leftConflict.setChangedStatus(false);
                        middleConflict.setChangedStatus(false);
                        rightConflict.setChangedStatus(false);
                        leftConflict.setConflictStatus(true);
                        middleConflict.setConflictStatus(true);
                        rightConflict.setConflictStatus(true);
                    }
                    middleConflict.addLine(line);
                    leftConflict.addLine(line);

                } else if (initial != '\\') {
                    leftChanged = false;
                    rightChanged = false;
                    if (middleConflict.isChanged() || middleConflict.isConflicting()) {
                        left.add(leftConflict);
                        middle.add(middleConflict);
                        right.add(rightConflict);
                        leftConflict = new ConflictLine(false);
                        middleConflict = new ConflictLine(false);
                        rightConflict = new ConflictLine(false);
                    }
                    leftConflict.addLine(line);
                    middleConflict.addLine(line);
                    rightConflict.addLine(line);

                }
                //System.out.println(line.substring(1));
            }
            left.add(leftConflict);
            middle.add(middleConflict);
            right.add(rightConflict);
            for (ConflictLine conflictLine : left){
                System.out.println(conflictLine.isChanged()+" "+conflictLine.isConflicting());
                for(String line : conflictLine.getLines()){
                    System.out.println(line);
                }
            }
            for (ConflictLine conflictLine : middle){
                System.out.println(conflictLine.isChanged()+" "+conflictLine.isConflicting());
                for(String line : conflictLine.getLines()){
                    System.out.println(line);
                }
            }
            for (ConflictLine conflictLine : right){
                System.out.println(conflictLine.isChanged()+" "+conflictLine.isConflicting());
                for(String line : conflictLine.getLines()){
                    System.out.println(line);
                }
            }
            ArrayList<ArrayList> list = new ArrayList<>();
            list.add(left);
            list.add(middle);
            list.add(right);
            //return list;
        } catch (IOException e){
            throw new ExceptionAdapter(e);
        }


    }
    private boolean changed(String line, ArrayList<String> base, int leftCounter, ArrayList<String> merged, int rightCounter){
        if ((base.size() >leftCounter) && (merged.size()>rightCounter)) {
            return (!line.equals(base.get(leftCounter)) || !line.equals(merged.get(rightCounter)));
        }
        return true;
    }
}



