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
public class RepoModel {

    private UsernamePasswordCredentialsProvider ownerAuth; // TODO: Make an Owner object
    private Repository repo;
    private String remoteURL;

    private File localPath;

    public RepoModel(File directoryPath, String ownerToken, boolean directoryContainsRepo) {
        this.ownerAuth = new UsernamePasswordCredentialsProvider(ownerToken,"");
        this.remoteURL = "https://github.com/grahamearley/jgit-test.git"; // TODO: pass this in!

        this.localPath = directoryPath;

        // This ensures that the path is a directory, not a folder
        //  ( .delete() will delete any file at the end of the path )
        this.localPath.delete();

        if (directoryContainsRepo) {
            this.repo = this.findExistingRepo();
        } else {
            this.repo = this.cloneRepo();
        }

    }

    private Repository cloneRepo() {
        // TODO: make this not just clone a dummy repo...

        CloneCommand cloneCommand = Git.cloneRepository();
        cloneCommand.setURI(this.remoteURL);
        cloneCommand.setCredentialsProvider(this.ownerAuth);

        cloneCommand.setDirectory(this.localPath);
        Git cloneCall = null;

        try {
            cloneCall = cloneCommand.call();
        } catch (GitAPIException e) {
            e.printStackTrace();
            // TODO: better error handling
        }

        return cloneCall.getRepository();
    }

    private Repository findExistingRepo() {
        // TODO: for now, make there be only two options: clone *or* have a repo already cloned
        //  Then, figure out how to create a repo from scratch...
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        try {
            return builder.findGitDir(this.localPath)
                    .readEnvironment()
                    .build();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

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
