package elegit;

import javafx.concurrent.Task;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;
import java.util.Set;

/**
 * Created by connellyj on 6/29/16.
 *
 * Class used to watch a conflictingRepoFile to see if it's been modified
 * after the user has been informed that the file was conflicting
 */

public class ConflictingFileWatcher {

    // list of files that were modified after the user was informed they were conflicting
    private static final ArrayList<String> conflictingThenModifiedFiles = new ArrayList<>();

    /**
     * returns a list of the files that were conflicting and then recently modified
     * @return ArrayList<String>
     */
    public static ArrayList<String> getConflictingThenModifiedFiles() {
        return conflictingThenModifiedFiles;
    }

    /**
     * removes a file from the list of files that were modified after conflicting
     * @param file String to remove from list
     */
    public static void removeFile(String file) {
        conflictingThenModifiedFiles.remove(file);
    }

    /**
     * Spins off a new thread to watch the directories that contain conflicting files
     * @param currentRepo RepoHelper
     * @throws GitAPIException
     * @throws IOException
     */
    public static void watchConflictingFiles(RepoHelper currentRepo) throws GitAPIException, IOException {

        if(currentRepo == null) return;

        Thread watcherThread = new Thread(new Task<Void>() {

            @Override
            protected Void call() throws IOException, GitAPIException {
                // gets the conflicting files
                Set<String> conflictingFiles = (new Git(currentRepo.getRepo()).status().call()).getConflicting();

                // gets the path to the repo directory
                Path directory = (new File(currentRepo.getRepo().getDirectory().getParent())).toPath();

                // creates a WatchService
                WatchService watcher = FileSystems.getDefault().newWatchService();

                // a list of keys to represent the directories the service is watching
                ArrayList<WatchKey> keys = new ArrayList<>();

                // a list of paths that are already being watched
                ArrayList<Path> alreadyWatching = new ArrayList<>();

                // for each conflicting file, add a watcher to its parent directory if it's not already being watched
                for(String fileToWatch : conflictingFiles) {
                    Path fileToWatchPath = directory.resolve((new File(fileToWatch)).toPath().getParent());
                    if(!alreadyWatching.contains(fileToWatchPath)) {
                        alreadyWatching.add(fileToWatchPath);
                        System.out.println(directory.resolve(fileToWatchPath));
                        WatchKey key = fileToWatchPath.register(watcher, StandardWatchEventKinds.ENTRY_MODIFY);
                        keys.add(key);
                    }
                }

                // while there are conflicting files, check each key to see if a file was modified
                while(conflictingFiles.size() > 0) {
                    for(WatchKey key : keys) {
                        List<WatchEvent<?>> events = key.pollEvents();
                        for(WatchEvent<?> event : events) {
                            if (event.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {

                                // if a conflicting file was modified, remove it from conflictingFiles and add it to conflictingThenModifiedFiles
                                Path path = (new File(event.context().toString())).toPath();
                                if(conflictingFiles.contains(path.toString())) {
                                    conflictingFiles.remove(path.toString());
                                    synchronized (conflictingThenModifiedFiles) {
                                        conflictingThenModifiedFiles.add(path.toString());
                                    }
                                }
                            }
                        }
                    }
                }
                return null;
            }
        });

        watcherThread.setDaemon(true);
        watcherThread.setName("watcherThread");
        watcherThread.start();
    }
}
