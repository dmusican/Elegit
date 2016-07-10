package elegit;

import de.jensd.fx.glyphs.GlyphsDude;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;
import javafx.util.Pair;
import elegit.exceptions.CancelledAuthorizationException;
import elegit.exceptions.NoRepoSelectedException;
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
public class ClonedRepoHelperBuilder extends RepoHelperBuilder {

    private static String prevRemoteURL, prevDestinationPath, prevRepoName;

    static final Logger logger = LogManager.getLogger();

    private ButtonType cloneButtonType;
    private TextField remoteURLField;
    private TextField enclosingFolderField;
    private TextField repoNameField;

    public ClonedRepoHelperBuilder(SessionModel sessionModel) {
        super(sessionModel);
    }

    /**
     * Builds (with a grid) and shows dialogs that prompt the user for
     * information needed to construct a ClonedRepoHelper.
     *
     * @return the new ClonedRepoHelper.
     */
    @Override
    public RepoHelper getRepoHelperFromDialogs() throws GitAPIException, IOException, NoRepoSelectedException, CancelledAuthorizationException{

        Dialog<Pair<String, String>> dialog = createCloneDialog();
        setUpDialogButtons(dialog);
        arrangeDialogFields(dialog);
        configureCloneButton(dialog);

        Optional<Pair<String, String>> result = dialog.showAndWait();

        RepoHelper repoHelper = processDialogResponses(result);
        return repoHelper;
    }

    private Dialog<Pair<String, String>> createCloneDialog() {
        logger.info("Load remote repo dialog started");
        Dialog<Pair<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Clone");
        dialog.setHeaderText("Clone a remote repository");

        return dialog;
    }

    private void setUpDialogButtons(Dialog<Pair<String, String>> dialog) {
        // Set the button types.
        cloneButtonType = new ButtonType("Clone", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(cloneButtonType, ButtonType.CANCEL);
        Node cloneButton = dialog.getDialogPane().lookupButton(cloneButtonType);
        cloneButton.setDisable(true);   // starts off as disabled
        dialog.setOnCloseRequest(new EventHandler<DialogEvent>() {
            @Override
            public void handle(DialogEvent event) {
                logger.info("Closed clone from remote dialog");
            }
        });
    }

    private void arrangeDialogFields(Dialog<Pair<String, String>> dialog) {

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10, 10, 10, 10));

        Text instructionsText = new Text("Select an enclosing folder for the repository folder\n" +
                "to be created in.");

        remoteURLField = new TextField();
        remoteURLField.setPromptText("Remote URL");
        if(prevRemoteURL != null) remoteURLField.setText(prevRemoteURL);

        enclosingFolderField = new TextField();
        enclosingFolderField.setEditable(false); // for now, it will just show the folder you selected
        if(prevDestinationPath != null) enclosingFolderField.setText(prevDestinationPath);

        Text enclosingDirectoryPathText = new Text();

        Button chooseDirectoryButton = new Button();
        Text folderIcon = GlyphsDude.createIcon(FontAwesomeIcon.FOLDER_OPEN);
        chooseDirectoryButton.setGraphic(folderIcon);
        chooseDirectoryButton.setOnAction(t -> {
            File cloneRepoDirectory = this.getDirectoryPathFromChooser("Choose clone destination folder", null);
            if (cloneRepoDirectory != null) {
                enclosingFolderField.setText(cloneRepoDirectory.toString());
                enclosingDirectoryPathText.setText(cloneRepoDirectory.toString() + File.separator);
            }
        });

        repoNameField = new TextField();
        repoNameField.setPromptText("Repository name...");
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


    private void configureCloneButton(Dialog<Pair<String, String>> dialog) {
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

        repoNameField.textProperty().addListener((observable, oldValue, newValue) -> {
            invalidRepoNameProperty.set(newValue.trim().contains("/") || newValue.trim().contains("."));
        });


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

    private RepoHelper processDialogResponses(Optional<Pair<String, String>> result) throws GitAPIException, IOException, CancelledAuthorizationException, NoRepoSelectedException {
        if (result.isPresent()) {
            // Unpack the destination-remote Pair created above:
            Path destinationPath = Paths.get(result.get().getKey());
            String remoteURL = result.get().getValue();

            // Always use authentication. If authentication is unneeded (HTTP), it will still work even if the wrong
            // username password is used. This is what Eclipse does; in fact, it asks for username/password in the
            // same dialog box that the URL is entered.

            // Try calling `git ls-remote ___` on the remote URL to see if it's valid
            TransportCommand command = Git.lsRemoteRepository().setRemote(remoteURL);

            // Get authentication as needed. Set up both sets of credentials (username/password pair, as well
            // as just password. Only one of these will be used, but that is determined automatically via the JGit
            // callback mechanism.
            RepoHelperBuilder.AuthDialogResponse response = RepoHelperBuilder.getAuthCredentialFromDialog();
            UsernamePasswordCredentialsProvider credentials =
                    new UsernamePasswordCredentialsProvider(response.username, response.password);
            String sshPassword = response.password;

            try {
                RepoHelper lsHelper = new RepoHelper(sshPassword);
                lsHelper.wrapAuthentication(command, credentials);
                command.call();
            } catch (TransportException e) {
                // If the URL doesn't have a repo, a Transport Exception is thrown when this command is called.
                //  We want the SessionController to report an InvalidRemoteException, though, because
                //  that's the issue.
                logger.error("Invalid remote exception thrown");
                throw new InvalidRemoteException("Caught invalid repository when building a ClonedRepoHelper.");
            }

            // Without the above try/catch block, the next line would run and throw the desired InvalidRemoteException,
            //  but it would create a destination folder for the repo before stopping. By catching the error above,
            //  we prevent unnecessary folder creation. By making it to this point, we've verified that the repo
            // is valid and that we can authenticate to it.

            ClonedRepoHelper repoHelper;
            if (response.protocol == AuthMethod.SSH) {
                repoHelper = new ClonedRepoHelper(destinationPath, remoteURL, response.password);
                repoHelper.obtainRepository(remoteURL);
            } else {
                repoHelper = new ClonedRepoHelper(destinationPath, remoteURL, credentials);
                repoHelper.obtainRepository(remoteURL);
            }

            return repoHelper;
        } else {
            logger.info("Cloned repo helper dialog canceled");
            // This happens when the user pressed cancel.
            throw new NoRepoSelectedException();
        }
    }


    public String getPrevDestinationPath() {
        return prevDestinationPath;
    }

    public String getPrevRepoName() {
        return prevRepoName;
    }
}
