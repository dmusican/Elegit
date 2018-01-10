package elegit.gui;

import com.jcraft.jsch.UserInfo;
import de.jensd.fx.glyphs.GlyphsDude;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import elegit.Main;
import elegit.models.AuthMethod;
import elegit.models.ClonedRepoHelper;
import elegit.models.RepoHelper;
import elegit.sshauthentication.ElegitUserInfoGUI;
import io.reactivex.Single;
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler;
import io.reactivex.schedulers.Schedulers;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;
import javafx.util.Pair;
import elegit.exceptions.CancelledAuthorizationException;
import elegit.exceptions.NoRepoSelectedException;
import org.apache.http.annotation.ThreadSafe;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.TransportCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 *
 * An implementation of the abstract RepoHelperBuilder that builds
 * a ClonedRepoHelper by presenting dialogs to get the necessary
 * parameters.
 *
 */
@ThreadSafe
// but only because everything here runs on the FX thread.
public class ClonedRepoHelperBuilder extends RepoHelperBuilder {

    private static String prevRemoteURL, prevDestinationPath, prevRepoName;

    private static final Logger logger = LogManager.getLogger();

    private ButtonType cloneButtonType;
    private TextField remoteURLField;
    private TextField enclosingFolderField;
    private TextField repoNameField;

    /**
     * Builds (with a grid) and shows dialogs that prompt the user for
     * information needed to construct a ClonedRepoHelper.
     *
     * @return the new ClonedRepoHelper.
     */
    @Override
    public Single<RepoHelper> getRepoHelperFromDialogsWhenSubscribed() {
        Main.assertFxThread();
        Dialog<Pair<String, String>> dialog = createCloneDialog();
        setUpDialogButtons(dialog);
        arrangeDialogFields(dialog);
        configureCloneButton(dialog);

        Optional<Pair<String, String>> result = dialog.showAndWait();
        if (result.isPresent()) {
            // Unpack the destination-remote Pair created above:
            Path destinationPath = Paths.get(result.get().getKey());
            String remoteURL = result.get().getValue();
            String additionalPrivateKey = null;
            String knownHostsLocation = null;

            // If in test mode, enter in a new private key file. Normally, this would be expected to be in an
            // ssh/.config.
            if (Main.testMode && remoteURL.startsWith("ssh:")) {
                additionalPrivateKey = getFileByTypingPath("Enter private key location:").toString();
                knownHostsLocation = getFileByTypingPath("Enter known hosts location:").toString();
            }

            RepoHelperBuilder.AuthDialogResponse response = RepoHelperBuilder.getAuthCredentialFromDialog();

            return cloneRepositoryWithChecksWhenSubscribed
                    (
                            remoteURL, destinationPath, response, new ElegitUserInfoGUI(), additionalPrivateKey,
                            knownHostsLocation
                    )
                    .subscribeOn(Schedulers.io())
                    .observeOn(JavaFxScheduler.platform());

        } else {
            logger.info("Cloned repo helper dialog canceled");
            // This happens when the user pressed cancel.
            throw new NoRepoSelectedException();
        }
    }


    private Dialog<Pair<String, String>> createCloneDialog() {
        Main.assertFxThread();
        logger.info("Load remote repo dialog started");
        Dialog<Pair<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Clone");
        dialog.setHeaderText("Clone a remote repository");

        return dialog;
    }

    private void setUpDialogButtons(Dialog<Pair<String, String>> dialog) {
        Main.assertFxThread();
        // Set the button types.
        cloneButtonType = new ButtonType("Clone", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(cloneButtonType, ButtonType.CANCEL);
        Node cloneButton = dialog.getDialogPane().lookupButton(cloneButtonType);
        cloneButton.setId("cloneButton");
        cloneButton.setDisable(true);   // starts off as disabled
        dialog.setOnCloseRequest(event -> logger.info("Closed clone from remote dialog"));
    }

    private void arrangeDialogFields(Dialog<Pair<String, String>> dialog) {
        Main.assertFxThread();

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10, 10, 10, 10));

