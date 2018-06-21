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

        try {
            BufferedReader reader = new BufferedReader(new FileReader(path));
            String line = reader.readLine();
            while(line!=null){
                if(line.contains("<<<<<<<")){
                    line = reader.readLine();
                    while(!line.contains("=======")){
                        left.add(new ConflictLine(line, true));
                        line = reader.readLine();
                    }
                    line = reader.readLine();
                    while(!line.contains(">>>>>>>")){
                        right.add(new ConflictLine(line, true));
                        line = reader.readLine();
                    }
                    line = reader.readLine();
                }
                else if(line.contains("<<===<<")){
                    baseBranch = reader.readLine();
                    mergedBranch = reader.readLine();
                    line = reader.readLine();
                    line = reader.readLine();
                }
                else{
                    left.add(new ConflictLine(line, false));
                    middle.add(new ConflictLine(line, false));
                    right.add(new ConflictLine(line, false));
                    line = reader.readLine();
                }
            }
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
