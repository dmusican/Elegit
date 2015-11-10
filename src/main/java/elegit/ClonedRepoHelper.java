package main.java.elegit;

import main.java.elegit.exceptions.CancelledAuthorizationException;
import main.java.elegit.exceptions.NoOwnerInfoException;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;

import java.io.IOException;
import java.nio.file.Path;

/**
 * A RepoHelper implementation for a repository cloned into an empty folder.
 */
public class ClonedRepoHelper extends RepoHelper {
    public ClonedRepoHelper(Path directoryPath, String remoteURL, String username) throws IOException, NoOwnerInfoException, GitAPIException{
        super(directoryPath, remoteURL, username);
    }

    /**
     * Clones the repository into the desired folder and returns
     * the JGit Repository object.
     *
     * @return the RepoHelper's associated Repository object.
     * @throws GitAPIException if the `git clone` call fails.
     */
    @Override
    protected Repository obtainRepository() throws GitAPIException {
        CloneCommand cloneCommand = Git.cloneRepository();
        cloneCommand.setURI(this.remoteURL);
        try {
            cloneCommand.setCredentialsProvider(super.presentAuthorizeDialog());
        } catch (CancelledAuthorizationException e) {
            // If no authorization is given, then we don't bother setting the credentialsProvider
        }

        cloneCommand.setDirectory(this.localPath.toFile());
        Git cloneCall = cloneCommand.call();

        cloneCall.close();
        return cloneCall.getRepository();
    }
}
