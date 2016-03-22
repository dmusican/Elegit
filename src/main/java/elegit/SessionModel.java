package main.java.elegit;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import main.java.elegit.exceptions.CancelledAuthorizationException;
import main.java.elegit.exceptions.MissingRepoException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.stream.Stream;

/**
 * The singleton SessionModel stores all the Repos (contained in RepoHelper objects)
 * in the session and lets the user switch between repos.
 */
public class SessionModel {

    // Keys for preferences recall
    private static final String RECENT_REPOS_LIST_KEY = "RECENT_REPOS_LIST";
    private static final String LAST_OPENED_REPO_PATH_KEY = "LAST_OPENED_REPO_PATH";

    private RepoHelper currentRepoHelper;
    public ObjectProperty<RepoHelper> currentRepoHelperProperty;

    List<RepoHelper> allRepoHelpers;
    private static SessionModel sessionModel;

    private String defaultUsername;

    Preferences preferences;

    static final Logger logger = LogManager.getLogger();

    static enum AuthMethod { HTTP, HTTPS, SSHPASSWORD };

    /**
     * @return the SessionModel object
     */
    public static SessionModel getSessionModel() {
        if (sessionModel == null) {
            sessionModel = new SessionModel();
        }
        return sessionModel;
    }

    /**
     * Private constructor for the SessionModel singleton
     */
    private SessionModel() {
        this.allRepoHelpers = new ArrayList<>();
        this.preferences = Preferences.userNodeForPackage(this.getClass());
        currentRepoHelperProperty = new SimpleObjectProperty<>(currentRepoHelper);
    }

    /**
     * Loads the repository (from its RepoHelper) that was open when the app was
     * last closed. If this repo has been moved or deleted, it doesn't load anything.
     *
     * Uses the Java Preferences API (wrapped in IBM's PrefObj class) to load the repo.
     */
    public void loadMostRecentRepoHelper() {
        try{
            String lastOpenedRepoPathString = (String) PrefObj.getObject(
                    this.preferences, LAST_OPENED_REPO_PATH_KEY
            );
            if (lastOpenedRepoPathString != null) {
                Path path = Paths.get(lastOpenedRepoPathString);
                try {
                    ExistingRepoHelper existingRepoHelper =
                            new ExistingRepoHelper(path, this.defaultUsername);
                    this.openRepoFromHelper(existingRepoHelper);
                } catch (IllegalArgumentException e) {
                    logger.warn("Recent repo not found in directory it used to be in");
                    // The most recent repo is no longer in the directory it used to be in,
                    // so just don't load it.
                }catch(GitAPIException | MissingRepoException e) {
                    logger.error("Git error or missing repo exception");
                    logger.debug(e.getStackTrace());
                    e.printStackTrace();
                } catch (CancelledAuthorizationException e) {
                // Should never be used, as no authorization is needed for loading local files.
                }
            }
        }catch(IOException | BackingStoreException | ClassNotFoundException e){
            logger.error("Some sort of error loading most recent repo helper");
            logger.debug(e.getStackTrace());
            e.printStackTrace();
        }
    }

    /**
     * Loads all recently loaded repositories (stored with the Java Preferences API)
     * into the recent repos menubar.
     */
    public void loadRecentRepoHelpersFromStoredPathStrings() {
        try{
            ArrayList<String> storedRepoPathStrings = (ArrayList<String>) PrefObj.getObject(this.preferences, RECENT_REPOS_LIST_KEY);
            if (storedRepoPathStrings != null) {
                for (String pathString : storedRepoPathStrings) {
                    System.out.println(pathString);
                    Path path = Paths.get(pathString);
                    try {
                        ExistingRepoHelper existingRepoHelper = new ExistingRepoHelper(path, this.defaultUsername);
                        this.allRepoHelpers.add(existingRepoHelper);
                    } catch (IllegalArgumentException e) {
                        logger.warn("Repository has been moved, we move along");
                        // This happens when this repository has been moved.
                        // We'll just move along.
                    } catch(GitAPIException e){
                        logger.error("Git error loading recent repo helpers");
                        logger.debug(e.getStackTrace());
                        e.printStackTrace();
                    } catch (CancelledAuthorizationException e) {
                        // This shouldn't happen loading local files.
                    }
                }
            }
        } catch(IOException | ClassNotFoundException | BackingStoreException e){
            logger.error("Some sort of exception loading recent repo helpers");
            logger.debug(e.getStackTrace());
            e.printStackTrace();
        }
    }

