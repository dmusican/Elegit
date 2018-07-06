package elegit.models;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;

/**
 * Created by gorram on 6/6/18.
 */
public class TranscriptHelper {
    private static final Logger logger = LogManager.getLogger();
    private static final Logger console = LogManager.getLogger("briefconsolelogger");


    public static void post(String command) {
        String log_file_path = System.getProperty("logFolder") + "/transcript.log";

        BufferedWriter output;
        try {
            output = new BufferedWriter(new FileWriter(log_file_path, true));
            output.append(command);
            output.newLine();
            output.close();
        } catch (IOException e) {
            logger.info(e);
        }
}

    public static void clear() {
        console.info("Transcript is being cleared.");
        String log_file_path = System.getProperty("logFolder") + "/transcript.log";

        try {
            FileWriter writer = new FileWriter(log_file_path, false);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            logger.info(e);
        }
    }
}
