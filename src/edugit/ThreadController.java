package edugit;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.text.Text;

/**
 * Created by makik on 6/9/15.
 */
public class ThreadController {

    private Controller controller;

    private boolean isLoadingUIThreadRunning;

    public ThreadController(Controller controller){
        this.controller = controller;
        this.isLoadingUIThreadRunning = false;
    }

    public void startLoadingUIThread(Text target){
        isLoadingUIThreadRunning = true;
        Task task = new Task<Void>() {
            @Override
            public Void call() throws Exception {
                int i = 0;
                while (isLoadingUIThreadRunning) {
                    final int finalI = i;
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            String s = ".";
                            for (int j = 0; j < finalI; j++) {
                                s = s + " .";
                            }
                            target.setText(s);
                        }
                    });
                    i = (i+1)%4;
                    Thread.sleep(300);
                }
                return null;
            }
        };
        Thread th = new Thread(task);
        th.setDaemon(true);
        th.start();
    }

    public void endLoadingUIThread(){
        isLoadingUIThreadRunning = false;
    }

    public boolean isLoadingUIThreadRunning() {
        return isLoadingUIThreadRunning;
    }
}
