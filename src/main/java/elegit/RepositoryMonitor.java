package elegit;

import elegit.controllers.SessionController;
import elegit.models.BranchHelper;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A class that creates a thread to watch the current remote repository for new changes
 */
// TODO: Make sure threadsafe
public class RepositoryMonitor{

    // How long to pause between checks
    public static final long REMOTE_CHECK_INTERVAL = 6000;
    public static final long LOCAL_CHECK_INTERVAL = 5000;
    public static final int CHECK_INTERVAL = 1;

    // Whether there are new remote changes
    public static BooleanProperty hasFoundNewRemoteChanges = new SimpleBooleanProperty(false);

    // Whether to ignore any new changes
    private static boolean ignoreNewRemoteChanges = false;
    private static boolean pauseLocalMonitor = false;

    private static int pauseCounter = 0;
    private static AtomicInteger pauseCount = new AtomicInteger(0);

    // Thread information
    private static Thread th;
    private static boolean interrupted = false;

    private static SessionModel currentModel;

    private static boolean alreadyWatching = false;

    private static void setSessionModel(SessionModel model) {
        currentModel = model;
    }

    private static Observable interval;

    public static void startWatching(SessionModel model, SessionController controller) {
        startWatchingRemoteAndMaybeLocal(model, controller, true);
    }


    public static void startWatchingRemoteAndMaybeLocal(SessionModel model, SessionController controller,
        boolean watchingLocal) {
        setSessionModel(model);
        if(!alreadyWatching){
            Main.allSubscriptions.add(
                    Observable.interval(CHECK_INTERVAL, TimeUnit.SECONDS, Schedulers.io())
                            .subscribe()
            );
            if (watchingLocal)
                beginWatchingLocal(controller);
            beginWatchingRemote();
            alreadyWatching = true;
        }
    }

    // For unit testing purposes only
    public static void startWatchingRemoteOnly(SessionModel model) {
        setSessionModel(model);
        if(!alreadyWatching){
            Main.allSubscriptions.add(
                    Observable.interval(CHECK_INTERVAL, TimeUnit.SECONDS, Schedulers.io())
                            .subscribe()
            );
            beginWatchingRemote();
            alreadyWatching = true;
        }
    }


    /**
     * Associates the given model with this monitor. Updating the current
     * repository will cause the monitor to stop watching the old repository
     * and begin watching the new one
     */
    private static void beginWatchingRemote(){
        currentModel.currentRepoHelperProperty.addListener(
            (observable, oldValue, newValue) -> watchRepoForRemoteChanges(newValue)
        );
        watchRepoForRemoteChanges(currentModel.getCurrentRepoHelper());
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
                if (remoteHasNewChanges(repo))
                    setFoundNewChanges();

                try{
                    Thread.sleep(REMOTE_CHECK_INTERVAL);
                }catch(InterruptedException e){
                    interrupted = true;
                }
            }
        });

        th.setDaemon(true);
        th.setName("Remote monitor for repository \"" + repo + "\"");
        th.setPriority(2);
        th.start();

        unpause();
    }

    private static boolean remoteHasNewChanges(RepoHelper repo) {
        try{
            List<BranchHelper> localOriginHeads = repo.getBranchModel().getBranchListUntyped(BranchModel.BranchType.REMOTE);
            Collection<Ref> remoteHeads = repo.getRefsFromRemote(false);

            if(localOriginHeads.size() >= remoteHeads.size()){
                for(Ref ref : remoteHeads){
                    boolean hasFoundMatchingBranch = false;
                    boolean hasFoundNewChanges = false;

                    for(BranchHelper branch : localOriginHeads){
                        if(ref.getName().equals("refs/heads/"+repo.getRepo().shortenRemoteBranchName(branch.getRefPathString()))){
                            hasFoundMatchingBranch = true;
                            if(!branch.getHeadId().equals(ref.getObjectId())){
                                hasFoundNewChanges = true;
                            }
                            break;
                        }
                    }

                    if(hasFoundNewChanges || !hasFoundMatchingBranch){
                        return true;
                    }
                }
            }else{
                return true;
            }
        }catch(GitAPIException | IOException e)
        {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Sets hasFoundNewRemoteChanges to true if not ignoring new changes
     */
    private static void setFoundNewChanges(){
        // Not threadsafe. Needs to be fixed.
        if(!ignoreNewRemoteChanges)
            hasFoundNewRemoteChanges.set(true);
    }

    /**
     * Sets hasFoundNewRemoteChanges to false. If ignore is true, ignores
     * new changes indefinitely, else ignores them for a short grace
     * period (2 check cycles) and then begins monitoring again
     */
    public static synchronized void resetFoundNewChanges(){
        resetFoundNewChanges(REMOTE_CHECK_INTERVAL * 2);
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
        pauseWatchingRemote(millis);
        hasFoundNewRemoteChanges.set(false);
    }

    private static synchronized void beginWatchingLocal(SessionController controller){
        Thread thread = new Thread(() -> {
            while(true){
                if(!pauseLocalMonitor && currentModel.getCurrentRepoHelper() != null && currentModel.getCurrentRepoHelper().exists()){
                    Platform.runLater(controller::gitStatus);
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

        if(millis < 0) {
            return;
        }

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

        if(millis < 0) {
            return;
        }

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
