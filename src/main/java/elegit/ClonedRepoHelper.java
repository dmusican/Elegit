package elegit;

import com.jcraft.jsch.UserInfo;
import elegit.exceptions.CancelledAuthorizationException;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

/**
 * A RepoHelper implementation for a repository cloned into an empty folder.
 */
public class ClonedRepoHelper extends RepoHelper {

    // Authentication via username/password combo
    public ClonedRepoHelper(Path directoryPath, String remoteURL, UsernamePasswordCredentialsProvider ownerAuth)
            throws GitAPIException, IOException, CancelledAuthorizationException {
        super(directoryPath, ownerAuth);
    }

    // Authentication via SSH password; no username since thats encoded in the URL
    public ClonedRepoHelper(Path directoryPath, String remoteURL, String sshPassword)
            throws GitAPIException, IOException, CancelledAuthorizationException {
        super(directoryPath, sshPassword);
    }

    public ClonedRepoHelper(Path directoryPath, String remoteURL, String sshPassword, UserInfo userInfo)
            throws GitAPIException, IOException, CancelledAuthorizationException {
        super(directoryPath, sshPassword, userInfo);
    }

    public ClonedRepoHelper(Path directoryPath, String sshPassword)
            throws GitAPIException, IOException, CancelledAuthorizationException {
        super(directoryPath, sshPassword);
    }

    /**
     * Clones the repository into the desired folder and returns
     * the JGit Repository object.
     *
     * @throws GitAPIException if the `git clone` call fails.
     */
    protected void obtainRepository(String remoteURL) throws GitAPIException, IOException,
            CancelledAuthorizationException {
        CloneCommand cloneCommand = Git.cloneRepository();
        cloneCommand.setURI(remoteURL);
        myWrapAuthentication(cloneCommand);
        File destination = this.localPath.toFile();
        cloneCommand.setDirectory(destination);
        Git cloneCall = cloneCommand.call();

        cloneCall.close();
        repo = cloneCall.getRepository();
        setup();
    }

}
