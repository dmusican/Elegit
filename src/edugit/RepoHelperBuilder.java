package edugit;

import javafx.scene.control.Dialog;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;
import javafx.util.Pair;

import java.io.File;

/**
 * Created by grahamearley on 6/16/15.
 */
public abstract class RepoHelperBuilder {

    SessionModel sessionModel;
    private String defaultFilePickerStartFolder = System.getProperty("user.home");

    public RepoHelperBuilder(SessionModel sessionModel) {
        this.sessionModel = sessionModel;
    }

    public abstract RepoHelper getRepoHelperFromDialogs() throws Exception;

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
