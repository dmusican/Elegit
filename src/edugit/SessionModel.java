package edugit;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

/**
 * The Singleton SessionModel stores all the Repos in the session and lets the user
 * switch between repos.
 */
public class SessionModel {

    RepoHelper currentRepoHelper;
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
        // TODO: remove this horribly out-of-place line
        Path dirpath = Paths.get(System.getProperty("user.home") + "/Desktop/cl"); //this.sessionModel.currentRepoHelper.getDirectory().toString();
        try {
            openRepoFromHelper(new ExistingRepoHelper(dirpath, SECRET_CONSTANTS.TEST_GITHUB_TOKEN));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public File[] getFilesInCurrentDirectory() {
        return this.currentRepoHelper.getDirectory().toFile().listFiles();
    }

    public void addRepo(RepoHelper anotherRepoHelper) {
        this.allRepoHelpers.add(anotherRepoHelper);
    }

    public void openRepoAtIndex(int index) {
        this.currentRepoHelper = this.allRepoHelpers.get(index);
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
