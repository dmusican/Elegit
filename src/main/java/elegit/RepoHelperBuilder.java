package main.java.elegit;

import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;
import javafx.util.Pair;
import main.java.elegit.exceptions.CancelledAuthorizationException;
import main.java.elegit.exceptions.NoRepoSelectedException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

/**
 * An abstract class for building RepoHelpers by presenting dialogs to
 * get the required parameters.
 */
public abstract class RepoHelperBuilder {

    SessionModel sessionModel;
    private String defaultFilePickerStartFolder = System.getProperty("user.home");
    public UsernamePasswordCredentialsProvider ownerAuth;

    public RepoHelperBuilder(SessionModel sessionModel) {
        this.sessionModel = sessionModel;
    }

    public abstract RepoHelper getRepoHelperFromDialogs() throws GitAPIException, IOException, NoRepoSelectedException, CancelledAuthorizationException;

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
        chooser.setInitialDirectory(path);

        returnFile = chooser.showDialog(parent);
        return returnFile;
    }
}
