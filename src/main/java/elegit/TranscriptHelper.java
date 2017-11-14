package elegit;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

public class TranscriptHelper {
    public static void post(String command) {
        String log_file_path = System.getProperty("logFolder") + "/transcript.log";

        Writer output;
        try {
            output = new BufferedWriter(new FileWriter(log_file_path, true));
            output.append(command + "\n");
            output.close();
        } catch (IOException e) {

        }
    }

    public static void clear() {
        String log_file_path = System.getProperty("logFolder") + "/transcript.log";

        try {
            FileWriter writer = new FileWriter(log_file_path, false);
            writer.flush();
            writer.close();
        } catch (IOException e) {

        }
    }
}