    /**
     * Opens the given repository
     *
     * @param repoHelper the repository to open
     */
    public void openRepo(RepoHelper repoHelper) throws BackingStoreException, IOException, ClassNotFoundException {
        if(!this.allRepoHelpers.contains(repoHelper)) {
            this.allRepoHelpers.add(repoHelper);
        }
        this.currentRepoHelper = repoHelper;
        currentRepoHelperProperty.set(this.currentRepoHelper);
        this.saveListOfRepoPathStrings();
        this.saveMostRecentRepoPathString();
    }

    /**
     * Loads a RepoHelper by checking to see if that RepoHelper's directory is already
     * loaded into the Model. If it is already loaded, this method will load that RepoHelper.
     * If not, this method will add the new RepoHelper and then load it.
     *
     * @param repoHelperToLoad the RepoHelper to be loaded.
     */
    public void openRepoFromHelper(RepoHelper repoHelperToLoad) throws BackingStoreException, IOException, ClassNotFoundException, MissingRepoException {
        RepoHelper matchedRepoHelper = this.matchRepoWithAlreadyLoadedRepo(repoHelperToLoad);
        if (matchedRepoHelper == null) {
            // So, this repo isn't loaded into the model yet
            this.allRepoHelpers.add(repoHelperToLoad);
            this.openRepo(repoHelperToLoad);
        } else {
            // So, this repo is already loaded into the model
            if(matchedRepoHelper.exists()){
                this.openRepo(matchedRepoHelper);
            }else{
                this.allRepoHelpers.remove(matchedRepoHelper);
                throw new MissingRepoException();
            }
        }
    }

    /**
     * Checks if a repoHelper is already loaded in the model by comparing repository directories.
     *
     * @param repoHelperCandidate the repoHelper being checked
     * @return the repo helper that points to the same repository as the candidate
     *          (by directory), or null if there is no such RepoHelper already in the model.
     */
    private RepoHelper matchRepoWithAlreadyLoadedRepo(RepoHelper repoHelperCandidate) {
        if(repoHelperCandidate != null) {
            for (RepoHelper repoHelper : this.allRepoHelpers) {
                if (repoHelper.getLocalPath().equals(repoHelperCandidate.getLocalPath())) {
                    return repoHelper;
                }
            }
        }
        return null;
    }

    /**
     * @return the current repository
     */
    public RepoHelper getCurrentRepoHelper(){
        return currentRepoHelper;
    }

    /**
     * @return the current JGit repository associated with the current RepoHelper
     */
    public Repository getCurrentRepo() {
        return this.currentRepoHelper.getRepo();
    }

    /**
     * @return the default owner that will be assigned to new repositories
     */
    public String getDefaultUsername() {
        return this.defaultUsername;
    }

    public void setCurrentDefaultUsername(String username) {
        this.defaultUsername = username;
    }

    /**
     * Gets a list of all repositories held in this session. Repositories
     * that no longer exist are removed (and not returned)
     * @return a list of all existing repositories held in the session
     */
    public List<RepoHelper> getAllRepoHelpers() {
        List<RepoHelper> tempList = new ArrayList<>(allRepoHelpers);
        for(RepoHelper r : tempList){
            if(!r.exists()){
                allRepoHelpers.remove(r);
            }
        }
        return allRepoHelpers;
    }

    /**
     * Calls `git status` and returns the set of untracked files that Git reports.
     *
     * @return a set of untracked filenames in the working directory.
     * @throws GitAPIException if the `git status` call fails.
     */
    public Set<String> getUntrackedFiles(Status status) throws GitAPIException {
        if(status == null) {
            status = new Git(this.getCurrentRepo()).status().call();
        }

        return status.getUntracked();
    }

    /**
     * Calls `git status` and returns the set of untracked files that Git reports.
     *
     * @return a set of untracked filenames in the working directory.
     * @throws GitAPIException if the `git status` call fails.
     */
    public Set<String> getIgnoredFiles(Status status) throws GitAPIException {
        if(status == null) {
            status = new Git(this.getCurrentRepo()).status().call();
        }

        return status.getIgnoredNotInIndex();
    }

    /**
     * Calls `git status` and returns the set of conflicting files that Git reports.
     *
     * @return a set of conflicting filenames in the working directory.
     * @throws GitAPIException
     */
    public Set<String> getConflictingFiles(Status status) throws GitAPIException {
        if(status == null) {
            status = new Git(this.getCurrentRepo()).status().call();
        }

        return status.getConflicting();
    }

    /**
     * Calls `git status` and returns the set of missing files that Git reports.
     *
     * @return a set of missing filenames in the working directory.
     * @throws GitAPIException if the `git status` call fails.
     */
    public Set<String> getMissingFiles(Status status) throws GitAPIException {
        if(status == null) {
            status = new Git(this.getCurrentRepo()).status().call();
        }

        return status.getMissing();
    }

