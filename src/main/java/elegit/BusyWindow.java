package elegit;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

/**
 * Created by makik on 7/8/15.
 */
public class BusyWindow{

    private static Stage window = initWindow();

    private static int numProcessesActive = 0;

    private static Stage initWindow(){
        window = new Stage();

        window.setMaxHeight(100);
        window.setMaxWidth(150);
        window.setMinHeight(100);
        window.setMinWidth(150);

        window.initStyle(StageStyle.UNDECORATED);
        window.initModality(Modality.APPLICATION_MODAL);

        window.setScene(new Scene(getRootOfScene()));

        return window;
    }

    private static Parent getRootOfScene(){
        ProgressIndicator p = new ProgressIndicator();
        HBox parent = new HBox(p);
        parent.setAlignment(Pos.CENTER);
        return parent;
    }

    public static void setParentWindow(Window parent){
        window.initOwner(parent);
    }

    public static void show(){
        if(numProcessesActive == 0) Platform.runLater(window::show);
        numProcessesActive++;
    }

    public static void hide(){
        numProcessesActive--;
        if(numProcessesActive == 0) Platform.runLater(window::hide);
    }
}
