package main.java.edugit;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        /*
        Until we figure out a better way to handle this,
        when you want to get a gradle build (for shipping), use the commented-out code
        below (and comment out the `intelliJ run` line).
         */

        Parent root = FXMLLoader.load(getClass().getResource("../../resources/edugit/fxml/MainView.fxml")); //intelliJ run
//        Parent root = FXMLLoader.load(getClass().getResource("/fxml/MainView.fxml")); //gradle build
        primaryStage.setTitle("EduGit");

        Scene scene = new Scene(root, 1200, 650); // width, height
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
