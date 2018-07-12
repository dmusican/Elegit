package elegit.models;

import elegit.Main;
import elegit.exceptions.ExceptionAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;

/**
 * Created by gorram on 6/6/18.
 * Given a terminal as a command, this class posts it to a log so the user can export their history of terminal
 * commands or simply view it. It also clears the log.
 */
public class TranscriptHelper {
    private static final Logger logger = LogManager.getLogger();
    private static final Logger console = LogManager.getLogger("briefconsolelogger");


    public static void post(String command) {
        Main.assertFxThread();
        String log_file_path = System.getProperty("logFolder") + "/transcript.log";

        BufferedWriter output;
        try {
            output = new BufferedWriter(new FileWriter(log_file_path, true));
            output.append(command);
            output.newLine();
            output.close();
        } catch (IOException e) {
            throw new ExceptionAdapter(e);
        }
}

    public static void clear() {
        Main.assertFxThread();
        console.info("Transcript is being cleared.");
        String log_file_path = System.getProperty("logFolder") + "/transcript.log";

        try {
            FileWriter writer = new FileWriter(log_file_path, false);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            throw new ExceptionAdapter(e);
        }
    }
}
