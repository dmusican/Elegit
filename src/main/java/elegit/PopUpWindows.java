package elegit;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.Dialog;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by connellyj on 7/7/16.
 *
 * Class that initializes a given pop up window
 */
public class PopUpWindows {

    static final Logger logger = LogManager.getLogger();

    /**
     * Informs the user that they are about to commit a conflicting file
     * @return String user's response to the dialog
     */
    public static String showCommittingConflictingFileAlert() {
        String resultType;

        Alert alert = new Alert(Alert.AlertType.WARNING);

        ButtonType resolveButton = new ButtonType("Open Editor");
        ButtonType addButton = new ButtonType("Add");
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType helpButton = new ButtonType("Help", ButtonBar.ButtonData.HELP);

        alert.getButtonTypes().setAll(helpButton, resolveButton, addButton, cancelButton);

        alert.setResizable(true);
        alert.getDialogPane().setPrefSize(450, 200);

        alert.setTitle("Warning: conflicting file");
        alert.setHeaderText("You're adding a conflicting file");
        alert.setContentText("You can open an editor to resolve the conflicts, or add the changes anyways. What do you want to do?");

        ImageView img = new ImageView(new javafx.scene.image.Image("/elegit/images/conflict.png"));
        img.setFitHeight(40);
        img.setFitWidth(80);
        img.setPreserveRatio(true);
        alert.setGraphic(img);

        Optional<ButtonType> result = alert.showAndWait();

        if(result.get() == resolveButton){
            logger.info("Chose to resolve conflicts");
            resultType = "resolve";
        }else if(result.get() == addButton){
            logger.info("Chose to add file");
            resultType = "add";
        }else if(result.get() == helpButton) {
            logger.info("Chose to get help");
            resultType = "help";
        } else{
            // User cancelled the dialog
            logger.info("Cancelled dialog");
            resultType = "cancel";
        }

        return resultType;
    }

    /**
     * Shows a window with instructions on how to fix a conflict
     */
    public static void showConflictingHelpAlert() {
        Platform.runLater(() -> {
            Alert window = new Alert(Alert.AlertType.INFORMATION);
            window.setResizable(true);
            window.getDialogPane().setPrefSize(550, 350);
            window.setTitle("How to fix conflicting files");
            window.setHeaderText("How to fix conflicting files");
            window.setContentText("1. First, open up the file that is marked as conflicting.\n" +
                    "2. In the file, you should see something like this:\n\n" +
                    "\t<<<<<< <branch_name>\n" +
                    "\tChanges being made on the branch that is being merged into.\n" +
                    "\tIn most cases, this is the branch that you currently have checked out (i.e. HEAD).\n" +
                    "\t=======\n" +
                    "\tChanges made on the branch that is being merged in.\n" +
                    "\t>>>>>>> <branch name>\n\n" +
                    "3. Delete the contents you don't want to keep after the merge\n" +
                    "4. Remove the markers (<<<<<<<, =======, >>>>>>>) git put in the file\n" +
                    "5. Done! You can now safely add and commit the file");
            window.showAndWait();
        });
    }

