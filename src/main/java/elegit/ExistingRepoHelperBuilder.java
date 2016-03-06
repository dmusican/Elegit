package main.java.elegit;

import main.java.elegit.exceptions.CancelledAuthorizationException;
import main.java.elegit.exceptions.NoRepoSelectedException;
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
public class ExistingRepoHelperBuilder extends RepoHelperBuilder {
    public ExistingRepoHelperBuilder(SessionModel sessionModel) {
        super(sessionModel);
    }

    /**
     * Shows a file chooser dialog and makes the ExistingRepoHelper from it.
     *  (The ExistingRepoHelper is generalized into a RepoHelper so that all
     *   RepoHelpers can be stored in the same list and acted on uniformly).
     *
     * @throws Exception why? has to do with the new ExistingRepoHelper(...).
     */
    @Override
    public RepoHelper getRepoHelperFromDialogs() throws GitAPIException, IOException, NoRepoSelectedException, CancelledAuthorizationException{
        File existingRepoDirectoryFile = this.getDirectoryPathFromChooser("Choose existing repository directory", null);

        if (existingRepoDirectoryFile == null) {
            // If the user pressed cancel
            throw new NoRepoSelectedException();
        }

        Path directoryPath = existingRepoDirectoryFile.toPath();

        RepoHelper existingRepoHelper = new ExistingRepoHelper(directoryPath, this.sessionModel.getDefaultUsername());

        return existingRepoHelper;
    }
}
