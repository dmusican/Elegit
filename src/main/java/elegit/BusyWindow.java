package main.java.elegit;

import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Created by makik on 7/8/15.
 */
public class BusyWindow extends Stage{

    private static BusyWindow window = new BusyWindow();

    private BusyWindow(){
        super();
        setAlwaysOnTop(true);
        setResizable(false);
        setMaxHeight(100);
        setMaxWidth(150);
        setMinHeight(100);
        setMinWidth(150);
        setTitle("Loading...");

        initModality(Modality.APPLICATION_MODAL);

        setScene(new Scene(getRootOfScene()));
    }

    private Parent getRootOfScene(){
        ProgressIndicator p = new ProgressIndicator();
        return new StackPane(p);
    }

    public static void appear(){
        Platform.runLater(window::show);
    }

    public static void disappear(){
        Platform.runLater(window::hide);
    }
}
