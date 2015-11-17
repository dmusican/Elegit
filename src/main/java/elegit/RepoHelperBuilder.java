package elegit;

import javafx.stage.DirectoryChooser;
import javafx.stage.Window;
import elegit.exceptions.NoOwnerInfoException;
import elegit.exceptions.NoRepoSelectedException;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.File;
import java.io.IOException;

/**
 * An abstract class for building RepoHelpers by presenting dialogs to
 * get the required parameters.
 */
public abstract class RepoHelperBuilder {

    SessionModel sessionModel;
    private String defaultFilePickerStartFolder = System.getProperty("user.home");

    public RepoHelperBuilder(SessionModel sessionModel) {
        this.sessionModel = sessionModel;
    }

    public abstract RepoHelper getRepoHelperFromDialogs() throws GitAPIException, NoOwnerInfoException, IOException, NoRepoSelectedException;

    /**
     * Presents a file chooser and returns the chosen file.
     *
     * @param title the title of the file chooser window.
     * @param parent the parent Window for the file chooser. Can be null (then
     *               the chooser won't be anchored to any window).
     * @return the chosen file from the file chooser.
     */
    public File getDirectoryPathFromChooser(String title, Window parent) {
        File path = new File(this.defaultFilePickerStartFolder); // start the file browser in the user's home folder

        File returnFile;
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle(title);
        chooser.setInitialDirectory(path.getParentFile());

        returnFile = chooser.showDialog(parent);
        return returnFile;
    }
}
