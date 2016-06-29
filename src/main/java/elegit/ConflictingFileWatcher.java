package elegit;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.File;
import java.util.ArrayList;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by connellyj on 6/29/16.
 *
 * Class used to watch a conflictingRepoFile to see if it's been modified
 * after the user has been informed that the file was conflicting
 */

public class ConflictingFileWatcher extends TimerTask {
    private long timeStamp;
    private File file;
    private String filePath;
    private Timer timer;
    private static ArrayList<String> conflictingThenModifiedFiles = new ArrayList<>();
    private static ArrayList<String> conflictingFiles = new ArrayList<>();

    public ConflictingFileWatcher(String filePath) {
        this.filePath = filePath;
        file = new File(filePath);
        this.timeStamp = file.lastModified();
        timer = new Timer(true);
        timer.scheduleAtFixedRate(this, 0, 1000);
    }

    @Override
    public void run() {
        long tmp = file.lastModified();
        if (timeStamp != tmp) {
            conflictingThenModifiedFiles.add(filePath);
            conflictingFiles.remove(filePath);
            timer.cancel();
        }
    }

    public static ArrayList<String> getConflictingThenModifiedFiles() {
        return conflictingThenModifiedFiles;
    }

    public static void watchConflictingFiles(RepoHelper currentRepo) throws GitAPIException {
        Set<String> newConflictingFiles = (new Git(currentRepo.getRepo()).status().call()).getConflicting();
        for(String filePath : newConflictingFiles) {
            if(!conflictingFiles.contains(filePath)) {
                conflictingFiles.add(filePath);
                new ConflictingFileWatcher(filePath);
            }
        }
    }
}
