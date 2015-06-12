package edugit;

import javafx.scene.control.CheckBoxTreeItem;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Set;

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

    public Repository getCurrentRepo() {
        return this.currentRepoHelper.getRepo();
    }

    public Set<String> getUntrackedFiles() throws GitAPIException {
        Status status = new Git(this.getCurrentRepo()).status().call();
        Set<String> untrackedFiles = status.getUntracked();

        return untrackedFiles;
    }

    public Set<String> getMissingFiles() throws GitAPIException {
        Status status = new Git(this.getCurrentRepo()).status().call();
        Set<String> missingFiles = status.getMissing();

        return missingFiles;
    }

    public Set<String> getModifiedFiles() throws GitAPIException {
        Status status = new Git(this.getCurrentRepo()).status().call();
        Set<String> modifiedFiles = status.getModified();

        return modifiedFiles;
    }

}
