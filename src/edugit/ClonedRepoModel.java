package edugit;

import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;

import java.io.File;

/**
 * Created by grahamearley on 6/9/15.
 */
public class ClonedRepoModel extends AbstractRepoModel {

    String destinationFolderName = "";

    public ClonedRepoModel(File directoryPath, String ownerToken) throws Exception {
        super(directoryPath, ownerToken);
    }

    public void setDestinationFolderName(String destinationFolderName) {
        // The destination folder is the name of the new folder you want
        //  to store the cloned repo.
        this.destinationFolderName = destinationFolderName;
    }

    @Override
    protected Repository obtainRepository() {
        // TODO: make this not just clone a dummy repo...

        File destinationDirectory = new File(this.localPath.toString(), this.destinationFolderName);

        CloneCommand cloneCommand = Git.cloneRepository();
        cloneCommand.setURI(this.remoteURL);
        cloneCommand.setCredentialsProvider(this.ownerAuth);

        cloneCommand.setDirectory(destinationDirectory);
        Git cloneCall = null;

        try {
            cloneCall = cloneCommand.call();
        } catch (GitAPIException e) {
            e.printStackTrace();
            // TODO: better error handling
        }

        cloneCall.close();
        return cloneCall.getRepository();
    }
}
