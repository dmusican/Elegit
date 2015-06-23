package edugit;

import edugit.exceptions.NoOwnerInfoException;
import edugit.exceptions.NoRepoSelectedException;

import java.io.File;
import java.nio.file.Path;

/**
 * Created by grahamearley on 6/16/15.
 */
public class ExistingRepoHelperBuilder extends RepoHelperBuilder {
    public ExistingRepoHelperBuilder(SessionModel sessionModel) {
        super(sessionModel);
    }

    /**
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

        if (this.sessionModel.getDefaultOwner() == null) {
            throw new NoOwnerInfoException();
        }

        RepoHelper existingRepoHelper = new ExistingRepoHelper(directoryPath, this.sessionModel.getDefaultOwner());

        return existingRepoHelper;
    }
}
