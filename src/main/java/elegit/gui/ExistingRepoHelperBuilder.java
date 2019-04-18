package elegit.gui;

import elegit.Main;
import elegit.exceptions.ExceptionAdapter;
import elegit.exceptions.NoRepoSelectedException;
import elegit.models.ExistingRepoHelper;
import elegit.models.RepoHelper;
import elegit.sshauthentication.ElegitUserInfoGUI;
import io.reactivex.Single;
import net.jcip.annotations.ThreadSafe;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

/**
 *
 * An implementation of the abstract RepoHelperBuilder that prompts the
 * user (using dialogs) for the parameters required to build an ExistingRepoHelper.
 *
 */
@ThreadSafe
// all methods execute on FX thread anyway. If something comes off, verify thread safety
public class ExistingRepoHelperBuilder extends RepoHelperBuilder {
    private static Path directoryPath;

    /**
     * Standard constructor
     */
    public ExistingRepoHelperBuilder() {
        super();
    }

    /**
     * Specify repoPath in advance; designed only for testing purposes
     * @param repoPath
     */
    public ExistingRepoHelperBuilder(Path repoPath) {
        super(repoPath);
        Main.assertFxThread();
    }

    /**
     * Shows a file chooser dialog and makes the ExistingRepoHelper from it.
     *  (The ExistingRepoHelper is generalized into a RepoHelper so that all
     *   RepoHelpers can be stored in the same list and acted on uniformly).
     *
     * @throws Exception why? has to do with the new ExistingRepoHelper(...).
     */
    @Override
    public Single<RepoHelper> getRepoHelperFromDialogsWhenSubscribed() {
        Main.assertFxThread();
        File existingRepoDirectoryFile = this.getDirectoryPathFromChooser("Choose existing repository directory");

        if (existingRepoDirectoryFile == null) {
            // If the user pressed cancel
            throw new NoRepoSelectedException();
        }

        directoryPath = existingRepoDirectoryFile.toPath();

        try {

            SshFileData sshFileData = getSshFileDataIfTesting(directoryPath);

            RepoHelper repoHelper = new ExistingRepoHelper(directoryPath,
                                                           new ElegitUserInfoGUI(),
                                                           sshFileData.additionalPrivateKey,
                                                           sshFileData.knownHostsLocation);

            return Single.fromCallable(() -> repoHelper);

        } catch (Exception e) {
            throw new ExceptionAdapter(e);
        }
    }

    // For test purposes, we need to have tests insert their own private key and host name location if this
    // is an ssh repo. So create a repo helper with barebones information in order to find out remote URL is
    // ssh; if so, insert via test info appropriately.
    private SshFileData getSshFileDataIfTesting(Path directoryPath) {
        SshFileData sshFileData = new SshFileData();
        try {
            if (Main.testMode) {
                RepoHelper initialHelper = new ExistingRepoHelper(directoryPath, new ElegitUserInfoGUI());
                List<String> remotes = initialHelper.getLinkedRemoteRepoURLs();
                if (remotes.size() > 0 && remotes.get(0).startsWith("ssh:")) {
                    sshFileData.additionalPrivateKey =
                            getFileByTypingPath("Enter private key location:").toString();
                    sshFileData.knownHostsLocation =
                            getFileByTypingPath("Enter known hosts location:").toString();
                }
            }
            return sshFileData;
        } catch (Exception e) {
            throw new ExceptionAdapter(e);
        }


    }

    private class SshFileData {
        private String additionalPrivateKey;
        private String knownHostsLocation;
    }
    @Override
    public String getCommandLineText(){return "";}
}
