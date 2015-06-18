package edugit;

import com.sun.jdi.InvocationException;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.*;
import org.eclipse.jgit.errors.AmbiguousObjectException;
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

	private ArrayList<CommitHelper> localCommits;
    private Map<ObjectId, CommitHelper> localCommitIdMap;
    private ArrayList<String> branchStrings;


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
        this.localCommitIdMap = new HashMap<>();
        this.localCommits = this.parseLocalCommits();

        // TODO: Use DirectoryWatcher for auto-refreshes.
        // TODO: performance? depth limit for parsing commits or something
    }

    /// Constructor for EXISTING repos to inherit (they don't need the Remote URL)
    public RepoHelper(Path directoryPath, RepoOwner owner) throws GitAPIException, IOException {
        this.ownerAuth = new UsernamePasswordCredentialsProvider(owner.getUsername(), owner.getPassword());
        this.localPath = directoryPath;

        this.repo = this.obtainRepository();
        this.localCommitIdMap = new HashMap<>();
        this.localCommits = this.parseLocalCommits();
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

    public ArrayList<CommitHelper> getLocalCommits(){
        return this.localCommits;
    }

    /**
     * @return the CommitHelper that contains the current HEAD
     */
    public CommitHelper getCurrentHeadCommit(){
        try{
            ObjectId commitId = repo.resolve("HEAD");
            return this.localCommitIdMap.get(commitId);
        }catch(Exception e){
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Constructs a list of all local commits found by parsing the repository using getAllCommits(), each
     * wrapped into a CommitHelper with the appropriate parents and children
     *
     * @return a list of CommitHelpers for all local commits
     * @throws IOException
     */
    private ArrayList<CommitHelper> parseLocalCommits() throws IOException{
        PlotCommitList<PlotLane> commitList = this.parseRawLocalCommits();

        this.localCommitIdMap = new HashMap<>(commitList.size());
        ArrayList<CommitHelper> commitHelperList = new ArrayList<>(commitList.size());
        for(int i = commitList.size()-1; i >= 0; i--){
            RevCommit curCommit = commitList.get(i);

            CommitHelper curCommitHelper = new CommitHelper(curCommit, this);
            localCommitIdMap.put(curCommit.getId(), curCommitHelper);

            RevCommit[] parents = curCommit.getParents();
            for(RevCommit p : parents){
                CommitHelper parentCommitHelper = localCommitIdMap.get(p.getId());
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
     * @return a list of raw commits
     * @throws IOException
     */
    public PlotCommitList<PlotLane> parseRawLocalCommits() throws IOException{
        PlotWalk w = new PlotWalk(repo);
        ObjectId rootId = repo.resolve("HEAD");
        RevCommit root = w.parseCommit(rootId);
        w.markStart(root);
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

    public ArrayList<String> getLocalBranchNames() throws GitAPIException {
        // see JGit cookbook for how to get Remote branches too
        List<Ref> getBranchesCall = new Git(this.repo).branchList().call();

        ArrayList<String> branchNames = new ArrayList<>();

        for (Ref ref : getBranchesCall) {
            branchNames.add(ref.getName());
        }

        return branchNames;
    }

    public void checkoutBranch(String branchName) throws GitAPIException {
        new Git(this.repo).checkout().setName(branchName).call();
    }

    public String getCurrentBranchName() throws IOException {
        return this.repo.getBranch();
    }
}

