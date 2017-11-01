package elegit.models;

import elegit.Main;
import elegit.monitors.ConflictingFileWatcher;
import elegit.repofile.*;
import elegit.sshauthentication.ElegitUserInfoGUI;
import elegit.treefx.CommitTreeModel;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.subjects.PublishSubject;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import elegit.exceptions.CancelledAuthorizationException;
import elegit.exceptions.MissingRepoException;
import org.apache.http.annotation.GuardedBy;
import org.apache.http.annotation.ThreadSafe;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;

import javax.xml.bind.DatatypeConverter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.stream.Stream;

/**
 * The singleton SessionModel stores all the Repos (contained in RepoHelper objects)
 * in the session and lets the user switch between repos.
 */
@ThreadSafe
// at least with regards to memory. Should be thought through with respect to working tree git operations.
public class SessionModel {

    // Keys for preferences recall
    private static final String RECENT_REPOS_LIST_KEY = "RECENT_REPOS_LIST";
    public static final String LAST_OPENED_REPO_PATH_KEY = "LAST_OPENED_REPO_PATH";
    private static final String LAST_UUID_KEY="LAST_UUID";
    @GuardedBy("this") private final List<RepoHelper> allRepoHelpers;
    private static final Logger logger = LogManager.getLogger();
    private final AtomicReference<RepoHelper> currentRepoHelper = new AtomicReference<>();
    //private final ObjectProperty<RepoHelper> currentRepoHelperProperty = new SimpleObjectProperty<>();
    private static final AtomicInteger constructorCount = new AtomicInteger();
    private final static AtomicReference<SessionModel> sessionModel = new AtomicReference<>();
    private final Preferences preferences;
    private final PublishSubject<RepoHelper> openedRepos = PublishSubject.create();
    private static Class<?> preferencesNodeClass = SessionModel.class;

    /**
     * @return the SessionModel object
     */
    // synchronized is critical for sessionModel, to make sure that it is updated as one operation.
    // compareAndSet would be briefer, but dramatically slower.
    public synchronized static SessionModel getSessionModel() {
        if (sessionModel.get() == null) {
            sessionModel.set(new SessionModel());
        }
        return sessionModel.get();
    }


    /**
     * Private constructor for the SessionModel singleton
     */
    private SessionModel() {
        this.allRepoHelpers = new ArrayList<>();
        this.preferences = Preferences.userNodeForPackage(preferencesNodeClass);
        loadRecentRepoHelpersFromStoredPathStrings();
        loadMostRecentRepoHelper();
    }


    /**
     * Indicate that unit testing is being run, so that things can be set up differently if need be
     */
    public static void setPreferencesNodeClass(Class<?> preferencesNodeClass) {
        SessionModel.preferencesNodeClass = preferencesNodeClass;
    }

