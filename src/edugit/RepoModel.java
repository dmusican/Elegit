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

    private File localPath = new File(System.getProperty("user.home")+File.separator+"Desktop"+File.separator+"TestClone");

    public RepoModel(String ownerToken) {
        this.ownerAuth = new UsernamePasswordCredentialsProvider(SECRET_CONSTANTS.TEST_GITHUB_TOKEN,"");
        this.remoteURL = "https://github.com/grahamearley/jgit-test.git"; // TODO: pass this in!

        this.localPath.delete();
    }

    public void cloneRepo() {
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

        this.repo = cloneCall.getRepository();
    }

    public void findRepo() {
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        try {
            this.repo = builder.findGitDir(this.localPath)
                    .readEnvironment() // scan environment GIT_* variables
                    .build();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void pushNewFile(String fileNameString, String commitMessage) {
        Git git = new Git(this.repo);
        String fileName = fileNameString;

        // Create file
        File myfile = new File(repo.getDirectory().getParent(), fileName);
        try {
            myfile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // git add:
        try {
            git.add()
                    .addFilepattern(fileName)
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
