package main.java.elegit;

import main.java.elegit.exceptions.CancelledAuthorizationException;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

/**
 * A RepoHelper implementation for a repository cloned into an empty folder.
 */
public class ClonedRepoHelper extends RepoHelper {
    public ClonedRepoHelper(Path directoryPath, String remoteURL) throws IOException, GitAPIException, CancelledAuthorizationException{
        super(directoryPath, remoteURL);
    }

    public ClonedRepoHelper(Path directoryPath, String remoteURL, UsernamePasswordCredentialsProvider ownerAuth)
            throws GitAPIException, IOException, CancelledAuthorizationException {
        super(directoryPath, remoteURL, ownerAuth);
    }

    public ClonedRepoHelper(Path directoryPath, String remoteURL, String sshPassword)
            throws GitAPIException, IOException, CancelledAuthorizationException {
        super(directoryPath, remoteURL, sshPassword);
    }

    /**
     * Clones the repository into the desired folder and returns
     * the JGit Repository object.
     *
     * @return the RepoHelper's associated Repository object.
     * @throws GitAPIException if the `git clone` call fails.
     */
    @Override
    protected Repository obtainRepository() throws GitAPIException, CancelledAuthorizationException {
        CloneCommand cloneCommand = Git.cloneRepository();
        cloneCommand.setURI(this.remoteURL);
        myWrapAuthentication(cloneCommand);
        File destination = this.localPath.toFile();
        cloneCommand.setDirectory(destination);
        Git cloneCall = cloneCommand.call();

        cloneCall.close();
        SessionModel.getSessionModel().setAuthPref(destination.toString(), protocol);
        return cloneCall.getRepository();
    }

}
