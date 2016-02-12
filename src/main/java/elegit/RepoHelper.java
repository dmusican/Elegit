package main.java.elegit;

import javafx.event.EventHandler;
import javafx.stage.WindowEvent;
import main.java.elegit.exceptions.ConflictingFilesException;
import main.java.elegit.exceptions.MissingRepoException;
import main.java.elegit.exceptions.PushToAheadRemoteError;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Pair;
import main.java.elegit.exceptions.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.controlsfx.control.NotificationPane;
import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revplot.PlotCommitList;
import org.eclipse.jgit.revplot.PlotLane;
import org.eclipse.jgit.revplot.PlotWalk;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTag;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/**
 * The abstract RepoHelper class, used for interacting with a repository.
 */
public abstract class RepoHelper {

    //protected UsernamePasswordCredentialsProvider ownerAuth;

    public String username;
    protected String password;

    private Repository repo;
    protected String remoteURL;

    protected Path localPath;

	private List<CommitHelper> localCommits;
    private List<CommitHelper> remoteCommits;

    private List<TagHelper> localTags;
    private List<TagHelper> remoteTags;

    private Map<String, CommitHelper> commitIdMap;
    private Map<ObjectId, String> idMap;
    private Map<String, TagHelper> tagIdMap;

    private List<LocalBranchHelper> localBranches;
    private List<RemoteBranchHelper> remoteBranches;
    private LocalBranchHelper branchHelper;
    private BranchManagerModel branchManagerModel;

    public BooleanProperty hasRemoteProperty;
    public BooleanProperty hasUnpushedCommitsProperty;
    public BooleanProperty hasUnmergedCommitsProperty;
    public BooleanProperty hasUnpushedTagsProperty;

    static final Logger logger = LogManager.getLogger();

    /**
     * Creates a RepoHelper object for holding a Repository and interacting with it
     * through JGit.
     *
     * @param directoryPath the path of the repository.
     * @throws GitAPIException if the obtainRepository() call throws this exception..
     * @throws IOException if the obtainRepository() call throws this exception.
     * @throws CancelledAuthorizationException if the obtainRepository() call throws this exception.
     */
    public RepoHelper(Path directoryPath, String remoteURL, String username) throws GitAPIException, IOException, CancelledAuthorizationException {
        this.remoteURL = remoteURL;
        this.username = username;

        this.localPath = directoryPath;

        this.repo = this.obtainRepository();

        this.commitIdMap = new HashMap<>();
        this.idMap = new HashMap<>();
        this.tagIdMap = new HashMap<>();

        this.localCommits = this.parseAllLocalCommits();
        this.remoteCommits = this.parseAllRemoteCommits();

        this.localTags = this.getAllLocalTags();

        this.branchManagerModel = new BranchManagerModel(this.callGitForLocalBranches(), this.callGitForRemoteBranches(), this);

        hasRemoteProperty = new SimpleBooleanProperty(!getLinkedRemoteRepoURLs().isEmpty());

        hasUnpushedCommitsProperty = new SimpleBooleanProperty(getAllCommitIDs().size() > remoteCommits.size());
        hasUnmergedCommitsProperty = new SimpleBooleanProperty(getAllCommitIDs().size() > localCommits.size());

        hasUnpushedTagsProperty = new SimpleBooleanProperty(false);
    }

    /// Constructor for ExistingRepoHelpers to inherit (they don't need the Remote URL)
    public RepoHelper(Path directoryPath, String username) throws GitAPIException, IOException, CancelledAuthorizationException {
        this.username = username;

        this.localPath = directoryPath;

        this.repo = this.obtainRepository();

        this.commitIdMap = new HashMap<>();
        this.idMap = new HashMap<>();
        this.tagIdMap = new HashMap<>();

        this.localCommits = this.parseAllLocalCommits();
        this.remoteCommits = this.parseAllRemoteCommits();

        this.localTags = this.getAllLocalTags();

        this.branchManagerModel = new BranchManagerModel(this.callGitForLocalBranches(), this.callGitForRemoteBranches(), this);

        hasRemoteProperty = new SimpleBooleanProperty(!getLinkedRemoteRepoURLs().isEmpty());

        hasUnpushedCommitsProperty = new SimpleBooleanProperty(getAllCommitIDs().size() > remoteCommits.size());
        hasUnmergedCommitsProperty = new SimpleBooleanProperty(getAllCommitIDs().size() > localCommits.size());

        hasUnpushedTagsProperty = new SimpleBooleanProperty();
    }

    /**
     * @return true if the corresponding repository still exists in the expected location
     */
    public boolean exists(){
        logger.debug("Checked if repo still exists");
        return localPath.toFile().exists() && localPath.toFile().list((dir, name) -> name.equals(".git")).length > 0;
    }

    /**
     * Gets or builds the repository using the appropriate method for
     * the kind of repository (new, cloned, or existing).
     *
     * @return the RepoHelper's repository.
     * @throws GitAPIException (see subclasses).
     * @throws IOException (see subclasses).
     */
    protected abstract Repository obtainRepository() throws GitAPIException, IOException, CancelledAuthorizationException;

