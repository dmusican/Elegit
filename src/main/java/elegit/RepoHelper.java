package elegit;

import com.jcraft.jsch.*;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import elegit.exceptions.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.controlsfx.control.NotificationPane;
import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.RefAlreadyExistsException;
import org.eclipse.jgit.ignore.IgnoreNode;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revplot.PlotCommitList;
import org.eclipse.jgit.revplot.PlotLane;
import org.eclipse.jgit.revplot.PlotWalk;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTag;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.*;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.util.FS;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The abstract RepoHelper class, used for interacting with a repository.
 */
public abstract class RepoHelper {

    //protected UsernamePasswordCredentialsProvider ownerAuth;

    public String username;
    protected String password;

    protected Repository repo;

    protected Path localPath;
    protected File credentialsFile;
    protected List<String> credentialsList;

    private List<CommitHelper> localCommits;
    private List<CommitHelper> remoteCommits;

    private List<TagHelper> upToDateTags;
    private List<TagHelper> unpushedTags;
    private List<String> tagsWithUnpushedCommits;

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
    protected UsernamePasswordCredentialsProvider ownerAuth;

    /**
     * Creates a RepoHelper object for holding a Repository and interacting with it
     * through JGit.
     *
     * @param directoryPath the path of the repository.
     * @throws GitAPIException                 if the obtainRepository() call throws this exception..
     * @throws IOException                     if the obtainRepository() call throws this exception.
     * @throws CancelledAuthorizationException if the obtainRepository() call throws this exception.
     */
    public RepoHelper(Path directoryPath) throws GitAPIException, IOException, CancelledAuthorizationException {
        this.username = null;
        this.localPath = directoryPath;

    }

    public RepoHelper(Path directoryPath, UsernamePasswordCredentialsProvider ownerAuth)
            throws GitAPIException, IOException, CancelledAuthorizationException {
        this.localPath = directoryPath;
        this.ownerAuth = ownerAuth;
        this.password = null;
    }

    public RepoHelper(Path directoryPath, String sshPassword)
            throws GitAPIException, IOException, CancelledAuthorizationException {
        this.localPath = directoryPath;
        this.ownerAuth = null;
        this.password = sshPassword;
    }

    public RepoHelper(Path directoryPath, File credentialsFile)
            throws GitAPIException, IOException, CancelledAuthorizationException {
        this.localPath = directoryPath;
        this.ownerAuth = null;
        this.password = null;
        this.credentialsFile = credentialsFile;
    }

    public RepoHelper(Path directoryPath, List<String> credentialsList)
            throws GitAPIException, IOException, CancelledAuthorizationException {
        this.localPath = directoryPath;
        this.ownerAuth = null;
        this.password = null;
        this.credentialsFile = null;
        this.credentialsList = credentialsList;
    }



    /* This method requires credentials be passed in as a parameter; that's because it must be used by
        lsRemoteRepository, for example, that is used before we've actually created a RepoHelper object. Without a
        RepoHelper, there isn't an ownerAuth instance variable, so we don't have it yet.
     */
    static void wrapAuthentication(TransportCommand command,
                                   UsernamePasswordCredentialsProvider ownerAuth) {
        wrapAuthentication(command, ownerAuth, null, null, null);
    }


    static void wrapAuthentication(TransportCommand command, String sshPassword) {
        wrapAuthentication(command, null, sshPassword, null, null);
    }

    static void wrapAuthentication(TransportCommand command, UsernamePasswordCredentialsProvider ownerAuth,
                                   String sshPassword) {
        wrapAuthentication(command, ownerAuth, sshPassword, null, null);
    }

    static void wrapAuthentication(TransportCommand command,
                                   File credentialsFile) {
        wrapAuthentication(command, null, null, credentialsFile, null);
    }

    static void wrapAuthentication(TransportCommand command, List<String> credentialsList) {
        wrapAuthentication(command, null, null, null, credentialsList);
    }

    static void wrapAuthentication(TransportCommand command, UsernamePasswordCredentialsProvider ownerAuth,
                                   String sshPassword, File credentialsFile, List<String> credentialsList) {

        if (ownerAuth != null)
            command.setCredentialsProvider(ownerAuth);
        else
            command.setCredentialsProvider(new ElegitCredentialsProvider(credentialsList));

        command.setTransportConfigCallback(
                new TransportConfigCallback() {
                    @Override
                    public void configure(Transport transport) {

                        if (transport instanceof TransportGitSsh) {
                            SshSessionFactory sshSessionFactory = new JschConfigSessionFactory() {
                                @Override
                                protected void configure(OpenSshConfig.Host host, Session session) {
                                    session.setPassword(sshPassword);
                                }

                                @Override
                                protected JSch createDefaultJSch(FS fs) throws JSchException {
                                    JSch defaultJSch = super.createDefaultJSch(fs);
                                    defaultJSch.removeAllIdentity();
                                    return defaultJSch;
                                }
                            };
                            SshTransport sshTransport = (SshTransport) transport;
                            sshTransport.setSshSessionFactory(sshSessionFactory);
                        }

                    }
                });
    }

    protected void myWrapAuthentication(TransportCommand command) {
        wrapAuthentication(command, this.ownerAuth, this.password, this.credentialsFile, this.credentialsList);
    }

    // Common setup tasks shared by constructors
    protected void setup() throws GitAPIException, IOException {
        //this.repo = this.obtainRepository();
        this.username = null;

        this.commitIdMap = new HashMap<>();
        this.idMap = new HashMap<>();
        this.tagIdMap = new HashMap<>();

        this.localCommits = this.parseAllLocalCommits();
        this.remoteCommits = this.parseAllRemoteCommits();

        this.tagsWithUnpushedCommits = new ArrayList<>();

        this.upToDateTags = this.getAllLocalTags();
        this.unpushedTags = new ArrayList<>();

        this.branchManagerModel = new BranchManagerModel(this.getListOfLocalBranches(), this.getListOfRemoteBranches(), this);

        hasRemoteProperty = new SimpleBooleanProperty(!getLinkedRemoteRepoURLs().isEmpty());

        hasUnpushedCommitsProperty = new SimpleBooleanProperty(getAllCommitIDs().size() > remoteCommits.size());
        hasUnmergedCommitsProperty = new SimpleBooleanProperty(getAllCommitIDs().size() > localCommits.size());

        hasUnpushedTagsProperty = new SimpleBooleanProperty();

    }

