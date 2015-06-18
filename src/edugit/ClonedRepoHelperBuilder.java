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
public class ClonedRepoHelperBuilder extends RepoHelperBuilder {

    public ClonedRepoHelperBuilder(SessionModel sessionModel) {
        super(sessionModel);
    }

    /**
     *
     * @return
     * @throws Exception when constructing the new ClonedRepoHelper
     */
    @Override
    public RepoHelper getRepoHelperFromDialogs() throws Exception {
        // Inspired by: http://code.makery.ch/blog/javafx-dialogs-official/

        // Create the custom dialog.
        Dialog<Pair<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Clone a Repository");
        dialog.setHeaderText("Enter a remote repository to clone into the destination path.");

        // Set the button types.
        ButtonType cloneButtonType = new ButtonType("Clone", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(cloneButtonType, ButtonType.CANCEL);

        // Create the Remote URL and destination path labels and fields.
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField remoteURLField = new TextField();
        remoteURLField.setPromptText("Remote URL");

        TextField destinationPathField = new TextField();
        destinationPathField.setEditable(false); // for now, it will just show the folder you selected

        Button chooseDirectoryButton = new Button();
        chooseDirectoryButton.setText("..."); // TODO: folder icon or something, instead of text
        chooseDirectoryButton.setOnAction(t -> {
            File cloneRepoDirectory = this.getDirectoryPathFromChooser("Choose clone destination folder", null);
            destinationPathField.setText(cloneRepoDirectory.toString());
        });

        grid.add(new Label("Remote URL:"), 0, 0);
        grid.add(remoteURLField, 1, 0);
        grid.add(new Label("Destination Path:"), 0, 1);
        grid.add(destinationPathField, 1, 1);
        grid.add(chooseDirectoryButton, 2, 1);

        dialog.getDialogPane().setContent(grid);

        // Request focus on the remote URL field by default.
        Platform.runLater(remoteURLField::requestFocus);

        // Convert the result to a destination-remote pair when the clone button is clicked.
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == cloneButtonType) {
                return new Pair<>(destinationPathField.getText(), remoteURLField.getText());
            }
            return null;
        });

        Optional<Pair<String, String>> result = dialog.showAndWait();

        if (result.isPresent()) {
            Path destinationPath = Paths.get(result.get().getKey());
            RepoHelper repoHelper = new ClonedRepoHelper(destinationPath, result.get().getValue(), this.sessionModel.getOwner());
            return repoHelper;
        }
        return null;
    }
}
