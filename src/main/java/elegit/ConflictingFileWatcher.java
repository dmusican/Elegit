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
    private static final ArrayList<String> conflictingThenModifiedFiles = new ArrayList<>();
    private static ArrayList<String> conflictingFiles = new ArrayList<>();

    /**
     * returns a list of the files that were conflicting and then recently modified
     * @return ArrayList<String>
     */
    public static ArrayList<String> getConflictingThenModifiedFiles() {
        return conflictingThenModifiedFiles;
    }

    public static void removeFile(String file) {
        conflictingThenModifiedFiles.remove(file);
    }

    public static void watchConflictingFiles(RepoHelper currentRepo) throws GitAPIException, IOException {
        Thread watcherThread = new Thread(new Task<Void>() {
            @Override
            protected Void call() throws IOException, GitAPIException {
                Set<String> newConflictingFiles = (new Git(currentRepo.getRepo()).status().call()).getConflicting();
                for(String conflictingFile : newConflictingFiles) {
                    if(!conflictingFiles.contains(conflictingFile)) {
                        conflictingFiles.add(conflictingFile);
                    }
                }

                Path directory = (new File(currentRepo.getRepo().getDirectory().getParent())).toPath();
                WatchService watcher = FileSystems.getDefault().newWatchService();

                ArrayList<WatchKey> keys = new ArrayList<>();
                ArrayList<Path> alreadyWatching = new ArrayList<>();
                for(String fileToWatch : conflictingFiles) {
                    Path fileToWatchPath = directory.resolve((new File(fileToWatch)).toPath());
                    if(!alreadyWatching.contains(fileToWatchPath)) {
                        alreadyWatching.add(fileToWatchPath);
                        System.out.println(directory.resolve(fileToWatchPath));
                        WatchKey key = fileToWatchPath.register(watcher, StandardWatchEventKinds.ENTRY_MODIFY);
                        keys.add(key);
                    }
                }

                while(conflictingFiles.size() > 0) {
                    for(WatchKey key : keys) {
                        List<WatchEvent<?>> events = key.pollEvents();
                        for(WatchEvent<?> event : events) {
                            if (event.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {
                                Path path = (new File(event.context().toString())).toPath();
                                if(conflictingFiles.contains(path.toString())) {
                                    conflictingFiles.remove(path.toString());
                                    synchronized (conflictingThenModifiedFiles) {
                                        conflictingThenModifiedFiles.add(path.toString());
                                    }
                                    key.cancel();
                                    keys.remove(key);
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