    /**
     * Loads all recently loaded repositories (stored with the Java Preferences API)
     * into the recent repos menubar.
     */
    // synchronized for allRepoHelpers
    public synchronized void loadRecentRepoHelpersFromStoredPathStrings() {
        try{
            @SuppressWarnings("unchecked")
            ArrayList<String> storedRepoPathStrings = (ArrayList<String>) PrefObj.getObject(this.preferences, RECENT_REPOS_LIST_KEY);
            if (storedRepoPathStrings != null) {
                for (String pathString : storedRepoPathStrings) {
                    Path path = Paths.get(pathString);
                    try {
                        ExistingRepoHelper existingRepoHelper = new ExistingRepoHelper(path, new ElegitUserInfoGUI());
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
     * Loads the repository (from its RepoHelper) that was open when the app was
     * last closed. If this repo has been moved or deleted, it doesn't load anything.
     *
     * Uses the Java Preferences API (wrapped in IBM's PrefObj class) to load the repo.
     */
    public void loadMostRecentRepoHelper() {
        try{
            String lastOpenedRepoPathString = (String) PrefObj.getObject(
                    preferences, LAST_OPENED_REPO_PATH_KEY
            );
            if (lastOpenedRepoPathString != null) {
                Path path = Paths.get(lastOpenedRepoPathString);
                try {
                    ExistingRepoHelper existingRepoHelper =
                            new ExistingRepoHelper(path, new ElegitUserInfoGUI());
                    openRepoFromHelper(existingRepoHelper);
                    return;
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
            List<RepoHelper> allRepoHelpers = getAllRepoHelpers();
            if (allRepoHelpers.size()>0) {
                RepoHelper helper = allRepoHelpers.get(0);
                try {
                    openRepoFromHelper(helper);
                } catch (MissingRepoException e) {
                    logger.error("Missing repo exception");
                    e.printStackTrace();
                }
            }
        }catch(IOException | BackingStoreException | ClassNotFoundException e){
            logger.error("Some sort of error loading most recent repo helper");
            logger.debug(e.getStackTrace());
            e.printStackTrace();
        }
    }

    /**
     * Opens the given repository
     * Note that this runs directly in the FX thread when being done at startup, but within a separate thread
     * when a different repo is being opened.
     *
     * @param repoHelper the repository to open
     */
    // synchronized for allRepoHelpers, and openedRepos
    private synchronized void openRepo(RepoHelper repoHelper) throws BackingStoreException, IOException, ClassNotFoundException {
        if(!this.allRepoHelpers.contains(repoHelper)) {
            this.allRepoHelpers.add(repoHelper);
        }
        this.currentRepoHelper.set(repoHelper);
        this.saveListOfRepoPathStrings();
        this.saveMostRecentRepoPathString();
        CommitTreeModel.getCommitTreeModel().getTreeGraph().treeGraphModel.resetLayoutAtLeastOnce();

        openedRepos.onNext(this.currentRepoHelper.get());
    }

    public synchronized void subscribeToOpenedRepos(Consumer<RepoHelper> consumer) {
        openedRepos.subscribe(consumer);
    }

    /**
     * Loads a RepoHelper by checking to see if that RepoHelper's directory is already
     * loaded into the Model. If it is already loaded, this method will load that RepoHelper.
     * If not, this method will add the new RepoHelper and then load it.
     * Note that this runs directly in the FX thread when being done at startup, but within a separate thread
     * when a different repo is being opened.
     * TODO: Make sure this is appropriately synchronized
     *
     * @param repoHelperToLoad the RepoHelper to be loaded.
     */
    // synchronized for allRepoHelpers
    public synchronized void openRepoFromHelper(RepoHelper repoHelperToLoad) throws BackingStoreException, IOException, ClassNotFoundException, MissingRepoException {
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
     * Note that this runs directly in the FX thread when being done at startup, but within a separate thread
     * when a different repo is being opened.

     * @param repoHelperCandidate the repoHelper being checked
     * @return the repo helper that points to the same repository as the candidate
     *          (by directory), or null if there is no such RepoHelper already in the model.
     */
    // synchronized for allRepoHelpers
    private synchronized RepoHelper matchRepoWithAlreadyLoadedRepo(RepoHelper repoHelperCandidate) {
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
        return currentRepoHelper.get();
    }

    /**
     * @return the current JGit repository associated with the current RepoHelper
     */
    private Repository getCurrentRepo() {
        return this.getCurrentRepoHelper().getRepo();
    }

    /**
     * Gets a list of all repositories held in this session. Repositories
     * that no longer exist are removed (and not returned)
     * @return a list of all existing repositories held in the session
     */
    // synchronized for allRepoHelpers
    public synchronized List<RepoHelper> getAllRepoHelpers() {
        List<RepoHelper> tempList = new ArrayList<>(allRepoHelpers);
        for(RepoHelper r : tempList){
            if(!r.exists()){
                allRepoHelpers.remove(r);
            }
        }
        return Collections.unmodifiableList(allRepoHelpers);
    }

    /**
     * Calls `git status` and returns the set of untracked files that Git reports.
     *
     * @return a set of untracked filenames in the working directory.
     * @throws GitAPIException if the `git status` call fails.
     */
    private Set<String> getUntrackedFiles(Status status) throws GitAPIException {
        if(status == null) {
            status = new Git(this.getCurrentRepo()).status().call();
        }

        return Collections.unmodifiableSet(status.getUntracked());
    }

    /**
     * Calls `git status` and returns the set of untracked files that Git reports.
     *
     * @return a set of untracked filenames in the working directory.
     * @throws GitAPIException if the `git status` call fails.
     */
    private Set<String> getIgnoredFiles(Status status) throws GitAPIException {
        if(status == null) {
            status = new Git(this.getCurrentRepo()).status().call();
        }

        return Collections.unmodifiableSet(status.getIgnoredNotInIndex());
    }

    /**
     * Calls `git status` and returns the set of conflicting files that Git reports.
     *
     * @return a set of conflicting filenames in the working directory.
     * @throws GitAPIException
     */
    private Set<String> getConflictingFiles(Status status) throws GitAPIException {
        if (status == null) {
            status = new Git(this.getCurrentRepo()).status().call();
        }

        return Collections.unmodifiableSet(status.getConflicting());
    }

    /**
     * Calls `git status` and returns the set of missing files that Git reports.
     *
     * @return a set of missing filenames in the working directory.
     * @throws GitAPIException if the `git status` call fails.
     */
    private Set<String> getMissingFiles(Status status) throws GitAPIException {
        if(status == null) {
            status = new Git(this.getCurrentRepo()).status().call();
        }

        return Collections.unmodifiableSet(status.getMissing());
    }

    /**
     * Calls `git status` and returns the set of modified files that Git reports.
     *
     * Modified files differ between the disk and the index
     *
     * @return a set of modified filenames in the working directory.
     * @throws GitAPIException if the `git status` call fails.
     */
    private Set<String> getModifiedFiles(Status status) throws GitAPIException {
        if(status == null) {
            status = new Git(this.getCurrentRepo()).status().call();
        }

        return Collections.unmodifiableSet(status.getModified());
    }


    /**
     * Calls `git status` and returns the set of staged files that Git reports.
     *
     * @return a set of modified filenames in the working directory.
     * @throws GitAPIException if the `git status` call fails.
     */
    private Set<String> getStagedFiles(Status status) throws GitAPIException {
        if(status == null) {
            status = new Git(this.getCurrentRepo()).status().call();
        }
        Set<String> stagedFiles = ConcurrentHashMap.newKeySet();
        stagedFiles.addAll(status.getChanged());
        stagedFiles.addAll(status.getAdded());
        return Collections.unmodifiableSet(stagedFiles);
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
        Set<String> stagedFiles = getStagedFiles(status);
        Set<String> conflictingThenModifiedFiles = ConflictingFileWatcher.getConflictingThenModifiedFiles();

        List<RepoFile> changedRepoFiles = new ArrayList<>();

        ArrayList<String> conflictingRepoFileStrings = new ArrayList<>();

        for (String conflictingFileString : conflictingFiles) {
            // If a file is conflicting but was also recently modified, make it a ConflictingThenModifiedRepoFile instead
            if(conflictingThenModifiedFiles.contains(conflictingFileString)) {
                ConflictingThenModifiedRepoFile conflictingThenModifiedRepoFile = new ConflictingThenModifiedRepoFile(conflictingFileString, this.getCurrentRepoHelper());
                changedRepoFiles.add(conflictingThenModifiedRepoFile);
            }else {
                ConflictingRepoFile conflictingRepoFile = new ConflictingRepoFile(conflictingFileString, this.getCurrentRepoHelper());
                changedRepoFiles.add(conflictingRepoFile);
            }
            // Store these paths to make sure this file isn't registered as a modified file or something.
            //  If it's conflicting, the app should focus only on the conflicting state of the
            //  file first.
            //
            // e.g. If a modification causes a conflict, that file should have its conflicts resolved
            //      before it gets added.
            conflictingRepoFileStrings.add(conflictingFileString);
        }

        // Remove files from conflictingThenModifedFiles when they are no longer conflicting

        // We have to record these to avoid a ConcurrentModificationException
        List<String> toRemove = new ArrayList<>();
        for (String str : conflictingThenModifiedFiles) {
            if(!conflictingFiles.contains(str)) {
                toRemove.add(str);
            }
        }
        for (String str : toRemove) {
            ConflictingFileWatcher.removeFile(str);
        }
        for (String stagedFileString : stagedFiles) {
            if (!conflictingRepoFileStrings.contains(stagedFileString) && !modifiedFiles.contains(stagedFileString)) {
                StagedRepoFile stagedRepoFile = new StagedRepoFile(stagedFileString, this.getCurrentRepoHelper());
                changedRepoFiles.add(stagedRepoFile);
            } else if (!conflictingRepoFileStrings.contains(stagedFileString)) {
                StagedAndModifiedRepoFile stagedAndModifiedRepoFile = new StagedAndModifiedRepoFile(stagedFileString, this.getCurrentRepoHelper());
                changedRepoFiles.add(stagedAndModifiedRepoFile);
            }
        }
        for (String modifiedFileString : modifiedFiles) {
            if (!conflictingRepoFileStrings.contains(modifiedFileString) && !stagedFiles.contains(modifiedFileString)) {
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
        return Collections.unmodifiableList(changedRepoFiles);
    }
    /**
     * Assembles all files in the repository's folder into RepoFiles
     * and returns a list of them.
     *
     * @return a list of changed files, contained in RepoFile objects.
     * @throws GitAPIException if the `git status` calls fail.
     */
    public List<RepoFile> getAllRepoFiles() throws GitAPIException, IOException {
        List<RepoFile> allFiles = new ArrayList<>(getAllChangedRepoFiles());

        Status status = new Git(this.getCurrentRepo()).status().call();

        for(String ignoredFileString : getIgnoredFiles(status)){
            IgnoredRepoFile ignoredRepoFile = new IgnoredRepoFile(ignoredFileString, this.getCurrentRepoHelper());
            allFiles.add(ignoredRepoFile);
        }

        List<Path> allPaths = new LinkedList<>();

        try (Stream<Path> paths = Files.walk(getCurrentRepoHelper().getLocalPath())
                .filter(path -> !path.toString().contains(File.separator + ".git" + File.separator)
                        && !path.equals(getCurrentRepoHelper().getLocalPath())
                        && !path.endsWith(".git"))) {
            paths.forEach(allPaths::add);
        }

        Set<Path> addedPaths = new HashSet<>();
        for(RepoFile file : allFiles){
            addedPaths.add(file.getFilePath());
        }

        for(Path path : allPaths){
            path = getCurrentRepoHelper().getLocalPath().relativize(path);
            if(!addedPaths.contains(path)){
                RepoFile temp;
                if(path.toFile().isDirectory()){
                    temp = new DirectoryRepoFile(path, getCurrentRepoHelper());
                }else{
                    temp = new RepoFile(path, getCurrentRepoHelper());
                }
                allFiles.add(temp);
                addedPaths.add(path);
            }
        }

        Collections.sort(allFiles);
        return Collections.unmodifiableList(allFiles);
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
    // synchronized for allRepoHelpers
    private synchronized void saveListOfRepoPathStrings() throws BackingStoreException, IOException, ClassNotFoundException {
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
        String pathString = this.getCurrentRepoHelper().getLocalPath().toString();

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
    private void clearStoredPreferences() throws BackingStoreException, IOException, ClassNotFoundException {
        PrefObj.putObject(this.preferences, RECENT_REPOS_LIST_KEY, null);
        PrefObj.putObject(this.preferences, LAST_OPENED_REPO_PATH_KEY, null);
        PrefObj.putObject(this.preferences, LAST_UUID_KEY, null);
    }

    public void setAuthPref(String pathname, AuthMethod authTechnique) {
        Preferences authPrefs = preferences.node("authentication");
        authPrefs.putInt(hashPathname(pathname), authTechnique.getEnumValue());
    }

    public AuthMethod getAuthPref(String pathname)  {
        Preferences authPrefs = preferences.node("authentication");
        int enumValue = authPrefs.getInt(hashPathname(pathname), -1);
        if (enumValue == -1)
            throw new NoSuchElementException("AuthPref not present");

        return AuthMethod.getEnumFromValue(enumValue);
    }

    public void removeAuthPref(String pathname) {
        Preferences authPrefs = preferences.node("authentication");
        authPrefs.remove(hashPathname(pathname));
    }

    public List<String> listAuthPaths() {
        Preferences authPrefs = preferences.node("authentication");
        try {
            return Collections.unmodifiableList(Arrays.asList(authPrefs.keys()));
        } catch (BackingStoreException e) {
            throw new RuntimeException(e);
        }
    }

    // Preferences API has a limit of 80 characters max, and some pathnames
    // may be longer than that. Hashing it will solve that problem.
    public String hashPathname(String pathname) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        md.update(pathname.getBytes());
        String prefKey;
        //try {
            //prefKey = new String(md.digest(), "US-ASCII");
            prefKey = DatatypeConverter.printHexBinary(md.digest());
            //prefKey = "hello";
//        } catch (UnsupportedEncodingException e) {
//            throw new RuntimeException(e);
//        }
        return prefKey;
    }

    // synchronized for allRepoHelpers
    public synchronized void removeRepoHelpers(List<RepoHelper> checkedItems) {
        this.allRepoHelpers.removeAll(checkedItems);
    }

    /**
     * After the last RepoHelper is closed by user, sessionModel needs to be
     * updated and reflect the new view.
     */
    // synchronized is critical for sessionModel, to make sure that it is updated as one operation.
    // compareAndSet would be briefer, but dramatically slower.
    public synchronized void resetSessionModel() {
        try {
            clearStoredPreferences();
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        sessionModel.set(new SessionModel());
    }



}
