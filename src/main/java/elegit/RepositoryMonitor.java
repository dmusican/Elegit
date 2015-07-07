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

    public static final long CHECK_INTERVAL = 5000;

    public static BooleanProperty hasFoundNewChanges = new SimpleBooleanProperty(false);

    private static boolean ignoreNewChanges = false;

    private static Thread th;
    private static boolean interrupted = false;

    public static void beginWatching(SessionModel model){
        model.currentRepoHelperProperty.addListener((observable, oldValue, newValue) -> watchRepoForRemoteChanges(newValue));
        watchRepoForRemoteChanges(model.getCurrentRepoHelper());
    }

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

    private static void setFoundNewChanges(){
        if(!ignoreNewChanges) hasFoundNewChanges.set(true);
    }

    public static void resetFoundNewChanges(boolean ignore){
        if(ignore){
            resetFoundNewChanges(-1);
        }else{
            resetFoundNewChanges(CHECK_INTERVAL);
        }
    }

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