    /**
     * Adds a file to the repository.
     *
     * @param filePath the path of the file to add.
     * @throws GitAPIException if the `git add` call fails.
     */
    public void addFilePath(Path filePath) throws GitAPIException {
        Git git = new Git(this.repo);
        // git add:
        Path relativizedFilePath = this.localPath.relativize(filePath);
        git.add()
                .addFilepattern(relativizedFilePath.toString())
                .call();
        git.close();
    }

    /**
     * Adds multiple files to the repository.
     *
     * @param filePaths an ArrayList of file paths to add.
     * @throws GitAPIException if the `git add` call fails.
     */
    public void addFilePaths(ArrayList<Path> filePaths) throws GitAPIException {
        Git git = new Git(this.repo);
        // git add:
        AddCommand adder = git.add();
        for (Path filePath : filePaths) {
            Path localizedFilePath = this.localPath.relativize(filePath);
            adder.addFilepattern(localizedFilePath.toString());
        }
        adder.call();
        git.close();
    }

    /**
     * Gets a list of all remotes associated with this repository. The URLs
     * correspond to the output seen by running 'git remote -v'
     * @return a list of the remote URLs associated with this repository
     */
    public List<String> getLinkedRemoteRepoURLs(){
        Config storedConfig = this.repo.getConfig();
        Set<String> remotes = storedConfig.getSubsections("remote");
        ArrayList<String> urls = new ArrayList<>(remotes.size());
        for(String remote : remotes){
            urls.add(storedConfig.getString("remote", remote, "url"));
        }
        return urls;
    }

    /**
     * @return true if this repository has an associated remote
     */
    public boolean hasRemote(){
        return hasRemoteProperty.get();
    }

    /**
     * @return true if there are local commits that haven't been pushed
     */
    public boolean hasUnpushedCommits(){
        return hasUnpushedCommitsProperty.get();
    }

    /**
     * @return true if there are local tags that haven't been pushed
     */
    public boolean hasUnpushedTags(){
        return hasUnpushedTagsProperty.get();
    }

    /**
     * @return true if there are remote commits that haven't been merged into local
     */
    public boolean hasUnmergedCommits(){
        return hasUnmergedCommitsProperty.get();
    }

    /**
     * Commits changes to the repository.
     *
     * @param commitMessage the message for the commit.
     * @throws GitAPIException if the `git commit` call fails.
     */
    public void commit(String commitMessage) throws GitAPIException, MissingRepoException {
        logger.info("Attempting commit");
        if(!exists()) throw new MissingRepoException();
        // should this Git instance be class-level?
        Git git = new Git(this.repo);
        // git commit:
        git.commit()
                .setMessage(commitMessage)
                .call();
        git.close();
        this.hasUnpushedCommitsProperty.set(true);
    }

    /**
     * Tags a commit
     *
     * @param tagName the name for the tag.
     * @throws GitAPIException if the 'git tag' call fails.
     */
    public void tag(String tagName, String commitName) throws GitAPIException, MissingRepoException, IOException {
        logger.info("Attempting tag");
        if(!exists()) throw new MissingRepoException();
        Git git = new Git(this.repo);
        // This creates a lightweight tag
        // TODO: add support for annotated (heavyweight) tag
        CommitHelper c = commitIdMap.get(commitName);
        Ref r = git.tag().setName(tagName).setObjectId(c.getCommit()).setAnnotated(false).call();
        git.close();
        TagHelper t = makeTagHelper(r,tagName);
        this.hasUnpushedTagsProperty.set(true);
    }

    /**
     * Pushes all changes.
     *
     * @throws GitAPIException if the `git push` call fails.
     */
    public void pushAll(UsernamePasswordCredentialsProvider ownerAuth) throws GitAPIException, MissingRepoException, PushToAheadRemoteError {
        logger.info("Attempting push");
        if(!exists()) throw new MissingRepoException();
        if(!hasRemote()) throw new InvalidRemoteException("No remote repository");
        Git git = new Git(this.repo);
        PushCommand push = git.push().setPushAll();

        if (ownerAuth != null) {
            push.setCredentialsProvider(ownerAuth);
        }
//        ProgressMonitor progress = new TextProgressMonitor(new PrintWriter(System.out));
        ProgressMonitor progress = new SimpleProgressMonitor();
        push.setProgressMonitor(progress);

        Iterable<PushResult> pushResult = push.call();
        boolean allPushesWereRejected = true;
        boolean anyPushWasRejected = false;

        for (PushResult result : pushResult) {
            for (RemoteRefUpdate remoteRefUpdate : result.getRemoteUpdates()) {
                if (remoteRefUpdate.getStatus() != (RemoteRefUpdate.Status.REJECTED_NONFASTFORWARD)) {
                    allPushesWereRejected = false;
                } else {
                    anyPushWasRejected = true;
                }
            }
        }

        if (allPushesWereRejected || anyPushWasRejected) {
            throw new PushToAheadRemoteError(allPushesWereRejected);
        }

        git.close();
        this.hasUnpushedCommitsProperty.set(false);
    }

