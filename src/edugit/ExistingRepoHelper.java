package edugit;

import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.IOException;
import java.nio.file.Path;

/**
 * A RepoHelper for pre-existing repositories
 */
public class ExistingRepoHelper extends RepoHelper {
    public ExistingRepoHelper(Path directoryPath, String ownerToken) throws Exception {
        super(directoryPath, ownerToken);
    }

    @Override
    protected Repository obtainRepository() {
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        try {
            return builder.findGitDir(this.localPath.toFile())
                    .readEnvironment()
                    .build();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