    /**
     * Calls `git status` and returns the set of modified files that Git reports.
     * Also returns those considered changed rather than modified. Changed files
     * appear for example when committing a fixed conflict while merging.
     *
     * Changed files differ between the HEAD and the index
     * Modified files differ between the disk and the index
     *
     * @return a set of modified filenames in the working directory.
     * @throws GitAPIException if the `git status` call fails.
     */
    public Set<String> getModifiedFiles(Status status) throws GitAPIException {
        if(status == null) {
            status = new Git(this.getCurrentRepo()).status().call();
        }

        Set<String> modifiedAndChangedFiles = new HashSet<>(status.getChanged());
        modifiedAndChangedFiles.addAll(status.getModified());

        return modifiedAndChangedFiles;
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

        DirectoryRepoFile parentDirectoryRepoFile = new DirectoryRepoFile(fullPath, this.getCurrentRepoHelper());
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

                    Status status = new Git(this.getCurrentRepo()).status().call();

                    Set<String> modifiedFiles = getModifiedFiles(status);
                    Set<String> missingFiles = getMissingFiles(status);
                    Set<String> untrackedFiles = getUntrackedFiles(status);
                    Set<String> conflictingFiles = getConflictingFiles(status);

                    // Relativize the path to the repository, because that's the file structure JGit
                    //  looks for in an 'add' command
                    Path repoDirectory = this.getCurrentRepo().getWorkTree().toPath();
                    Path relativizedPath = repoDirectory.relativize(path);

                    // Determine what type of RepoFile we're dealing with.
                    //  Is it modified? Untracked/new? Missing? Just a plain file?
                    //  Construct the appropriate RepoFile and add it to the parent directory.
                    if (conflictingFiles.contains(relativizedPath.toString())) {
                        ConflictingRepoFile conflictingFile = new ConflictingRepoFile(path, this.getCurrentRepoHelper());
                        superDirectory.addChild(conflictingFile);
                    } else if (modifiedFiles.contains(relativizedPath.toString())) {
                        ModifiedRepoFile modifiedFile = new ModifiedRepoFile(path, this.getCurrentRepoHelper());
                        superDirectory.addChild(modifiedFile);
                    } else if (missingFiles.contains(relativizedPath.toString())) {
                        MissingRepoFile missingFile = new MissingRepoFile(path, this.getCurrentRepoHelper());
                        superDirectory.addChild(missingFile);
                    } else if (untrackedFiles.contains(relativizedPath.toString())) {
                        UntrackedRepoFile untrackedFile = new UntrackedRepoFile(path, this.getCurrentRepoHelper());
                        superDirectory.addChild(untrackedFile);
                    } else {
                        RepoFile plainRepoFile = new RepoFile(path, this.getCurrentRepoHelper());
                        superDirectory.addChild(plainRepoFile);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Exception trying to populate directory repo file");
            logger.debug(e.getStackTrace());
            e.printStackTrace();
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
    public List<RepoFile> getAllChangedRepoFiles() throws GitAPIException {
        Status status = new Git(this.getCurrentRepo()).status().call();
        Set<String> modifiedFiles = getModifiedFiles(status);
        Set<String> missingFiles = getMissingFiles(status);
        Set<String> untrackedFiles = getUntrackedFiles(status);
        Set<String> conflictingFiles = getConflictingFiles(status);

        List<RepoFile> changedRepoFiles = new ArrayList<>();

        ArrayList<String> conflictingRepoFileStrings = new ArrayList<>();

        for (String conflictingFileString : conflictingFiles) {
            ConflictingRepoFile conflictingRepoFile = new ConflictingRepoFile(conflictingFileString, this.getCurrentRepoHelper());
            changedRepoFiles.add(conflictingRepoFile);

            // Store these paths to make sure this file isn't registered as a modified file or something.
            //  If it's conflicting, the app should focus only on the conflicting state of the
            //  file first.
            //
            // e.g. If a modification causes a conflict, that file should have its conflicts resolved
            //      before it gets added.
            conflictingRepoFileStrings.add(conflictingFileString);
        }

        for (String modifiedFileString : modifiedFiles) {
            if (!conflictingRepoFileStrings.contains(modifiedFileString)) {
                ModifiedRepoFile modifiedRepoFile = new ModifiedRepoFile(modifiedFileString, this.getCurrentRepoHelper());
                changedRepoFiles.add(modifiedRepoFile);
            }
        }

        for (String missingFileString : missingFiles) {
            if (!conflictingRepoFileStrings.contains(missingFileString)) {
                MissingRepoFile missingRepoFile = new MissingRepoFile(missingFileString, this.getCurrentRepoHelper());
                changedRepoFiles.add(missingRepoFile);
            }
        }

        for (String untrackedFileString : untrackedFiles) {
            if (!conflictingRepoFileStrings.contains(untrackedFileString)) {
                UntrackedRepoFile untrackedRepoFile = new UntrackedRepoFile(untrackedFileString, this.getCurrentRepoHelper());
                changedRepoFiles.add(untrackedRepoFile);
            }
        }

        Collections.sort(changedRepoFiles);
        return changedRepoFiles;
    }
    /**
     * Assembles all files in the repository's folder into RepoFiles
     * and returns a list of them.
     *
     * @return a list of changed files, contained in RepoFile objects.
     * @throws GitAPIException if the `git status` calls fail.
     */
    public List<RepoFile> getAllRepoFiles() throws GitAPIException, IOException {
        List<RepoFile> allFiles = getAllChangedRepoFiles();

        Status status = new Git(this.getCurrentRepo()).status().call();

        for(String ignoredFileString : getIgnoredFiles(status)){
            IgnoredRepoFile ignoredRepoFile = new IgnoredRepoFile(ignoredFileString, this.getCurrentRepoHelper());
            allFiles.add(ignoredRepoFile);
        }

        List<Path> allPaths = new LinkedList<>();

        try (Stream<Path> paths = Files.walk(currentRepoHelper.getLocalPath())
                .filter(path -> !path.toString().contains(File.separator + ".git" + File.separator)
                        && !path.equals(currentRepoHelper.getLocalPath())
                        && !path.endsWith(".git"))) {
            paths.forEach(allPaths::add);
        }

        Set<Path> addedPaths = new HashSet<>();
        for(RepoFile file : allFiles){
            addedPaths.add(file.getFilePath());
        }

        for(Path path : allPaths){
            path = currentRepoHelper.getLocalPath().relativize(path);
            if(!addedPaths.contains(path)){
                RepoFile temp;
                if(path.toFile().isDirectory()){
                    temp = new DirectoryRepoFile(path, currentRepoHelper);
                }else{
                    temp = new RepoFile(path, currentRepoHelper);
                }
                allFiles.add(temp);
                addedPaths.add(path);
            }
        }

        Collections.sort(allFiles);
        return allFiles;
    }

    /**
     * Saves the model's list of RepoHelpers using the Preferences API (and the PrefObj wrapper
     *  from IBM).
     *
     * We store these as a list of file strings instead of Paths
     *  because Paths aren't serializable.
     *
     * @throws BackingStoreException
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private void saveListOfRepoPathStrings() throws BackingStoreException, IOException, ClassNotFoundException {
        ArrayList<String> repoPathStrings = new ArrayList<>();
        for (RepoHelper repoHelper : this.allRepoHelpers) {
            Path path = repoHelper.getLocalPath();
            repoPathStrings.add(path.toString());
        }

        // Store the list object using IBM's PrefObj helper class:
        PrefObj.putObject(this.preferences, RECENT_REPOS_LIST_KEY, repoPathStrings);
    }

    /**
     * Saves the most recently opened repository to the Preferences API (to be
     *  re-opened next time the app is opened).
     *
     * @throws BackingStoreException
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private void saveMostRecentRepoPathString() throws BackingStoreException, IOException, ClassNotFoundException {
        String pathString = this.currentRepoHelper.getLocalPath().toString();

        PrefObj.putObject(this.preferences, LAST_OPENED_REPO_PATH_KEY, pathString);
    }

    /**
     * Clears the information stored by the Preferences API:
     *  recent repos and the last opened repo.
     *
     * @throws BackingStoreException
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public void clearStoredPreferences() throws BackingStoreException, IOException, ClassNotFoundException {
        PrefObj.putObject(this.preferences, RECENT_REPOS_LIST_KEY, null);
        PrefObj.putObject(this.preferences, LAST_OPENED_REPO_PATH_KEY, null);
    }

    public void setAuthPref(String pathname, int authTechnique) {
        Preferences authPrefs = preferences.node("authentication");
        authPrefs.putInt(pathname, authTechnique);
    }

    public int getAuthPref(String pathname) {
        Preferences authPrefs = preferences.node("authentication");
        return authPrefs.getInt(pathname, -1);
    }

    public void removeRepoHelpers(List<RepoHelper> checkedItems) {
        for (RepoHelper item : checkedItems) {
            this.allRepoHelpers.remove(item);
        }
    }

    /**
     * After the last RepoHelper is closed by user, sessionModel needs to be
     * updated and reflect the new view.
     */
    public void resetSessionModel() {
        try {
            clearStoredPreferences();
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        /*currentRepoHelper = null;
        currentRepoHelperProperty = new SimpleObjectProperty<>(null);*/
        sessionModel = new SessionModel();
    }
}