    /**
     * Pushes all tags in /refs/tags/.
     *
     * @throws GitAPIException if the `git push --tags` call fails.
     */
    public void pushTags(UsernamePasswordCredentialsProvider ownerAuth) throws GitAPIException, MissingRepoException, PushToAheadRemoteError {
        logger.info("Attempting push tags");
        if(!exists()) throw new MissingRepoException();
        if(!hasRemote()) throw new InvalidRemoteException("No remote repository");
        Git git = new Git(this.repo);
        PushCommand push = git.push().setPushAll();

        if (ownerAuth != null) {
            push.setCredentialsProvider(ownerAuth);
        }
//        ProgressMonitor progress = new TextProgressMonitor(new PrintWriter(System.out));
        ProgressMonitor progress = new SimpleProgressMonitor();
        push.setProgressMonitor(progress);

        Iterable<PushResult> pushResult = push.setPushTags().call();
        boolean allPushesWereRejected = true;
        boolean anyPushWasRejected = false;

        for (PushResult result : pushResult) {
            for (RemoteRefUpdate remoteRefUpdate : result.getRemoteUpdates()) {
                if (remoteRefUpdate.getStatus() != (RemoteRefUpdate.Status.REJECTED_NONFASTFORWARD)) {
                    allPushesWereRejected = false;
                } else {
                    anyPushWasRejected = true;
                }
            }
        }

        if (allPushesWereRejected || anyPushWasRejected) {
            throw new PushToAheadRemoteError(allPushesWereRejected);
        }

        git.close();
        this.hasUnpushedTagsProperty.set(false);
    }

    /**
     * Fetches changes into FETCH_HEAD (`git -fetch`).
     *
     * @throws GitAPIException
     * @throws MissingRepoException
     */
    public boolean fetch(UsernamePasswordCredentialsProvider ownerAuth) throws GitAPIException, MissingRepoException{
        logger.info("Attempting fetch");
        if(!exists()) throw new MissingRepoException();
        Git git = new Git(this.repo);

        FetchCommand fetch = git.fetch().setTagOpt(TagOpt.AUTO_FOLLOW);

        if (ownerAuth != null) {
            fetch.setCredentialsProvider(ownerAuth);
        }

        // The JGit docs say that if setCheckFetchedObjects
        //  is set to true, objects received will be checked for validity.
        //  Not sure what that means, but sounds good so I'm doing it...
        fetch.setCheckFetchedObjects(true);

//        ProgressMonitor progress = new TextProgressMonitor(new PrintWriter(System.out));
        ProgressMonitor progress = new SimpleProgressMonitor();
        fetch.setProgressMonitor(progress);

        FetchResult result = fetch.call();
        git.close();
        this.hasUnmergedCommitsProperty.set(this.hasUnmergedCommits() || !result.getTrackingRefUpdates().isEmpty());
        return !result.getTrackingRefUpdates().isEmpty();
    }

    /**
     * Merges FETCH_HEAD into the current branch.
     * Combining fetch and merge is the same as `git -pull`.
     *
     * @throws IOException
     * @throws GitAPIException
     * @throws MissingRepoException
     */
    public boolean mergeFromFetch() throws IOException, GitAPIException, MissingRepoException, ConflictingFilesException {
        logger.info("Attempting merge from fetch");
        if(!exists()) throw new MissingRepoException();
        if(!hasRemote()) throw new InvalidRemoteException("No remote repository");
        Git git = new Git(this.repo);
        ObjectId fetchHeadID = this.repo.resolve("FETCH_HEAD");
//        if(fetchHeadID == null); // This might pop up at some point as an issue. Might not though
        MergeResult result = git.merge()
                .include(fetchHeadID)
                .call();
        git.close();

        MergeResult.MergeStatus status = result.getMergeStatus();
        this.hasUnmergedCommitsProperty.set(status == MergeResult.MergeStatus.ABORTED || status == MergeResult.MergeStatus.CHECKOUT_CONFLICT);
        this.hasUnpushedCommitsProperty.set(this.hasUnpushedCommits() || status == MergeResult.MergeStatus.MERGED);
        if(status == MergeResult.MergeStatus.CONFLICTING) throw new ConflictingFilesException(result.getConflicts());
        return result.getMergeStatus().isSuccessful();
    }

