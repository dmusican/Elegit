package elegit;

import com.jcraft.jsch.UserInfo;
import elegit.exceptions.CancelledAuthorizationException;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;

/**
 * A RepoHelper implementation for a repository cloned into an empty folder.
 */
public class ClonedRepoHelper extends RepoHelper {

    // Authentication via username/password combo
    public ClonedRepoHelper(Path directoryPath, String remoteURL, UsernamePasswordCredentialsProvider ownerAuth)
            throws GitAPIException, IOException, CancelledAuthorizationException {
        super(directoryPath, ownerAuth);
        repo = obtainRepository(remoteURL);
        setup();
    }

    // Authentication via SSH password; no username since thats encoded in the URL
    public ClonedRepoHelper(Path directoryPath, String remoteURL, String sshPassword)
            throws GitAPIException, IOException, CancelledAuthorizationException {
        super(directoryPath, sshPassword);
        repo = obtainRepository(remoteURL);
        setup();
    }

    // Constructor specifically designed for unit testing; file containing credentials passed in
    public ClonedRepoHelper(Path directoryPath, String remoteURL, File credentialsFile)
            throws GitAPIException, IOException, CancelledAuthorizationException {
        super(directoryPath, credentialsFile);
        repo = obtainRepository(remoteURL);
        setup();
    }

    public ClonedRepoHelper(Path directoryPath, String remoteURL, UserInfo userInfo)
            throws GitAPIException, IOException, CancelledAuthorizationException {
        super(directoryPath, userInfo);
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
        return cloneCall.getRepository();
    }

}
