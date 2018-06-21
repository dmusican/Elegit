package elegit.models;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;



/**
 * Created by grenche on 6/19/18.
 * Model for the conflict management tool. Parses the documents.
 */
public class ConflictManagementModel {
    private static final Logger logger = LogManager.getLogger();
    private static String baseBranch;
    private static String mergedBranch;

    public ArrayList<ArrayList> parseConflicts(String path){
        logger.info("Parsing the file.");

        ArrayList<ConflictLine> left = new ArrayList<>();
        ArrayList<ConflictLine> middle = new ArrayList<>();
        ArrayList<ConflictLine> right = new ArrayList<>();
        ConflictLine noConflict = new ConflictLine(false);
        try {
            BufferedReader reader = new BufferedReader(new FileReader(path));

            String line = reader.readLine();
            while(line!=null){
                if(line.contains("<<<<<<<")){
                    left.add(noConflict);
                    middle.add(noConflict);
                    right.add(noConflict);
                    line = reader.readLine();
                    ConflictLine leftConflict = new ConflictLine(true);
                    while(!line.contains("=======")){
                        leftConflict.addLine(line);
                        line = reader.readLine();
                    }
                    left.add(leftConflict);
                    line = reader.readLine();
                    ConflictLine rightConflict = new ConflictLine(true);
                    while(!line.contains(">>>>>>>")){
                        rightConflict.addLine(line);
                        line = reader.readLine();
                    }
                    right.add(rightConflict);
                    line = reader.readLine();
                    middle.add(new ConflictLine(true));
                    noConflict = new ConflictLine(false);
                }
                else if(line.contains("<<===<<")){
                    baseBranch = reader.readLine();
                    mergedBranch = reader.readLine();
                    reader.readLine();
                    line = reader.readLine();
                }
                else{
                    noConflict.addLine(line);
                    line = reader.readLine();
                }
            }
            left.add(noConflict);
            middle.add(noConflict);
            right.add(noConflict);
        }
        catch (IOException e){
            logger.info(e);
            e.printStackTrace();
        }
        ArrayList<ArrayList> list = new ArrayList<>();
        list.add(left);
        list.add(middle);
        list.add(right);
        return list;
    }
    public String getBaseBranch(){
        return baseBranch;
    }
    public String getMergedBranch(){
        return mergedBranch;
    }
}
