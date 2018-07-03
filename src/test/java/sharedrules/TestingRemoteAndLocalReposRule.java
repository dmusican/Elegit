package sharedrules;

import elegit.models.SessionModel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.api.Git;
import org.junit.rules.ExternalResource;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TestingRemoteAndLocalReposRule extends ExternalResource {

    private Path directoryPath;

    private static final Logger console = LogManager.getLogger("briefconsolelogger");

    private boolean bareRemoteRepo;
    private static boolean deleteTempFiles = true;

    public TestingRemoteAndLocalReposRule(boolean bareRemoteRepo) {
        this.bareRemoteRepo = bareRemoteRepo;
    }

    public static void doNotDeleteTempFiles() {
        deleteTempFiles = false;
    }

    @Override
    protected void before() throws Exception {
        System.err.println("Helper remoteAndLocal: "+ SessionModel.getSessionModel().getCurrentRepoHelper());

        directoryPath = Files.createTempDirectory("unitTestRepos");
        if (deleteTempFiles) {
            directoryPath.toFile().deleteOnExit();
        }

        // Create a bare repo on the remote to be cloned.
        Git remoteHandle = Git.init().setDirectory(getRemoteFull().toFile()).setBare(bareRemoteRepo).call();

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

    public Path getDirectoryPath() {
        return directoryPath;
    }

    public Path getRemoteFull() {
        return directoryPath.resolve("remote");
    }

    public Path getRemoteBrief() {
        return Paths.get("remote");
    }

    public Path getLocalFull() {
        return directoryPath.resolve("local");
    }

    public Path getLocalBrief() {
        return Paths.get("local");
    }

}