        Text instructionsText = new Text("Select an enclosing folder for the repository folder\n" +
                "to be created in.");

        remoteURLField = new TextField();
        remoteURLField.setId("remoteURLField");
        remoteURLField.setPromptText("Remote URL");
        if(prevRemoteURL != null) remoteURLField.setText(prevRemoteURL);

        enclosingFolderField = new TextField();
        enclosingFolderField.setId("enclosingFolderField");

        // for now, it will just show the folder you selected, unless running system tests
        enclosingFolderField.setEditable(Main.testMode);

        if(prevDestinationPath != null) enclosingFolderField.setText(prevDestinationPath);

        Text enclosingDirectoryPathText = new Text();

        Button chooseDirectoryButton = new Button();
        Text folderIcon = GlyphsDude.createIcon(FontAwesomeIcon.FOLDER_OPEN);
        chooseDirectoryButton.setGraphic(folderIcon);
        chooseDirectoryButton.setOnAction(t -> {
            File cloneRepoDirectory = this.getDirectoryPathFromChooser("Choose clone destination folder");
            if (cloneRepoDirectory != null) {
                enclosingFolderField.setText(cloneRepoDirectory.toString());
                enclosingDirectoryPathText.setText(cloneRepoDirectory.toString() + File.separator);
            }
        });

        repoNameField = new TextField();
        repoNameField.setId("repoNameField");
        repoNameField.setPromptText("Repository name...");
        remoteURLField.textProperty().addListener((obs, oldText, newText) -> {
            repoNameField.setText(guessRepoName(newText));
        });
        if(prevRepoName != null) repoNameField.setText(prevRepoName);

        int instructionsRow = 0;
        int remoteURLRow = instructionsRow + 1;
        int enclosingFolderRow = remoteURLRow + 1;
        int repositoryNameRow = enclosingFolderRow + 1;

        grid.add(instructionsText, 0, instructionsRow, 2, 1);

        grid.add(new Label("Remote URL:"), 0, remoteURLRow);
        grid.add(remoteURLField, 1, remoteURLRow);

        grid.add(new Label("Enclosing folder:"), 0, enclosingFolderRow);
        grid.add(enclosingFolderField, 1, enclosingFolderRow);
        grid.add(chooseDirectoryButton, 2, enclosingFolderRow);

        grid.add(new Label("Repository name:"), 0, repositoryNameRow);
        grid.add(repoNameField, 1, repositoryNameRow);

        dialog.getDialogPane().setContent(grid);

