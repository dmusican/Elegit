package edugit;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Set;

/**
 * The singleton SessionModel stores all the Repos (contained in RepoHelper objects)
 * in the session and lets the user switch between repos.
 */
public class SessionModel {

    RepoHelper currentRepoHelper;

    ArrayList<RepoHelper> allRepoHelpers; // for when we support multiple repositories!
    private static SessionModel sessionModel;
    private RepoOwner owner;

    public static SessionModel getSessionModel() {
        if (sessionModel == null) {
            sessionModel = new SessionModel();
            sessionModel.owner = new RepoOwner();
            sessionModel.owner.presentLoginDialogsToSetValues();
        }
        return sessionModel;
    }

    private SessionModel() {
        this.allRepoHelpers = new ArrayList<RepoHelper>();
    }

    /**
     * Adds a new repository (contained in a RepoHelper) to the session.
     *
     * @param anotherRepoHelper the RepoHelper to add.
     */
    public void addRepo(RepoHelper anotherRepoHelper) {
        this.allRepoHelpers.add(anotherRepoHelper);
    }

    /**
     * Opens a repository stored at a certain index in the list of
     * RepoHelpers.
     *
     * @param index the index of the repository to open.
     */
    public void openRepoAtIndex(int index) {
        this.currentRepoHelper = this.allRepoHelpers.get(index);
    }

    /**
     * Adds a new RepoHelper to the session and then opens it.
     *
     * @param repoHelperToLoad the RepoHelper to be loaded.
     */
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

    /**
     * Calls `git status` and returns the set of untracked files that Git reports.
     *
     * @return a set of untracked files in the working directory.
     * @throws GitAPIException if the `git status` call fails.
     */
    public Set<String> getUntrackedFiles() throws GitAPIException {
        Status status = new Git(this.getCurrentRepo()).status().call();
        Set<String> untrackedFiles = status.getUntracked();

        return untrackedFiles;
    }

    public Set<String> getConflictingFiles() throws GitAPIException {
        Status status = new Git(this.getCurrentRepo()).status().call();
        Set<String> conflictingFiles = status.getConflicting();

        return conflictingFiles;
    }

    /**
     * Calls `git status` and returns the set of missing files that Git reports.
     *
     * @return a set of missing files in the working directory.
     * @throws GitAPIException if the `git status` call fails.
     */
    public Set<String> getMissingFiles() throws GitAPIException {
        Status status = new Git(this.getCurrentRepo()).status().call();
        Set<String> missingFiles = status.getMissing();

        return missingFiles;
    }

    /**
     * Calls `git status` and returns the set of modified files that Git reports.
     *
     * @return a set of modified files in the working directory.
     * @throws GitAPIException if the `git status` call fails.
     */
    public Set<String> getModifiedFiles() throws GitAPIException {
        Status status = new Git(this.getCurrentRepo()).status().call();
        Set<String> modifiedFiles = status.getModified();

        return modifiedFiles;
    }

    /**
     * Get (construct) the current repo's working directory DirectoryRepoFile
     * by creating and populating a new DirectoryRepoFile from the repository's
     * parent directory.
     *
     * @return the populated DirectoryRepoFile for the current repository's parent directory.
     * @throws GitAPIException if the call to `populateDirectoryRepoFile(...)` fails.
     * @throws IOException if the call to `populateDirectoryRepoFile(...)` fails.
     */
    public DirectoryRepoFile getParentDirectoryRepoFile() throws GitAPIException, IOException {
        Path fullPath = this.currentRepoHelper.getLocalPath();

        // FIXME? what should the pathString be for this file?
        // well, this works, so maybe it's correct...
        DirectoryRepoFile parentDirectoryRepoFile = new DirectoryRepoFile(fullPath, this.getCurrentRepo());
        parentDirectoryRepoFile = this.populateDirectoryRepoFile(parentDirectoryRepoFile);

        return parentDirectoryRepoFile;
    }

