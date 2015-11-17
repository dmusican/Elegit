package elegit;

import elegit.exceptions.NoOwnerInfoException;
import elegit.exceptions.NoRepoSelectedException;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

/**
 *
 * An implementation of the abstract elegit.RepoHelperBuilder that prompts the
 * user (using dialogs) for the parameters required to build an elegit.ExistingRepoHelper.
 *
 */
public class ExistingRepoHelperBuilder extends RepoHelperBuilder {
    public ExistingRepoHelperBuilder(SessionModel sessionModel) {
        super(sessionModel);
    }

    /**
     * Shows a file chooser dialog and makes the elegit.ExistingRepoHelper from it.
     *  (The elegit.ExistingRepoHelper is generalized into a elegit.RepoHelper so that all
     *   RepoHelpers can be stored in the same list and acted on uniformly).
     *
     * @throws Exception why? has to do with the new elegit.ExistingRepoHelper(...).
     */
    @Override
    public RepoHelper getRepoHelperFromDialogs() throws GitAPIException, NoOwnerInfoException, IOException, NoRepoSelectedException {
        File existingRepoDirectoryFile = this.getDirectoryPathFromChooser("Choose existing repository directory", null);

        if (existingRepoDirectoryFile == null) {
            // If the user pressed cancel
            throw new NoRepoSelectedException();
        }

        Path directoryPath = existingRepoDirectoryFile.toPath();

        RepoHelper existingRepoHelper = new ExistingRepoHelper(directoryPath, this.sessionModel.getDefaultOwner());

        return existingRepoHelper;
    }
}
