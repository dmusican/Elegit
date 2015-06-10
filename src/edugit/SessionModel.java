package edugit;

import java.io.File;
import java.util.ArrayList;

/**
 * The Singleton SessionModel stores all the Repos in the session and lets the user
 * switch between repos.
 */
public class SessionModel {

    RepoHelper openRepoHelper;
    ArrayList<RepoHelper> allRepoHelpers;
    private static SessionModel sessionModel;

    public static SessionModel getSessionModel() {
        if (sessionModel == null) {
            sessionModel = new SessionModel();
        }

        return sessionModel;
    }

    private SessionModel() {
        this.allRepoHelpers = new ArrayList<RepoHelper>();
    }

    public File[] getFilesInCurrentDirectory() {
        return this.openRepoHelper.getDirectory().toFile().listFiles();
    }

    public void addRepo(RepoHelper anotherRepoHelper) {
        this.allRepoHelpers.add(anotherRepoHelper);
    }

    public void openRepoAtIndex(int index) {
        this.openRepoHelper = this.allRepoHelpers.get(index);
    }

    public void openRepoFromHelper(RepoHelper repoHelperToLoad) {
        if (this.allRepoHelpers.contains(repoHelperToLoad)) {
            this.openRepoAtIndex(this.allRepoHelpers.indexOf(repoHelperToLoad));
        } else {
            this.allRepoHelpers.add(repoHelperToLoad);
            this.openRepoAtIndex(this.allRepoHelpers.size() - 1);
        }
    }

}
