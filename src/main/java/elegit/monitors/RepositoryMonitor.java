package elegit.monitors;

import elegit.exceptions.ExceptionAdapter;
import elegit.models.SessionModel;
import elegit.controllers.SessionController;
import elegit.models.BranchHelper;
import elegit.models.BranchModel;
import elegit.models.RepoHelper;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.CompositeException;
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler;
import io.reactivex.schedulers.Schedulers;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import org.apache.http.annotation.GuardedBy;
import org.apache.http.annotation.ThreadSafe;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.lib.Ref;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A class that creates a thread to watch the current remote repository for new changes
 */
@ThreadSafe
public class RepositoryMonitor{

    // How long to pause between checks
    public static final long REMOTE_CHECK_INTERVAL = 6000;
    public static final long LOCAL_CHECK_INTERVAL = 5000;

    // Whether there are new remote changes
    public static final BooleanProperty hasFoundNewRemoteChanges = new SimpleBooleanProperty(false);

    // Used purely for unit testing
    private static final AtomicInteger numRemoteChecks = new AtomicInteger();
    private static final AtomicInteger numLocalChecks = new AtomicInteger();

    // Used purely for debugging purposes to turn off monitor permanently
    private static final boolean monitorOff = false;

    @GuardedBy("this") private static int pauseCounter = 0;
    @GuardedBy("this") private static Disposable remoteTimer = Observable.empty().subscribe();
    @GuardedBy("this") private static Disposable localTimer = Observable.empty().subscribe();
    @GuardedBy("this") private static SessionController controller;
    @GuardedBy("this") private static int exceptionCounter = 0;  // used for testing

    private static final AtomicReference<SessionController> sessionController = new AtomicReference<>();

    private static final Logger logger = LogManager.getLogger();

    public synchronized static void init(SessionController controller) {
        RepositoryMonitor.controller = controller;
        beginWatchingLocal();
        initRemote();
    }

    public synchronized static void initRemote() {
        SessionModel.getSessionModel().subscribeToOpenedRepos(RepositoryMonitor::watchRepoForRemoteChanges);
        beginWatchingRemote();
    }


    /**
     * Associates the given model with this monitor. Updating the current
     * repository will cause the monitor to stop watching the old repository
     * and begin watching the new one
     */
    private static synchronized void beginWatchingRemote(){
        watchRepoForRemoteChanges(SessionModel.getSessionModel().getCurrentRepoHelper());
    }

    /**
     * Creates a low priority thread that will monitor the remote repository
     * and compare it to the locally stored copy of the remote. When new changes
     * are detected in the remote, sets hasFoundNewRemoteChanges to true (if not ignoring
     * changes)
     * @param repo the repository to monitor
     */
    private static synchronized void watchRepoForRemoteChanges(RepoHelper repo){
        if(repo == null || !repo.exists() || !repo.hasRemote() || monitorOff) {
            return;
        }
        remoteTimer.dispose();


        remoteTimer = Observable
                .interval(0, REMOTE_CHECK_INTERVAL, TimeUnit.MILLISECONDS, Schedulers.io())
                .doOnNext(i -> {
                    numRemoteChecks.getAndIncrement();
                    if (remoteHasNewChanges(repo))
                        setFoundNewChanges();
                })
                .subscribe();
    }

    private static synchronized void stopWatchingRemoteRepo() {
        remoteTimer.dispose();
        remoteTimer = Observable.empty().subscribe();
    }