    /**
     * Presents dialogs that request the user's username and password,
     * and sets the username and password fields accordingly.
     *
     * @throws CancelledAuthorizationException if the user presses cancel or closes the dialog.
     */
    public UsernamePasswordCredentialsProvider presentAuthorizeDialog() throws CancelledAuthorizationException {
        logger.info("Creating authorization dialog");
        // Create the custom dialog.
        Dialog<Pair<String,Pair<String,Boolean>>> dialog = new Dialog<>();
        dialog.setTitle("Authorize");
        dialog.setHeaderText("Please enter your remote repository password.");

        // Set the button types.
        ButtonType loginButtonType = new ButtonType("Authorize", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);

        dialog.setOnCloseRequest(new EventHandler<DialogEvent>() {
            @Override
            public void handle(DialogEvent event) {
                logger.info("Closing authorization dialog");
            }
        });

        // Create the username and password labels and fields.
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        grid.add(new Label("Username:"), 0, 0);

        // Conditionally ask for the username if it hasn't yet been set.
        TextField username = new TextField();
        if (this.username == null) {
            username.setPromptText("Username");
            grid.add(username, 1, 0);
        } else {
            grid.add(new Label(this.username), 1, 0);
        }

        grid.add(new Label("Password:"), 0, 1);

        PasswordField password = new PasswordField();
        CheckBox remember = new CheckBox("Remember Password");

        if (this.password != null) {
            password.setText(this.password);
            remember.setSelected(true);
        }
        password.setPromptText("Password");
        grid.add(password, 1, 1);

        remember.setIndeterminate(false);
        grid.add(remember, 1, 2);

        // Enable/Disable login button depending on whether a password was entered.
        Node loginButton = dialog.getDialogPane().lookupButton(loginButtonType);
        if (this.password == null || this.username == null) {
            loginButton.setDisable(true);
        }

        // Do some validation (using the Java 8 lambda syntax).
        password.textProperty().addListener((observable, oldValue, newValue) -> {
            loginButton.setDisable(newValue.trim().isEmpty());
        });
        username.textProperty().addListener((observable, oldValue, newValue) -> {
            loginButton.setDisable(newValue.trim().isEmpty());
        });

        dialog.getDialogPane().setContent(grid);

        // Request focus for the first text field by default.
        if (this.username == null) {
            Platform.runLater(() -> username.requestFocus());
        } else {
            Platform.runLater(() -> password.requestFocus());
        }

        // Return the password when the authorize button is clicked.
        // If the username hasn't been set yet, then update the username.
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == loginButtonType) {
                if (this.username != null)
                    return new Pair<>(this.username, new Pair<>(password.getText(), new Boolean(remember.isSelected())));
                else
                    return new Pair<>(username.getText(), new Pair<>(password.getText(), new Boolean(remember.isSelected())));
            }
            return null;
        });

        Optional<Pair<String, Pair<String, Boolean>>> result = dialog.showAndWait();

        UsernamePasswordCredentialsProvider ownerAuth;

        if (result.isPresent()) {
            if (this.username == null) {
                this.username = username.getText();
            } else {
                this.username = result.get().getKey();
            }
            //Only store password if remember password was selected
            if (result.get().getValue().getValue()) {
                logger.info("Selected remember password");
                this.password = result.get().getValue().getKey();
            }
            ownerAuth = new UsernamePasswordCredentialsProvider(this.username, result.get().getValue().getKey());
        } else {
            logger.info("Cancelled authorization dialog");
            throw new CancelledAuthorizationException();
        }
        logger.info("Entered authorization credentials");
        return ownerAuth;
    }

    /**
    * Presents dialogs that request the user's username, then sets it for the RepoHelper
    *
    * @throws CancelledUsernameException if the user presses cancel or closes the dialog.
    */
    public void presentUsernameDialog() throws CancelledUsernameException {
        logger.info("Opened username dialog");
        // Create the custom dialog.
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Username");
        dialog.setHeaderText("Please enter your remote repository username.");

        // Set the button types.
        ButtonType okButtonType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okButtonType, ButtonType.CANCEL);

        dialog.setOnCloseRequest(new EventHandler<DialogEvent>() {
            @Override
            public void handle(DialogEvent event) {
                logger.info("Closed username dialog");
            }
        });

        // Create the username and password labels and fields.
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        grid.add(new Label("Username:"), 0, 0);

        // Conditionally ask for the username if it hasn't yet been set.
        TextField username;
        if (this.username == null) {
            username = new TextField();
            username.setPromptText("Username");
        } else {
            username = new TextField(this.username);
        }
        grid.add(username, 1, 0);

        // Enable/Disable login button depending on whether a password was entered.
        Node loginButton = dialog.getDialogPane().lookupButton(okButtonType);
        loginButton.setDisable(true);

        // Do some validation (using the Java 8 lambda syntax).
        username.textProperty().addListener((observable, oldValue, newValue) -> {
            loginButton.setDisable(newValue.trim().isEmpty());
        });

        dialog.getDialogPane().setContent(grid);

        // Request focus for the username text field by default.
        Platform.runLater(() -> username.requestFocus());

        // Return the password when the authorize button is clicked.
        // If the username hasn't been set yet, then update the username.
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == okButtonType) {
                return username.getText();
            }
            return null;
        });

        Optional<String> result = dialog.showAndWait();

        UsernamePasswordCredentialsProvider ownerAuth;

        if (result.isPresent()) {
            logger.info("Set username");
            this.username = result.get();
        } else {
            logger.info("Cancelled username dialog");
            throw new CancelledUsernameException();
        }
    }

    public void closeRepo() {
        this.repo.close();
    }

    /**
     * @return the JGit repository of this RepoHelper
     */
    public Repository getRepo() {
        return this.repo;
    }

    /**
     * @return the local path to the directory holding the repository
     */
    public Path getLocalPath() {
        return localPath;
    }

    /**
     * @return all local commits that have already been parsed
     */
    public List<CommitHelper> getLocalCommits(){
        return this.localCommits;
    }

    /**
     * @return all remote commits that have already been parsed
     */
    public List<CommitHelper> getRemoteCommits(){
        return this.remoteCommits;
    }

    /**
     * Attempts first to use the parameter as an ID string that maps
     * to a commit. If that fails, attempts to parse it as as a reference
     * string and find the ID it maps to, then returning the commit
     * associated with that id
     * @param idOrRefString either an ID or reference string corresponding
     *                      to an object in this repository
     * @return the commit associated with the parameter
     */
    public CommitHelper getCommit(String idOrRefString){
        if(commitIdMap.containsKey(idOrRefString)){
            return commitIdMap.get(idOrRefString);
        }else{
            try{
                return getCommit(repo.resolve(idOrRefString));
            }catch(IOException e){
                logger.error("IOException during getCommit");
                logger.debug(e.getStackTrace());
                return null;
            }
        }
    }

    /**
     * @param id the id of the commit to get
     * @return the commit associated with the given id, if it has been parsed
     */
    public CommitHelper getCommit(ObjectId id){
        if(idMap.containsKey(id)){
            return getCommit(idMap.get(id));
        }else{
            return null;
        }
    }

    public TagHelper getTag(String tagName) {
        return tagIdMap.get(tagName);
    }

    /**
     * @return a list of all commit IDs in this repository
     */
    public List<String> getAllCommitIDs(){
        return new ArrayList<>(commitIdMap.keySet());
    }

    /**
     * @return the head of the current branch
     */
    public CommitHelper getHead(){
        return (this.branchHelper == null) ? null : this.branchHelper.getHead();
    }

    /**
     * Uses JGit to find and parse all local commits between the given branches and
     * every leaf in the repository
     * @param oldLocalBranches the previous branch heads associated with a branch name. Commits
     *                         older than the heads of these branches will be ignored
     * @return all local commits newer than the given branch heads
     * @throws GitAPIException
     * @throws IOException
     */
    public List<CommitHelper> getNewLocalCommits(Map<String, BranchHelper> oldLocalBranches) throws GitAPIException, IOException{
        return getNewCommits(oldLocalBranches, this.callGitForLocalBranches());
    }

    /**
     * Uses JGit to find and parse all remote commits between the given branches and
     * every leaf in the repository
     * @param oldRemoteBranches the previous branch heads associated with a branch name. Commits
     *                         older than the heads of these branches will be ignored
     * @return all remote commits newer than the given branch heads
     * @throws GitAPIException
     * @throws IOException
     */
    public List<CommitHelper> getNewRemoteCommits(Map<String, BranchHelper> oldRemoteBranches) throws GitAPIException, IOException{
        return getNewCommits(oldRemoteBranches, this.callGitForRemoteBranches());
    }

    /**
     * Helper method that returns commits between the given old branch heads and new branch heads
     * @param oldBranches previous locations of branch heads
     * @param newBranches current locations of branch heads
     * @return a list of all commits found between oldBranches and newBranches
     * @throws GitAPIException
     * @throws IOException
     */
    private List<CommitHelper> getNewCommits(Map<String, BranchHelper> oldBranches, List<? extends BranchHelper> newBranches) throws GitAPIException, IOException{
        List<ObjectId> startPoints = new ArrayList<>();
        List<ObjectId> stopPoints = new ArrayList<>();

        for(BranchHelper newBranch : newBranches){
            if(oldBranches.containsKey(newBranch.getBranchName())){
                ObjectId newBranchHeadID = newBranch.getHeadId();
                ObjectId oldBranchHeadID = oldBranches.get(newBranch.getBranchName()).getHeadId();
                if(!newBranchHeadID.equals(oldBranchHeadID)){
                    startPoints.add(newBranchHeadID);
                }
                stopPoints.add(oldBranchHeadID);
            }else{
                startPoints.add(newBranch.getHeadId());
            }
        }
        PlotCommitList<PlotLane> newCommits = this.parseRawCommits(startPoints, stopPoints);
        return wrapRawCommits(newCommits);
    }


    /**
     * Constructs a list of all local commits found by parsing the repository for raw RevCommit objects,
     * then wrapping them into a CommitHelper with the appropriate parents and children
     * @return a list of CommitHelpers for all local commits
     * @throws IOException
     */
    private List<CommitHelper> parseAllLocalCommits() throws IOException, GitAPIException{
        PlotCommitList<PlotLane> commitList = this.parseAllRawLocalCommits();
        return wrapRawCommits(commitList);
    }

    /**
     * Constructs a list of all remote commits found by parsing the repository for raw RevCommit objects,
     * then wrapping them into a CommitHelper with the appropriate parents and children
     * @return a list of CommitHelpers for all remote commits
     * @throws IOException
     */
    private List<CommitHelper> parseAllRemoteCommits() throws IOException, GitAPIException{
        PlotCommitList<PlotLane> commitList = this.parseAllRawRemoteCommits();
        return wrapRawCommits(commitList);
    }

    /**
     * Constructs a list of all local tags found by parsing the tag refs from the repo
     * then wrapping them into a TagHelper with the appropriate commit
     * @return a list of TagHelpers for all the tags
     * @throws IOException
     * @throws GitAPIException
     */
    private List<TagHelper> getAllLocalTags() throws IOException, GitAPIException {
        Map<String, Ref> tagMap = repo.getTags();
        List<TagHelper> tags = new ArrayList<>();
        for (String s: tagMap.keySet()) {
            Ref r = tagMap.get(s);
            tags.add(makeTagHelper(r,s));
        }
        return tags;
    }

    /**
     * Helper method to make a tagHelper given a ref and a name of the tag. Also adds the
     * tag helper to the tagIdMap
     * @param r the ref to make a tagHelper for. This can be a peeled or unpeeled tag
     * @param tagName the name of the tag
     * @return a tagHelper object with the information stored
     * @throws IOException
     * @throws GitAPIException
     */
    private TagHelper makeTagHelper(Ref r, String tagName) throws IOException, GitAPIException {
        String commitName;
        if (r.getPeeledObjectId()!=null) commitName=r.getPeeledObjectId().getName();
        else commitName=r.getObjectId().getName();
        CommitHelper c = this.commitIdMap.get(commitName);
        TagHelper t;
        // If the ref has a peeled objectID, then it is a lightweight tag
        if (r.getPeeledObjectId()==null) {
            t = new TagHelper(tagName, c);
            c.addTag(t);
        }
        // Otherwise, it is an annotated tag
        else {
            ObjectReader objectReader = repo.newObjectReader();
            ObjectLoader objectLoader = objectReader.open(r.getObjectId());
            RevTag tag = RevTag.parse(objectLoader.getBytes());
            objectReader.release();
            t = new TagHelper(tag, c);
            c.addTag(t);
        }
        if (!tagIdMap.containsKey(tagName)) {
            tagIdMap.put(tagName, t);
        }
        return t;
    }

    /**
     * Given a list of raw JGit commit objects, constructs CommitHelper objects to wrap them and gives
     * them the appropriate parents and children. Updates the commit id and id maps appropriately.
     * @param commitList the raw commits to wrap
     * @return a list of CommitHelpers for the given commits
     * @throws IOException
     */
    private List<CommitHelper> wrapRawCommits(PlotCommitList<PlotLane> commitList) throws IOException{
        List<CommitHelper> commitHelperList = new ArrayList<>();
        List<ObjectId> wrappedIDs = new ArrayList<>();
        List<CommitHelper> commitsWithMissingParents = new ArrayList<>();
        for(int i = commitList.size()-1; i >= 0; i--){
            RevCommit curCommit = commitList.get(i);
            ObjectId curCommitID = curCommit.getId();

            if(wrappedIDs.contains(curCommitID)){
                continue;
            }

            CommitHelper curCommitHelper = new CommitHelper(curCommit);
            String curCommitHelperID = curCommitHelper.getId();

            if(!commitIdMap.containsKey(curCommitHelperID)){
                commitIdMap.put(curCommitHelper.getId(), curCommitHelper);
                idMap.put(curCommitID, curCommitHelper.getId());
            }else{
                curCommitHelper = commitIdMap.get(curCommitHelperID);
            }
            wrappedIDs.add(curCommitID);

            RevCommit[] parents = curCommit.getParents();
            for(RevCommit p : parents){
                CommitHelper parentCommitHelper = getCommit(p.getId());
                if(parentCommitHelper == null){
                    commitsWithMissingParents.add(curCommitHelper);
                }else{
                    curCommitHelper.addParent(parentCommitHelper);
                }
            }

            commitHelperList.add(curCommitHelper);
        }
        while(!commitsWithMissingParents.isEmpty()){
            CommitHelper curCommitHelper = commitsWithMissingParents.remove(0);
            RevCommit[] parents = curCommitHelper.commit.getParents();
            for(RevCommit p : parents){
                CommitHelper parentCommitHelper = getCommit(p.getId());
                if(parentCommitHelper == null){
                    commitsWithMissingParents.add(curCommitHelper);
                }else if(!curCommitHelper.getParents().contains(parentCommitHelper)){
                    curCommitHelper.addParent(parentCommitHelper);
                }
            }
        }
        return commitHelperList;
    }

    /**
     * Utilizes JGit to walk through the repo and create raw commit objects - more
     * specifically, JGit objects of (super)type RevCommit. This is an expensive
     * operation and should only be called when necessary
     * @return a list of raw local commits
     * @throws IOException
     */
    private PlotCommitList<PlotLane> parseAllRawLocalCommits() throws IOException, GitAPIException{
        ObjectId headId = repo.resolve("HEAD");
        if(headId == null) return new PlotCommitList<>();
        List<ObjectId> examinedCommitIDs = new ArrayList<>();
        PlotCommitList<PlotLane> rawLocalCommits = parseRawCommits(headId, examinedCommitIDs);
        examinedCommitIDs.add(headId);

        List<LocalBranchHelper> branches = callGitForLocalBranches();
        for(BranchHelper branch : branches){
            ObjectId branchId = branch.getHeadId();
            PlotCommitList<PlotLane> toAdd = parseRawCommits(branchId, examinedCommitIDs);
            if(toAdd.size() > 0){
                rawLocalCommits.addAll(toAdd);
                examinedCommitIDs.add(toAdd.get(0).getId());
            }
        }
        return rawLocalCommits;
    }

    /**
     * Utilizes JGit to walk through the repo and create raw commit objects - more
     * specifically, JGit objects of (super)type RevCommit. This is an expensive
     * operation and should only be called when necessary
     * @return a list of raw remote commits
     * @throws IOException
     */
    private PlotCommitList<PlotLane> parseAllRawRemoteCommits() throws IOException, GitAPIException{
        List<ObjectId> examinedCommitIDs = new ArrayList<>();
        PlotCommitList<PlotLane> rawRemoteCommits = new PlotCommitList<>();

        List<RemoteBranchHelper> branches = callGitForRemoteBranches();
        for(BranchHelper branch : branches){
            ObjectId branchId = branch.getHeadId();
            PlotCommitList<PlotLane> toAdd = parseRawCommits(branchId, examinedCommitIDs);
            if(toAdd.size() > 0){
                rawRemoteCommits.addAll(toAdd);
                examinedCommitIDs.add(toAdd.get(0).getId());
            }
        }
        return rawRemoteCommits;
    }

    /**
     * Utilizes JGit to walk through the repo and create raw commit objects - more
     * specifically, JGit objects of (super)type RevCommit. This is an expensive
     * operation and should only be called when necessary
     * @param startPoints the starting ids to parse from
     * @param stopPoints the ids at which parsing should stop
     * @return a list of raw commits starting from each id in startPoints, excluding those beyond each id in stopPoints
     * @throws IOException
     */
    private PlotCommitList<PlotLane> parseRawCommits(List<ObjectId> startPoints, List<ObjectId> stopPoints) throws IOException{
        PlotCommitList<PlotLane> plotCommitList = new PlotCommitList<>();

        PlotWalk w = new PlotWalk(repo);
        for(ObjectId stopId : stopPoints){
            w.markUninteresting(w.parseCommit(stopId));
        }

        for(ObjectId startId : startPoints){
            w.markStart(w.parseCommit(startId));

            PlotCommitList<PlotLane> temp = new PlotCommitList<>();
            temp.source(w);
            temp.fillTo(Integer.MAX_VALUE);

            plotCommitList.addAll(temp);
        }

        w.dispose();

        return plotCommitList;
    }

    /**
     * Utilizes JGit to walk through the repo and create raw commit objects - more
     * specifically, JGit objects of (super)type RevCommit. This is an expensive
     * operation and should only be called when necessary
     * @param startingID the starting point to parse from
     * @param stopPoints the ids at which parsing should stop
     * @return a list of raw commits starting from the given id
     * @throws IOException
     */
    private PlotCommitList<PlotLane> parseRawCommits(ObjectId startingID, List<ObjectId> stopPoints) throws IOException{
        List<ObjectId> asList = new ArrayList<>(1);
        asList.add(startingID);
        return parseRawCommits(asList, stopPoints);
    }

    /**
     * Utilizes JGit to parse a commit with the given ID and returns it in
     * raw format
     * @param id the ID of the commit
     * @return the raw commit corresponding to the given ID
     * @throws IOException
     */
    public RevCommit parseRawCommit(ObjectId id) throws IOException{
        RevWalk w = new RevWalk(repo);
        w.dispose();
        return w.parseCommit(id);
    }

    @Override
    public String toString() {
        return this.localPath.getFileName().toString();
    }

    /**
     * Utilizes JGit to get a list of all local branches
     * @return a list of all local branches
     * @throws GitAPIException
     * @throws IOException
     */
    public List<LocalBranchHelper> callGitForLocalBranches() throws GitAPIException, IOException {
        List<Ref> getBranchesCall = new Git(this.repo).branchList().call();

        if(localBranches != null){
            for(BranchHelper branch : localBranches) getCommit(branch.getHeadId()).removeAsHead(branch);
        }

        localBranches = new ArrayList<>();

        for (Ref ref : getBranchesCall) localBranches.add(new LocalBranchHelper(ref, this));

        return localBranches;
    }

    public List<LocalBranchHelper> getLocalBranchesFromManager() {
        return this.branchManagerModel.getLocalBranches();
    }

    /**
     * Utilizes JGit to get a list of all remote branches
     * @return a list of all remtoe branches
     * @throws GitAPIException
     */
    public List<RemoteBranchHelper> callGitForRemoteBranches() throws GitAPIException, IOException{
        List<Ref> getBranchesCall = new Git(this.repo).branchList().setListMode(ListBranchCommand.ListMode.REMOTE).call();

        if(remoteBranches != null){
            for(BranchHelper branch : remoteBranches) getCommit(branch.getHeadId()).removeAsHead(branch);
        }

        remoteBranches = new ArrayList<>();

        for (Ref ref : getBranchesCall) {

            // It appears that grabbing the remote branches also gets the HEAD.
            if (!ref.getName().equals("HEAD")) {
                remoteBranches.add(new RemoteBranchHelper(ref, this));
            }
        }

        return remoteBranches;
    }

    /**
     * Sets the currently checkout branch. Does not call 'git checkout'
     * or any variation, simply updates the local variable
     * @param branchHelper the new current branch
     */
    public void setCurrentBranch(LocalBranchHelper branchHelper) {
        this.branchHelper = branchHelper;
    }

    /**
     * @return the currently checkout out branch
     */
    public LocalBranchHelper getCurrentBranch() {
        return this.branchHelper;
    }

    /**
     * Updates the current branch by checking the repository for which
     * branch is currently checked out
     * @throws IOException
     */
    public void refreshCurrentBranch() throws IOException {
        String currentBranchRefString = this.repo.getFullBranch();

        for(LocalBranchHelper branch : localBranches){
            if(branch.getRefPathString().equals(currentBranchRefString)){
                this.setCurrentBranch(branch);
                return;
            }
        }

        LocalBranchHelper currentBranch = new LocalBranchHelper(currentBranchRefString, this);
        this.setCurrentBranch(currentBranch);
    }

    public void showBranchManagerWindow() throws IOException {
        logger.info("Opened branch manager window");
        // Create and display the Stage:
        NotificationPane fxmlRoot = FXMLLoader.load(getClass().getResource("/elegit/fxml/BranchManager.fxml"));

        Stage stage = new Stage();
        stage.setTitle("Branch Manager");
        stage.setScene(new Scene(fxmlRoot, 550, 450));
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                logger.info("Closed branch manager window");
            }
        });
        stage.show();
    }

    /**
     * Gets a list of the local branches of this repository. Also updates
     * the head of each local branch if it was missing
     * @return the local branches of this repository
     */
    public List<BranchHelper> getLocalBranches(){
        for(BranchHelper branch : localBranches){
            if(branch.getHead() == null){
                try{
                    branch.getHeadId();
                }catch(IOException e){
                    logger.error("IOException getting local branches");
                    logger.debug(e.getStackTrace());
                    e.printStackTrace();
                }
            }
        }
        return new ArrayList<>(localBranches);
    }

    /**
     * Gets a list of the remote branches of this repository. Also updates
     * the head of each remote branch if it was missing
     * @return the remote branches of this repository
     */
    public List<BranchHelper> getRemoteBranches(){
        for(BranchHelper branch : remoteBranches){
            if(branch.getHead() == null){
                try{
                    branch.getHeadId();
                }catch(IOException e){
                    logger.error("IOException getting remote branches");
                    logger.debug(e.getStackTrace());
                    e.printStackTrace();
                }
            }
        }
        return new ArrayList<>(remoteBranches);
    }

    /**
     * Checks to see if the given branch is tracked. If branch is
     * a local branch, looks to see if there is a branch in the
     * remote branches that has the same name, and vice versa.
     * Note that tracking status is determined solely by name
     * @param branch the branch to check
     * @return true if branch is being tracked, else false
     */
    public boolean isBranchTracked(BranchHelper branch){
        String branchName = branch.getBranchName();
        if(branch instanceof LocalBranchHelper){
            for(BranchHelper remote : remoteBranches){
                if(remote.getBranchName().equals(branchName)){
                    return true;
                }
            }
        }else{
            for(BranchHelper local : localBranches){
                if(local.getBranchName().equals(branchName)){
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Gets a list of references (branch heads and tags) from the
     * remote repository without fetching any changes. Equivalent
     * to 'git ls-remote -h -t' if includeTags is true, or
     * 'git ls-remote --heads' if false
     * @param includeTags whether to include the tags
     * @return a list of remotre references
     * @throws GitAPIException
     */
    public Collection<Ref> getRefsFromRemote(boolean includeTags) throws GitAPIException{

        //No UsernamePasswordCredentialsProvider is needed for this, as far as I can tell
        //TODO: see if UsernamePasswordCredentialsProvider is needed to getRefsFromRemote
        /*UsernamePasswordCredentialsProvider ownerAuth;
        try {
            ownerAuth = presentAuthorizeDialog();
        } catch (CancelledAuthorizationException e) {
            // If the user doesn't enter credentials for this action, then we'll leave the ownerAuth
            // as null.
            ownerAuth = null;
        }
        if(includeTags) return new Git(repo).lsRemote().setHeads(true).setTags(true).setCredentialsProvider(ownerAuth).call();
        else return new Git(repo).lsRemote().setHeads(true).setCredentialsProvider(ownerAuth).call();*/

        if(includeTags) return new Git(repo).lsRemote().setHeads(true).setTags(includeTags).call();
        else return new Git(repo).lsRemote().setHeads(true).call();
    }

    public BranchManagerModel getBranchManagerModel() {
        return this.branchManagerModel;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) { this.username = username; }
}
