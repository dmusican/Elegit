package edugit;

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
     * @throws Exception why?
     */
    @Override
    public RepoHelper getRepoHelperFromDialogs() throws Exception {
        File existingRepoDirectoryFile = this.getDirectoryPathFromChooser("Choose existing repository directory", null);

        if (existingRepoDirectoryFile == null) {
            // If the user pressed cancel
            throw new NoRepoSelectedException();
        }

        Path directoryPath = existingRepoDirectoryFile.toPath();
        RepoHelper existingRepoHelper = new ExistingRepoHelper(directoryPath, this.sessionModel.getOwner());

        return existingRepoHelper;
    }
}
