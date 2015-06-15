package edugit;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;

/**
 * Created by grahamearley on 6/12/15.
 */
public class TrackedRepoFile extends RepoFile {
    public TrackedRepoFile(String filePathString, Repository repo) {
        super(filePathString, repo);
    }

    @Override
    public void updateFileStatusInRepo() throws GitAPIException {
        System.out.printf("File already tracked: %s", this.toString());
    }
}
