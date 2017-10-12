package elegit;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;
import elegit.exceptions.*;
import elegit.treefx.Cell;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.ignore.IgnoreNode;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revplot.PlotCommit;
import org.eclipse.jgit.revplot.PlotCommitList;
import org.eclipse.jgit.revplot.PlotLane;
import org.eclipse.jgit.revplot.PlotWalk;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevCommitList;
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

/**
 * The RepoHelper class, used for interacting with a repository.
 */
// TODO: Make sure threadsafe
public class RepoHelper {

    //protected UsernamePasswordCredentialsProvider ownerAuth;

    public String username;
    protected String password;

    protected Repository repo;

    public Path localPath;
    protected File credentialsFile;
    protected List<String> credentialsList;
    protected UserInfo userInfo;
    protected SshSessionFactory sshSessionFactory;

    private List<CommitHelper> localCommits;
    private List<CommitHelper> remoteCommits;

    private Map<String, CommitHelper> commitIdMap;
    private Map<ObjectId, String> idMap;

    private BranchModel branchModel;
    private TagModel tagModel;

    public BooleanProperty hasRemoteProperty;

    static final Logger logger = LogManager.getLogger();
    public UsernamePasswordCredentialsProvider ownerAuth;


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
        setupSshSessionFactory();

    }

    public RepoHelper(Path directoryPath, UsernamePasswordCredentialsProvider ownerAuth)
            throws GitAPIException, IOException, CancelledAuthorizationException {
        this.localPath = directoryPath;
        this.ownerAuth = ownerAuth;
        this.password = null;
        setupSshSessionFactory();
    }

    public RepoHelper(Path directoryPath, String sshPassword)
            throws GitAPIException, IOException, CancelledAuthorizationException {
        this.localPath = directoryPath;
        this.ownerAuth = null;
        this.password = sshPassword;
        setupSshSessionFactory();
    }

    public RepoHelper(Path directoryPath, File credentialsFile)
            throws GitAPIException, IOException, CancelledAuthorizationException {
        this.localPath = directoryPath;
        this.ownerAuth = null;
        this.password = null;
        this.credentialsFile = credentialsFile;
        setupSshSessionFactory();
    }

    public RepoHelper(Path directoryPath, UserInfo userInfo)
            throws GitAPIException, IOException, CancelledAuthorizationException {
        this.localPath = directoryPath;
        this.ownerAuth = null;
        this.password = null;
        this.credentialsFile = null;
        this.userInfo = userInfo;
        setupSshSessionFactory();
    }

    public RepoHelper(Path directoryPath, String sshPassword, UserInfo userInfo)
            throws GitAPIException, IOException, CancelledAuthorizationException {
        this.localPath = directoryPath;
        this.ownerAuth = null;
        this.password = sshPassword;
        this.credentialsFile = null;
        this.userInfo = userInfo;
        setupSshSessionFactory();
    }

    public RepoHelper(String sshPassword) {
        this.password = sshPassword;
        setupSshSessionFactory();
    }

    public RepoHelper(UserInfo userInfo) {
        this.userInfo = userInfo;
        setupSshSessionFactory();
    }

    public void setupSshSessionFactory() {
        sshSessionFactory = new JschConfigSessionFactory() {
            @Override
            protected void configure(OpenSshConfig.Host host, Session session) {
                session.setPassword(password);
                session.setUserInfo(userInfo);
            }

            @Override
            protected JSch createDefaultJSch(FS fs) throws JSchException {
                JSch defaultJSch = super.createDefaultJSch(fs);
                defaultJSch.removeAllIdentity();
                return defaultJSch;
            }
        };
    }



    /* This method requires credentials be passed in as a parameter; that's because it must be used by
        lsRemoteRepository, for example, that is used before we've actually created a RepoHelper object. Without a
        RepoHelper, there isn't an ownerAuth instance variable, so we don't have it yet.
     */
    void wrapAuthentication(TransportCommand command,
                                   UsernamePasswordCredentialsProvider ownerAuth) {
        wrapAuthentication(command, ownerAuth, null, null, null, null);
    }


    void wrapAuthentication(TransportCommand command, String sshPassword) {
        wrapAuthentication(command, null, sshPassword, null, null, null);
    }

    void wrapAuthentication(TransportCommand command, UsernamePasswordCredentialsProvider ownerAuth,
                                   String sshPassword) {
        wrapAuthentication(command, ownerAuth, sshPassword, null, null, null);
    }

    void wrapAuthentication(TransportCommand command,
                                   File credentialsFile) {
        wrapAuthentication(command, null, null, credentialsFile, null, null);
    }

    void wrapAuthentication(TransportCommand command, List<String> credentialsList) {
        wrapAuthentication(command, null, null, null, credentialsList, null);
    }

    void wrapAuthentication(TransportCommand command, UserInfo userInfo) {
        wrapAuthentication(command, null, null, null, null, userInfo);
    }

    void wrapAuthentication(TransportCommand command) {
        wrapAuthentication(command, null, null, null, null, null);
    }

    void wrapAuthentication(TransportCommand command, UsernamePasswordCredentialsProvider ownerAuth,
                                   String sshPassword, File credentialsFile, List<String> credentialsList,
                                   UserInfo userInfo) {

        if (ownerAuth != null)
            command.setCredentialsProvider(ownerAuth);
        else
            command.setCredentialsProvider(new ElegitCredentialsProvider(credentialsList));

        command.setTransportConfigCallback(
                new TransportConfigCallback() {
                    @Override
                    public void configure(Transport transport) {

                        if (transport instanceof TransportGitSsh) {
                            SshTransport sshTransport = (SshTransport) transport;
                            sshTransport.setSshSessionFactory(sshSessionFactory);
                        }

                    }
                });
    }

    protected void myWrapAuthentication(TransportCommand command) {
        wrapAuthentication(command, this.ownerAuth, this.password, this.credentialsFile, this.credentialsList,
                           this.userInfo);
    }

    // Common setup tasks shared by constructors
    protected void setup() throws GitAPIException, IOException {
        //this.repo = this.obtainRepository();
        this.username = null;

        this.commitIdMap = new HashMap<>();
        this.idMap = new HashMap<>();

        this.branchModel = new BranchModel(this);

        this.localCommits = this.parseAllLocalCommits();
        this.remoteCommits = this.parseAllRemoteCommits();

        this.tagModel = new TagModel(this);

        hasRemoteProperty = new SimpleBooleanProperty(!getLinkedRemoteRepoURLs().isEmpty());

    }

    public void setUsernamePasswordCredentials(UsernamePasswordCredentialsProvider ownerAuth) {
        this.ownerAuth = ownerAuth;
    }

    /**
     * Updates the entire model, including commits, branches and tags
     * Note: this is expensive, but avoids possible errors that faster
     * possible solutions have
     */
    public void updateModel() throws GitAPIException, IOException {
        this.commitIdMap = new HashMap<>();
        this.idMap = new HashMap<>();

        branchModel.updateAllBranches();
        // Reparse commits
        this.localCommits = this.parseAllLocalCommits();
        this.remoteCommits = this.parseAllRemoteCommits();

        tagModel.updateTags();
    }

    /**
     * @return true if the corresponding repository still exists in the expected location
     */
    public boolean exists() {
        logger.debug("Checked if repo still exists");
        return localPath.toFile().exists() && localPath.toFile().list((dir, name) -> name.equals(".git")).length > 0;
    }


    /**
     * Adds a file to the repository, has relativizing for unit tests
     *
     * @param filePath the path of the file to add.
     * @throws GitAPIException if the `git add` call fails.
     */
    public void addFilePathTest(Path filePath) throws GitAPIException {
        Git git = new Git(this.repo);
        // git add:
        Path relativizedFilePath = this.localPath.relativize(filePath);
        String pathToAdd = relativizedFilePath.toString();
        if (!File.separator.equals("/")) {
            if (File.separator.equals("\\"))
                pathToAdd = pathToAdd.replaceAll("\\\\", "/");
            else
                pathToAdd = pathToAdd.replaceAll(File.separator, "/");
        }
        git.add()
                .addFilepattern(pathToAdd)
                .call();
        git.close();
    }

    /**
     * Adds multiple files to the repository, has relativizing for unit tests
     *
     * @param filePaths an ArrayList of file paths to add.
     * @throws GitAPIException if the `git add` call fails.
     */
    public void addFilePathsTest(ArrayList<Path> filePaths) throws GitAPIException {
        Git git = new Git(this.repo);
        // git add:
        AddCommand adder = git.add();
        for (Path filePath : filePaths) {
            Path localizedFilePath = this.localPath.relativize(filePath);
            String pathToAdd = localizedFilePath.toString();
            if (!File.separator.equals("/")) {
                if (File.separator.equals("\\"))
                    pathToAdd = pathToAdd.replaceAll("\\\\", "/");
                else
                    pathToAdd = pathToAdd.replaceAll(File.separator, "/");
            }
            adder.addFilepattern(pathToAdd);
        }
        adder.call();
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
            String pathToAdd = filePath.toString();
            if (!File.separator.equals("/")) {
                if (File.separator.equals("\\"))
                    pathToAdd = pathToAdd.replaceAll("\\\\", "/");
                else
                    pathToAdd = pathToAdd.replaceAll(File.separator, "/");
            }
            adder.addFilepattern(pathToAdd);
        }
        adder.call();
        git.close();
    }

    /**
     * Checks out a file from the index
     * @param filePath the file to check out
     */
    public void checkoutFile(Path filePath) throws GitAPIException {
        Git git = new Git(this.repo);
        git.checkout().addPath(filePath.toString()).call();
        git.close();
    }

    /**
     * Checks out files from the index
     * @param filePaths the files to check out
     */
    public void checkoutFiles(List<Path> filePaths) throws GitAPIException {
        Git git = new Git(this.repo);
        CheckoutCommand checkout = git.checkout();
        for (Path filePath : filePaths)
            checkout.addPath(filePath.toString());
        checkout.call();
        git.close();
    }

    /**
     * Checks out a file from the specified point
     * @param filePath the file to check out
     * @param startPoint the tree-ish point to checkout the file from
     */
    public void checkoutFile(String filePath, String startPoint) throws GitAPIException {
        Git git = new Git(this.repo);
        git.checkout().setStartPoint(startPoint).addPath(filePath).call();
        git.close();
    }

    /**
     * Checks out files from the specified point
     * @param filePaths the files to check out
     * @param startPoint the tree-ish point to checkout the file from
     *
     * @return the result of the checkout
     */
    public CheckoutResult checkoutFiles(List<String> filePaths, String startPoint) throws GitAPIException {
        Git git = new Git(this.repo);
        CheckoutCommand checkout = git.checkout().setStartPoint(startPoint);
        for (String filePath : filePaths)
            checkout.addPath(filePath);
        checkout.call();
        return checkout.getResult();
    }

    /**
     * Removes a file from the repository.
     *
     * @param filePath the path of the file to remove.
     * @throws GitAPIException if the `git rm` call fails.
     */
    public void removeFilePath(Path filePath) throws GitAPIException {
        Git git = new Git(this.repo);
        // git rm:
        git.rm()
                .addFilepattern(filePath.toString())
                .call();
        git.close();
    }

    /**
     * Removes multiple files from the repository.
     *
     * @param filePaths an ArrayList of file paths to remove.
     * @throws GitAPIException if the `git rm` call fails.
     */
    public void removeFilePaths(ArrayList<Path> filePaths) throws GitAPIException {
        Git git = new Git(this.repo);
        // git rm:
        RmCommand remover = git.rm();
        for (Path filePath : filePaths) {
            remover.addFilepattern(filePath.toString());
        }
        remover.call();
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
     * @return the number of commits that local has that haven't been pushed
     */
    public int getAheadCount() throws IOException {
        if (this.branchModel.getCurrentBranch().getStatus() != null)
            return this.branchModel.getCurrentBranch().getStatus().getAheadCount();
        else return -1;
    }

    /**
     * @return the number of commits that all branches are ahead of remote cumulatively
     * @throws IOException
     */
    public int getAheadCountAll() throws IOException {
        int aheadCount = 0;
        for (BranchHelper helper : this.branchModel.getLocalBranchesTyped()) {
            if (helper.getStatus() != null)
                aheadCount += helper.getStatus().getAheadCount();
        }
        return aheadCount;
    }

    /**
     * @return the number of commits that remote has that haven't been merged in
     * @throws IOException
     */
    public int getBehindCount() throws IOException {
        if (this.branchModel.getCurrentBranch().getStatus() != null)
            return this.branchModel.getCurrentBranch().getStatus().getBehindCount();
        else return -1;
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
            e.printStackTrace();
        }
    }

    public void commitAll(String message) throws MissingRepoException, GitAPIException, IOException {
        if (!exists()) throw new MissingRepoException();


        Git git = new Git(repo);
        git.commit().setMessage(message).setAll(true).call();
        git.close();

        localCommits = parseAllLocalCommits();
    }

    /**
     * pushes only the current branch
     * @throws MissingRepoException
     * @throws GitAPIException
     * @throws PushToAheadRemoteError
     */
    public PushCommand prepareToPushCurrentBranch(boolean isTest) throws MissingRepoException, GitAPIException,
            PushToAheadRemoteError, IOException, NoCommitsToPushException {
        BranchHelper branchToPush = this.getBranchModel().getCurrentBranch();
        logger.info("attempting to push current branch");
        if (!exists()) throw new MissingRepoException();
        if (!hasRemote()) throw new InvalidRemoteException("No remote repository");


        String remote = getRemote();
        if (remote.equals("cancel"))
            throw new InvalidRemoteException("No remote selected.");

        Git git = new Git(this.repo);
        PushCommand push = git.push().setRemote(remote).add(branchToPush.getRefPathString());

        ProgressMonitor progress = new SimpleProgressMonitor();
        push.setProgressMonitor(progress);

        if(this.getBranchModel().getCurrentRemoteBranch() == null) {
            if(isTest || PopUpWindows.trackCurrentBranchRemotely(branchToPush.getRefName())){
                setUpstreamBranch(branchToPush, remote);
            }else {
                throw new NoCommitsToPushException();
            }
        }

        return push;
        //pushCurrentBranch(git, push);
    }

    public void pushCurrentBranch(PushCommand push) throws GitAPIException, PushToAheadRemoteError, IOException {
        myWrapAuthentication(push);
        Iterable<PushResult> pushResult = push.call();

        for(PushResult result : pushResult) {
            for(RemoteRefUpdate remoteRefUpdate : result.getRemoteUpdates()) {
                if(!remoteRefUpdate.getStatus().equals(RemoteRefUpdate.Status.OK)) {
                    throw new PushToAheadRemoteError(false);
                }
            }
        }

        push.getRepository().close();

        this.remoteCommits = parseAllRemoteCommits();
    }

    /**
     * Helper method for push that sets the upstream branch
     * @param branch local branch that needs an upstream branch
     * @param remote String
     */
    private void setUpstreamBranch(BranchHelper branch, String remote) throws IOException {
        Git git = new Git(this.repo);
        StoredConfig config = git.getRepository().getConfig();
        String branchName = branch.getRefName();
        config.setString(ConfigConstants.CONFIG_BRANCH_SECTION, branchName,  ConfigConstants.CONFIG_KEY_REMOTE, remote);
        config.setString(ConfigConstants.CONFIG_BRANCH_SECTION, branchName, ConfigConstants.CONFIG_KEY_MERGE, Constants.R_HEADS + branchName);
        config.save();
    }

    /**
     * Helper method that returns either the only remote, or the remote chosen by the user
     * @return String remote
     */
    private String getRemote() {
        Git git = new Git(this.repo);
        StoredConfig config = git.getRepository().getConfig();
        Set<String> remotes = config.getSubsections("remote");
        if (remotes.size() == 1) {
            return (String) remotes.toArray()[0];
        }
        return PopUpWindows.pickRemoteToPushTo(remotes);
    }

    /**
     * Pushes all changes.
     *
     * @throws GitAPIException if the `git push` call fails.
     */
    public PushCommand prepareToPushAll() throws GitAPIException, MissingRepoException, PushToAheadRemoteError,
            IOException, NoCommitsToPushException {
        logger.info("Attempting push");
        if (!exists()) throw new MissingRepoException();
        if (!hasRemote()) throw new InvalidRemoteException("No remote repository");

        // Gets the remote
        String remote = getRemote();
        if (remote.equals("cancel"))
            throw new InvalidRemoteException("No remote selected.");


        Git git = new Git(this.repo);
        PushCommand push = git.push().setRemote(remote);

        ArrayList<LocalBranchHelper> untrackedLocalBranches = new ArrayList<>();
        ArrayList<LocalBranchHelper> branchesToTrack = new ArrayList<>();

        // Gets all local branches with remote branches and adds them to the push call
        for(LocalBranchHelper branch : this.branchModel.getLocalBranchesTyped()) {
            if(BranchTrackingStatus.of(this.repo, branch.getRefName()) != null) {
                push.add(branch.getRefPathString());
            }else {
                untrackedLocalBranches.add(branch);
            }
        }

        // Asks the user which untracked local branches to push and track
        if(untrackedLocalBranches.size() > 0) {
            branchesToTrack = PopUpWindows.getUntrackedBranchesToPush(untrackedLocalBranches);
            if(branchesToTrack != null) {
                for(LocalBranchHelper branch : branchesToTrack) {
                    push.add(branch.getRefPathString());
                    setUpstreamBranch(branch, remote);
                }
            }else {
                if(this.getAheadCountAll() < 1) {
                    throw new NoCommitsToPushException();
                }
            }
        }

        ProgressMonitor progress = new SimpleProgressMonitor();
        push.setProgressMonitor(progress);

        return push;
        //pushAll(git, push);
    }

    public void pushAll(PushCommand push) throws GitAPIException, PushToAheadRemoteError, IOException {
        myWrapAuthentication(push);
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

        push.getRepository().close();

        this.remoteCommits = parseAllRemoteCommits();
    }

    /**
     * Pushes all tags in /refs/tags/.
     *
     * @throws GitAPIException if the `git push --tags` call fails.
     */
    public Iterable<PushResult> pushTags() throws GitAPIException, MissingRepoException, PushToAheadRemoteError, IOException {
        logger.info("Attempting push tags");
        if (!exists()) throw new MissingRepoException();
        if (!hasRemote()) throw new InvalidRemoteException("No remote repository");
        Git git = new Git(this.repo);
        PushCommand push = git.push();
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
                    System.out.println(remoteRefUpdate);
                }
            }
        }

        if (allPushesWereRejected || anyPushWasRejected) {
            throw new PushToAheadRemoteError(allPushesWereRejected);
        }

        git.close();

        this.tagModel.updateTags();

        return pushResult;
    }

    /**
     * Fetches changes into FETCH_HEAD (`git -fetch`).
     *
     * @throws GitAPIException
     * @throws MissingRepoException
     */
    public boolean fetch(boolean prune) throws
            GitAPIException, MissingRepoException, IOException {
        assert(!Platform.isFxApplicationThread());
        logger.info("Attempting fetch");
        if (!exists()) throw new MissingRepoException();
        Git git = new Git(this.repo);
        StoredConfig config = this.repo.getConfig();

        if(prune){
            config.setString("fetch", null, "prune", "true");
            config.save();
        }

        FetchCommand fetch = git.fetch().setTagOpt(TagOpt.AUTO_FOLLOW);

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

        this.branchModel.updateRemoteBranches();

        if(prune){
            config.unsetSection("fetch", null);
            config.save();
        }

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
    public MergeResult.MergeStatus mergeFromFetch() throws IOException, GitAPIException, MissingRepoException,
            ConflictingFilesException, NoTrackingException {
        logger.info("Attempting merge from fetch");
        if (!exists()) throw new MissingRepoException();
        if (!hasRemote()) throw new InvalidRemoteException("No remote repository");

        // Get the remote branch the current branch is tracking
        // and merge the current branch with the just fetched remote branch
        MergeResult result;
        Config config = repo.getConfig();
        // Check if this branch is being tracked locally
        if (config.getSubsections("branch").contains(this.repo.getBranch())) {
            String remote = config.getString("branch", this.repo.getBranch(), "remote")+"/";
            String remote_tracking = config.getString("branch", this.repo.getBranch(), "merge");
            result = branchModel.mergeWithBranch(this.branchModel.getBranchByName(BranchModel.BranchType.REMOTE, remote+this.repo.shortenRefName(remote_tracking)));
        } else {
            throw new NoTrackingException();
        }

        try {
            this.localCommits = parseAllLocalCommits();
        } catch (IOException e) {
            // This shouldn't occur once we have the repo up and running.
        }

        MergeResult.MergeStatus status = result.getMergeStatus();
        if (status == MergeResult.MergeStatus.CONFLICTING)
            throw new ConflictingFilesException(Collections.unmodifiableMap(result.getConflicts()));
        //return result.getMergeStatus().isSuccessful();
        return status;
    }

    //******************** REVERT SECTION ********************

    /**
     * Reverts a list of commit helpers. Calls revert on their objectIds
     * @param commits the commit helpers to revert
     * @throws MissingRepoException
     * @throws GitAPIException
     */
    public void revertHelpers(List<CommitHelper> commits) throws MissingRepoException, GitAPIException {
        List<AnyObjectId> commitIds = new ArrayList<>();
        for (CommitHelper helper : commits)
            commitIds.add(helper.getCommit());
        revert(commitIds);
    }

    /**
     * Reverts all of the commits listed
     * @param commits the object ids of commits to revert
     * @throws MissingRepoException
     * @throws GitAPIException
     */
    void revert(List<AnyObjectId> commits) throws MissingRepoException, GitAPIException {
        logger.info("Attempting reverts");
        if (!exists()) throw new MissingRepoException();
        Git git = new Git(this.repo);
        RevertCommand revertCommand = git.revert();
        for (AnyObjectId commit : commits)
            revertCommand.include(commit);
        revertCommand.call();
        git.close();

        // Update the local commits
        try {
            this.localCommits = parseAllLocalCommits();
        } catch (IOException e) {
            // This shouldn't occur once we have the repo up and running.
        }
    }

    /**
     * Reverts the changes that happened in the given commit, stores changes in working directory
     * if conflicting, otherwise, makes a new commit
     *
     * @param helper the commit to revert changes for
     * @throws MissingRepoException
     * @throws GitAPIException
     */
    public void revert(CommitHelper helper) throws MissingRepoException, GitAPIException {
        logger.info("Attempting revert");
        if (!exists()) throw new MissingRepoException();
        Git git = new Git(this.repo);
        // git revert:
        git.revert().include(helper.getObjectId()).call();
        git.close();

        // Update the local commits
        try {
            this.localCommits = parseAllLocalCommits();
        } catch (IOException e) {
            // This shouldn't occur once we have the repo up and running.
        }


    }

    //******************** RESET SECTION ********************
    // Relativizing of repository paths is for unit testing

    // File resetting
    /**
     * Resets the given file to the version in HEAD
     *
     * @param path the path of the file to reset
     * @throws MissingRepoException
     * @throws GitAPIException
     */
    void reset(Path path) throws MissingRepoException, GitAPIException {
        logger.info("Attempting reset file");
        if (!exists()) throw new MissingRepoException();
        Git git = new Git(this.repo);
        git.reset().addPath(this.localPath.relativize(path).toString()).call();
        git.close();
    }

    /**
     * Resets the given files to the version stored in HEAD
     *
     * @param paths a list of files to reset
     * @throws MissingRepoException
     * @throws GitAPIException
     */
    void reset(List<Path> paths) throws MissingRepoException, GitAPIException {
        logger.info("Attempting reset files");
        if (!exists()) throw new MissingRepoException();
        Git git = new Git(this.repo);
        ResetCommand resetCommand = git.reset();
        paths.forEach(path -> resetCommand.addPath(this.localPath.relativize(path).toString()));
        resetCommand.call();
        git.close();
    }

    // Commit resetting
    /**
     * Resets to the given commit using the default mode: mixed
     *
     * @param commit the commit to reset to
     * @throws MissingRepoException
     * @throws GitAPIException
     */
    void reset(CommitHelper commit) throws MissingRepoException, GitAPIException {
        reset(commit.getName(), ResetCommand.ResetType.MIXED);
    }

    /**
     * Resets to the given commit with the given mode
     * @param ref the ref (commit id or branch label) to reset to
     * @param mode the mode of reset to use (hard, mixed, soft, merge, or keep)
     * @throws MissingRepoException
     * @throws GitAPIException
     */
    public void reset(String ref, ResetCommand.ResetType mode) throws MissingRepoException, GitAPIException {
        logger.info("Attempting reset");
        if (!exists()) throw new MissingRepoException();
        Git git = new Git(this.repo);
        git.reset().setRef(ref).setMode(mode).call();
        git.close();
    }

    //******************** STASH SECTION ********************
    /* NOTE: jgit says stashcreate, but 'git stash create' only
     * makes a stash, it doesn't store it in refs/stash */

    /**
     * Stashes the current working directory and index changes with the
     * default message (the branch name)
     *
     * @param includeUntracked: whether or not to include untracked files
     */
    public void stashSave(boolean includeUntracked) throws GitAPIException, NoFilesToStashException {
        logger.info("Attempting stash save");
        Git git = new Git(this.repo);
        RevCommit stash = git.stashCreate().setIncludeUntracked(includeUntracked).call();
        if (stash == null) throw new NoFilesToStashException();
    }

    /**
     * Stashes the current working directory changes with the
     * given message
     *
     * @param includeUntracked: whether or not to include untracked files
     * @param wdMessage: the message used when committing working directory changes
     * @param indexMessage: the messaged used when committing the index changes
     */
    public void stashSave(boolean includeUntracked, String wdMessage, String indexMessage) throws GitAPIException, NoFilesToStashException {
        logger.info("Attempting stash save with message");
        Git git = new Git(this.repo);
        RevCommit stash = git.stashCreate().setIncludeUntracked(includeUntracked).setWorkingDirectoryMessage(wdMessage)
                .setIndexMessage(indexMessage).call();
        if (stash == null) throw new NoFilesToStashException();
    }

    /**
     * Returns a list of the stashed commits
     *
     * @return a list of commit helpers that make up the stash
     */
    public List<CommitHelper> stashList() throws GitAPIException, IOException {
        logger.info("Attempting stash list");
        Git git = new Git(this.repo);
        List<CommitHelper> stashCommitList = new ArrayList<>();

        for (RevCommit commit : git.stashList().call()) {
            stashCommitList.add(new CommitHelper(commit));
        }
        return Collections.unmodifiableList(stashCommitList);
    }

    /**
     * Applies the given stash to the repository, by default restores the index and
     * untracked files.
     *
     * @param stashRef the string that corresponds to the stash to apply
     * @param force whether or not to force apply
     */
    public void stashApply(String stashRef, boolean force) throws GitAPIException {
        logger.info("Attempting stash apply");
        Git git = new Git(this.repo);
        git.stashApply().setStashRef(stashRef).ignoreRepositoryState(force).call();
    }

    /**
     * Applies the given stash to the repository, with special instructions for
     * the index and untracked files
     *
     * @param stashRef the string that corresponds to the stash to apply
     * @param force whether or not to force apply
     * @param applyIndex true if the command should restore the index state
     * @param applyUntracked true if the command should restore the untracked files
     */
    void stashApply(String stashRef, boolean force, boolean applyIndex, boolean applyUntracked) throws GitAPIException {
        logger.info("Attempting stash apply with params");
        Git git = new Git(this.repo);
        StashApplyCommand stashApply = git.stashApply().setStashRef(stashRef).ignoreRepositoryState(force);
        stashApply.setApplyIndex(applyIndex);
        stashApply.setApplyUntracked(applyUntracked);
        stashApply.call();
    }

    /**
     * Deletes all the stashed commits
     *
     * @return the value of the stash reference after the drop occurs
     */
    public ObjectId stashClear() throws GitAPIException{
        logger.info("Attempting stash drop all");
        Git git = new Git(this.repo);
        return git.stashDrop().setAll(true).call();
    }

    /**
     * Deletes a single stashed reference
     *
     * @param stashRef the stash reference int to drop (0-based)
     * @return the value of the value of the stashed reference
     */
    public ObjectId stashDrop(int stashRef) throws GitAPIException{
        logger.info("Attempting stash drop");
        Git git = new Git(this.repo);
        return git.stashDrop().setStashRef(stashRef).call();
    }


    /**
     * Checks if the remote tracking head refers to the same commit
     * as the local head for the current branch
     * @return true if there are unmerged commits in the current branch, else false
     */
    public boolean checkUnmergedCommits() {
        Config config = this.repo.getConfig();
        String remoteBranch = config.getString("branch", this.branchModel.getCurrentBranch().getRefName(), "merge");
        String remote = config.getString("branch", this.branchModel.getCurrentBranch().getRefName(), "remote");
        if (remoteBranch == null || remote == null) return false;
        remoteBranch = remote + "/" + Repository.shortenRefName(remoteBranch);
        try {
            if (this.branchModel.getBranchByName(BranchModel.BranchType.REMOTE, remoteBranch) != null
                    && this.branchModel.getCurrentBranch() != null) {
                return !this.branchModel.getBranchByName(BranchModel.BranchType.REMOTE, remoteBranch).getHeadId()
                        .equals(this.branchModel.getCurrentBranch().getHeadId());
            }else return false;
        } catch (IOException e) {
            return false;
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
        return String.format("Commit ID: %s\n\nAuthor: %s\n\nTime: %s\n\nMessage: %s",
                commitHelper.getName().substring(0,8),
                commitHelper.getAuthorName(),
                commitHelper.getFormattedWhen(),
                commitHelper.getMessage(fullCommitMessage));
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

    /**
     * Helper method to determine if a commit is on both local and remote,
     * just on remote, or not merged in/tracked on local
     * @param helper the commit to check
     * @return the cell type, useful for drawing the tree
     */
    public Cell.CellType getCommitType(CommitHelper helper) {
        if (this.localCommits.contains(helper))
            if (this.remoteCommits.contains(helper))
                return Cell.CellType.BOTH;
            else
                return Cell.CellType.LOCAL;
        return Cell.CellType.REMOTE;
    }

    /**
     * @return a list of all commit IDs in this repository
     */
    public List<String> getAllCommitIDs() {
        return new ArrayList<>(commitIdMap.keySet());
    }

    public boolean canPush() throws IOException {
        return branchModel.getCurrentRemoteBranch() == null || getAheadCount() > 0;
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
        return getNewCommits(oldLocalBranches, this.branchModel.getBranchListTyped(BranchModel.BranchType.LOCAL));
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
        return getNewCommits(oldRemoteBranches, this.branchModel.getBranchListTyped(BranchModel.BranchType.REMOTE));
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
            if (oldBranches.containsKey(newBranch.getRefName())) {
                ObjectId newBranchHeadID = newBranch.getHeadId();
                ObjectId oldBranchHeadID = oldBranches.get(newBranch.getRefName()).getHeadId();
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
            String curCommitHelperID = curCommitHelper.getName();

            if (!commitIdMap.containsKey(curCommitHelperID)) {
                // TODO: can't possibly need both of these maps
                commitIdMap.put(curCommitHelper.getName(), curCommitHelper);
                idMap.put(curCommitID, curCommitHelper.getName());
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
            RevCommit[] parents = curCommitHelper.getCommit().getParents();
            for (RevCommit p : parents) {
                CommitHelper parentCommitHelper = getCommit(p.getId());
                if (parentCommitHelper == null) {
                    commitsWithMissingParents.add(curCommitHelper);
                } else if (!curCommitHelper.parentsContains(parentCommitHelper)) {
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
        Set<ObjectId> allStarts = new HashSet<ObjectId>();
        allStarts.add(repo.resolve("HEAD"));
        List<LocalBranchHelper> branches = this.branchModel.getLocalBranchesTyped();
        for (BranchHelper branch : branches) {
            ObjectId branchId = branch.getHeadId();
            allStarts.add(branchId);
        }
        PlotCommitList<PlotLane> rawLocalCommits = parseRawCommitsMultipleStarts(allStarts);

        return rawLocalCommits;
//
//
//        ObjectId headId = repo.resolve("HEAD");
//        if (headId == null) return new PlotCommitList<>();
//        List<ObjectId> examinedCommitIDs = new ArrayList<>();
//        PlotCommitList<PlotLane> rawLocalCommits = parseRawCommits(headId, examinedCommitIDs);
//        for (PlotCommit<PlotLane> commit : rawLocalCommits) {
//            examinedCommitIDs.add(commit.getId());
//        }
//        //examinedCommitIDs.add(headId);
//
//        List<LocalBranchHelper> branches = this.branchModel.getLocalBranchesTyped();
//        for (BranchHelper branch : branches) {
//            ObjectId branchId = branch.getHeadId();
//            PlotCommitList<PlotLane> toAdd = parseRawCommits(branchId, examinedCommitIDs);
//            for (PlotCommit<PlotLane> commit : toAdd) {
//                examinedCommitIDs.add(commit.getId());
//            }
//            if (toAdd.size() > 0) {
//                rawLocalCommits.addAll(toAdd);
////                examinedCommitIDs.add(toAdd.get(0).getId());
//            }
//        }
//
//        return rawLocalCommits;
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
//        List<ObjectId> examinedCommitIDs = new ArrayList<>();
//        PlotCommitList<PlotLane> rawRemoteCommits = new PlotCommitList<>();
//
//        for (BranchHelper branch : this.branchModel.getRemoteBranchesTyped()) {
//            ObjectId branchId = branch.getHeadId();
//            PlotCommitList<PlotLane> toAdd = parseRawCommits(branchId, examinedCommitIDs);
//            if (toAdd.size() > 0) {
//                rawRemoteCommits.addAll(toAdd);
//                examinedCommitIDs.add(toAdd.get(0).getId());
//            }
//        }
//        return rawRemoteCommits;

        Set<ObjectId> allStarts = new HashSet<ObjectId>();
        List<RemoteBranchHelper> branches = this.branchModel.getRemoteBranchesTyped();
        for (BranchHelper branch : branches) {
            ObjectId branchId = branch.getHeadId();
            allStarts.add(branchId);
        }
        return parseRawCommitsMultipleStarts(allStarts);
    }

    /**
     * Utilizes JGit to walk through the repo and create raw commit objects - more
     * specifically, JGit objects of (super)type RevCommit. This is an expensive
     * operation and should only be called when necessary
     *
     * @return a list of raw commits starting from each id in startPoints, excluding those beyond each id in stopPoints
     * @throws IOException
     */
    private PlotCommitList<PlotLane> parseRawCommitsMultipleStarts(Set<ObjectId> allStarts) throws IOException {
        PlotWalk w = new PlotWalk(repo);
        Set<RevCommit> allStartCommits = new HashSet<>();
        for (ObjectId start : allStarts) {
            allStartCommits.add(w.parseCommit(start));
        }
        w.markStart(allStartCommits);

        PlotCommitList<PlotLane> commitsFound = new PlotCommitList<>();
        commitsFound.source(w);
        commitsFound.fillTo(Integer.MAX_VALUE);
        w.dispose();

        return commitsFound;
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

    public List<RefHelper> getRefsForCommit(CommitHelper helper) {
        List<RefHelper> helpers = new ArrayList<>();
        if (this.branchModel.getBranchesWithHead(helper).size() > 0)
            helpers.addAll(this.branchModel.getAllBranchHeads().get(helper));
        Map<CommitHelper, List<TagHelper>> tagMap = this.tagModel.getTagCommitMap();
        if (tagMap.containsKey(helper))
            helpers.addAll(tagMap.get(helper));
        return helpers;
    }

    public BranchModel getBranchModel() {
        return this.branchModel;
    }

    public TagModel getTagModel() { return this.tagModel; }

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

    public UsernamePasswordCredentialsProvider getOwnerAuthCredentials() throws CancelledAuthorizationException {
        return this.ownerAuth;
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