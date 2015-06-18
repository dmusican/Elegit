package edugit;

import com.sun.jdi.InvocationException;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.*;
import org.eclipse.jgit.errors.AmbiguousObjectException;
import org.eclipse.jgit.lib.ObjectId;
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

	private ArrayList<CommitHelper> localCommits;
    private ArrayList<CommitHelper> remoteCommits;

    private Map<String, CommitHelper> commitIdMap;
    private Map<ObjectId, String> idMap;


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
            return this.commitIdMap.get(commitId);
        }catch(Exception e){
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Constructs a list of all local commits found by parsing the repository for raw RevCommit objects,
     * then wrapping them into a CommitHelper with the appropriate parents and children
     *
     * @return a list of CommitHelpers for all local commits
     * @throws IOException
     */
    private ArrayList<CommitHelper> parseAllLocalCommits() throws IOException{
        PlotCommitList<PlotLane> commitList = this.parseAllRawLocalCommits();
        return wrapRawCommits(commitList, commitIdMap);
    }

    /**
     * Constructs a list of all remote commits found by parsing the repository for raw RevCommit objects,
     * then wrapping them into a CommitHelper with the appropriate parents and children
     *
     * @return a list of CommitHelpers for all remote commits
     * @throws IOException
     */
    private ArrayList<CommitHelper> parseAllRemoteCommits() throws IOException{
        PlotCommitList<PlotLane> commitList = this.parseAllRawRemoteCommits();
        return wrapRawCommits(commitList, commitIdMap);
    }

    /**
     * Given a list of raw JGit commit objects, constructs CommitHelper objects to wrap them and gives
     * them the appropriate parents and children
     * @param commitList the raw commits to wrap
     * @param commitIdMap the map for placing ids and commits into
     * @return a list of CommitHelpers for the given commits
     * @throws IOException
     */
    private ArrayList<CommitHelper> wrapRawCommits(PlotCommitList<PlotLane> commitList,
                                                   Map<String, CommitHelper> commitIdMap) throws IOException{
        ArrayList<CommitHelper> commitHelperList = new ArrayList<>(commitList.size());
        for(int i = commitList.size()-1; i >= 0; i--){
            RevCommit curCommit = commitList.get(i);

            CommitHelper curCommitHelper = new CommitHelper(curCommit, this);
            commitIdMap.put(CommitTreeModel.getId(curCommitHelper), curCommitHelper);
            idMap.put(curCommit.getId(),CommitTreeModel.getId(curCommitHelper));

            RevCommit[] parents = curCommit.getParents();
            for(RevCommit p : parents){
                CommitHelper parentCommitHelper = commitIdMap.get(idMap.get(p.getId()));
                curCommitHelper.addParent(parentCommitHelper);
            }

            commitHelperList.add(curCommitHelper);
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
    private PlotCommitList<PlotLane> parseAllRawLocalCommits() throws IOException{
        // TODO: maybe resolve different branches (e.g. "refs/heads/master")?
        ObjectId headId = repo.resolve("HEAD");
        return parseAllRawCommits(headId);
    }
    /**
     * Utilizes JGit to walk through the repo and create raw commit objects - more
     * specifically, JGit objects of (super)type RevCommit. This is an expensive
     * operation and should only be called when necessary
     * @return a list of raw remote commits
     * @throws IOException
     */
    private PlotCommitList<PlotLane> parseAllRawRemoteCommits() throws IOException{
        ObjectId originHeadId = repo.resolve("refs/remotes/origin/HEAD");
        if(originHeadId == null){
            originHeadId = repo.resolve("refs/remotes/origin/master");
        }
        return parseAllRawCommits(originHeadId);
    }
    /**
     * Utilizes JGit to walk through the repo and create raw commit objects - more
     * specifically, JGit objects of (super)type RevCommit. This is an expensive
     * operation and should only be called when necessary
     * @param id the starting point to parse from
     * @return a list of raw commits starting from the given id
     * @throws IOException
     */
    private PlotCommitList<PlotLane> parseAllRawCommits(ObjectId id) throws IOException{
        PlotWalk w = new PlotWalk(repo);
        RevCommit start = w.parseCommit(id);
        w.markStart(start);
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
}

