package main.java.edugit;

import main.java.edugit.exceptions.NoOwnerInfoException;
import main.java.edugit.exceptions.NoRepoSelectedException;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.File;
import java.io.IOException;
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
    public RepoHelper getRepoHelperFromDialogs() throws GitAPIException, NoOwnerInfoException, IOException, NoRepoSelectedException{
        File existingRepoDirectoryFile = this.getDirectoryPathFromChooser("Choose existing repository directory", null);

        if (existingRepoDirectoryFile == null) {
            // If the user pressed cancel
            throw new NoRepoSelectedException();
        }

        Path directoryPath = existingRepoDirectoryFile.toPath();

//        if (this.sessionModel.getDefaultOwner() == null) {
//            throw new NoOwnerInfoException();
//        }

        RepoHelper existingRepoHelper = new ExistingRepoHelper(directoryPath, this.sessionModel.getDefaultOwner());

        return existingRepoHelper;
    }
}
