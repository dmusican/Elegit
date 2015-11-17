package elegit;

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
    public static final long REMOTE_CHECK_INTERVAL = 6000;
    public static final long LOCAL_CHECK_INTERVAL = 5000;

    // Whether there are new remote changes
    public static BooleanProperty hasFoundNewRemoteChanges = new SimpleBooleanProperty(false);

    // Whether to ignore any new changes
    private static boolean ignoreNewRemoteChanges = false;

    private static boolean pauseLocalMonitor = false;

    private static int pauseCounter = 0;

    // Thread information
    private static Thread th;
    private static boolean interrupted = false;

    /**
     * Associates the given model with this monitor. Updating the current
     * repository will cause the monitor to stop watching the old repository
     * and begin watching the new one
     * @param model the model to pull the repositories from
     */
    public static void beginWatchingRemote(SessionModel model){
        model.currentRepoHelperProperty.addListener((observable, oldValue, newValue) -> watchRepoForRemoteChanges(newValue));
        watchRepoForRemoteChanges(model.getCurrentRepoHelper());
    }

    /**
     * Creates a low priority thread that will monitor the remote repository
     * and compare it to the locally stored copy of the remote. When new changes
     * are detected in the remote, sets hasFoundNewRemoteChanges to true (if not ignoring
     * changes)
     * @param repo the repository to monitor
     */
    private static synchronized void watchRepoForRemoteChanges(RepoHelper repo){
        pause();
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

        if(repo == null || !repo.exists() || !repo.hasRemoteProperty.get()) {
            unpause();
            return;
        }

        th = new Thread(() -> {

            while(!interrupted){
                try{
                    List<BranchHelper> localOriginHeads = repo.getRemoteBranches();
                    Collection<Ref> remoteHeads = repo.getRefsFromRemote(false);

                    if(localOriginHeads.size() >= remoteHeads.size()){
                        for(Ref ref : remoteHeads){
                            boolean hasFoundMatchingBranch = false;
                            boolean hasFoundNewChanges = false;

                            for(BranchHelper branch : localOriginHeads){
                                if(ref.getName().equals("refs/heads/"+branch.getBranchName())){
                                    hasFoundMatchingBranch = true;
                                    if(!branch.getHeadId().equals(ref.getObjectId())){
                                        hasFoundNewChanges = true;
                                    }
                                    break;
                                }
                            }

                            if(hasFoundNewChanges || !hasFoundMatchingBranch){
                                setFoundNewChanges();
                                break;
                            }
                        }
                    }else{
                        setFoundNewChanges();
                    }
                }catch(GitAPIException | IOException ignored){}

                try{
                    Thread.sleep(REMOTE_CHECK_INTERVAL);
                }catch(InterruptedException e){
                    interrupted = true;
                }
            }
        });

        resetFoundNewChanges(REMOTE_CHECK_INTERVAL * 2);

        th.setDaemon(true);
        th.setName("Remote monitor for repository \"" + repo + "\"");
        th.setPriority(2);
        th.start();

        unpause();
    }

    /**
     * Sets hasFoundNewRemoteChanges to true if not ignoring new changes
     */
    private static void setFoundNewChanges(){
        if(!ignoreNewRemoteChanges) hasFoundNewRemoteChanges.set(true);
    }

    /**
     * Sets hasFoundNewRemoteChanges to false. If ignore is true, ignores
     * new changes indefinitely, else ignores them for a short grace
     * period (2 check cycles) and then begins monitoring again
     * @param ignore whether to ignore new changes indefinitely
     */
    public static synchronized void resetFoundNewChanges(boolean ignore){
        if(ignore){
            resetFoundNewChanges(-1);
        }else{
            resetFoundNewChanges(REMOTE_CHECK_INTERVAL * 2);
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
    public static synchronized void resetFoundNewChanges(long millis){
        hasFoundNewRemoteChanges.set(false);
        pauseWatchingRemote(millis);
    }

    public static synchronized void beginWatchingLocal(SessionController controller, SessionModel model){
        Thread thread = new Thread(() -> {
            while(true){
                if(!pauseLocalMonitor && model.getCurrentRepoHelper() != null
                        && model.getCurrentRepoHelper().exists() && !controller.workingTreePanelView.isAnyFileSelectedProperty.get()){
                    controller.gitStatus();
                }

                try{
                    Thread.sleep(LOCAL_CHECK_INTERVAL);
                }catch(InterruptedException ignored){}
            }
        });
        thread.setDaemon(true);
        thread.setName("Local monitor");
        thread.setPriority(2);
        thread.start();
    }

    private static void pauseWatchingRemote(long millis){
        ignoreNewRemoteChanges = true;

        if(millis < 0) return;

        Thread waitThread = new Thread(() -> {
            try{
                Thread.sleep(millis);
            }catch(InterruptedException ignored){
            }finally{
                ignoreNewRemoteChanges = false;
            }
        });

        waitThread.setDaemon(true);
        waitThread.setName("Remote monitor ignore timer");
        waitThread.setPriority(2);
        waitThread.start();
    }

    private static void pauseWatchingLocal(long millis){
        pauseLocalMonitor = true;

        if(millis < 0) return;

        Thread waitThread = new Thread(() -> {
            try{
                Thread.sleep(millis);
            }catch(InterruptedException ignored){
            }finally{
                pauseLocalMonitor = false;
            }
        });

        waitThread.setDaemon(true);
        waitThread.setName("Local monitor pause timer");
        waitThread.setPriority(2);
        waitThread.start();
    }

    public static synchronized void pause(long millis){
        pauseWatchingLocal(millis);
        pauseWatchingRemote(millis);
    }

    public static synchronized void pause(){
        if(pauseCounter == 0) pause(-1);
        pauseCounter++;
    }

    public static synchronized void unpause(){
        pauseCounter--;
        if(pauseCounter == 0) pause(0);
    }
}