    // This method is not synchronized because it is called from the monitor thread, and a slow network connection
    // could block it up considerably. It uses no shared memory, and it makes calls to threadsafe libraries.
    private static boolean remoteHasNewChanges(RepoHelper repo) {
        try {
            List<BranchHelper> localOriginHeads = repo.getBranchModel().getBranchListUntyped(
                    BranchModel.BranchType.REMOTE);
            Collection<Ref> remoteHeads = repo.getRefsFromRemote(false);

            if (localOriginHeads.size() >= remoteHeads.size()) {
                for (Ref ref : remoteHeads) {
                    boolean hasFoundMatchingBranch = false;
                    boolean hasFoundNewChanges = false;

                    for (BranchHelper branch : localOriginHeads) {
                        if (ref.getName().equals(
                                "refs/heads/" + repo.getRepo().shortenRemoteBranchName(branch.getRefPathString()))) {
                            hasFoundMatchingBranch = true;
                            if (!branch.getHeadId().equals(ref.getObjectId())) {
                                hasFoundNewChanges = true;
                            }
                            break;
                        }
                    }

                    if (hasFoundNewChanges || !hasFoundMatchingBranch) {
                        return true;
                    }
                }
            } else {
                return true;
            }
        }catch(GitAPIException | IOException e)
        {
            System.out.println("Made it in here");
            SessionController sessionController = RepositoryMonitor.sessionController.get();
            // This work has been happening off FX thread, so notification needs to go back on it
            Platform.runLater(() -> {
                sessionController.showExceptionAsGlobalNotification(
                        new SessionController.Result(SessionController.ResultOperation.CHECK_REMOTE_FOR_CHANGES, e));
            });
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Sets hasFoundNewRemoteChanges to true if not ignoring new changes
     */
    private static void setFoundNewChanges() {
        // hasFoundNewRemoteChanges is a property which is bound within SessionController. Critical that this
        // update is done on FX thread so that when it fires listener, that happens on FX thread.
        Platform.runLater(() -> hasFoundNewRemoteChanges.set(true));
    }

    public static synchronized void resetFoundNewChanges(){
        // hasFoundNewRemoteChanges is a property which is bound within SessionController. Critical that this
        // update is done on FX thread so that when it fires listener, that happens on FX thread.
        Platform.runLater(() -> hasFoundNewRemoteChanges.set(false));
    }

    private static synchronized void beginWatchingLocal(){
        //System.out.println("Starting repo mon");
        if (SessionModel.getSessionModel().getCurrentRepoHelper() == null || monitorOff)
            return;

        localTimer.dispose();


        localTimer = Observable
                .interval(LOCAL_CHECK_INTERVAL, LOCAL_CHECK_INTERVAL, TimeUnit.MILLISECONDS, Schedulers.io())
                .doOnNext(i -> numLocalChecks.getAndIncrement())
                .observeOn(JavaFxScheduler.platform())
                //.subscribe();
                // TODO: Get status back in here once I have it threaded right
                // TODO: This is still really messy; it calls gitStatus, which pauses, which starts up again...
                .subscribe(i -> controller.gitStatus(), throwable -> {
                    exceptionCounter++;
                    throw new ExceptionAdapter(throwable);
                });
    }

    private static synchronized void stopWatchingLocal(){
        localTimer.dispose();
        localTimer = Observable.empty().subscribe();
    }


    public static synchronized void pause(){
//        logger.info("Repository monitor asked to pause");
        if(pauseCounter == 0) {
//            logger.info("Repository monitor pausing");
            stopWatchingRemoteRepo();
            stopWatchingLocal();
        }
        pauseCounter++;
    }

    public static synchronized void unpause(){
//        logger.info("Repository monitor asked to unpause");
        pauseCounter--;
        if(pauseCounter == 0) {
//            logger.info("Repository monitor unpausing");
            beginWatchingLocal();
            beginWatchingRemote();
        }
    }

    public static synchronized void disposeTimers() {
        localTimer.dispose();
        remoteTimer.dispose();
    }

    public static int getNumRemoteChecks() {
        return numRemoteChecks.get();
    }

    public static int getNumLocalChecks() {
        return numLocalChecks.get();
    }

    public static int getExceptionCounter() {
        return exceptionCounter;
    }

    public static void setSessionController(SessionController sessionController) {
        RepositoryMonitor.sessionController.set(sessionController);
    }
}