    /**
     * @return true if the corresponding repository still exists in the expected location
     */
    public boolean exists() {
        logger.debug("Checked if repo still exists");
        return localPath.toFile().exists() && localPath.toFile().list((dir, name) -> name.equals(".git")).length > 0;
    }


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
     *
     * @return a list of the remote URLs associated with this repository
     */
    public List<String> getLinkedRemoteRepoURLs() {
        Config storedConfig = this.repo.getConfig();
        Set<String> remotes = storedConfig.getSubsections("remote");
        ArrayList<String> urls = new ArrayList<>(remotes.size());
        for (String remote : remotes) {
            urls.add(storedConfig.getString("remote", remote, "url"));
        }
        return urls;
    }

    /**
     * @return true if this repository has an associated remote
     */
    public boolean hasRemote() {
        return hasRemoteProperty.get();
    }

    /**
     * @return true if there are local commits that haven't been pushed
     */
    public boolean hasUnpushedCommits() {
        return hasUnpushedCommitsProperty.get();
    }

    /**
     * @return true if there are local tags that haven't been pushed
     */
    public boolean hasUnpushedTags() {
        if (this.unpushedTags == null || this.unpushedTags.size() == 0) {
            this.hasUnpushedTagsProperty.set(false);
        }
        return hasUnpushedTagsProperty.get();
    }

    /**
     * @return true if there are remote commits that haven't been merged into local
     */
    public boolean hasUnmergedCommits() {
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
        if (!exists()) throw new MissingRepoException();
        // should this Git instance be class-level?
        Git git = new Git(this.repo);
        // git commit:
        git.commit()
                .setMessage(commitMessage)
                .call();
        git.close();

        // Update the local commits
        try {
            this.localCommits = parseAllLocalCommits();
        } catch (IOException e) {
            // This shouldn't occur once we have the repo up and running.
        }

        this.hasUnpushedCommitsProperty.set(true);
    }

    /**
     * Tags a commit
     *
     * @param tagName the name for the tag.
     * @throws GitAPIException if the 'git tag' call fails.
     */
    public void tag(String tagName, String commitName) throws GitAPIException, MissingRepoException, IOException, TagNameExistsException {
        logger.info("Attempting tag");
        if (!exists()) throw new MissingRepoException();
        Git git = new Git(this.repo);
        // This creates a lightweight tag
        // TODO: add support for annotated tags?
        CommitHelper c = commitIdMap.get(commitName);
        if (c.getTagNames().contains(tagName))
            throw new TagNameExistsException();
        Ref r = git.tag().setName(tagName).setObjectId(c.getCommit()).setAnnotated(false).call();
        git.close();
        TagHelper t = makeTagHelper(r, tagName);
        this.unpushedTags.add(t);
        this.hasUnpushedTagsProperty.set(true);
    }

    /**
     * Pushes all changes.
     *
     * @throws GitAPIException if the `git push` call fails.
     */
    public void pushAll() throws GitAPIException, MissingRepoException, PushToAheadRemoteError {
        logger.info("Attempting push");
        if (!exists()) throw new MissingRepoException();
        if (!hasRemote()) throw new InvalidRemoteException("No remote repository");
        Git git = new Git(this.repo);
        PushCommand push = git.push().setPushAll();

        myWrapAuthentication(push);
        ProgressMonitor progress = new SimpleProgressMonitor();
        push.setProgressMonitor(progress);

//        boolean authUpdateNeeded = false;
//        Iterable<PushResult> pushResult = null;
//        do {
//            authUpdateNeeded = false;
//            try {
//                pushResult = push.call();
//            } catch (TransportException e) {
//                authUpdateNeeded = true;
//                RepoHelperBuilder.AuthDialogResponse response;
//                try {
//                    response = RepoHelperBuilder.getAuthCredentialFromDialog(remoteURL);
//                } catch (CancelledAuthorizationException e2) {
//                    git.close();
//                    return;
//                }
//                this.protocol = response.protocol;
//                this.password = response.password;
//                this.username = response.username;
//                myWrapAuthentication(push);
//            }
//        } while (authUpdateNeeded);

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

        try {
            this.remoteCommits = parseAllRemoteCommits();
        } catch (IOException e) {
            // This shouldn't occur once we have the repo up and running.
        }

        this.hasUnpushedCommitsProperty.set(false);
    }

    /**
     * Pushes all tags in /refs/tags/.
     *
     * @throws GitAPIException if the `git push --tags` call fails.
     */
    public void pushTags() throws GitAPIException, MissingRepoException, PushToAheadRemoteError {
        logger.info("Attempting push tags");
        if (!exists()) throw new MissingRepoException();
        if (!hasRemote()) throw new InvalidRemoteException("No remote repository");
        Git git = new Git(this.repo);
        PushCommand push = git.push().setPushAll();
        myWrapAuthentication(push);
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
        this.upToDateTags.addAll(this.unpushedTags);
        this.unpushedTags = new ArrayList<>();
        this.hasUnpushedTagsProperty.set(false);
    }

