package main.java.edugit;

import main.java.edugit.exceptions.NoRepoSelectedException;

import java.io.File;
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
    public RepoHelper getRepoHelperFromDialogs() throws Exception {
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
