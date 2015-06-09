package edugit;

import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.File;
import java.io.IOException;

/**
 * Created by grahamearley on 6/9/15.
 */
public abstract class AbstractRepoModel {

    protected UsernamePasswordCredentialsProvider ownerAuth; // TODO: Make an Owner object
    private Repository repo;
    protected String remoteURL;

    protected File localPath;

    public AbstractRepoModel(File directoryPath, String ownerToken) throws Exception {
        this.ownerAuth = new UsernamePasswordCredentialsProvider(ownerToken,"");
        this.remoteURL = "https://github.com/grahamearley/jgit-test.git"; // TODO: pass this in!

        this.localPath = directoryPath;

        // This ensures that the path is a directory, not a folder
        //  ( .delete() will delete any file at the end of the path )
        this.localPath.delete();

        this.repo = this.obtainRepository();

    }

    protected abstract Repository obtainRepository() throws GitAPIException;

    public void pushNewFile(File file, String commitMessage) {
        Git git = new Git(this.repo);
        // git add:
        try {
            git.add()
                    .addFilepattern(file.getName())
                    .call();
        } catch (GitAPIException e) {
            e.printStackTrace();
        }

        // git commit:
        try {
            git.commit()
                    .setMessage(commitMessage)
                    .call();
        } catch (GitAPIException e) {
            e.printStackTrace();
        }

        try {
            git.push().setPushAll().setCredentialsProvider(this.ownerAuth).call();
        } catch (GitAPIException e) {
            e.printStackTrace();
        }

    }

    public void closeRepo() {
        this.repo.close();
    }

}