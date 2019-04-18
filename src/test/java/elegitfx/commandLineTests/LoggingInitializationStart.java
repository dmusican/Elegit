package elegitfx.commandLineTests;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.rules.ExternalResource;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by grenche on 6/13/18.
 * Used to avoid creating and logging in the "${sys:logFolder}", but rather send everything to the "logs" folder
 * Required for tests that don't go through Main (where this is already done immediately in a static block) and use
 * loggers.
 */
public class LoggingInitializationStart extends ExternalResource {
    static {
        // -----------------------Logging Initialization Start---------------------------
        Path logPath = Paths.get("logs");
        String s = logPath.toAbsolutePath().toString();
        System.setProperty("logFolder", s);
    }
}
