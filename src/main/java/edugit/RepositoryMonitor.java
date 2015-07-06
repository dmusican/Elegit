package main.java.edugit;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

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

    private static void watchRepoForRemoteChanges(RepoHelper repoHelper){
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

        if(repoHelper == null) return;

        th = new Thread(() -> {

            Map<String, ObjectId> oldRefs = null;

            while(!interrupted){
                try{
                    Collection<Ref> newRefs = repoHelper.getRemoteRefs();

                    if(oldRefs != null){
                        Map<String, ObjectId> temp = new HashMap<>();
                        boolean hasFoundNewChanges = false;

                        for(Ref newRef : newRefs){
                            String refName = newRef.getName();
                            ObjectId refId = newRef.getObjectId();

                            if(oldRefs.containsKey(refName)){
                                if(!oldRefs.get(refName).equals(refId)){
                                    hasFoundNewChanges = true;
                                }
                            }else{
                                hasFoundNewChanges = true;
                            }

                            temp.put(refName, refId);
                        }

                        if(hasFoundNewChanges) setFoundNewChanges();

                        oldRefs = temp;
                    }else{
                        oldRefs = new HashMap<>();
                        for(Ref newRef : newRefs){
                            String refName = newRef.getName();
                            ObjectId refId = newRef.getObjectId();

                            oldRefs.put(refName, refId);
                        }
                    }
                }catch(GitAPIException ignored){}

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
        hasFoundNewChanges.set(false);
        ignoreNewChanges = ignore;
    }

    public static void resetFoundNewChanges(long millis){
        hasFoundNewChanges.set(false);
        ignoreNewChanges = true;

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