    /**
     * Adds all the children files contained within a directory to that directory's DirectoryRepoFile.
     *
     * @param superDirectory the RepoFile of the directory to be populated.
     * @return the populated RepoFile of the initially passed-in directory.
     * @throws GitAPIException if the `git status` methods' calls to Git fail (for getting
     * @throws IOException if the DirectoryStream fails.
     */
    private DirectoryRepoFile populateDirectoryRepoFile(DirectoryRepoFile superDirectory) throws GitAPIException, IOException {
        try {
            DirectoryStream<Path> directoryStream = Files.newDirectoryStream(superDirectory.getFilePath());
            for (Path path : directoryStream) {
                if (path.equals(this.getCurrentRepo().getDirectory().toPath())) {

                    // If the path is the Repository's .git folder, don't populate it.

                } else if (Files.isDirectory(path)) {
                    // Recurse! Populate the directory.
                    DirectoryRepoFile subdirectory = new DirectoryRepoFile(path, superDirectory.getRepo());
                    populateDirectoryRepoFile(subdirectory);
                    superDirectory.addChild(subdirectory);

                } else {
                    // So, this is a file and not a directory.

                    Set<String> modifiedFiles = getModifiedFiles();
                    Set<String> missingFiles = getMissingFiles();
                    Set<String> untrackedFiles = getUntrackedFiles();
                    Set<String> conflictingFiles = getConflictingFiles();

                    // Relativize the path to the repository, because that's the file structure JGit
                    //  looks for in an 'add' command
                    Path repoDirectory = this.getCurrentRepo().getWorkTree().toPath();
                    Path relativizedPath = repoDirectory.relativize(path);

                    // Determine what type of RepoFile we're dealing with.
                    //  Is it modified? Untracked/new? Missing? Just a plain file?
                    //  Construct the appropriate RepoFile and add it to the parent directory.
                    if (modifiedFiles.contains(relativizedPath.toString())) {
                        ModifiedRepoFile modifiedFile = new ModifiedRepoFile(path, this.getCurrentRepo());
                        superDirectory.addChild(modifiedFile);
                    } else if (missingFiles.contains(relativizedPath.toString())) {
                        MissingRepoFile missingFile = new MissingRepoFile(path, this.getCurrentRepo());
                        superDirectory.addChild(missingFile);
                    } else if (untrackedFiles.contains(relativizedPath.toString())) {
                        UntrackedRepoFile untrackedFile = new UntrackedRepoFile(path, this.getCurrentRepo());
                        superDirectory.addChild(untrackedFile);
                    } else if (conflictingFiles.contains(relativizedPath.toString())) {
                        ConflictingRepoFile conflictingFile = new ConflictingRepoFile(path, this.getCurrentRepo());
                        superDirectory.addChild(conflictingFile);
                    } else {
                        RepoFile plainRepoFile = new RepoFile(path, this.getCurrentRepo());
                        superDirectory.addChild(plainRepoFile);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println(e.getStackTrace());
        }
        return superDirectory;
    }

    /**
     * Assembles all the changed files (modified, missing, untracked) into RepoFiles
     * and returns a list of them.
     *
     * @return a list of changed files, contained in RepoFile objects.
     * @throws GitAPIException if the `git status` calls fail.
     */
    public ArrayList<RepoFile> getAllChangedRepoFiles() throws GitAPIException {
        Set<String> modifiedFiles = getModifiedFiles();
        Set<String> missingFiles = getMissingFiles();
        Set<String> untrackedFiles = getUntrackedFiles();
        Set<String> conflictingFiles = getConflictingFiles();

        ArrayList<RepoFile> changedRepoFiles = new ArrayList<>();

        for (String modifiedFileString : modifiedFiles) {
            ModifiedRepoFile modifiedRepoFile = new ModifiedRepoFile(modifiedFileString, this.getCurrentRepo());
            changedRepoFiles.add(modifiedRepoFile);
        }

        for (String missingFileString : missingFiles) {
            MissingRepoFile missingRepoFile = new MissingRepoFile(missingFileString, this.getCurrentRepo());
            changedRepoFiles.add(missingRepoFile);
        }

        for (String untrackedFileString : untrackedFiles) {
            UntrackedRepoFile untrackedRepoFile = new UntrackedRepoFile(untrackedFileString, this.getCurrentRepo());
            changedRepoFiles.add(untrackedRepoFile);
        }

        for (String conflictingFileString : conflictingFiles) {
            ConflictingRepoFile conflictingRepoFile = new ConflictingRepoFile(conflictingFileString, this.getCurrentRepo());
            changedRepoFiles.add(conflictingRepoFile);
        }

        return changedRepoFiles;
    }

    public RepoHelper getCurrentRepoHelper() {
        return currentRepoHelper;
    }

    public RepoOwner getOwner() {
        return owner;
    }

    public void setOwner(RepoOwner owner) {
        this.owner = owner;
    }

    public ArrayList<RepoHelper> getAllRepoHelpers() {
        return allRepoHelpers;
    }
}