        // Request focus on the remote URL field by default.
        Platform.runLater(remoteURLField::requestFocus);


    }

    public static String guessRepoName(String url) {
        if (url.endsWith(".git")) {
            url = url.substring(0, url.length() - 4);
        }
        String[] urlComponents = url.split("/");
        return urlComponents[urlComponents.length - 1];
    }


    private void configureCloneButton(Dialog<Pair<String, String>> dialog) {
        Main.assertFxThread();
        Node cloneButton = dialog.getDialogPane().lookupButton(cloneButtonType);

        //////
        // Do some validation:
        //  On completion of every field, check that the other fields
        //  are also filled in and with valid characters. Then enable login.
        BooleanProperty invalidRepoNameProperty =
                new SimpleBooleanProperty(repoNameField.getText().trim().contains("/") ||
                        repoNameField.getText().trim().contains("."));

        cloneButton.disableProperty().bind(enclosingFolderField.textProperty().isEmpty()
                .or(repoNameField.textProperty().isEmpty())
                .or(remoteURLField.textProperty().isEmpty())
                .or(invalidRepoNameProperty));

        repoNameField.textProperty().addListener((observable, oldValue, newValue) -> invalidRepoNameProperty.set(newValue.trim().contains("/") || newValue.trim().contains(".")));


        // Convert the result to a destination-remote pair when the clone button is clicked.
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == cloneButtonType) {
                // Store these values for callback after a login (if user isn't logged in):
                prevRemoteURL = remoteURLField.getText().trim();
                prevDestinationPath = enclosingFolderField.getText().trim();
                prevRepoName = repoNameField.getText().trim();

                return new Pair<>(enclosingFolderField.getText().trim() + File.separator + repoNameField.getText().trim(), remoteURLField.getText().trim());
            }
            return null;
        });

    }

    // This method accesses no shared memory at all, and all calls within are threadsafe; hence
    // this does not actually have to run on the FX thread.
    public static RepoHelper cloneRepositoryWithChecks(String remoteURL, Path destinationPath,
                                                       RepoHelperBuilder.AuthDialogResponse response,
                                                       UserInfo userInfo,
                                                       String additionalPrivateKey,
                                                       String knownHostsLocation)
            throws GitAPIException, IOException, CancelledAuthorizationException, NoRepoSelectedException {

        // Always use authentication. If authentication is unneeded (HTTP), it will still work even if the wrong
        // username password is used. This is what Eclipse does; in fact, it asks for username/password in the
        // same dialog box that the URL is entered.
        //
        // Set up both sets of credentials (username/password pair, as well
        // as just password. Only one of these will be used, but that is determined automatically via the JGit
        // callback mechanism.
        UsernamePasswordCredentialsProvider credentials =
                new UsernamePasswordCredentialsProvider(response.username, response.password);
        String sshPassword = response.password;

        // Try calling `git ls-remote ___` on the remote URL to see if it's valid
        TransportCommand command = Git.lsRemoteRepository().setRemote(remoteURL);

        ClonedRepoHelper repoHelper = new ClonedRepoHelper(destinationPath, remoteURL, sshPassword, userInfo,
                                                           additionalPrivateKey, knownHostsLocation);
        repoHelper.wrapAuthentication(command, credentials);
        try {
            command.call();
        } catch (TransportException e) {
            // If the URL doesn't have a repo, a Transport Exception is thrown when this command is called.
            //  We want the SessionController to report an InvalidRemoteException, though, because
            //  that's the issue.
            System.out.println(e.getMessage());
            System.out.println(e.getMessage().endsWith("not authorized") || e.getMessage().endsWith("not authorized."));
            System.out.println(e.getMessage().endsWith("not found") || e.getMessage().endsWith("not found."));
            /*Throwable exception = e;
            while (exception != null) {
                System.err.println("======================");
                exception.printStackTrace();
                exception = exception.getCause();
            }*/
            if (e.getMessage().endsWith("not found") || e.getMessage().endsWith("not found.")) {
                logger.error("Invalid remote exception thrown");
                throw new InvalidRemoteException("Caught invalid repository when building a ClonedRepoHelper.");
            } else if (e.getMessage().endsWith("not authorized") || e.getMessage().endsWith("not authorized.")) {
                logger.error("Authentication error");
            }
        }

        // Without the above try/catch block, the next line would run and throw the desired InvalidRemoteException,
        //  but it would create a destination folder for the repo before stopping. By catching the error above,
        //  we prevent unnecessary folder creation. By making it to this point, we've verified that the repo
        // is valid and that we can authenticate to it.

        if (response.protocol == AuthMethod.SSH) {
            repoHelper.obtainRepository(remoteURL);
        } else {
            repoHelper.setOwnerAuth(credentials);
            repoHelper.obtainRepository(remoteURL);
        }

        return repoHelper;
    }

    public static Single<RepoHelper> cloneRepositoryWithChecksWhenSubscribed(
            String remoteURL, Path destinationPath,
            RepoHelperBuilder.AuthDialogResponse response,
            UserInfo userInfo,
            String additionalPrivateKey,
            String knownHostsLocation)      {
        return Single.fromCallable(() -> cloneRepositoryWithChecks(remoteURL,
                                                                   destinationPath,
                                                                   response,
                                                                   userInfo,
                                                                   additionalPrivateKey,
                                                                   knownHostsLocation));
    }

}
