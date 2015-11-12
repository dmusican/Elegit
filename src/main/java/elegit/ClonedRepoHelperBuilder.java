package main.java.elegit;

import de.jensd.fx.glyphs.GlyphsDude;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;
import javafx.util.Pair;
import main.java.elegit.exceptions.CancelledAuthorizationException;
import main.java.elegit.exceptions.NoRepoSelectedException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LsRemoteCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
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

    public ClonedRepoHelperBuilder(SessionModel sessionModel) {
        super(sessionModel);
    }

    /**
     * Builds (with a grid) and shows dialogs that prompt the user for
     * information needed to construct a ClonedRepoHelper.
     *
     * @return the new ClonedRepoHelper.
     * @throws Exception when constructing the new ClonedRepoHelper
     */
    @Override
    public RepoHelper getRepoHelperFromDialogs() throws GitAPIException, IOException, NoRepoSelectedException, CancelledAuthorizationException{
        // Inspired by: http://code.makery.ch/blog/javafx-dialogs-official/

        // Create the custom dialog.
        Dialog<Pair<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Clone");
        dialog.setHeaderText("Clone a remote repository");

        Text instructionsText = new Text("Select an enclosing folder for the repository folder\n" +
                                         "to be created in.");

        // Set the button types.
        ButtonType cloneButtonType = new ButtonType("Clone", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(cloneButtonType, ButtonType.CANCEL);

        // Create the Remote URL and destination path labels and fields.
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10, 10, 10, 10));

        TextField remoteURLField = new TextField();
        remoteURLField.setPromptText("Remote URL");
        if(prevRemoteURL != null) remoteURLField.setText(prevRemoteURL);

        TextField enclosingFolderField = new TextField();
        enclosingFolderField.setEditable(false); // for now, it will just show the folder you selected
        if(prevDestinationPath != null) enclosingFolderField.setText(prevDestinationPath);

        Text enclosingDirectoryPathText = new Text();

        Button chooseDirectoryButton = new Button();
        Text folderIcon = GlyphsDude.createIcon(FontAwesomeIcon.FOLDER_OPEN);
        chooseDirectoryButton.setGraphic(folderIcon);
        chooseDirectoryButton.setOnAction(t -> {
            File cloneRepoDirectory = this.getDirectoryPathFromChooser("Choose clone destination folder", null);
            enclosingFolderField.setText(cloneRepoDirectory.toString());
            enclosingDirectoryPathText.setText(cloneRepoDirectory.toString() + File.separator);
        });

        TextField repoNameField = new TextField();
        repoNameField.setPromptText("Repository name...");
        if(prevRepoName != null) repoNameField.setText(prevRepoName);

        grid.add(instructionsText, 0, 0, 2, 1);

        grid.add(new Label("Remote URL:"), 0, 1);
        grid.add(remoteURLField, 1, 1);
        grid.add(new Label("Enclosing folder:"), 0, 2);
        grid.add(enclosingFolderField, 1, 2);
        grid.add(chooseDirectoryButton, 2, 2);

        grid.add(new Label("Repository name:"), 0, 3);
        grid.add(repoNameField, 1, 3);

        // Enable/Disable login button depending on whether a username was entered.
        Node cloneButton = dialog.getDialogPane().lookupButton(cloneButtonType);
        cloneButton.setDisable(true);

        //////
        // Do some validation:
        //  On completion of every field, check that the other fields
        //  are also filled in and with valid characters. Then enable login.
        BooleanProperty invalidRepoNameProperty = new SimpleBooleanProperty(repoNameField.getText().trim().contains("/") || repoNameField.getText().trim().contains("."));

        cloneButton.disableProperty().bind(enclosingFolderField.textProperty().isEmpty()
                .or(repoNameField.textProperty().isEmpty())
                .or(remoteURLField.textProperty().isEmpty())
                .or(invalidRepoNameProperty));

        repoNameField.textProperty().addListener((observable, oldValue, newValue) -> {
            invalidRepoNameProperty.set(newValue.trim().contains("/") || newValue.trim().contains("."));
        });
        //////

        dialog.getDialogPane().setContent(grid);

        // Request focus on the remote URL field by default.
        Platform.runLater(remoteURLField::requestFocus);

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

        Optional<Pair<String, String>> result = dialog.showAndWait();

        if (result.isPresent()) {
            // Unpack the destination-remote Pair created above:
            Path destinationPath = Paths.get(result.get().getKey());
            String remoteURL = result.get().getValue();

            try {
                // Try calling `git ls-remote ___` on the remote URL to see if it's valid
                LsRemoteCommand lsRemoteCommand = new LsRemoteCommand(this.sessionModel.getCurrentRepo());
                lsRemoteCommand.setRemote(remoteURL);
                lsRemoteCommand.call();
            } catch (TransportException e) {
                // If the URL doesn't have a repo, a Transport Exception is thrown when this command is called.
                //  We want the SessionController to report an InvalidRemoteException, though, because
                //  that's the issue.
                throw new InvalidRemoteException("Caught invalid repository when building a ClonedRepoHelper.");
            }

            // Without the above try/catch block, the next line would run and throw the desired InvalidRemoteException,
            //  but it would create a destination folder for the repo before stopping. By catching the error above,
            //  we prevent unnecessary folder creation.
            RepoHelper repoHelper = new ClonedRepoHelper(destinationPath, remoteURL, this.sessionModel.getDefaultUsername());

            return repoHelper;
        } else {
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
