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
    private static ArrayList<String> conflictingThenModifiedFiles = new ArrayList<>();
    private static ArrayList<String> conflictingFiles = new ArrayList<>();

    // boolean to help deal with concurrency issues
    private static boolean watching = false;

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
        if(!watching) {
            conflictingThenModifiedFiles.remove(file);
        }
    }

    /**
     * Spins off a new thread to watch the directories that contain conflicting files
     * @param currentRepo RepoHelper
     * @throws GitAPIException
     * @throws IOException
     */
    public static void watchConflictingFiles(RepoHelper currentRepo) throws GitAPIException, IOException {

        if(currentRepo == null) return;

        watching = true;

        Thread watcherThread = new Thread(new Task<Void>() {

            @Override
            protected Void call() throws IOException, GitAPIException {
                // gets the conflicting files
                Set<String> newConflictingFiles = (new Git(currentRepo.getRepo()).status().call()).getConflicting();
                for(String newFile : newConflictingFiles) {
                    if(!conflictingFiles.contains(newFile)) {
                        conflictingFiles.add(newFile);
                    }
                }

                // gets the path to the repo directory
                Path directory = (new File(currentRepo.getRepo().getDirectory().getParent())).toPath();

                // a list of paths that are already being watched
                ArrayList<Path> alreadyWatching = new ArrayList<>();

                // for each conflicting file, add a watcher to its parent directory if it's not already being watched
                for(String fileToWatch : conflictingFiles) {
                    Path fileToWatchPath = directory.resolve((new File(fileToWatch)).toPath()).getParent();
                    if(!alreadyWatching.contains(fileToWatchPath)) {
                        alreadyWatching.add(fileToWatchPath);
                        watch(fileToWatchPath);
                    }
                }
                watching = false;
                return null;
            }

            private void watch(Path directoryToWatch) throws IOException {
                Thread watch = new Thread(new Task<Void>() {
                    @Override
                    protected Void call() throws IOException {
                        // creates a WatchService
                        WatchService watcher = FileSystems.getDefault().newWatchService();
                        WatchKey key = directoryToWatch.register(watcher, StandardWatchEventKinds.ENTRY_MODIFY);

                        // while there are conflicting files, check each key to see if a file was modified
                        while(conflictingFiles.size() > 0) {
                            List<WatchEvent<?>> events = key.pollEvents();
                            for(WatchEvent<?> event : events) {

                                // if a conflicting file was modified, remove it from conflictingFiles and add it to conflictingThenModifiedFiles
                                String path = event.context().toString();
                                for(String str : conflictingFiles) {
                                    Path tmp = (new File(str)).toPath();
                                    // the path in conflictingFiles is either the file name itself or a path that ends with the file name
                                    if(tmp.endsWith(path) || tmp.toString().equals(path)) {
                                        conflictingFiles.remove(tmp.toString());
                                        conflictingThenModifiedFiles.add(tmp.toString());
                                    }
                                }
                            }
                            key.reset();
                        }
                        return null;
                    }
                });
                watch.setDaemon(true);
                watch.setName("watching a directory");
                watch.start();
            }
        });

        watcherThread.setDaemon(true);
        watcherThread.setName("watcherThread");
        watcherThread.start();
    }
}
