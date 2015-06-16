package edugit;

import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.lib.Repository;

import java.nio.file.Path;

/**
 * A RepoHelper implementation for a repository cloned into an empty folder.
 */
public class ClonedRepoHelper extends RepoHelper {
    public ClonedRepoHelper(Path directoryPath, String remoteURL, RepoOwner owner) throws Exception {
        super(directoryPath, remoteURL, owner);
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
        // TODO: make this not just clone a dummy repo...

        CloneCommand cloneCommand = Git.cloneRepository();
        cloneCommand.setURI(this.remoteURL);
        cloneCommand.setCredentialsProvider(this.ownerAuth);

        cloneCommand.setDirectory(this.localPath.toFile());
        Git cloneCall = null;

        cloneCall = cloneCommand.call();

        cloneCall.close();
        return cloneCall.getRepository();
    }
}
