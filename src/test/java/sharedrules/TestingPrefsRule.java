package sharedrules;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.api.Git;
import org.junit.rules.ExternalResource;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TestingPrefsRule extends ExternalResource {

    private Path directoryPath;

    private static final Logger console = LogManager.getLogger("briefconsolelogger");

    private static boolean deleteTempFiles = true;


    @Override
    protected void before() throws Exception {
        directoryPath = Files.createTempDirectory("tempPrefs");
        if (deleteTempFiles) {
            directoryPath.toFile().deleteOnExit();
        }


    }

    @Override
    protected void after()  {
        if (deleteTempFiles) {
            removeAllFilesFromDirectory(directoryPath.toFile());
        }
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
