package elegit.models;

import elegit.Main;
import javafx.beans.property.*;
import org.apache.http.HttpEntity;
import net.jcip.annotations.GuardedBy;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.prefs.BackingStoreException;

public class LoggingModel {

    private static final String LOGGING_LEVEL_KEY="LOGGING_LEVEL";
    private static final String LAST_UUID_KEY="LAST_UUID";
    private static final Logger logger = LogManager.getLogger();
    @GuardedBy("this") private static final BooleanProperty loggingStatus = new SimpleBooleanProperty(false);

    private static final String submitUrl = "http://elegit.mathcs.carleton.edu/logging/upload.php";
    private static final String LOG_FILE_NAME = "elegit.log";
    private static final String TRANSCRIPT_FILE_NAME = "transcript.log";

    public synchronized static void bindLogging(ReadOnlyBooleanProperty status) {
        loggingStatus.bind(status);
    }

    public static void submitLog() {
        try {
            String lastUUID = getLastUUID();
            setLastUUID(submitData(lastUUID));
        } catch (BackingStoreException | ClassNotFoundException | IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            try { setLastUUID(""); }
            catch (Exception f) { // This shouldn't happen
            }
        }
    }

    /**
     * Once files are uploaded to the server, we have a UUID to remember for the next
     * upload of files.
     */
    private static void setLastUUID(String uuid) throws BackingStoreException, ClassNotFoundException, IOException {
        PrefObj.putObject(Main.preferences, LAST_UUID_KEY, uuid);
    }

    /**
     * To upload a file to the server, we need to find the last uuid
     */
    private static String getLastUUID() throws BackingStoreException, ClassNotFoundException, IOException {
        return (String) PrefObj.getObject(Main.preferences, LAST_UUID_KEY);
    }


    /**
     * Helper method to change whether or not this session is logging, also
     * stores this in preferences
     * @param level the level to set the logging to
     */
    public static void changeLogging(Level level) {
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Configuration config = ctx.getConfiguration();
        LoggerConfig loggerConfig = config.getLoggerConfig(LogManager.ROOT_LOGGER_NAME);
        loggerConfig.setLevel(level);
        ctx.updateLoggers();

        setLoggingLevelPref(level);
    }

    public static Level getLoggingLevel() {
        try {
            return (Level) PrefObj.getObject(Main.preferences, LOGGING_LEVEL_KEY);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean loggingOn() {
        Level level = getLoggingLevel();
        return (level != null && level.equals(Level.INFO));
    }

    private static void setLoggingLevelPref(Level level) {
        try {
            PrefObj.putObject(Main.preferences, LOGGING_LEVEL_KEY, level);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized static void toggleLogging() {
        if (loggingStatus.get()) {
            changeLogging(Level.INFO);
            logger.log(Level.INFO, "Toggled logging on");
        } else {
            changeLogging(Level.OFF);
        }
    }

    // Since this deletes a local file, which has a fixed name, this is synchronized to ensure
    // it doesn't get run more than once simultaneously
    private synchronized static String submitData(String uuid) {
        // If logging is not enabled, do not upload anything, even if the log file has something in it
        if (!loggingStatus.get()) {
            return null;
        }

        logger.info("Submit data called");
        String logPath = Paths.get("logs").toString();

        File logDirectory = new File(logPath);
        File[] logsToUpload=logDirectory.listFiles();

        String lastUUID="";
        if (uuid==null || uuid.equals("")) {
            uuid= UUID.randomUUID().toString();
            logger.info("Making a new uuid.");
        }

        if (logsToUpload==null)
            logsToUpload = new File[0];
        for (File logFile: logsToUpload) {
            if (!logFile.isFile() || logFile.getName().equals(LOG_FILE_NAME) || logFile.getName().equals(TRANSCRIPT_FILE_NAME)) {
                if (logsToUpload.length == 1) {
                    logger.info("No new logs to upload today");
                    break;
                }
            }

            // Move the file to a uuid filename for upload to the server.
            try {
                logFile = Files.copy(logFile.toPath(), logFile.toPath().resolveSibling(uuid+".log")).toFile();
            } catch (IOException e) {
                e.printStackTrace();
            }

            logger.info("Attempting to upload log: {}",logFile.getName());
            CloseableHttpClient httpclient = HttpClients.createDefault();
            try {
                HttpPost httppost = new HttpPost(submitUrl);

                MultipartEntityBuilder builder = MultipartEntityBuilder.create();
                builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
                FileBody fileBody = new FileBody(logFile);

                builder.addPart("fileToUpload",fileBody);
                HttpEntity builtEntity = builder.build();

                httppost.setEntity(builtEntity);

                logger.info(httppost.getRequestLine());
                CloseableHttpResponse response = httpclient.execute(httppost);
                try {
                    logger.info("Executing request: " + response.getStatusLine());
                    logger.info(EntityUtils.toString(response.getEntity()));
                    lastUUID=uuid;
                } catch (Exception e) {
                    logger.error("Response status check failed.");
                    response.close();
                    return null;
                }
            } catch (Exception e) {
                logger.error("Failed to execute request. Attempting to close client.");
                try {
                    httpclient.close();
                } catch (Exception f) {
                    logger.error("Failed to close client.");
                    return null;
                }
                return null;
            }
            // Delete the log file as we might be uploading more!
            if (!logFile.delete()) {
                logger.error("Failed to delete log file.");
            }

        }
        // Clean up the directory
        File[] logsToDelete = logDirectory.listFiles();
        if (logsToDelete == null)
            logsToDelete = new File[0];
        for (File file: logsToDelete) {
            if (!file.getName().equals(LOG_FILE_NAME) && !file.getName().equals(TRANSCRIPT_FILE_NAME))
                if (!file.delete()) {
                    logger.error("Failed to delete a file in the log directory.");
                }
        }

        return lastUUID;
    }

}
