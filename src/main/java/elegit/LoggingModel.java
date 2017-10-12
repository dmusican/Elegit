package elegit;

import elegit.PrefObj;
import javafx.beans.property.*;
import org.apache.http.annotation.GuardedBy;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;

import java.io.IOException;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

// TODO: Make sure that PrefObj is threadsafe, also DataSubmitter
// TODO: Make sure that this class if threadsafe
public class LoggingModel {

    private static final Preferences preferences = Preferences.userNodeForPackage(LoggingModel.class);
    private static final String LOGGING_LEVEL_KEY="LOGGING_LEVEL";
    private static final String LAST_UUID_KEY="LAST_UUID";
    private static final DataSubmitter d = new DataSubmitter();
    private static final Logger logger = LogManager.getLogger();
    @GuardedBy("this") private static final BooleanProperty loggingStatus = new SimpleBooleanProperty(false);

    public synchronized static void bindLogging(ReadOnlyBooleanProperty status) {
        loggingStatus.bind(status);
    }

    public static void submitLog() {
        try {
            String lastUUID = getLastUUID();
            setLastUUID(d.submitData(lastUUID));
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
        PrefObj.putObject(preferences, LAST_UUID_KEY, uuid);
    }

    /**
     * To upload a file to the server, we need to find the last uuid
     */
    private static String getLastUUID() throws BackingStoreException, ClassNotFoundException, IOException {
        return (String) PrefObj.getObject(preferences, LAST_UUID_KEY);
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
            return (Level) PrefObj.getObject(preferences, LOGGING_LEVEL_KEY);
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
            PrefObj.putObject(preferences, LOGGING_LEVEL_KEY, level);
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


}
