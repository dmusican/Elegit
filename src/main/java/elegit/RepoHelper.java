package main.java.elegit;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import main.java.elegit.exceptions.MissingRepoException;
import main.java.elegit.exceptions.NoOwnerInfoException;
import main.java.elegit.exceptions.PushToAheadRemoteError;
import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revplot.PlotCommitList;
import org.eclipse.jgit.revplot.PlotLane;
import org.eclipse.jgit.revplot.PlotWalk;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.FetchResult;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/**
 * The abstract RepoHelper class, used for interacting with a repository.
 */
public abstract class RepoHelper {

    protected UsernamePasswordCredentialsProvider ownerAuth;
    private Repository repo;
    protected String remoteURL;

    protected Path localPath;
    private DirectoryWatcher directoryWatcher;

	private List<CommitHelper> localCommits;
    private List<CommitHelper> remoteCommits;

    private Map<String, CommitHelper> commitIdMap;
    private Map<ObjectId, String> idMap;

    private List<LocalBranchHelper> localBranches;
    private List<RemoteBranchHelper> remoteBranches;
    private LocalBranchHelper branchHelper;
    private BranchManager branchManager;

    public BooleanProperty hasRemoteProperty;
    public BooleanProperty hasUnpushedCommitsProperty;
    public BooleanProperty hasUnmergedCommitsProperty;

    /**
     * Creates a RepoHelper object for holding a Repository and interacting with it
     * through JGit.
     *
     * @param directoryPath the path of the repository.
     * @throws GitAPIException if the obtainRepository() call throws this exception..
     * @throws IOException if the obtainRepository() call throws this exception.
     */
    public RepoHelper(Path directoryPath, String remoteURL, RepoOwner owner) throws GitAPIException, IOException, NoOwnerInfoException {

        if (owner == null || (owner.getUsername() == null && owner.getPassword() == null)) {
            throw new NoOwnerInfoException();
        }

        this.ownerAuth = new UsernamePasswordCredentialsProvider(owner.getUsername(), owner.getPassword());

        this.remoteURL = remoteURL;

        this.localPath = directoryPath;

        this.repo = this.obtainRepository();

        this.commitIdMap = new HashMap<>();
        this.idMap = new HashMap<>();

        // TODO: performance? depth limit for parsing commits or something
        this.localCommits = this.parseAllLocalCommits();
        this.remoteCommits = this.parseAllRemoteCommits();

        this.branchManager = new BranchManager(this.callGitForLocalBranches(), this.callGitForRemoteBranches(), this);

        hasRemoteProperty = new SimpleBooleanProperty(!getLinkedRemoteRepoURLs().isEmpty());
        hasUnpushedCommitsProperty = new SimpleBooleanProperty(this.localCommits.size() > this.remoteCommits.size());
        hasUnmergedCommitsProperty = new SimpleBooleanProperty(this.localCommits.size() < this.remoteCommits.size());
    }

    /// Constructor for EXISTING repos to inherit (they don't need the Remote URL)
    public RepoHelper(Path directoryPath, RepoOwner owner) throws GitAPIException, IOException, NoOwnerInfoException {
        // If the user hasn't signed in (owner == null), then there is no authentication:
        if (owner == null) {
            this.ownerAuth = null;
        } else {
            this.ownerAuth = new UsernamePasswordCredentialsProvider(owner.getUsername(), owner.getPassword());
        }

        this.localPath = directoryPath;

        this.repo = this.obtainRepository();

        this.commitIdMap = new HashMap<>();
        this.idMap = new HashMap<>();

        this.localCommits = this.parseAllLocalCommits();
        this.remoteCommits = this.parseAllRemoteCommits();
        this.branchManager = new BranchManager(this.callGitForLocalBranches(), this.callGitForRemoteBranches(), this);

        hasRemoteProperty = new SimpleBooleanProperty(!getLinkedRemoteRepoURLs().isEmpty());
        hasUnpushedCommitsProperty = new SimpleBooleanProperty(this.localCommits.size() > this.remoteCommits.size());
        hasUnmergedCommitsProperty = new SimpleBooleanProperty(this.localCommits.size() < this.remoteCommits.size());

        // TODO: unify these two constructors (less copied-and-pasted code)
    }

    public boolean exists(){
        return localPath.toFile().exists() && localPath.toFile().list((dir, name) -> name.equals(".git")).length > 0;
    }

    public void setBranchManager(BranchManager branchManager) {
        this.branchManager = branchManager;
    }

