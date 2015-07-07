package main.java.elegit;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

/**
 * A class that creates a thread to watch the current remote repository for new changes
 */
public class RepositoryMonitor{

    // How long to pause between checks
    public static final long CHECK_INTERVAL = 5000;

    // Whether there are new remote changes
    public static BooleanProperty hasFoundNewChanges = new SimpleBooleanProperty(false);

    // Wheteher to ignore any new changes
    private static boolean ignoreNewChanges = false;

    // Thread information
    private static Thread th;
    private static boolean interrupted = false;

    /**
     * Associates the given model with this monitor. Updating the current
     * repository will cause the monitor to stop watching the old repository
     * and begin watching the new one
     * @param model the model to pull the repositories from
     */
    public static void beginWatching(SessionModel model){
        model.currentRepoHelperProperty.addListener((observable, oldValue, newValue) -> watchRepoForRemoteChanges(newValue));
        watchRepoForRemoteChanges(model.getCurrentRepoHelper());
    }

    /**
     * Creates a low priority thread that will monitor the remote repository
     * and compare it to the locally stored copy of the remote. When new changes
     * are detected in the remote, sets hasFoundNewChanges to true (if not ignoring
     * changes)
     * @param repoHelper the repository to monitor
     */
    private static synchronized void watchRepoForRemoteChanges(RepoHelper repoHelper){
        if(th != null){
            interrupted = true;
            try{
                th.join();
            }catch(InterruptedException ignored){
            }finally{
                interrupted = false;
                th = null;
            }
        }

        if(repoHelper == null || !repoHelper.hasRemoteProperty.get()) return;

        th = new Thread(() -> {

            while(!interrupted){
                try{
                    List<BranchHelper> localOriginHeads = repoHelper.getRemoteBranches();
                    Collection<Ref> remoteHeads = repoHelper.getRefsFromRemote(false);

                    if(localOriginHeads.size() == remoteHeads.size()){
                        for(BranchHelper branch : localOriginHeads){
                            boolean hasFoundMatchingBranch = false;
                            boolean hasFoundNewChanges2 = false;

                            for(Ref ref : remoteHeads){
                                if(ref.getName().equals("refs/heads/"+branch.getBranchName())){
                                    hasFoundMatchingBranch = true;
                                    if(!branch.getHeadID().equals(ref.getObjectId())){
                                        hasFoundNewChanges2 = true;
                                        break;
                                    }
                                }
                            }

                            if(hasFoundNewChanges2 || !hasFoundMatchingBranch){
                                setFoundNewChanges();
                                break;
                            }
                        }
                    }else{
                        setFoundNewChanges();
                    }
                }catch(GitAPIException | IOException ignored){}

                try{
                    Thread.sleep(CHECK_INTERVAL);
                }catch(InterruptedException e){
                    interrupted = true;
                }
            }
        });

        th.setDaemon(true);
        th.setName("Remote monitor for repository \"" + repoHelper + "\"");
        th.setPriority(2);
        th.start();
    }

    /**
     * Sets hasFoundNewChanges to true if not ignoring new changes
     */
    private static void setFoundNewChanges(){
        if(!ignoreNewChanges) hasFoundNewChanges.set(true);
    }

    /**
     * Sets hasFoundNewChanges to false. If ignore is true, ignores
     * new changes indefinitely, else ignores them for a short grace
     * period (1 check cycle) and then begins monitoring again
     * @param ignore whether to ignore new changes indefinitely
     */
    public static void resetFoundNewChanges(boolean ignore){
        if(ignore){
            resetFoundNewChanges(-1);
        }else{
            resetFoundNewChanges(CHECK_INTERVAL);
        }
    }

    /**
     * Sets hasFoundNewChangess to false and then ignores any
     * new changes for the given amount of time (in milliseconds).
     * Passing in a negative value tells the monitor to ignore
     * new changes for an indefinite amount of time
     * @param millis the amount of time (in milliseconds) to ignore
     *               new changes. A negative value indicates an
     *               indefinite wait.
     */
    public static void resetFoundNewChanges(long millis){
        hasFoundNewChanges.set(false);
        ignoreNewChanges = true;

        if(millis < 0) return;

        Thread waitThread = new Thread(() -> {
            try{
                Thread.sleep(millis);
            }catch(InterruptedException ignored){
            }finally{
                ignoreNewChanges = false;
            }
        });

        waitThread.setDaemon(true);
        waitThread.setName("Remote monitor ignore timer");
        waitThread.setPriority(2);
        waitThread.start();
    }
}
