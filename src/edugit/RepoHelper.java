package edugit;

import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revplot.PlotCommit;
import org.eclipse.jgit.revplot.PlotCommitList;
import org.eclipse.jgit.revplot.PlotLane;
import org.eclipse.jgit.revplot.PlotWalk;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.IOException;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * The abstract RepoHelper class, used for interacting with a repository.
 */
public abstract class RepoHelper {

    protected UsernamePasswordCredentialsProvider ownerAuth; // TODO: Make an Owner object
    private Repository repo;
    protected String remoteURL;

    protected Path localPath;
    private DirectoryWatcher directoryWatcher;

    public RepoHelper(Path directoryPath, String ownerToken) throws Exception {
        this.ownerAuth = new UsernamePasswordCredentialsProvider(ownerToken,"");
        this.remoteURL = "https://github.com/grahamearley/jgit-test.git"; // TODO: pass this in!

        this.localPath = directoryPath;

        this.repo = this.obtainRepository();

        this.directoryWatcher = new DirectoryWatcher(this.localPath);
//        this.directoryWatcher.beginProcessingEvents();

    }

    protected abstract Repository obtainRepository() throws GitAPIException;

    public void addFilePath(Path filePath) {
        Git git = new Git(this.repo);
        // git add:
        try {
            Path relativizedFilePath = this.localPath.relativize(filePath);
            git.add()
                    .addFilepattern(relativizedFilePath.toString())
                    .call();
        } catch (GitAPIException e) {
            e.printStackTrace();
        }
        git.close();
    }

    public void addFilePaths(ArrayList<Path> filePaths) {
        Git git = new Git(this.repo);
        // git add:
        try {
            AddCommand adder = git.add();
            for (Path filePath : filePaths) {
                Path localizedFilePath = this.localPath.relativize(filePath);
                adder.addFilepattern(localizedFilePath.toString());
            }
            adder.call();
        } catch (GitAPIException e) {
            e.printStackTrace();
        }
        git.close();
    }

    public void commitFile(String commitMessage) {
        // should this Git instance be class-level?
        Git git = new Git(this.repo);
        // git commit:
        try {
            git.commit()
                    .setMessage(commitMessage)
                    .call();
        } catch (GitAPIException e) {
            e.printStackTrace();
        }
        git.close();
    }

    public void pushAll() {
        Git git = new Git(this.repo);
        try {
            git.push().setPushAll().setCredentialsProvider(this.ownerAuth).call();
        } catch (GitAPIException e) {
            e.printStackTrace();
        }
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


    public PlotCommitList<PlotLane> getAllCommits() throws IOException{
        PlotWalk w = new PlotWalk(repo);
        ObjectId rootId = repo.resolve("HEAD");
        RevCommit root = w.parseCommit(rootId);
        w.markStart(root);
        PlotCommitList<PlotLane> plotCommitList = new PlotCommitList<>();
        plotCommitList.source(w);
        plotCommitList.fillTo(Integer.MAX_VALUE);

        return plotCommitList;
    }

    public ArrayList<String> getAllCommitsInfo() throws IOException{
        PlotCommitList<PlotLane> commits = this.getAllCommits();
        ArrayList<String> strings = new ArrayList<>(commits.size());
        for(int i = 0; i<commits.size(); i++){
            PlotCommit<PlotLane> commit = commits.get(i);

            DateFormat formatter = new SimpleDateFormat("h:mm a MMM dd yyyy");

            PersonIdent authorIdent = commit.getAuthorIdent();
            Date date = authorIdent.getWhen();
            String dateFormatted = formatter.format(date);

            PlotLane lane = commit.getLane();

            String s = commit.getName();
            s = s + " - " + authorIdent.getName();
            s = s + " - " + dateFormatted;
            s = s + " - Children: " + commit.getChildCount();
            s = s + " - Parents: " + commit.getParentCount();
            s = s + " - Lane: " + lane.getPosition();
            s = s + " - " + commit.getShortMessage();
            strings.add(s);
        }
        return strings;
    }

    public CommitHelper getCurrentHeadCommit() throws IOException{
        PlotWalk w = new PlotWalk(repo);
        ObjectId commitId = repo.resolve("HEAD");
        return new CommitHelper(w.parseCommit(commitId));
    }
}

