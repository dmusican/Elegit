package edugit;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
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

    public static SessionModel getSessionModel() throws GitAPIException {
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

    private DirectoryRepoFile populateDirectoryRepoFile(DirectoryRepoFile superDirectory) throws GitAPIException {
        // Get the directories and subdirectories
        Set<String> modifiedFiles = getModifiedFiles();
        Set<String> missingFiles = getMissingFiles();
        Set<String> untrackedFiles = getUntrackedFiles();

        System.out.println(missingFiles.toString());

        try {
            DirectoryStream<Path> directoryStream = Files.newDirectoryStream(superDirectory.getFilePath());
            for (Path path : directoryStream) {
                if (Files.isDirectory(path)) {
                    // Recurse!
                    DirectoryRepoFile subdirectory = new DirectoryRepoFile(path, superDirectory.getRepo());
                    populateDirectoryRepoFile(subdirectory);
                    superDirectory.addChildFile(subdirectory);
                }
                else { // So it's a file, not a directory.
                    // Relativize the path to the repository, because that's the file structure JGit
                    //  looks for in an 'add' command
                    Path repoDirectory = this.sessionModel.getCurrentRepo().getWorkTree().toPath();
                    Path relativizedPath = repoDirectory.relativize(path);

                    if (modifiedFiles.contains(relativizedPath.toString())) {
                        ModifiedRepoFile modifiedFile = new ModifiedRepoFile(path, this.getCurrentRepo());
                        superDirectory.addChildFile(modifiedFile);
                    } else if (missingFiles.contains(relativizedPath.toString())) {
                        MissingRepoFile missingFile = new MissingRepoFile(path, this.getCurrentRepo());
                        superDirectory.addChildFile(missingFile);
                    } else if (untrackedFiles.contains(relativizedPath.toString())) {
                        UntrackedRepoFile untrackedFile = new UntrackedRepoFile(path, this.getCurrentRepo());
                        superDirectory.addChildFile(untrackedFile);
                    } else {
                        RepoFile boringRepoFile = new RepoFile(path, this.getCurrentRepo());
                        superDirectory.addChildFile(boringRepoFile);
                    }
                }
            }
            directoryStream.close(); // Have to close this to prevent overflow!
        } catch (IOException ex) {}

        return superDirectory;
    }

    public DirectoryRepoFile getParentDirectoryRepoFile() throws GitAPIException {
        Path fullPath = this.currentRepoHelper.localPath;

        //FIXME: what should the pathString be for this file?
        // NOTE: well, this works, so maybe it's correct...
        DirectoryRepoFile parentDirectoryRepoFile = new DirectoryRepoFile(fullPath, this.getCurrentRepo());
        parentDirectoryRepoFile = this.populateDirectoryRepoFile(parentDirectoryRepoFile);

        return parentDirectoryRepoFile;
    }

}
