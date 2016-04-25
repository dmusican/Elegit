package main.java.elegit;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.text.Text;
import javafx.scene.control.MenuBar;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.scene.image.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.xml.XmlConfigurationFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.URI;
/**
 * The starting point for this JavaFX application.
 */
public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{

        // -----------------------------------------------------------------------
        System.setProperty("log4j.configurationFile", "src/main/resources/elegit/config/log4j2.xml");
        final Logger logger = LogManager.getLogger();

        File logConfigFile = new File( "src/main/resources/elegit/config/log4j2.xml" );

        try {
            FileInputStream fis = new FileInputStream( logConfigFile );

            XmlConfigurationFactory fc = new XmlConfigurationFactory( );
            fc.getConfiguration(  new ConfigurationSource( fis ) );

            URI configuration = logConfigFile.toURI();
            Configurator.initialize("config", null, configuration);

            org.apache.logging.log4j.core.LoggerContext ctx =
                    (org.apache.logging.log4j.core.LoggerContext) LogManager.getContext( true );
            ctx.reconfigure();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        logger.info("Starting up.");


        Pane root = FXMLLoader.load(getClass().getResource
                ("/elegit/fxml/MainView.fxml"));
        primaryStage.setTitle("Elegit");

        //sets the icon
        Image img = new Image("file:src/main/resources/elegit/elegit_icon.png");
        primaryStage.getIcons().add(img);

        //puts text over the browser image
        Text txt = new Text(1075, 130, "HARDCODED");
        root.getChildren().add(txt);

        primaryStage.setOnCloseRequest(event -> logger.info("Closed"));

        BusyWindow.setParentWindow(primaryStage);

        Scene scene = new Scene(root, 1200, 650); // width, height

        // create the menubar here
        MenuBar menuBar = MenuPopulator.getInstance().populate();
        // ((Pane) scene.getRoot()).getChildren().addAll(menuBar);

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
