package edugit;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.ProgressIndicator;

/**
 * Created by makik on 6/9/15.
 */
public class ThreadHelper{

    private static boolean isProgressThreadRunning = false;

    public static void startProgressThread(ProgressIndicator indicator){
        isProgressThreadRunning = true;
        Task task = new Task<Void>() {
            @Override
            public Void call() throws Exception {
                while (isProgressThreadRunning) {
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
//                            indicator.setProgress();
                        }
                    });
                    Thread.sleep(300);
                }
                return null;
            }
        };
        Thread th = new Thread(task);
        th.setDaemon(true);
        th.start();
    }

    public static void endProgressThread(){
        isProgressThreadRunning = false;
    }

    public static boolean isProgressThreadRunning() {
        return isProgressThreadRunning;
    }

    public static void startThread(Runnable r){
        Thread th = new Thread(r);
        th.setDaemon(false);
        th.start();
    }
}
