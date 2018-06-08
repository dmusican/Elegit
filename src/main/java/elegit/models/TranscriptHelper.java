package elegit.models;

import elegit.controllers.CommandLineController;

import java.io.*;

/**
 * Created by gorram on 6/6/18.
 */
public class TranscriptHelper {

    //private CommandLineController commandLineController;

    public TranscriptHelper(CommandLineController commandLineController) {
        //this.commandLineController = commandLineController;
    }

    public static void post(String command) {
        String log_file_path = System.getProperty("logFolder") + "/transcript.log";

        File transcript = new File(log_file_path);
        System.out.println("Before writing.");
        System.out.println(transcript.isFile());
        System.out.println(transcript.canRead());
        System.out.println(transcript.canWrite());

        Writer output;
        try {
            //output = new BufferedWriter(new FileWriter(log_file_path, true));
            output = new FileWriter(log_file_path, true);
            output.append(command + "\n");
            output.close();
        } catch (IOException e) {

        }
        System.out.println("Command: " + command);

        System.out.println("After writing.");
        System.out.println(transcript.isFile());
        System.out.println(transcript.canRead());
        System.out.println(transcript.canWrite());

        //sendCommand(command);
    }

    public static void clear() {
        System.out.println("Transcript is being cleared.");
        String log_file_path = System.getProperty("logFolder") + "/transcript.log";

        try {
            FileWriter writer = new FileWriter(log_file_path, false);
            writer.flush();
            writer.close();
        } catch (IOException e) {

        }
    }

    //private void sendCommand(String command) {
     //   this.commandLineController.updateCommandText(command);
    //}
}
