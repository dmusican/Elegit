package main.java.elegit;

import main.java.elegit.exceptions.CancelledAuthorizationException;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.IOException;
import java.nio.file.Path;

/**
 * A RepoHelper implementation for a repository cloned into an empty folder.
 */
public class ClonedRepoHelper extends RepoHelper {
    public ClonedRepoHelper(Path directoryPath, String remoteURL, String username,
                            UsernamePasswordCredentialsProvider ownerAuth) throws IOException, GitAPIException, CancelledAuthorizationException{
        super(directoryPath, remoteURL, username, ownerAuth);
    }

    /**
     * Clones the repository into the desired folder and returns
     * the JGit Repository object.
     *
     * @return the RepoHelper's associated Repository object.
     * @throws GitAPIException if the `git clone` call fails.
     * @param ownerAuth
     */
    @Override
    protected Repository obtainRepository(UsernamePasswordCredentialsProvider ownerAuth) throws GitAPIException, CancelledAuthorizationException {
        CloneCommand cloneCommand = Git.cloneRepository();
        cloneCommand.setURI(this.remoteURL);

        //Will throw CancelledAuthorizationException if dialog is cancelled
        cloneCommand.setCredentialsProvider(super.presentAuthorizeDialog());

        cloneCommand.setDirectory(this.localPath.toFile());
        Git cloneCall = cloneCommand.call();

        cloneCall.close();
        return cloneCall.getRepository();
    }
}
