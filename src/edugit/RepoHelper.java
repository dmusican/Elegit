package edugit;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.File;

/**
 * Created by grahamearley on 6/10/15.
 */
public abstract class RepoHelper {

    protected UsernamePasswordCredentialsProvider ownerAuth; // TODO: Make an Owner object
    private Repository repo;
    protected String remoteURL;

    protected File localPath;

    public RepoHelper(File directoryPath, String ownerToken) throws Exception {
        this.ownerAuth = new UsernamePasswordCredentialsProvider(ownerToken,"");
        this.remoteURL = "https://github.com/grahamearley/jgit-test.git"; // TODO: pass this in!

        this.localPath = directoryPath;

        // This ensures that the path is a directory, not a folder
        //  ( .delete() will delete any file at the end of the path )
        this.localPath.delete();

        this.repo = this.obtainRepository();

    }

    protected abstract Repository obtainRepository() throws GitAPIException;

    public void addFile(File file) {
        Git git = new Git(this.repo);
        // git add:
        try {
            git.add()
                    .addFilepattern(file.getName())
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

    public File getDirectory() {
        return this.localPath;
    }

}