    /**
     * Fetches changes into FETCH_HEAD (`git -fetch`).
     *
     * @throws GitAPIException
     * @throws MissingRepoException
     */
    public boolean fetch() throws
            GitAPIException, MissingRepoException, IOException {
        logger.info("Attempting fetch");
        if (!exists()) throw new MissingRepoException();
        Git git = new Git(this.repo);

        FetchCommand fetch = git.fetch().setTagOpt(TagOpt.AUTO_FOLLOW).setRemoveDeletedRefs(true);

        myWrapAuthentication(fetch);

        // The JGit docs say that if setCheckFetchedObjects
        //  is set to true, objects received will be checked for validity.
        //  Not sure what that means, but sounds good so I'm doing it...
        fetch.setCheckFetchedObjects(true);

        // ProgressMonitor progress = new TextProgressMonitor(new PrintWriter(System.out));
        ProgressMonitor progress = new SimpleProgressMonitor();
        fetch.setProgressMonitor(progress);

        FetchResult result = fetch.call();
        git.close();

        try {
            this.remoteCommits = parseAllRemoteCommits();
        } catch (IOException e) {
            // This shouldn't occur once we have the repo up and running.
        }

        this.branchManagerModel.getUpdatedRemoteBranches();
        this.hasUnmergedCommitsProperty.set(this.hasUnmergedCommits() || !result.getTrackingRefUpdates().isEmpty());
        return !result.getTrackingRefUpdates().isEmpty();
    }

    /**
     * Merges the current branch with the remote branch that this is tracking, as
     * found in the config for the repo
     *
     * @throws IOException
     * @throws GitAPIException
     * @throws MissingRepoException
     * @return the merge status merging these two branches
     */
    public MergeResult.MergeStatus mergeFromFetch() throws IOException, GitAPIException, MissingRepoException, ConflictingFilesException {
        logger.info("Attempting merge from fetch");
        if (!exists()) throw new MissingRepoException();
        if (!hasRemote()) throw new InvalidRemoteException("No remote repository");

        // Get the remote branch the current branch is tracking
        // and merge the current branch with the just fetched remote branch
        MergeResult result;
        Config config = repo.getConfig();
        if (config.getSubsections("branch").contains(this.repo.getBranch())) {
            String remote = config.getString("branch", this.repo.getBranch(), "remote")+"/";
            String remote_tracking = config.getString("branch", this.repo.getBranch(), "merge");
            result = mergeWithBranch(this.getRemoteBranchByName(remote+this.repo.shortenRefName(remote_tracking)));
        } else {
            return null;
        }

        try {
            this.localCommits = parseAllLocalCommits();
        } catch (IOException e) {
            // This shouldn't occur once we have the repo up and running.
        }

        MergeResult.MergeStatus status = result.getMergeStatus();
        this.hasUnmergedCommitsProperty.set(status == MergeResult.MergeStatus.ABORTED || status == MergeResult.MergeStatus.CHECKOUT_CONFLICT);
        this.hasUnpushedCommitsProperty.set(this.hasUnpushedCommits() || status == MergeResult.MergeStatus.MERGED);
        if (status == MergeResult.MergeStatus.CONFLICTING) throw new ConflictingFilesException(result.getConflicts());
        //return result.getMergeStatus().isSuccessful();
        return status;
    }

