package elegit.models;

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
 *
 * It is perhaps counterintuitive that the constructors don't take the URL of the remote. That's because the RepoHelper
 * object doesn't need to remember or know the remoteURL; it is stored in the Git repository itself on disk, and
 * JGit summons it whenever necessary to perform commands. The URL is only necessary when cloning the repo initially,
 * hence it is a parameter for obtainRepository.
 */
public class ClonedRepoHelper extends RepoHelper {

    public ClonedRepoHelper(Path directoryPath, UsernamePasswordCredentialsProvider ownerAuth)
            throws GitAPIException, IOException, CancelledAuthorizationException {
        super(directoryPath, ownerAuth);
    }

    public ClonedRepoHelper(Path directoryPath, String sshPassword, UserInfo userInfo,
                            String privateKeyFileLocation, String knownHostsFileLocation)
            throws GitAPIException, IOException, CancelledAuthorizationException {
        super(directoryPath, sshPassword, userInfo, privateKeyFileLocation, knownHostsFileLocation);
    }

    /**
     * Clones the repository into the desired folder and returns
     * the JGit Repository object.
     *
     * @throws GitAPIException if the `git clone` call fails.
     */
    public void obtainRepository(String remoteURL) throws GitAPIException, IOException,
            CancelledAuthorizationException {
        CloneCommand cloneCommand = Git.cloneRepository();
        cloneCommand.setURI(remoteURL);
        wrapAuthentication(cloneCommand);
        File destination = this.getLocalPath().toFile();
        cloneCommand.setDirectory(destination);
        System.out.println("ClonedRepoHelper.obtainRepository " + Thread.currentThread());
        Git cloneCall = cloneCommand.call();
        cloneCall.close();
            setRepo(cloneCall.getRepository());
        setup();
    }

}
