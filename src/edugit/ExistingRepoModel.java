package edugit;

import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.File;
import java.io.IOException;

/**
 * Created by grahamearley on 6/9/15.
 */
public class ExistingRepoModel extends AbstractRepoModel {

    public ExistingRepoModel(File directoryPath, String ownerToken) throws Exception {
        super(directoryPath, ownerToken);
    }

    @Override
    protected Repository obtainRepository() {
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        try {
            return builder.findGitDir(this.localPath)
                    .readEnvironment()
                    .build();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
