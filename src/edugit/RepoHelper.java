package edugit;

import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.NoMessageException;
import org.eclipse.jgit.api.errors.WrongRepositoryStateException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revplot.PlotCommitList;
import org.eclipse.jgit.revplot.PlotLane;
import org.eclipse.jgit.revplot.PlotWalk;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;

/**
 * The abstract RepoHelper class, used for interacting with a repository.
 */
public abstract class RepoHelper {

    protected UsernamePasswordCredentialsProvider ownerAuth; // TODO: Make an Owner object?
    private Repository repo;
    protected String remoteURL;

    protected Path localPath;
    private DirectoryWatcher directoryWatcher;

    /**
     * Creates a RepoHelper object for holding a Repository and interacting with it
     * through JGit.
     *
     * @param directoryPath the path of the repository.
     * @param ownerToken the user's login information. // TODO: make things less hardcoded.
     * @throws GitAPIException if the obtainRepository() call throws this exception..
     * @throws IOException if the obtainRepository() call throws this exception.
     */
    public RepoHelper(Path directoryPath, String ownerToken) throws GitAPIException, IOException {
        this.ownerAuth = new UsernamePasswordCredentialsProvider(ownerToken,"");
        this.remoteURL = "https://github.com/grahamearley/jgit-test.git"; // TODO: pass this in!

        this.localPath = directoryPath;

        this.repo = this.obtainRepository();

        // TODO: Use DirectoryWatcher for auto-refreshes.
//        this.directoryWatcher = new DirectoryWatcher(this.localPath);
//        this.directoryWatcher.beginProcessingEvents();

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

    public void closeRepo() {
        this.repo.close();
    }

    public Repository getRepo() {
        return this.repo;
    }

    public Path getDirectory() {
        return this.localPath;
    }


    /**
     * TODO: fill in this JavaDoc comment
     *
     * @return
     * @throws IOException
     */
    public ArrayList<String> getAllCommitMessages() throws IOException{
        PlotWalk w = new PlotWalk(repo);
        ObjectId rootId = repo.resolve("HEAD");
        RevCommit root = w.parseCommit(rootId);
        w.markStart(root);
        PlotCommitList<PlotLane> plotCommitList = new PlotCommitList<PlotLane>();
        plotCommitList.source(w);
        plotCommitList.fillTo(Integer.MAX_VALUE);

        ArrayList<String> m = new ArrayList<>(plotCommitList.size());
        for(int i = 0; i<plotCommitList.size(); i++){
            m.add(plotCommitList.get(i).getFullMessage());
        }
        return m;
    }

    public Path getLocalPath() {
        return localPath;
    }
}

