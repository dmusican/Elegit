package elegit;

import elegit.exceptions.CancelledAuthorizationException;
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

    // No authentication
    public ClonedRepoHelper(Path directoryPath, String remoteURL) throws IOException, GitAPIException, CancelledAuthorizationException{
        super(directoryPath);
        protocol = AuthMethod.HTTP;
        repo = obtainRepository(remoteURL);
        setup();
    }

    // Authentication via username/password combo
    public ClonedRepoHelper(Path directoryPath, String remoteURL, UsernamePasswordCredentialsProvider ownerAuth)
            throws GitAPIException, IOException, CancelledAuthorizationException {
        super(directoryPath, ownerAuth);
        protocol = AuthMethod.HTTPS;
        repo = obtainRepository(remoteURL);
        setup();
    }

    // Authentication via SSH password; no username since thats encoded in the URL
    public ClonedRepoHelper(Path directoryPath, String remoteURL, String sshPassword)
            throws GitAPIException, IOException, CancelledAuthorizationException {
        super(directoryPath, sshPassword);
        protocol = AuthMethod.SSHPASSWORD;
        repo = obtainRepository(remoteURL);
        setup();
    }

    /**
     * Clones the repository into the desired folder and returns
     * the JGit Repository object.
     *
     * @return the RepoHelper's associated Repository object.
     * @throws GitAPIException if the `git clone` call fails.
     */
    protected Repository obtainRepository(String remoteURL) throws GitAPIException, CancelledAuthorizationException {
        CloneCommand cloneCommand = Git.cloneRepository();
        cloneCommand.setURI(remoteURL);
        myWrapAuthentication(cloneCommand);
        File destination = this.localPath.toFile();
        cloneCommand.setDirectory(destination);
        Git cloneCall = cloneCommand.call();

        cloneCall.close();
        SessionModel.getSessionModel().setAuthPref(destination.toString(), protocol);
        return cloneCall.getRepository();
    }

}
