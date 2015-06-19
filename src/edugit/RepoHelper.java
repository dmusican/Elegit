package edugit;

import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revplot.PlotCommitList;
import org.eclipse.jgit.revplot.PlotLane;
import org.eclipse.jgit.revplot.PlotWalk;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private CommitHelper localHead;

    private Map<String, ObjectId> localBranches;
    private Map<String, ObjectId> remoteBranches;


    /**
     * Creates a RepoHelper object for holding a Repository and interacting with it
     * through JGit.
     *
     * @param directoryPath the path of the repository.
     * @throws GitAPIException if the obtainRepository() call throws this exception..
     * @throws IOException if the obtainRepository() call throws this exception.
     */
    public RepoHelper(Path directoryPath, String remoteURL, RepoOwner owner) throws GitAPIException, IOException {
        this.ownerAuth = new UsernamePasswordCredentialsProvider(owner.getUsername(), owner.getPassword());

        this.remoteURL = remoteURL;

        this.localPath = directoryPath;

        this.repo = this.obtainRepository();

        // TODO: Use DirectoryWatcher for auto-refreshes.
//        this.directoryWatcher = new DirectoryWatcher(this.localPath);
//        this.directoryWatcher.beginProcessingEvents();

        this.commitIdMap = new HashMap<>();
        this.idMap = new HashMap<>();

        // TODO: performance? depth limit for parsing commits or something
        this.localCommits = this.parseAllLocalCommits();
        this.remoteCommits = this.parseAllRemoteCommits();
    }

    /// Constructor for EXISTING repos to inherit (they don't need the Remote URL)
    public RepoHelper(Path directoryPath, RepoOwner owner) throws GitAPIException, IOException {
        this.ownerAuth = new UsernamePasswordCredentialsProvider(owner.getUsername(), owner.getPassword());
        this.localPath = directoryPath;

        this.repo = this.obtainRepository();

        this.commitIdMap = new HashMap<>();
        this.idMap = new HashMap<>();

        this.localCommits = this.parseAllLocalCommits();
        this.remoteCommits = this.parseAllRemoteCommits();
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

    /**
     * Commits changes to the repository.
     *
     * @param commitMessage the message for the commit.
     * @throws GitAPIException if the `git commit` call fails.
     */
    public void commit(String commitMessage) throws GitAPIException {
        // should this Git instance be class-level?
        Git git = new Git(this.repo);
        // git commit:
        git.commit()
                .setMessage(commitMessage)
                .call();
        git.close();
    }

    /**
     * Pushes all changes.
     *
     * @throws GitAPIException if the `git push` call fails.
     */
    public void pushAll() throws GitAPIException {
        Git git = new Git(this.repo);
        git.push().setPushAll()
                .setCredentialsProvider(this.ownerAuth)
                .call();
        git.close();
    }

    public void fetch() throws GitAPIException {
        Git git = new Git(this.repo);

        // The JGit docs say that if setCheckFetchedObjects
        //  is set to true, objects received will be checked for validity.
        //  Not sure what that means, but sounds good so I'm doing it...
        git.fetch().setCredentialsProvider(this.ownerAuth).setCheckFetchedObjects(true).call();
        git.close();
    }

    public void mergeFromFetch() throws IOException, GitAPIException {
        Git git = new Git(this.repo);
        git.merge().include(this.repo.resolve("FETCH_HEAD")).call();
        git.close();
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

    public CommitHelper getCommit(String id){
        return commitIdMap.get(id);
    }

    public List<String> getAllCommitIDs(){
        return new ArrayList<>(commitIdMap.keySet());
    }

    /**
     * @return the CommitHelper that contains the current HEAD
     */
    public CommitHelper getLocalHeadCommit(){
        try{
            ObjectId commitId = repo.resolve("HEAD");
            return commitIdMap.get(idMap.get(commitId));
        }catch(Exception e){
            e.printStackTrace();
            return null;
        }
    }

    public List<CommitHelper> getNewLocalCommits() throws GitAPIException, IOException{
        Map<String, ObjectId> oldBranchHeads = new HashMap<>(this.localBranches);
        List<String> newLocalBranchNames = this.getLocalBranchNames();
        List<CommitHelper> allNewCommits = new ArrayList<>();
        for(String branchName : newLocalBranchNames){
            ObjectId newBranchHead = this.localBranches.get(branchName);
            if(!oldBranchHeads.containsKey(branchName) || !oldBranchHeads.get(branchName).equals(newBranchHead)){
                PlotCommitList<PlotLane> newCommits = this.parseRawCommits(newBranchHead, new ArrayList<>(oldBranchHeads.values()));
                allNewCommits.addAll(wrapRawCommits(newCommits));
            }
        }
        return allNewCommits;
    }

    public List<CommitHelper> getNewRemoteCommits() throws GitAPIException, IOException{
        Map<String, ObjectId> oldBranchHeads = new HashMap<>(this.remoteBranches);
        List<String> newRemoteBranchNames = this.getRemoteBranchNames();
        List<CommitHelper> allNewCommits = new ArrayList<>();
        for(String branchName : newRemoteBranchNames){
            ObjectId newBranchHead = this.remoteBranches.get(branchName);
            if(!oldBranchHeads.containsKey(branchName) || !oldBranchHeads.get(branchName).equals(newBranchHead)){
                PlotCommitList<PlotLane> newCommits = this.parseRawCommits(newBranchHead, new ArrayList<>(oldBranchHeads.values()));
                allNewCommits.addAll(wrapRawCommits(newCommits));
            }
        }
        return allNewCommits;
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
                CommitHelper parentCommitHelper = commitIdMap.get(idMap.get(p.getId()));
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
                CommitHelper parentCommitHelper = commitIdMap.get(idMap.get(p.getId()));
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
        // TODO: maybe resolve different branches (e.g. "refs/heads/master")?
        ObjectId headId = repo.resolve("HEAD");
        List<ObjectId> examinedCommitIDs = new ArrayList<>();
        PlotCommitList<PlotLane> rawLocalCommits = parseRawCommits(headId, examinedCommitIDs);
        examinedCommitIDs.add(headId);

        List<String> branchNames = getLocalBranchNames();
        for(String branch : branchNames){
            ObjectId branchId = repo.resolve(branch);
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

        List<String> branchNames = getRemoteBranchNames();
        for(String branch : branchNames){
            ObjectId branchId = repo.resolve(branch);
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
     * @param startingID the starting point to parse from
     * @return a list of raw commits starting from the given id
     * @throws IOException
     */
    private PlotCommitList<PlotLane> parseRawCommits(ObjectId startingID, List<ObjectId> stopPoints) throws IOException{
        PlotWalk w = new PlotWalk(repo);
        RevCommit start = w.parseCommit(startingID);
        w.markStart(start);
        for(ObjectId stopId : stopPoints){
            w.markUninteresting(w.parseCommit(stopId));
        }
        PlotCommitList<PlotLane> plotCommitList = new PlotCommitList<>();
        plotCommitList.source(w);
        plotCommitList.fillTo(Integer.MAX_VALUE);

        return plotCommitList;
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
        return w.parseCommit(id);
    }

    public Path getLocalPath() {
        return localPath;
    }

    @Override
    public String toString() {
        return this.localPath.getFileName().toString();
    }

    public List<String> getLocalBranchNames() throws GitAPIException {
        // see JGit cookbook for how to get Remote branches too
        List<Ref> getBranchesCall = new Git(this.repo).branchList().call();

        this.localBranches = new HashMap<>();

        for (Ref ref : getBranchesCall) {
            localBranches.put(ref.getName(), ref.getObjectId());
        }

        return new ArrayList<>(localBranches.keySet());
    }

    public List<String> getRemoteBranchNames() throws GitAPIException {
        // see JGit cookbook for how to get Remote branches too
        List<Ref> getBranchesCall = new Git(this.repo).branchList().setListMode(ListBranchCommand.ListMode.REMOTE).call();

        this.remoteBranches = new HashMap<>();

        for (Ref ref : getBranchesCall) {
            remoteBranches.put(ref.getName(), ref.getObjectId());
        }

        return new ArrayList<>(remoteBranches.keySet());
    }

    public void checkoutBranch(String branchName) throws GitAPIException {
        new Git(this.repo).checkout().setName(branchName).call();
    }

    public String getCurrentBranchName() throws IOException {
        return this.repo.getBranch();
    }

    public void setOwner(RepoOwner owner) {
        this.ownerAuth = new UsernamePasswordCredentialsProvider(owner.getUsername(), owner.getPassword());
    }
}

