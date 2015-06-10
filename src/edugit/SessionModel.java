package edugit;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by grahamearley on 6/10/15.
 */
public class SessionModel {

    RepoHelper openRepoHelper;
    ArrayList<RepoHelper> allRepoHelpers;

    public SessionModel(RepoHelper repoHelper) {
        this.openRepoHelper = repoHelper;
        this.allRepoHelpers.add(repoHelper);
    }

    public File[] getFilesInCurrentDirectory() {
        return this.openRepoHelper.getDirectory().listFiles();
    }

    public void addRepo(RepoHelper anotherRepoHelper) {
        this.allRepoHelpers.add(anotherRepoHelper);
    }

    public void openRepoAtIndex(int index) {
        this.openRepoHelper = this.allRepoHelpers.get(index);
    }

}
