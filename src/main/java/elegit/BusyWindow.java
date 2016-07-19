package elegit;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
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

    private static Text loadingMessage;

    private static Stage initWindow(){
        window = new Stage();

        window.setMaxHeight(200);
        window.setMaxWidth(300);
        window.setMinHeight(200);
        window.setMinWidth(300);

        window.initStyle(StageStyle.UNDECORATED);
        window.initModality(Modality.APPLICATION_MODAL);

        loadingMessage = new Text("loading");
        loadingMessage.setFont(new Font(20));

        window.setScene(new Scene(getRootOfScene()));

        return window;
    }

    // link to .gif loading indicator creator: http://www.ajaxload.info
    private static Parent getRootOfScene(){
        ImageView img = new ImageView(new Image("/elegit/images/loading.gif"));
        VBox parent = new VBox(img, loadingMessage);
        parent.setSpacing(20);
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
        if(numProcessesActive == 0) {
            Platform.runLater(() -> {
                window.hide();
                loadingMessage.setText("Loading...");
            });
        }
    }

    public static void setLoadingText(String message) {
        loadingMessage.setText(message);
    }
}
