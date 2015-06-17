package edugit;

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
        Path existingRepoDirectory = this.getDirectoryPathFromChooser("Choose existing repository directory", null).toPath();
        RepoHelper existingRepoHelper = new ExistingRepoHelper(existingRepoDirectory, this.sessionModel.getOwner());

        return existingRepoHelper;
    }
}
