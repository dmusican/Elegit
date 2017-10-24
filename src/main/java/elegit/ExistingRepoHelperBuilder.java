package elegit;

import elegit.exceptions.CancelledAuthorizationException;
import elegit.exceptions.NoRepoSelectedException;
import elegit.models.ExistingRepoHelper;
import elegit.models.RepoHelper;
import elegit.models.SessionModel;
import elegit.sshauthentication.ElegitUserInfoGUI;
import org.apache.http.annotation.ThreadSafe;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

/**
 *
 * An implementation of the abstract RepoHelperBuilder that prompts the
 * user (using dialogs) for the parameters required to build an ExistingRepoHelper.
 *
 */
@ThreadSafe
// all methods execute on FX thread anyway. If something comes off, verify thread safety
public class ExistingRepoHelperBuilder extends RepoHelperBuilder {

    /**
     * Shows a file chooser dialog and makes the ExistingRepoHelper from it.
     *  (The ExistingRepoHelper is generalized into a RepoHelper so that all
     *   RepoHelpers can be stored in the same list and acted on uniformly).
     *
     * @throws Exception why? has to do with the new ExistingRepoHelper(...).
     */
    @Override
    public RepoHelper getRepoHelperFromDialogs() throws GitAPIException, IOException, NoRepoSelectedException, CancelledAuthorizationException{
        Main.assertFxThread();
        File existingRepoDirectoryFile = this.getDirectoryPathFromChooser("Choose existing repository directory");

        if (existingRepoDirectoryFile == null) {
            // If the user pressed cancel
            throw new NoRepoSelectedException();
        }

        Path directoryPath = existingRepoDirectoryFile.toPath();

        return new ExistingRepoHelper(directoryPath, new ElegitUserInfoGUI());
    }
}
