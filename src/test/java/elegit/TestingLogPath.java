package elegit;

import elegit.exceptions.ExceptionAdapter;
import org.junit.rules.ExternalResource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Before each test that uses this rule, move the execution logs to a temporary location so as not to
 * get in the way of actual logs generated in production.
 */
public class TestingLogPath extends ExternalResource {

    private static Path logPath;

    static {
        try {
            logPath = Files.createTempDirectory("elegitLogs");
        } catch (IOException e) {
            throw new ExceptionAdapter(e);
        }
        logPath.toFile().deleteOnExit();
        System.setProperty("logFolder", logPath.toString());
    }

    @Override
    protected void after()  {
        removeAllFilesFromDirectory(logPath.toFile());

    }

    private void removeAllFilesFromDirectory(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) removeAllFilesFromDirectory(file);
                file.delete();
            }
        }
    }


}
