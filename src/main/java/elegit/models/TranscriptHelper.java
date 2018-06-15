package elegit.models;

import java.io.*;

/**
 * Created by gorram on 6/6/18.
 */
public class TranscriptHelper {

    public static void post(String command) {
        String log_file_path = System.getProperty("logFolder") + "/transcript.log";

        BufferedWriter output;
        try {
            output = new BufferedWriter(new FileWriter(log_file_path, true));
            output.append(command);
            output.newLine();
            output.close();
        } catch (IOException e) {
            System.out.println(e);
        }
}

    public static void clear() {
        System.out.println("Transcript is being cleared.");
        String log_file_path = System.getProperty("logFolder") + "/transcript.log";

        try {
            FileWriter writer = new FileWriter(log_file_path, false);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            System.out.println(e);
        }
    }
}
