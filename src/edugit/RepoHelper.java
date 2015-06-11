package edugit;

import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.nio.file.Path;
import java.util.ArrayList;

/**
 * The abstract RepoHelper class, used for interacting with a repository.
 */
public abstract class RepoHelper {

    protected UsernamePasswordCredentialsProvider ownerAuth; // TODO: Make an Owner object
    private Repository repo;
    protected String remoteURL;

    protected Path localPath;
    private DirectoryWatcher directoryWatcher;

    public RepoHelper(Path directoryPath, String remoteURL, String username, String password) throws Exception {
        this.ownerAuth = new UsernamePasswordCredentialsProvider(username, password);
        this.remoteURL = remoteURL; //"https://github.com/grahamearley/jgit-test.git"; // TODO: pass this in!

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

}

