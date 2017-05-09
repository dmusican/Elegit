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
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.controlsfx.control.CheckListView;

import java.util.*;
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
     *
     * @return String user's response to the dialog
     */
    static String showCommittingConflictingFileAlert() {
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

        if (result.orElse(null) == resolveButton) {
            logger.info("Chose to resolve conflicts");
            resultType = "resolve";
        } else if (result.orElse(null) == addButton) {
            logger.info("Chose to add file");
            resultType = "add";
        } else if (result.orElse(null) == helpButton) {
            logger.info("Chose to get help");
            resultType = "help";
        } else {
            // User cancelled the dialog
            logger.info("Cancelled dialog");
            resultType = "cancel";
        }

        return resultType;
    }

    /**
     * Asks the user for permission to log anonymous usage data
     * @return true if the user selected yes to
     */
    public static boolean getLoggingPermissions() {
        Alert window = new Alert(Alert.AlertType.INFORMATION);

        ButtonType okButton = new ButtonType("Share");
        ButtonType buttonTypeCancel = new ButtonType("Don't Share", ButtonBar.ButtonData.CANCEL_CLOSE);

        window.getButtonTypes().setAll(okButton, buttonTypeCancel);
        window.setResizable(true);
        window.getDialogPane().setPrefSize(300, 200);
        window.setTitle("Usage Data");
        window.setHeaderText("Share anonymous usage data");
        window.setContentText("Click Share if you want to share anonymous usage data with us, " +
                "which helps us improve this tool. This can be changed at any time with the " +
                "preferences menu.");
        Optional<ButtonType> result = window.showAndWait();

        return result.orElse(null) == okButton;
    }

    /**
     * Shows a window with instructions on how to fix a conflict
     */
    static void showConflictingHelpAlert() {
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
    static void showResetHelpAlert() {
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
    static void showRevertHelpAlert() {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.getDialogPane().setPrefWidth(500);
            alert.setTitle("Revert Help");
            alert.setHeaderText("What is revert?");
            ImageView img = new ImageView(new Image("/elegit/images/undo.png"));
            img.setFitHeight(60);
            img.setFitWidth(60);
            alert.setGraphic(img);
            alert.setContentText("Basically, git revert takes your current files, " +
                    "and deletes any changes from the commit(s) you give it, making a new commit. " +
                    "See\n\nhttp://dmusican.github.io/Elegit/jekyll/update/2016/08/04/what-is-revert.html\n\n" +
                    "for more information");
            alert.showAndWait();
        });
    }

    /**
     * Shows a warning about checking out files from the index
     *
     * @return
     */
    public static boolean showCheckoutAlert() {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        ButtonType checkout = new ButtonType("Checkout");
        ButtonType cancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(checkout, cancel);

        alert.setTitle("Checkout Warning");
        alert.setContentText("Are you sure you want to checkout the selected files?\n" +
                "This will discard all changes that have not been added (staged).");
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == checkout;
    }

    /**
     * Informs the user that they are adding a previously conflicting file
     *
     * @return String result from user input
     */
    static String showAddingingConflictingThenModifiedFileAlert() {
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

        if (result.orElse(null) == commitButton) {
            logger.info("Chose to add");
            resultType = "add";
        } else {
            // User cancelled the dialog
            logger.info("Cancelled dialog");
            resultType = "cancel";
        }

        return resultType;
    }

    /**
     * Informs the user that they are tracking ignored files
     *
     * @param trackedIgnoredFiles collections of files being ignored
     */
    static void showTrackingIgnoredFilesWarning(Collection<String> trackedIgnoredFiles) {
        Platform.runLater(() -> {
            if (trackedIgnoredFiles.size() > 0) {
                String fileStrings = "";
                for (String s : trackedIgnoredFiles) {
                    fileStrings += "\n" + s;
                }
                Alert alert = new Alert(Alert.AlertType.WARNING, "The following files are being tracked by Git, " +
                                                                 "but also match an ignore pattern. If you want to ignore these files, remove them from Git.\n" + fileStrings);
                alert.showAndWait();
            }
        });
    }

    /**
     * Informs the user that there are conflicting files so they can't checkout a different branch
     *
     * @param conflictingPaths conflicting files
     */
    public static void showCheckoutConflictsAlert(List<String> conflictingPaths) {
        logger.warn("Checkout conflicts warning");
        String conflictList = "";
        for (String pathName : conflictingPaths) {
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
     *
     * @param conflictingPaths conflicting files
     */
    public static void showMergeConflictsAlert(List<String> conflictingPaths) {
        logger.warn("Merge conflicts warning");
        String conflictList = "";
        for (String pathName : conflictingPaths) {
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

        if (result.orElse(null) == trackButton) {
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
        alert.getButtonTypes().addAll(deleteButton, cancelButton);

        Optional<?> result = alert.showAndWait();

        return result.orElse(null) == deleteButton;
    }

    static String pickRemoteToPushTo(Set<String> remotes) {
        ReentrantLock lock = new ReentrantLock();
        Condition finishedAlert = lock.newCondition();

        final String[] result = new String[1];

        Platform.runLater(() -> {
            try {
                lock.lock();

                Alert alert = new Alert(Alert.AlertType.NONE);
                alert.setTitle("Multiple remotes found");
                alert.setHeaderText("There are multiple remote repositories associated with this repository.\nPick one to push to.");

                ComboBox<String> remoteRepos = new ComboBox<>();
                remoteRepos.setPromptText("Choose a remote...");
                remoteRepos.setItems(FXCollections.observableArrayList(remotes));

                ButtonType okButton = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
                ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

                alert.getDialogPane().setContent(remoteRepos);
                alert.getButtonTypes().addAll(cancelButton, okButton);

                Optional<?> alertResult = alert.showAndWait();

                if (alertResult.isPresent()) {
                    if (alertResult.get() == okButton) {
                        result[0] = remoteRepos.getSelectionModel().getSelectedItem();
                    }
                }

                finishedAlert.signal();
            } finally {
                lock.unlock();
            }
        });

        lock.lock();

        try {
            finishedAlert.await();

            if (result[0] != null) {
                return result[0];
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }

        return "cancel";
    }

    public static String getCommitMessage() {
        Alert alert = new Alert(Alert.AlertType.NONE);
        alert.setTitle("Commit message");
        alert.setResizable(true);

        TextArea textArea = new TextArea();
        textArea.setPromptText("Commit Message...");
        textArea.setWrapText(true);
        textArea.setPrefSize(250, 150);
        textArea.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);

        HBox hBox = new HBox(textArea);
        hBox.setAlignment(Pos.CENTER);

        ButtonType okButton = new ButtonType("Commit", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getDialogPane().setContent(hBox);
        alert.getButtonTypes().addAll(cancelButton, okButton);

        Optional<?> alertResult = alert.showAndWait();

        if (alertResult.isPresent()) {
            if (alertResult.get() == okButton && !textArea.getText().equals("")) {
                return textArea.getText();
            }
        }
        return "cancel";
    }

    static ArrayList<LocalBranchHelper> getUntrackedBranchesToPush(ArrayList<LocalBranchHelper> branches) {

        final ArrayList<LocalBranchHelper> result = new ArrayList<>(branches.size());

        Alert alert = new Alert(Alert.AlertType.NONE);
        alert.setTitle("Untracked local branches");
        alert.setHeaderText("The branches below are not tracked remotely.\n" +
                            "Select the branches you want to create an upstream remote branch for.");

        CheckListView<LocalBranchHelper> untrackedBranches = new CheckListView<>(FXCollections.observableArrayList(branches));

        ButtonType okButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType trackButton = new ButtonType("Track Branches", ButtonBar.ButtonData.APPLY);
        alert.getDialogPane().setContent(untrackedBranches);
        alert.getButtonTypes().addAll(trackButton, okButton);

        Optional<?> alertResult = alert.showAndWait();

        if (alertResult.isPresent()) {
            if (alertResult.get() == trackButton) {
                result.addAll(untrackedBranches.getCheckModel().getCheckedItems());
            }
        }

        if (result.size() > 0)
            return result;
        else
            return null;
    }

    static boolean trackCurrentBranchRemotely(String branchName) {

        final boolean[] result = new boolean[1];
        result[0] = false;

        Alert alert = new Alert(Alert.AlertType.NONE);
        alert.setTitle("push -u");

        Label branchLabel = new Label(branchName);
        HBox branchBox = new HBox(branchLabel);
        // The CSS style classes weren't working here
        branchBox.setStyle("    -fx-background-color: #1E90FF;\n" +
                           "    -fx-background-radius: 5;\n" +
                           "    -fx-padding: 0 3 0 3;");
        branchLabel.setStyle("    -fx-text-fill: #FFFFFF;\n" +
                             "    -fx-font-size: 14px;\n" +
                             "    -fx-font-weight: bold;\n" +
                             "    -fx-text-align: center;");

        Text txt1 = new Text(" is not currently tracked remotely.");
        Text txt2 = new Text("Would you like to create an upstream remote branch?");
        txt1.setFont(new Font(14));
        txt2.setFont(new Font(14));

        HBox hBox = new HBox(branchBox, txt1);

        VBox vBox = new VBox(hBox, txt2);
        vBox.setSpacing(10);
        vBox.setAlignment(Pos.CENTER);

        ButtonType okButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType trackButton = new ButtonType("Yes", ButtonBar.ButtonData.APPLY);

        alert.getDialogPane().setContent(vBox);
        alert.getButtonTypes().addAll(trackButton, okButton);

        Optional<?> alertResult = alert.showAndWait();

        if (alertResult.isPresent()) {
            if (alertResult.get() == trackButton) {
                result[0] = true;
            }
        }

        return result[0];
    }
}