    /**
     * Shows a window with some info about git reset
     */
    public static void showResetHelpAlert() {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.getDialogPane().setPrefSize(300, 300);
            alert.setTitle("Reset Help");
            alert.setHeaderText("What is reset?");
            ImageView img = new ImageView(new Image("/elegit/images/undo.png"));
            img.setFitHeight(60);
            img.setFitWidth(60);
            alert.setGraphic(img);
            alert.setContentText("Move the current branch tip backward to the selected commit, " +
                    "reset the staging area to match, " +
                    "but leave the working directory alone. " +
                    "All changes made since the selected commit will reside in the working directory.");
            alert.showAndWait();
        });
    }

    /**
     * Show a window with info about git revert
     */
    public static void showRevertHelpAlert() {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.getDialogPane().setPrefSize(400, 300);
            alert.setTitle("Revert Help");
            alert.setHeaderText("What is revert?");
            ImageView img = new ImageView(new Image("/elegit/images/undo.png"));
            img.setFitHeight(60);
            img.setFitWidth(60);
            alert.setGraphic(img);
            alert.setContentText("The git revert command undoes a committed snapshot. " +
                    "But, instead of removing the commit from the project history, " +
                    "it figures out how to undo the changes introduced by the commit and appends a new commit with the resulting content. " +
                    "This prevents Git from losing history, " +
                    "which is important for the integrity of your revision history and for reliable collaboration.\n" +
                    "To revert multiple commits, select multiple commits using shift+click, then right click on one of the "+
                    "selected commits.");
            alert.showAndWait();
        });
    }

    /**
     * Shows a warning about checking out files from the index
     * @return
     */
    public static boolean showCheckoutAlert() {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        ButtonType checkout = new ButtonType("Checkout");
        ButtonType cancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(checkout, cancel);

        alert.setTitle("Checkout Warning");
        alert.setContentText("Are you sure you want to checkout the selected files?\n"+
                            "This will discard all changes that have not been added (staged).");
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent())
            return result.get()==checkout;
        else
            return false;
    }

    /**
     * Informs the user that they are adding a previously conflicting file
     * @return String result from user input
     */
    public static String showAddingingConflictingThenModifiedFileAlert() {
        String resultType;

        Alert alert = new Alert(Alert.AlertType.INFORMATION);

        ButtonType commitButton = new ButtonType("Add");
        ButtonType buttonTypeCancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(commitButton, buttonTypeCancel);

        alert.setResizable(true);
        alert.getDialogPane().setPrefSize(300, 200);

        alert.setTitle("Adding previously conflicting file");
        alert.setHeaderText("You are adding a conflicting file that was recently modified to the commit");
        alert.setContentText("If the file is what you want it to be, you should commit. Otherwise, modify the file accordingly.");

        Optional<ButtonType> result = alert.showAndWait();

        if(result.get() == commitButton){
            logger.info("Chose to add");
            resultType = "add";
        }else{
            // User cancelled the dialog
            logger.info("Cancelled dialog");
            resultType = "cancel";
        }

        return resultType;
    }

    /**
     * Informs the user that they are tracking ignored files
     * @param trackedIgnoredFiles collections of files being ignored
     */
    public static void showTrackingIgnoredFilesWarning(Collection<String> trackedIgnoredFiles) {
        Platform.runLater(() -> {
            if(trackedIgnoredFiles.size() > 0){
                String fileStrings = "";
                for(String s : trackedIgnoredFiles){
                    fileStrings += "\n"+s;
                }
                Alert alert = new Alert(Alert.AlertType.WARNING, "The following files are being tracked by Git, " +
                        "but also match an ignore pattern. If you want to ignore these files, remove them from Git.\n"+fileStrings);
                alert.showAndWait();
            }
        });
    }

    /**
     * Informs the user that there are conflicting files so they can't checkout a different branch
     * @param conflictingPaths conflicting files
     */
    public static void showCheckoutConflictsAlert(List<String> conflictingPaths) {
        logger.warn("Checkout conflicts warning");
        String conflictList = "";
        for(String pathName : conflictingPaths){
            conflictList += "\n" + pathName;
        }
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Conflicting files");
        alert.setHeaderText("Can't checkout that branch");
        alert.setContentText("You can't switch to that branch because of the following conflicting files between that branch and your current branch: "
                + conflictList);

        alert.showAndWait();
    }

    /**
     * Informs the user that there were conflicts
     * @param conflictingPaths conflicting files
     */
    public static void showMergeConflictsAlert(List<String> conflictingPaths) {
        logger.warn("Merge conflicts warning");
        String conflictList = "";
        for(String pathName : conflictingPaths){
            conflictList += "\n" + pathName;
        }
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Conflicting files");
        alert.setHeaderText("Can't complete merge");
        alert.setContentText("There were conflicts in the following files: "
                + conflictList);

        alert.showAndWait();
    }

    public static RemoteBranchHelper showTrackDifRemoteBranchDialog(ObservableList<RemoteBranchHelper> remoteBranches) {
        Dialog dialog = new Dialog();
        dialog.getDialogPane().setPrefSize(320, 100);
        dialog.setTitle("Track a remote branch locally");

        Text trackText = new Text("Track ");
        Text localText = new Text(" locally.");

        ComboBox<RemoteBranchHelper> dropdown = new ComboBox<>(remoteBranches);
        dropdown.setPromptText("select a remote branch...");

        HBox hBox = new HBox(trackText, dropdown, localText);
        hBox.setSpacing(10);
        hBox.setAlignment(Pos.CENTER);

        dialog.getDialogPane().setContent(hBox);

        ButtonType trackButton = new ButtonType("Track");
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        dialog.getDialogPane().getButtonTypes().addAll(trackButton, cancelButton);

        Optional<?> result = dialog.showAndWait();

        if(result.get() == trackButton) {
            dialog.close();
            return dropdown.getSelectionModel().getSelectedItem();
        }

        return null;
    }

    public static boolean showForceDeleteBranchAlert() {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Deleting unmerged branch");
        alert.setHeaderText("The branch you are trying to delete is unmerged");
        alert.setContentText("The work done on this branch is not represented in any other local branch. " +
                "If you delete it, you will lose any local work done on this branch. " +
                "What would you like to do?");

        ButtonType deleteButton = new ButtonType("Force delete branch");
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().clear();
        alert.getButtonTypes().addAll(deleteButton ,cancelButton);

        Optional<?> result = alert.showAndWait();

        return result.get() == deleteButton;
    }

    public static String pickRemoteToPushTo(Set<String> remotes) {
        ReentrantLock lock = new ReentrantLock();
        Condition finishedAlert = lock.newCondition();

        final String[] result = new String[1];

        Platform.runLater(() -> {
            try {
                Alert alert = new Alert(Alert.AlertType.NONE);
                alert.setTitle("Multiple remotes found");
                alert.setHeaderText("There are multiple remote repositories associated with this repository. Pick one to push to.");

                ComboBox remoteRepos = new ComboBox();
                remoteRepos.setItems(FXCollections.observableArrayList(remotes));

                ButtonType okButton = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
                ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

                alert.getDialogPane().setContent(remoteRepos);
                alert.getButtonTypes().addAll(cancelButton, okButton);

                Optional<?> alertResult = alert.showAndWait();

                if(alertResult.isPresent()) {
                    if(alertResult.get() == okButton) {
                        if(remoteRepos.getSelectionModel().getSelectedItem() != null) {
                            result[0] =  (String) remoteRepos.getSelectionModel().getSelectedItem();
                        }
                    }
                }

                finishedAlert.signal();
            }finally {
                lock.unlock();
            }
        });

        lock.lock();

        try {
            finishedAlert.await();

            if(result[0] != null) {
                return result[0];
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        }finally {
            lock.unlock();
        }

        return "cancel";
    }
}