    /**
     * Gets or builds the repository using the appropriate method for
     * the kind of repository (new, cloned, or existing).
     *
     * @return the RepoHelper's repository.
     * @throws GitAPIException (see subclasses).
     * @throws IOException (see subclasses).
     */
    protected abstract Repository obtainRepository() throws GitAPIException, IOException;

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
     * Commits changes to the repository.
     *
     * @param commitMessage the message for the commit.
     * @throws GitAPIException if the `git commit` call fails.
     */
    public void commit(String commitMessage) throws GitAPIException, MissingRepoException{
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
     * Pushes all changes.
     *
     * @throws GitAPIException if the `git push` call fails.
     */
    public void pushAll() throws GitAPIException, MissingRepoException, PushToAheadRemoteError {
        if(!exists()) throw new MissingRepoException();
        if(this.getLinkedRemoteRepoURLs().size() == 0) throw new InvalidRemoteException("No remote repository");
        Git git = new Git(this.repo);
        PushCommand push = git.push().setPushAll();

        if (this.ownerAuth != null) {
            push.setCredentialsProvider(this.ownerAuth);
        }
//        ProgressMonitor progress = new TextProgressMonitor(new PrintWriter(System.out));
        ProgressMonitor progress = new SimpleProgressMonitor();
        push.setProgressMonitor(progress);

        Iterable<PushResult> pushResult = push.call();
        boolean allPushesWereRejected = true;

        for (PushResult result : pushResult) {
            for (RemoteRefUpdate remoteRefUpdate : result.getRemoteUpdates()) {
                if (remoteRefUpdate.getStatus() != (RemoteRefUpdate.Status.REJECTED_NONFASTFORWARD)) {
                    allPushesWereRejected = false;
                    break;
                }
            }
        }

        if (allPushesWereRejected) {
            throw new PushToAheadRemoteError();
        }

        git.close();
        this.hasUnpushedCommitsProperty.set(false);
    }

    /**
     * Fetches changes into FETCH_HEAD (`git -fetch`).
     *
     * @throws GitAPIException
     * @throws MissingRepoException
     */
    public void fetch() throws GitAPIException, MissingRepoException{
        if(!exists()) throw new MissingRepoException();
        Git git = new Git(this.repo);

        // The JGit docs say that if setCheckFetchedObjects
        //  is set to true, objects received will be checked for validity.
        //  Not sure what that means, but sounds good so I'm doing it...
        FetchCommand fetch = git.fetch();

        if (this.ownerAuth != null) {
            fetch.setCredentialsProvider(this.ownerAuth);
        }

        fetch.setCheckFetchedObjects(true);

//        ProgressMonitor progress = new TextProgressMonitor(new PrintWriter(System.out));
        ProgressMonitor progress = new SimpleProgressMonitor();
        fetch.setProgressMonitor(progress);

        FetchResult result = fetch.call();
        git.close();
//        System.out.println(result.getMessages());
        this.hasUnmergedCommitsProperty.set(!result.getTrackingRefUpdates().isEmpty());
    }

    /**
     * Merges FETCH_HEAD into the current branch.
     * Combining fetch and merge is the same as `git -pull`.
     *
     * @throws IOException
     * @throws GitAPIException
     * @throws MissingRepoException
     */
    public boolean mergeFromFetch() throws IOException, GitAPIException, MissingRepoException {
        if(!exists()) throw new MissingRepoException();
        if(getLinkedRemoteRepoURLs().size() == 0) throw new InvalidRemoteException("No remote repository");
        Git git = new Git(this.repo);
        ObjectId fetchHeadID = this.repo.resolve("FETCH_HEAD");
//        if(fetchHeadID == null); // This might pop up at some point as an issue. Might not though
        MergeResult result = git.merge()
                .include(fetchHeadID)
                .call();
        git.close();
        this.hasUnmergedCommitsProperty.set(!Arrays.asList(result.getMergedCommits()).contains(result.getNewHead()));
        return result.getMergeStatus().isSuccessful();
    }

    public void closeRepo() {
        this.repo.close();
    }

    public Repository getRepo() {
        return this.repo;
    }

    public Path getDirectory() {
        return this.localPath;
    }

    public List<CommitHelper> getLocalCommits(){
        return this.localCommits;
    }

    public List<CommitHelper> getRemoteCommits(){
        return this.remoteCommits;
    }

    public CommitHelper getCommit(String idOrRefString){
        if(commitIdMap.containsKey(idOrRefString)){
            return commitIdMap.get(idOrRefString);
        }else{
            try{
                return getCommit(repo.resolve(idOrRefString));
            }catch(IOException e){
                return null;
            }
        }
    }

    public CommitHelper getCommit(ObjectId id){
        if(idMap.containsKey(id)){
            return getCommit(idMap.get(id));
        }else{
            return null;
        }
    }

    public List<String> getAllCommitIDs(){
        return new ArrayList<>(commitIdMap.keySet());
    }

    public List<CommitHelper> getNewLocalCommits(Map<String, BranchHelper> oldLocalBranches) throws GitAPIException, IOException{
        return getNewCommits(oldLocalBranches, this.callGitForLocalBranches());
    }

    public List<CommitHelper> getNewRemoteCommits(Map<String, BranchHelper> oldRemoteBranches) throws GitAPIException, IOException{
        return getNewCommits(oldRemoteBranches, this.callGitForRemoteBranches());
    }

    private List<CommitHelper> getNewCommits(Map<String, BranchHelper> oldBranches, List<? extends BranchHelper> newBranches) throws GitAPIException, IOException{
        List<ObjectId> startPoints = new ArrayList<>();
        List<ObjectId> stopPoints = new ArrayList<>();

        for(BranchHelper newBranch : newBranches){
            if(oldBranches.containsKey(newBranch.getBranchName())){
                ObjectId newBranchHeadID = newBranch.getHeadID();
                ObjectId oldBranchHeadID = oldBranches.get(newBranch.getBranchName()).getHeadID();
                if(!newBranchHeadID.equals(oldBranchHeadID)){
                    startPoints.add(newBranchHeadID);
                }
                stopPoints.add(oldBranchHeadID);
            }else{
                startPoints.add(newBranch.getHeadID());
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
    private List<CommitHelper> parseAllLocalCommits() throws IOException, GitAPIException{
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
    private List<CommitHelper> parseAllRemoteCommits() throws IOException, GitAPIException{
        PlotCommitList<PlotLane> commitList = this.parseAllRawRemoteCommits();
        return wrapRawCommits(commitList);
    }

    /**
     * Given a list of raw JGit commit objects, constructs CommitHelper objects to wrap them and gives
     * them the appropriate parents and children
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

            commitIdMap.put(CommitTreeModel.getId(curCommitHelper), curCommitHelper);
            idMap.put(curCommitID,CommitTreeModel.getId(curCommitHelper));
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
            ObjectId branchId = branch.getHeadID();
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
            ObjectId branchId = branch.getHeadID();
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

    public Path getLocalPath() {
        return localPath;
    }

    @Override
    public String toString() {
        return this.localPath.getFileName().toString();
    }

    public List<LocalBranchHelper> callGitForLocalBranches() throws GitAPIException, IOException {
        List<Ref> getBranchesCall = new Git(this.repo).branchList().call();
        localBranches = new ArrayList<>();

        for (Ref ref : getBranchesCall) localBranches.add(new LocalBranchHelper(ref, this));

        return localBranches;
    }

    public List<LocalBranchHelper> getLocalBranchesFromManager() {
        return this.branchManager.getLocalBranches();
    }

    public List<RemoteBranchHelper> callGitForRemoteBranches() throws GitAPIException {
        List<Ref> getBranchesCall = new Git(this.repo).branchList().setListMode(ListBranchCommand.ListMode.REMOTE).call();
        remoteBranches = new ArrayList<>();

        for (Ref ref : getBranchesCall) {

            // It appears that grabbing the remote branches also gets the HEAD.
            if (!ref.getName().equals("HEAD")) {
                remoteBranches.add(new RemoteBranchHelper(ref, this));
            }
        }

        return remoteBranches;
    }

    public void setOwner(RepoOwner owner) {
        if (owner == null || (owner.getUsername() == null && owner.getPassword() == null)) {
            // If there's no owner, there's no authentication.
            this.ownerAuth = null;
        } else {
            this.ownerAuth = new UsernamePasswordCredentialsProvider(owner.getUsername(), owner.getPassword());
        }
    }

    public void setCurrentBranch(LocalBranchHelper branchHelper) {
        this.branchHelper = branchHelper;
    }

    public LocalBranchHelper getCurrentBranch() {
        return this.branchHelper;
    }

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

    public BranchManager getBranchManager() {
        return branchManager;
    }

    public List<BranchHelper> getLocalBranches(){
        for(BranchHelper branch : localBranches){
            if(branch.getHead() == null){
                try{
                    branch.setHead(getCommit(branch.getHeadID()));
                }catch(IOException e){
                    e.printStackTrace();
                }
            }
        }
        return new ArrayList<>(localBranches);
    }

    public List<BranchHelper> getRemoteBranches(){
        for(BranchHelper branch : remoteBranches){
            if(branch.getHead() == null){
                try{
                    branch.setHead(getCommit(branch.getHeadID()));
                }catch(IOException e){
                    e.printStackTrace();
                }
            }
        }
        return new ArrayList<>(remoteBranches);
    }

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

    public Collection<Ref> getRefsFromRemote(boolean includeTags) throws GitAPIException{
        if(includeTags) return new Git(repo).lsRemote().setCredentialsProvider(this.ownerAuth).call();
        else return new Git(repo).lsRemote().setHeads(true).setCredentialsProvider(this.ownerAuth).call();
    }
}

