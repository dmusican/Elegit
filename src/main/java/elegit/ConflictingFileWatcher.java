package elegit;

import javafx.concurrent.Task;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
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

    /**
     * returns a list of the files that were conflicting and then recently modified
     * @return ArrayList<String>
     */
    public static ArrayList<String> getConflictingThenModifiedFiles() {
        return conflictingThenModifiedFiles;
    }

    /**
     * Spins off a new thread to watch the directories that contain conflicting files
     *
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
                Set<String> newConflictingFiles = (new Git(currentRepo.getRepo()).status().call()).getConflicting();
                for(String newFile : newConflictingFiles) {
                    if(!conflictingFiles.contains(newFile)) {
                        conflictingFiles.add(newFile);
                    }
                }
                // removes files that aren't conflicting anymore from conflictingThenModifiedFiles
                for(String marked : conflictingThenModifiedFiles) {
                    if(!conflictingFiles.contains(marked)) {
                        conflictingThenModifiedFiles.remove(marked);
                    }
                }

                // gets the path to the repo directory
                Path directory = (new File(currentRepo.getRepo().getDirectory().getParent())).toPath();

                // for each conflicting file, watch its parent directory
                for(String fileToWatch : conflictingFiles) {
                    Path fileToWatchPath = directory.resolve((new File(fileToWatch)).toPath()).getParent();
                    watch(fileToWatchPath, fileToWatch);
                }
                return null;
            }

            /**
             * Spins off a new thread to watch each directory
             * @param directoryToWatch Path
             * @throws IOException
             */
            private void watch(Path directoryToWatch, String fileToWatch) throws IOException {
                Thread watch = new Thread(new Task<Void>() {
                    @Override
                    protected Void call() throws IOException {
                        // creates a WatchService
                        WatchService watcher = FileSystems.getDefault().newWatchService();
                        WatchKey key = directoryToWatch.register(watcher, StandardWatchEventKinds.ENTRY_MODIFY);

                        // while the file is conflicting, check to see if its been modified
                        while(conflictingFiles.contains(fileToWatch)) {
                            List<WatchEvent<?>> events = key.pollEvents();
                            for(WatchEvent<?> event : events) {

                                // if a conflicting file was modified, remove it from conflictingFiles and add it to conflictingThenModifiedFiles
                                String path = event.context().toString();
                                Path tmp = (new File(fileToWatch)).toPath();
                                // the path in conflictingFiles is either the file name itself or a path that ends with the file name
                                if(tmp.endsWith(path) || tmp.toString().equals(path)) {
                                    conflictingFiles.remove(tmp.toString());
                                    conflictingThenModifiedFiles.add(tmp.toString());
                                }
                            }
                            boolean valid = key.reset();
                            if(!valid) {
                                break;
                            }
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
        watcherThread.setName("initializing a watcher");
        watcherThread.start();
    }
}
