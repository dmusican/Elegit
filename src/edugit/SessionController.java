package edugit;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.util.Pair;

import java.io.File;
import java.util.Optional;

/**
 * Created by makik on 6/10/15.
 */
public class SessionController extends Controller {

    private SessionModel theModel;

    public MenuBar menuBar;
    public TextArea commitMessageField;
    public WorkingTreePanelView workingTreePanelView;
	public CommitTreePanelView localCommitTreePanelView;
    public CommitTreePanelView remoteCommitTreePanelView;

    CommitTreeModel localCommitTreeModel;
    CommitTreeModel remoteCommitTreeModel;

    /**
     * Initializes the environment by obtaining the model
     * and putting the views on display.
     *
     * This method is automatically called by JavaFX.
     */
    public void initialize() {
        this.theModel = SessionModel.getSessionModel();
        this.workingTreePanelView.setSessionModel(this.theModel);
        this.localCommitTreeModel = new LocalCommitTreeModel(this.theModel, this.localCommitTreePanelView);
        this.remoteCommitTreeModel = new RemoteCommitTreeModel(this.theModel, this.remoteCommitTreePanelView);

        this.initializeMenuBar();
    }

    /**
     * Sets up the MenuBar by adding some options to it (for cloning).
     *
     * Each option offers a different way of loading a repository, and each
     * option instantiates the appropriate RepoHelper class for the chosen
     * loading method.
     */
    private void initializeMenuBar() {
        // TODO: break this out into a separate controller?
        Menu openMenu = new Menu("Load a Repository");

        MenuItem cloneOption = new MenuItem("Clone");
        cloneOption.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent t) {
                File cloneRepoDirectory = getPathFromChooser(true, "Choose a Location", null);

                // NOTE: This is all stuff that uses pretty new Java features,
                // so make sure you have JDK 8u40 or later!
                //  Largely copied from: http://code.makery.ch/blog/javafx-dialogs-official/

                // Create the custom dialog.
                Dialog<Pair<String, String>> dialog = new Dialog<>();
                dialog.setTitle("Login");
                dialog.setHeaderText("Enter your credentials.");

                // Set the button types.
                ButtonType loginButtonType = new ButtonType("Login", ButtonBar.ButtonData.OK_DONE);
                dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);

                // Create the username and password labels and fields.
                GridPane grid = new GridPane();
                grid.setHgap(10);
                grid.setVgap(10);
                grid.setPadding(new Insets(20, 150, 10, 10));

                TextField username = new TextField();
                username.setPromptText("Username");
                PasswordField password = new PasswordField();
                password.setPromptText("Password");

                grid.add(new Label("Username:"), 0, 0);
                grid.add(username, 1, 0);
                grid.add(new Label("Password:"), 0, 1);
                grid.add(password, 1, 1);

                // Enable/Disable login button depending on whether a username was entered.
                Node loginButton = dialog.getDialogPane().lookupButton(loginButtonType);
                loginButton.setDisable(true);

                // Do some validation (using the Java 8 lambda syntax).
                username.textProperty().addListener((observable, oldValue, newValue) -> {
                    loginButton.setDisable(newValue.trim().isEmpty());
                });

                dialog.getDialogPane().setContent(grid);

                // Request focus on the username field by default.
                Platform.runLater(() -> username.requestFocus());

                // Convert the result to a username-password-pair when the login button is clicked.
                dialog.setResultConverter(dialogButton -> {
                    if (dialogButton == loginButtonType) {
                        return new Pair<>(username.getText(), password.getText());
                    }
                    return null;
                });

                Optional<Pair<String, String>> result = dialog.showAndWait();

                result.ifPresent(usernamePassword -> {
                    try{
                        RepoHelper repoHelper = new ClonedRepoHelper(cloneRepoDirectory.toPath(), "https://github.com/grahamearley/jgit-test.git", usernamePassword.getKey(), usernamePassword.getValue());
                        SessionModel.getSessionModel().openRepoFromHelper(repoHelper);
                    } catch(Exception e){
                        e.printStackTrace();
                    }
                });
            }
        });

        // TODO: understand lambda expressions...
        MenuItem existingOption = new MenuItem("Load existing repository");
        existingOption.setOnAction(t -> {
            File existingRepoDirectory = getPathFromChooser(true, "Choose a Location", null);
            try{
                RepoHelper repoHelper = new ExistingRepoHelper(existingRepoDirectory.toPath(), SECRET_CONSTANTS.TEST_GITHUB_TOKEN);
                SessionModel.getSessionModel().openRepoFromHelper(repoHelper);
            } catch(Exception e){
                e.printStackTrace();
            }
        });

        // TODO: implement New Repository option.
        MenuItem newOption = new MenuItem("Start a new repository");
        newOption.setDisable(true);

        openMenu.getItems().addAll(cloneOption, existingOption, newOption);
        menuBar.getMenus().addAll(openMenu);

    }

    /**
     * Perform the updateFileStatusInRepo() method for each file whose
     * checkbox is checked. Then commit with the commit message and push.
     *
     * TODO: Separate push into different button. Work this in with the local tree view.
     *
     * @param actionEvent the button click event.
     * @throws GitAPIException if the updateFileStatusInRepo() call fails.
     */
    public void handleCommitButton(ActionEvent actionEvent) throws GitAPIException {
        String commitMessage = commitMessageField.getText();

        for (RepoFile checkedFile : this.workingTreePanelView.getCheckedFilesInDirectory()) {
            checkedFile.updateFileStatusInRepo();
        }

        this.theModel.currentRepoHelper.commit(commitMessage);
        this.theModel.currentRepoHelper.pushAll();

    }

    public void handleMergeButton(ActionEvent actionEvent){
    }

    public void handlePushButton(ActionEvent actionEvent){
    }

    public void handleFetchButton(ActionEvent actionEvent){

    }

    /**
     * Reloads the panel views when the button is clicked.
     *
     * TODO: Implement automatic refresh!
     *
     * @param actionEvent the button click event.
     * @throws GitAPIException if the drawDirectoryView() call fails.
     * @throws IOException if the drawDirectoryView() call fails.
     */
    public void handleReloadButton(ActionEvent actionEvent) throws GitAPIException, IOException {
        this.workingTreePanelView.drawDirectoryView();
        this.localCommitTreeModel.update();
        this.remoteCommitTreeModel.update();
    }

//    public void handleCloneToDestinationButton(ActionEvent actionEvent) {
//        File cloneRepoFile = getPathFromChooser(true, "Choose a Location", ((Button)actionEvent.getSource()).getScene().getWindow());
//        try{
//            RepoHelper repoHelper = new ClonedRepoHelper(cloneRepoFile.toPath(), SECRET_CONSTANTS.TEST_GITHUB_TOKEN);
//            this.theModel.openRepoFromHelper(repoHelper);
//        } catch(Exception e){
//            e.printStackTrace();
//        }
//    }
}