    /**
     * Merges the current branch with the selected branch
     * @param branchToMergeFrom the branch to merge into the current branch
     * @return merge result, used in determining the notification in BranchManagerController
     * @throws GitAPIException
     * @throws IOException
     */
    public MergeResult mergeWithBranch(BranchHelper branchToMergeFrom) throws GitAPIException, IOException {
        Git git = new Git(this.repo);

        MergeCommand merge = git.merge();
        merge.include(this.repo.resolve(branchToMergeFrom.getRefPathString()));

        MergeResult mergeResult = merge.call();

        // If the merge was successful, there was a new commit or fast forward, so there are unpushed
        // commits. Otherwise, repo helper doesn't need to do anything else.
        if (mergeResult.getMergeStatus().equals(MergeResult.MergeStatus.MERGED)
                || mergeResult.getMergeStatus().equals(MergeResult.MergeStatus.FAST_FORWARD)){
            this.hasUnpushedCommitsProperty.setValue(true);
        }
        git.close();

        return mergeResult;
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
    public List<CommitHelper> getLocalCommits() {
        return this.localCommits;
    }

    /**
     * @return all remote commits that have already been parsed
     */
    public List<CommitHelper> getRemoteCommits() {
        return this.remoteCommits;
    }

    /**
     * @return all commits (remote and local) that have been parsed
     */
    public List<CommitHelper> getAllCommits () {
        List<CommitHelper> allCommits = new ArrayList<>();
        allCommits.addAll(this.localCommits);
        allCommits.addAll(this.remoteCommits);
        return allCommits;
    }

    /**
     * Returns a formatted string that describes the given commit
     * @param commitHelper the commit to get a label for
     * @return the label for the commit
     */
    public String getCommitDescriptorString(CommitHelper commitHelper, boolean fullCommitMessage){
        String s = commitHelper.getFormattedWhen() + "\n\n" + commitHelper.getMessage(fullCommitMessage);
        List<BranchHelper> branches = getAllBranchHeads().get(commitHelper);
        if(branches != null){
            s += "\n\nHead of branches: ";
            for(BranchHelper branch : branches){
                s = s + "\n" + branch.getBranchName();
            }
        }
        return s;
    }

    /**
     * Returns a formatted string that describes the given commit
     * @param commitId the id of the commit to get a label for
     * @return the label for the commit
     */
    public String getCommitDescriptorString(String commitId, boolean fullCommitMessage){
        return getCommitDescriptorString(getCommit(commitId), fullCommitMessage);
    }

    /**
     * Returns a unique identifier that will never be shown
     * @param commitHelper the commit to get an ID for
     * @return a unique identifying string to be used as a key in the tree's map
     */
    public static String getCommitId(CommitHelper commitHelper){
        return commitHelper.getName();
    }

    /**
     * Attempts first to use the parameter as an ID string that maps
     * to a commit. If that fails, attempts to parse it as as a reference
     * string and find the ID it maps to, then returning the commit
     * associated with that id
     *
     * @param idOrRefString either an ID or reference string corresponding
     *                      to an object in this repository
     * @return the commit associated with the parameter
     */
    public CommitHelper getCommit(String idOrRefString) {
        if (commitIdMap.containsKey(idOrRefString)) {
            return commitIdMap.get(idOrRefString);
        } else {
            try {
                return getCommit(repo.resolve(idOrRefString));
            } catch (IOException e) {
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
    public CommitHelper getCommit(ObjectId id) {
        if (idMap.containsKey(id)) {
            return getCommit(idMap.get(id));
        } else {
            return null;
        }
    }

    public TagHelper getTag(String tagName) {
        return tagIdMap.get(tagName);
    }

    public void deleteTag(String tagName) throws MissingRepoException, GitAPIException {
        TagHelper tagToRemove = tagIdMap.get(tagName);

        if (!exists()) throw new MissingRepoException();
        // should this Git instance be class-level?
        Git git = new Git(this.repo);
        // git tag -d
        git.tagDelete().setTags(tagToRemove.getName()).call();
        git.close();

        tagToRemove.getCommit().removeTag(tagName);
        if (!this.upToDateTags.remove(tagToRemove)) {
            this.unpushedTags.remove(tagToRemove);
            if (this.unpushedTags.size() == 0) {
                this.hasUnpushedTagsProperty.set(false);
            }
        } else {
            this.hasUnpushedTagsProperty.set(true);
        }
        this.tagIdMap.remove(tagName);
    }

    /**
     * @return a list of all commit IDs in this repository
     */
    public List<String> getAllCommitIDs() {
        return new ArrayList<>(commitIdMap.keySet());
    }

    /**
     * @return a list of all tag names in this repository
     */
    public List<String> getAllTagNames() {
        return new ArrayList<>(tagIdMap.keySet());
    }

    /**
     * @return the head of the current branch
     */
    public CommitHelper getHead() {
        return (this.branchHelper == null) ? null : this.branchHelper.getHead();
    }

    /**
     * Uses JGit to find and parse all local commits between the given branches and
     * every leaf in the repository
     *
     * @param oldLocalBranches the previous branch heads associated with a branch name. Commits
     *                         older than the heads of these branches will be ignored
     * @return all local commits newer than the given branch heads
     * @throws GitAPIException
     * @throws IOException
     */
    public List<CommitHelper> getNewLocalCommits(Map<String, BranchHelper> oldLocalBranches) throws GitAPIException, IOException {
        return getNewCommits(oldLocalBranches, this.getListOfLocalBranches());
    }

    /**
     * Uses JGit to find and parse all remote commits between the given branches and
     * every leaf in the repository
     *
     * @param oldRemoteBranches the previous branch heads associated with a branch name. Commits
     *                          older than the heads of these branches will be ignored
     * @return all remote commits newer than the given branch heads
     * @throws GitAPIException
     * @throws IOException
     */
    public List<CommitHelper> getNewRemoteCommits(Map<String, BranchHelper> oldRemoteBranches) throws GitAPIException, IOException {
        return getNewCommits(oldRemoteBranches, this.getListOfRemoteBranches());
    }

    /**
     * Helper method that returns commits between the given old branch heads and new branch heads
     *
     * @param oldBranches previous locations of branch heads
     * @param newBranches current locations of branch heads
     * @return a list of all commits found between oldBranches and newBranches
     * @throws GitAPIException
     * @throws IOException
     */
    private List<CommitHelper> getNewCommits(Map<String, BranchHelper> oldBranches, List<? extends BranchHelper> newBranches) throws GitAPIException, IOException {
        List<ObjectId> startPoints = new ArrayList<>();
        List<ObjectId> stopPoints = new ArrayList<>();

        for (BranchHelper newBranch : newBranches) {
            if (oldBranches.containsKey(newBranch.getBranchName())) {
                ObjectId newBranchHeadID = newBranch.getHeadId();
                ObjectId oldBranchHeadID = oldBranches.get(newBranch.getBranchName()).getHeadId();
                if (!newBranchHeadID.equals(oldBranchHeadID)) {
                    startPoints.add(newBranchHeadID);
                }
                stopPoints.add(oldBranchHeadID);
            } else {
                startPoints.add(newBranch.getHeadId());
            }
        }
        PlotCommitList<PlotLane> newCommits = this.parseRawCommits(startPoints, stopPoints);
        return wrapRawCommits(newCommits);
    }


    /**
     * Constructs a list of all local commits found by parsing the repository for raw RevCommit objects,
     * then wrapping them into a CommitHelper with the appropriate parents and children
     *
     * @return a list of CommitHelpers for all local commits
     * @throws IOException
     */
    private List<CommitHelper> parseAllLocalCommits() throws IOException, GitAPIException {
        PlotCommitList<PlotLane> commitList = this.parseAllRawLocalCommits();
        return wrapRawCommits(commitList);
    }

    /**
     * Constructs a list of all remote commits found by parsing the repository for raw RevCommit objects,
     * then wrapping them into a CommitHelper with the appropriate parents and children
     *
     * @return a list of CommitHelpers for all remote commits
     * @throws IOException
     */
    private List<CommitHelper> parseAllRemoteCommits() throws IOException, GitAPIException {
        PlotCommitList<PlotLane> commitList = this.parseAllRawRemoteCommits();
        return wrapRawCommits(commitList);
    }

    /**
     * Constructs a list of all local tags found by parsing the tag refs from the repo
     * then wrapping them into a TagHelper with the appropriate commit
     *
     * @return a list of TagHelpers for all the tags
     * @throws IOException
     * @throws GitAPIException
     */
    public List<TagHelper> getAllLocalTags() throws IOException, GitAPIException {
        Map<String, Ref> tagMap = repo.getTags();
        List<TagHelper> tags = new ArrayList<>();
        for (String s : tagMap.keySet()) {
            Ref r = tagMap.get(s);
            tags.add(makeTagHelper(r, s));
        }
        return tags;
    }

    /**
     * Looks through all the tags and checks that they are added to commit helpers
     *
     * @return true if there were changes, false if not
     * @throws IOException
     * @throws GitAPIException
     */
    public boolean updateTags() throws IOException, GitAPIException {
        Map<String, Ref> tagMap = repo.getTags();
        List<String> oldTagNames = getAllTagNames();
        int oldSize = oldTagNames.size();
        for (String s : tagMap.keySet()) {
            if (oldTagNames.contains(s)) {
                oldTagNames.remove(s);
                if (tagsWithUnpushedCommits.contains(s)) {
                    tagsWithUnpushedCommits.remove(s);
                }
                continue;
            } else {
                Ref r = tagMap.get(s);
                makeTagHelper(r, s);
            }
        }
        if (oldTagNames.size() > 0) { //There are tags that were deleted, so we remove them
            for (String s : oldTagNames) {
                this.commitIdMap.get(this.tagIdMap.get(s).getCommitId()).removeTag(s);
                this.tagIdMap.remove(s);
            }
        }
        if (oldSize == getAllTagNames().size() && oldTagNames.size() == 0) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Helper method to make a tagHelper given a ref and a name of the tag. Also adds the
     * tag helper to the tagIdMap
     *
     * @param r       the ref to make a tagHelper for. This can be a peeled or unpeeled tag
     * @param tagName the name of the tag
     * @return a tagHelper object with the information stored
     * @throws IOException
     * @throws GitAPIException
     */
    private TagHelper makeTagHelper(Ref r, String tagName) throws IOException, GitAPIException {
        String commitName;
        boolean isAnnotated = false;

        //Check if the tag is annotated or not, find the commit name accordingly
        if (r.getPeeledObjectId() != null) {
            commitName = r.getPeeledObjectId().getName();
            isAnnotated = true;
        } else commitName = r.getObjectId().getName();

        // Find the commit helper associated with the commit name
        CommitHelper c = this.commitIdMap.get(commitName);
        TagHelper t;

        // If the commit that this tag points to isn't in the commitIdMap,
        // then that commit has not yet been pushed, so warn the user
        if (c == null) {
            this.tagsWithUnpushedCommits.add(tagName);
            return null;
        } else if (this.tagsWithUnpushedCommits.contains(tagName)) {
            this.tagsWithUnpushedCommits.remove(tagName);
        }

        // If it's not an annotated tag, we make a lightweight tag helper
        if (!isAnnotated) {
            t = new TagHelper(tagName, c);
            c.addTag(t);
        }
        // Otherwise, the tag has a message and all the stuff a commit has
        else {
            ObjectReader objectReader = repo.newObjectReader();
            ObjectLoader objectLoader = objectReader.open(r.getObjectId());
            RevTag tag = RevTag.parse(objectLoader.getBytes());
            objectReader.close();
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
     *
     * @param commitList the raw commits to wrap
     * @return a list of CommitHelpers for the given commits
     * @throws IOException
     */
    private List<CommitHelper> wrapRawCommits(PlotCommitList<PlotLane> commitList) throws IOException {
        List<CommitHelper> commitHelperList = new ArrayList<>();
        List<ObjectId> wrappedIDs = new ArrayList<>();
        List<CommitHelper> commitsWithMissingParents = new ArrayList<>();
        for (int i = commitList.size() - 1; i >= 0; i--) {
            RevCommit curCommit = commitList.get(i);
            ObjectId curCommitID = curCommit.getId();

            if (wrappedIDs.contains(curCommitID)) {
                continue;
            }

            CommitHelper curCommitHelper = new CommitHelper(curCommit);
            String curCommitHelperID = curCommitHelper.getId();

            if (!commitIdMap.containsKey(curCommitHelperID)) {
                commitIdMap.put(curCommitHelper.getId(), curCommitHelper);
                idMap.put(curCommitID, curCommitHelper.getId());
            } else {
                curCommitHelper = commitIdMap.get(curCommitHelperID);
            }
            wrappedIDs.add(curCommitID);

            RevCommit[] parents = curCommit.getParents();
            for (RevCommit p : parents) {
                CommitHelper parentCommitHelper = getCommit(p.getId());
                if (parentCommitHelper == null) {
                    commitsWithMissingParents.add(curCommitHelper);
                } else {
                    curCommitHelper.addParent(parentCommitHelper);
                }
            }

            commitHelperList.add(curCommitHelper);
        }
        while (!commitsWithMissingParents.isEmpty()) {
            CommitHelper curCommitHelper = commitsWithMissingParents.remove(0);
            RevCommit[] parents = curCommitHelper.commit.getParents();
            for (RevCommit p : parents) {
                CommitHelper parentCommitHelper = getCommit(p.getId());
                if (parentCommitHelper == null) {
                    commitsWithMissingParents.add(curCommitHelper);
                } else if (!curCommitHelper.getParents().contains(parentCommitHelper)) {
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
     *
     * @return a list of raw local commits
     * @throws IOException
     */
    private PlotCommitList<PlotLane> parseAllRawLocalCommits() throws IOException, GitAPIException {
        ObjectId headId = repo.resolve("HEAD");
        if (headId == null) return new PlotCommitList<>();
        List<ObjectId> examinedCommitIDs = new ArrayList<>();
        PlotCommitList<PlotLane> rawLocalCommits = parseRawCommits(headId, examinedCommitIDs);
        examinedCommitIDs.add(headId);

        List<LocalBranchHelper> branches = getListOfLocalBranches();
        for (BranchHelper branch : branches) {
            ObjectId branchId = branch.getHeadId();
            PlotCommitList<PlotLane> toAdd = parseRawCommits(branchId, examinedCommitIDs);
            if (toAdd.size() > 0) {
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
     *
     * @return a list of raw remote commits
     * @throws IOException
     */
    private PlotCommitList<PlotLane> parseAllRawRemoteCommits() throws IOException, GitAPIException {
        List<ObjectId> examinedCommitIDs = new ArrayList<>();
        PlotCommitList<PlotLane> rawRemoteCommits = new PlotCommitList<>();

        List<RemoteBranchHelper> branches = getListOfRemoteBranches();
        for (BranchHelper branch : branches) {
            ObjectId branchId = branch.getHeadId();
            PlotCommitList<PlotLane> toAdd = parseRawCommits(branchId, examinedCommitIDs);
            if (toAdd.size() > 0) {
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
     *
     * @param startPoints the starting ids to parse from
     * @param stopPoints  the ids at which parsing should stop
     * @return a list of raw commits starting from each id in startPoints, excluding those beyond each id in stopPoints
     * @throws IOException
     */
    private PlotCommitList<PlotLane> parseRawCommits(List<ObjectId> startPoints, List<ObjectId> stopPoints) throws IOException {
        PlotCommitList<PlotLane> plotCommitList = new PlotCommitList<>();

        PlotWalk w = new PlotWalk(repo);
        for (ObjectId stopId : stopPoints) {
            w.markUninteresting(w.parseCommit(stopId));
        }

        for (ObjectId startId : startPoints) {
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
     *
     * @param startingID the starting point to parse from
     * @param stopPoints the ids at which parsing should stop
     * @return a list of raw commits starting from the given id
     * @throws IOException
     */
    private PlotCommitList<PlotLane> parseRawCommits(ObjectId startingID, List<ObjectId> stopPoints) throws IOException {
        List<ObjectId> asList = new ArrayList<>(1);
        asList.add(startingID);
        return parseRawCommits(asList, stopPoints);
    }

    /**
     * Utilizes JGit to parse a commit with the given ID and returns it in
     * raw format
     *
     * @param id the ID of the commit
     * @return the raw commit corresponding to the given ID
     * @throws IOException
     */
    public RevCommit parseRawCommit(ObjectId id) throws IOException {
        RevWalk w = new RevWalk(repo);
        w.dispose();
        return w.parseCommit(id);
    }

    /**
     * Parses all relevant git ignore files for ignore patterns, and then checks all
     * tracked files and directories for whether they match an ignore pattern.
     *
     * @return the set of paths (relative to the repo) of all tracked files that match an ignore pattern
     * @throws IOException
     */
    public Collection<String> getTrackedIgnoredFiles() throws IOException {
        // Build the ignore pattern matcher
        IgnoreNode ignoreNode = new IgnoreNode();
        for (Path path : getGitIgnorePaths()) {
            ignoreNode.parse(new BufferedInputStream(Files.newInputStream(path)));
        }

        // Set up the walks
        RevWalk walk = new RevWalk(this.repo);
        RevCommit head = walk.parseCommit(this.repo.resolve(Constants.HEAD));

        TreeWalk treeWalk = new TreeWalk(this.repo);
        treeWalk.addTree(head.getTree());
        treeWalk.setRecursive(false);

        // Keep track of whether the ancestors are being ignored
        Stack<Boolean> isParentAtDepthIgnored = new Stack<>();
        // Set the top level 'parent' to be false
        isParentAtDepthIgnored.push(false);

        Collection<String> trackedIgnoredFiles = new HashSet<>();

        // Loop through all tracked files (depth first)
        while (treeWalk.next()) {
            String pathString = treeWalk.getPathString();
            // Make sure the stack matches the appropriate depth of this file/directory
            while (treeWalk.getDepth() + 1 < isParentAtDepthIgnored.size()) {
                isParentAtDepthIgnored.pop();
            }
            boolean isParentIgnored = isParentAtDepthIgnored.peek();
            IgnoreNode.MatchResult result;

            // The current item is a directory
            if (treeWalk.isSubtree()) {
                result = ignoreNode.isIgnored(pathString, true);

                // TODO: Does not support a result of 'CHECK_PARENT_NEGATE_FIRST_MATCH' (mainly because I don't know what that means)
                // Update the stack with the information from this item
                if (result == IgnoreNode.MatchResult.IGNORED) isParentAtDepthIgnored.push(true);
                else if (result == IgnoreNode.MatchResult.NOT_IGNORED) isParentAtDepthIgnored.push(false);
                else isParentAtDepthIgnored.push(isParentAtDepthIgnored.peek());

                treeWalk.enterSubtree();
            } else {
                result = ignoreNode.isIgnored(pathString, false);
            }
            boolean isIgnored = (result == IgnoreNode.MatchResult.IGNORED) || (isParentIgnored && result == IgnoreNode.MatchResult.CHECK_PARENT);
            if (isIgnored) trackedIgnoredFiles.add(pathString);
        }
        return trackedIgnoredFiles;
    }

    /**
     * Finds and returns a list of all files from which ignore patterns are pulled for this repository.
     * This includes the global ignore file (if it exists), the info/exclude file, and any .gitignore
     * files in the repositories file structure
     *
     * @return a list of paths to files that define ignore rules for this repository
     */
    public List<Path> getGitIgnorePaths() throws IOException {
        List<Path> gitIgnorePaths = new LinkedList<>();

        Path globalIgnore = getGlobalGitIgnorePath();
        if (globalIgnore != null) gitIgnorePaths.add(globalIgnore);

        Path infoExclude = getInfoExcludePath();
        if (infoExclude != null) gitIgnorePaths.add(infoExclude);

        GitIgnoreFinder finder = new GitIgnoreFinder();
        Files.walkFileTree(this.localPath, finder);
        gitIgnorePaths.addAll(finder.getMatchedPaths());

        return gitIgnorePaths;
    }

    /**
     * Returns the path to the configured global git ignore file, or null if no such file
     * has been configured
     *
     * @return path to the global ignore file
     */
    public Path getGlobalGitIgnorePath() {
        Config repoConfig = this.repo.getConfig();
        FS fs = this.repo.getFS();

        String globalIgnorePath = repoConfig.get(CoreConfig.KEY).getExcludesFile();
        if (globalIgnorePath != null) {
            if (globalIgnorePath.startsWith("~/")) {
                globalIgnorePath = fs.resolve(fs.userHome(), globalIgnorePath.substring(2)).getAbsolutePath();
            }
            return Paths.get(globalIgnorePath);
        }
        return null;
    }

    /**
     * Returns the path to this repositories info/exclude file, or null if that file does not exists.
     * The path to this is defined by Git as '$GIT_DIR/info/exclude'
     *
     * @return path to this repositories info/exclude file
     */
    public Path getInfoExcludePath() {
        FS fs = this.repo.getFS();

        File repoExclude = fs.resolve(this.repo.getDirectory(), Constants.INFO_EXCLUDE);
        return repoExclude.exists() ? repoExclude.toPath() : null;
    }

    @Override
    public String toString() {
        return this.localPath.getFileName().toString();
    }

    @Override
    public boolean equals(Object o) {
        if (o != null && o.getClass().equals(this.getClass())) {
            RepoHelper other = (RepoHelper) o;
            return this.localPath.equals(other.localPath);
        }
        return false;
    }


    /**
     * Utilizes JGit to get a list of all local branches. It both returns a list of remote branches,
     * but also simultaneously updates an instance variable holding that same list.
     *
     * @return a list of all local branches
     * @throws GitAPIException
     * @throws IOException
     */
    public List<LocalBranchHelper> getListOfLocalBranches() throws GitAPIException, IOException {
        List<Ref> getBranchesCall = new Git(this.repo).branchList().call();

        localBranches = new ArrayList<>();
        for (Ref ref : getBranchesCall) {
            localBranches.add(new LocalBranchHelper(ref, this));
        }

        return localBranches;
    }


    /**
     * Utilizes JGit to get a list of all remote branches. It both returns a list of remote branches,
     * but also simultaneously updates an instance variable holding that same list.
     *
     * @return a list of all remote branches
     * @throws GitAPIException
     */
    public List<RemoteBranchHelper> getListOfRemoteBranches() throws GitAPIException, IOException {
        List<Ref> getBranchesCall = new Git(this.repo).branchList().setListMode(ListBranchCommand.ListMode.REMOTE).call();

        // Rebuild the remote branches list from scratch.
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
     * Method to get a branch helper by name
     * @param branchname the branch to get
     * @return the localBranchHelper with that name, or null if it is not found
     */
    public LocalBranchHelper getLocalBranchByName(String branchname) {
        for (LocalBranchHelper helper: this.localBranches) {
            if (helper.getBranchName().equals(branchname))
                return helper;
        }
        return null;
    }

    /**
     * Method to get a branch helper by name
     * @param branchname the branch to get
     * @return the remoteBranchHelper with that name, or null if it is not found
     */
    public RemoteBranchHelper getRemoteBranchByName(String branchname) {
        for (RemoteBranchHelper helper: this.remoteBranches) {
            if (helper.getBranchName().equals(branchname))
                return helper;
        }
        return null;
    }

    /**
     * Creates a local branch tracking a remote branch.
     *
     * @param remoteBranchHelper the remote branch to be tracked.
     * @return the LocalBranchHelper of the local branch tracking the given remote branch.
     * @throws GitAPIException
     * @throws IOException
     */
    private LocalBranchHelper createLocalTrackingBranchForRemote(RemoteBranchHelper remoteBranchHelper) throws GitAPIException, IOException {
        // Take off the 'origin/' before the branch name
        String localBranchName=this.repo.shortenRemoteBranchName(remoteBranchHelper.getRefPathString());
        Ref trackingBranchRef = new Git(this.repo).branchCreate().
                setName(localBranchName).
                setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK).
                setStartPoint(remoteBranchHelper.getRefPathString()).
                call();
        return new LocalBranchHelper(trackingBranchRef, this);
    }

    /**
     * Sets the currently checkout branch. Does not call 'git checkout'
     * or any variation, simply updates the local variable
     *
     * @param branchHelper the new current branch
     */
    public void setCurrentBranch(LocalBranchHelper branchHelper) {
        this.branchHelper = branchHelper;
    }

    /**
     * Creates a local branch and tracks it
     * @param remoteBranchHelper the remote branch to track
     * @return the localBranchHelper that was added
     * @throws GitAPIException
     * @throws IOException
     * @throws RefAlreadyExistsException
     */
    public LocalBranchHelper trackRemoteBranch(RemoteBranchHelper remoteBranchHelper) throws GitAPIException, IOException, RefAlreadyExistsException {
        LocalBranchHelper tracker = this.createLocalTrackingBranchForRemote(remoteBranchHelper);
        this.localBranches.add(tracker);
        return tracker;
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
     *
     * @throws IOException
     */
    public void refreshCurrentBranch() throws IOException {
        String currentBranchRefString = this.repo.getFullBranch();

        for (LocalBranchHelper branch : localBranches) {
            if (branch.getRefPathString().equals(currentBranchRefString)) {
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
        stage.setOnCloseRequest(event -> logger.info("Closed branch manager window"));
        stage.show();
    }

    /**
     * Gets a list of the local branches of this repository. Also updates
     * the head of each local branch if it was missing
     *
     * @return the local branches of this repository
     */
    public List<BranchHelper> getLocalBranches() {
        for (BranchHelper branch : localBranches) {
            if (branch.getHead() == null) {
                try {
                    branch.getHeadId();
                } catch (IOException e) {
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
     *
     * @return the remote branches of this repository
     */
    public List<BranchHelper> getRemoteBranches() {
        for (BranchHelper branch : remoteBranches) {
            if (branch.getHead() == null) {
                try {
                    branch.getHeadId();
                } catch (IOException e) {
                    logger.error("IOException getting remote branches");
                    logger.debug(e.getStackTrace());
                    e.printStackTrace();
                }
            }
        }
        return new ArrayList<>(remoteBranches);
    }

    /**
     * Gets a list of all the branches of this repository. Also updates
     * the head of each remote branch if it was missing
     *
     * @return the remote branches of this repository
     */
    public List<BranchHelper> getAllBranches() {
        List<BranchHelper> allBranches = new ArrayList<>();
        allBranches.addAll(getLocalBranches());
        allBranches.addAll(getRemoteBranches());
        return new ArrayList<>(allBranches);
    }

    public Map<CommitHelper, List<BranchHelper>> getAllBranchHeads(){
        Map<CommitHelper, List<BranchHelper>> heads = new HashMap<>();

        List<BranchHelper> branches = this.getLocalBranches();
        branches.addAll(this.getRemoteBranches());

        for(BranchHelper branch : branches){
            CommitHelper head = branch.getHead();
            if(heads.containsKey(head)){
                heads.get(head).add(branch);
            }else{
                heads.put(head, Stream.of(branch).collect(Collectors.toList()));
            }
        }
        return heads;
    }

    public List<String> getBranchesWithHead(String commitId) {
        return getBranchesWithHead(getCommit(commitId));
    }

    public List<String> getBranchesWithHead(CommitHelper commit) {
        List<BranchHelper> branches = getAllBranchHeads().get(commit);
        List<String> branchLabels = new LinkedList<>();
        if(branches != null) {
            branchLabels = branches.stream()
                    .map(BranchHelper::getBranchName)
                    .collect(Collectors.toList());
        }
        return branchLabels;
    }

    /**
     * Checks to see if the given branch is tracked. If branch is
     * a local branch, looks to see if there is a branch in the
     * remote branches that has the same name, and vice versa.
     * Note that tracking status is determined solely by name
     *
     * @param branch the branch to check
     * @return true if branch is being tracked, else false
     */
    public boolean isBranchTracked(BranchHelper branch) {
        String branchName = branch.getBranchName();
        if (branch instanceof LocalBranchHelper) {
            for (BranchHelper remote : remoteBranches) {
                if (this.repo.shortenRemoteBranchName(remote.getRefPathString())
                        .equals(this.repo.shortenRefName(this.repo.getConfig().getString("branch", branchName, "merge")))) {
                    return true;
                }
            }
        } else {
            for (BranchHelper local : localBranches) {
                if (this.repo.shortenRefName(this.repo.getConfig().getString("branch", local.getBranchName(), "merge"))
                        .equals(this.repo.shortenRemoteBranchName(branch.getRefPathString()))) {
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
     *
     * @param includeTags whether to include the tags
     * @return a list of remotre references
     * @throws GitAPIException
     */
    public Collection<Ref> getRefsFromRemote(boolean includeTags) throws GitAPIException {

        //No UsernamePasswordCredentialsProvider is needed for this, as far as I can tell
        //TODO: see if UsernamePasswordCredentialsProvider is needed to getRefsFromRemote
        /*UsernamePasswordCredentialsProvider ownerAuth;
        try {
            ownerAuth = setRepoHelperAuthCredentialFromDialog();
        } catch (CancelledAuthorizationException e) {
            // If the user doesn't enter credentials for this action, then we'll leave the ownerAuth
            // as null.
            ownerAuth = null;
        }
        if(includeTags) return new Git(repo).lsRemote().setHeads(true).setTags(true).setCredentialsProvider(ownerAuth).call();
        else return new Git(repo).lsRemote().setHeads(true).setCredentialsProvider(ownerAuth).call();*/

        if (includeTags) return new Git(repo).lsRemote().setHeads(true).setTags(includeTags).call();
        else return new Git(repo).lsRemote().setHeads(true).call();
    }

    public BranchManagerModel getBranchManagerModel() {
        return this.branchManagerModel;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return this.password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setAuthCredentials(UsernamePasswordCredentialsProvider authCredentials) {
        this.ownerAuth = authCredentials;
    }

    public boolean hasTagsWithUnpushedCommits() {
        return this.tagsWithUnpushedCommits.size() > 0;
    }

    public UsernamePasswordCredentialsProvider getOwnerAuthCredentials() throws CancelledAuthorizationException {
        return this.ownerAuth;
    }

    public void setUnpushedTags(List<TagHelper> tags) {
        for (TagHelper tag : tags) {
            if (this.upToDateTags.contains(tag)) {
                this.upToDateTags.remove(tag);
            }
            this.unpushedTags.add(tag);
        }
        this.hasUnpushedTagsProperty.set(true);
    }

    /**
     * A FileVisitor that keeps a list of all '.gitignore' files it finds
     */
    private class GitIgnoreFinder extends SimpleFileVisitor<Path> {
        private final PathMatcher matcher;
        private List<Path> matchedPaths;

        GitIgnoreFinder() {
            matcher = FileSystems.getDefault().getPathMatcher("glob:" + Constants.DOT_GIT_IGNORE);
            matchedPaths = new LinkedList<>();
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            Path name = file.getFileName();

            if (name != null && matcher.matches(name)) {
                matchedPaths.add(file);
            }

            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException e) {
            e.printStackTrace();
            return FileVisitResult.CONTINUE;
        }

        public Collection<Path> getMatchedPaths() {
            return matchedPaths;
        }
    }

    /**
     * Determine whether this repo is compatible with HTTP or SSH authentication.
     * Those are the only two that this will particularly return, because these are the only
     * two that are supported by JGit protocols; we will use other methods elsewhere to determine
     * specifically how to connect with each.
     */
    public AuthMethod getCompatibleAuthentication() {

        List<TransportProtocol> protocols = TransportGitSsh.getTransportProtocols();
        List<String> repoURLs = getLinkedRemoteRepoURLs();
        String repoURL = repoURLs.get(0);
        for (TransportProtocol protocol : protocols) {
            String protocolName = protocol.getName();
            try {
                if (protocol.canHandle(new URIish(repoURL))) {
                    if (protocolName.equals("HTTP"))
                        return AuthMethod.HTTP;
                    else if (protocolName.equals("SSH"))
                        return AuthMethod.SSH;
                    else
                        continue;
                }
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }

        // Failed to find an authentication method
        return AuthMethod.NONE;
    }

}