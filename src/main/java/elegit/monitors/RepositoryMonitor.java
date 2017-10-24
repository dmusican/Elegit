package elegit.monitors;

import elegit.SessionModel;
import elegit.controllers.SessionController;
import elegit.models.BranchHelper;
import elegit.models.BranchModel;
import elegit.models.RepoHelper;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler;
import io.reactivex.schedulers.Schedulers;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import org.apache.http.annotation.GuardedBy;
import org.apache.http.annotation.ThreadSafe;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

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

    @GuardedBy("this") private static int pauseCounter = 0;
    @GuardedBy("this") private static Disposable remoteTimer = Observable.empty().subscribe();
    @GuardedBy("this") private static Disposable localTimer = Observable.empty().subscribe();
    @GuardedBy("this") private static SessionController controller;

    public synchronized static void init(SessionController controller) {
        RepositoryMonitor.controller = controller;
        beginWatchingLocal();
        initRemote();
    }

    // For unit testing purposes only
    public synchronized static void initRemote() {
        SessionModel.getSessionModel().getCurrentRepoHelperProperty().addListener(
                (observable, oldValue, newValue) -> watchRepoForRemoteChanges(newValue)
        );
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
        if(repo == null || !repo.exists() || !repo.hasRemote()) {
            return;
        }

        remoteTimer.dispose();
        remoteTimer = Observable
                .interval(0, REMOTE_CHECK_INTERVAL, TimeUnit.MILLISECONDS, Schedulers.io())
                .subscribe(i -> {
                    if (remoteHasNewChanges(repo))
                        setFoundNewChanges();
                });
    }

    private static synchronized void stopWatchingRemoteRepo() {
        remoteTimer.dispose();
        remoteTimer = Observable.empty().subscribe();
    }

    private static synchronized boolean remoteHasNewChanges(RepoHelper repo) {
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
        localTimer.dispose();
        localTimer = Observable
                .interval(0, LOCAL_CHECK_INTERVAL, TimeUnit.MILLISECONDS, Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe();
                // TODO: Get status back in here once I have it threaded right
                //.subscribe(i -> controller.gitStatus());
    }

    private static synchronized void stopWatchingLocal(){
        localTimer.dispose();
        localTimer = Observable.empty().subscribe();
    }


    public static synchronized void pause(){
        if(pauseCounter == 0) {
            stopWatchingRemoteRepo();
            stopWatchingLocal();
        }
        pauseCounter++;
    }

    public static synchronized void unpause(){
        pauseCounter--;
        if(pauseCounter == 0) {
            beginWatchingLocal();
            beginWatchingRemote();
        }
    }

    public static synchronized void disposeTimers() {
        localTimer.dispose();
        remoteTimer.dispose();
    }
}
