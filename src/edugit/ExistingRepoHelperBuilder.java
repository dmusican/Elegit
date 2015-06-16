package edugit;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.util.Pair;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * Created by grahamearley on 6/16/15.
 */
public class ExistingRepoHelperBuilder extends RepoHelperBuilder {
    public ExistingRepoHelperBuilder(SessionModel sessionModel) {
        super(sessionModel);
    }

    /**
     *
     * @throws Exception (I think this happens if the user presses cancel on the directory chooser)
     */
    @Override
    public void presentDialogsToConstructRepoHelper() throws Exception {
        Path existingRepoDirectory = this.getDirectoryPathFromChooser("Choose existing repository directory", null).toPath();
        RepoHelper existingRepoHelper = new ExistingRepoHelper(existingRepoDirectory, this.sessionModel.getOwner());
        this.sessionModel.openRepoFromHelper(existingRepoHelper);
    }
}
