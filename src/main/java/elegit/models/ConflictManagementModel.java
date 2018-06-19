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

    public static ArrayList<ArrayList> parseConflicts(String path){
        logger.info("Parsing the file.");

        ArrayList<String> left = new ArrayList<>();
        ArrayList<String> center = new ArrayList<>();
        ArrayList<String> right = new ArrayList<>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(path));
            String line = reader.readLine();
            while(line!=null){
                if(line.contains("<<<<<<<")){
                    line = reader.readLine();
                    while(!line.contains("=======")){
                        left.add(line);
                        line = reader.readLine();
                    }
                    line = reader.readLine();
                    while(!line.contains(">>>>>>>")){
                        right.add(line);
                        line = reader.readLine();
                    }
                    line = reader.readLine();
                }
                else{
                    left.add(line);
                    center.add(line);
                    right.add(line);
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
        list.add(center);
        list.add(right);
        return list;
    }
}
