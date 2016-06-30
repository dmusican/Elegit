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
 *
 * http://docs.oracle.com/javase/tutorial/essential/io/notification.html#overview
 *
 * https://docs.oracle.com/javase/7/docs/api/java/nio/file/WatchService.html
 */

public class ConflictingFileWatcher {
    private static ArrayList<String> conflictingThenModifiedFiles = new ArrayList<>();
    private static ArrayList<String> conflictingFiles = new ArrayList<>();

    @SuppressWarnings("unchecked")
    static WatchEvent<Path> cast(WatchEvent<?> event) {
        return (WatchEvent<Path>)event;
    }

    public static ArrayList<String> getConflictingThenModifiedFiles() {
        return conflictingThenModifiedFiles;
    }

    public static void watchConflictingFiles(RepoHelper currentRepo) throws GitAPIException, IOException {
        Thread watcherThread = new Thread(new Task<Void>() {
            @Override
            protected Void call() throws IOException, GitAPIException {
                File pathName = new File(currentRepo.getRepo().getDirectory().getParent());
                Path directory = pathName.toPath();
                WatchService watcher = FileSystems.getDefault().newWatchService();
                WatchKey key = directory.register(watcher, StandardWatchEventKinds.ENTRY_MODIFY);

                Set<String> newConflictingFiles = (new Git(currentRepo.getRepo()).status().call()).getConflicting();
                for(String filePath : newConflictingFiles) {
                    if(!conflictingFiles.contains(filePath)) {
                        conflictingFiles.add(filePath);
                    }
                }

                while(conflictingFiles.size() > 0) {
                    List<WatchEvent<?>> events = key.pollEvents();
                    for(WatchEvent<?> event : events) {
                        if (event.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {
                            Path path = (new File(event.context().toString())).toPath();
                            if(conflictingFiles.contains(path.toString())) {
                                conflictingFiles.remove(path.toString());
                                conflictingThenModifiedFiles.add(path.toString());
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
