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
 */
public class ClonedRepoHelper extends RepoHelper {

    // Authentication via username/password combo
    public ClonedRepoHelper(Path directoryPath, String remoteURL, UsernamePasswordCredentialsProvider ownerAuth)
            throws GitAPIException, IOException, CancelledAuthorizationException {
        super(directoryPath, ownerAuth);
    }

    public ClonedRepoHelper(Path directoryPath, String remoteURL, String sshPassword, UserInfo userInfo)
            throws GitAPIException, IOException, CancelledAuthorizationException {
        super(directoryPath, sshPassword, userInfo);
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
        System.out.println("ClonedRepoHelper.obtainRepository");
        myWrapAuthentication(cloneCommand);
        File destination = this.getLocalPath().toFile();
        cloneCommand.setDirectory(destination);
        Git cloneCall = cloneCommand.call();

        cloneCall.close();
        setRepo(cloneCall.getRepository());
        setup();
    }

}
