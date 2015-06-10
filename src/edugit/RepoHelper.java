package edugit;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.File;
import java.nio.file.Path;

/**
 * The abstract RepoHelper class, used for interacting with a repository.
 */
public abstract class RepoHelper {

    protected UsernamePasswordCredentialsProvider ownerAuth; // TODO: Make an Owner object
    private Repository repo;
    protected String remoteURL;

    protected Path localPath;

    public RepoHelper(Path directoryPath, String ownerToken) throws Exception {
        this.ownerAuth = new UsernamePasswordCredentialsProvider(ownerToken,"");
        this.remoteURL = "https://github.com/grahamearley/jgit-test.git"; // TODO: pass this in!

        this.localPath = directoryPath;

        this.repo = this.obtainRepository();

    }

    protected abstract Repository obtainRepository() throws GitAPIException;

    public void addFilePath(Path filePath) {
        Git git = new Git(this.repo);
        // git add:
        try {
            git.add()
                    .addFilepattern(filePath.getFileName().toString())
                    .call();
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

