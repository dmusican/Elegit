package edugit;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainView extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        Parent root = FXMLLoader.load(getClass().getResource("resources/fxml/sample.fxml"));
        primaryStage.setTitle("Hello World");

        Scene scene = new Scene(root, 350, 275);
        primaryStage.setScene(scene);
//        scene.getStylesheets().add(Main.class.getResource("Sample.css").toExternalForm());
        primaryStage.show();
    }


    public static void main(String[] args) {
        launch(args);
    }
}
