package main.java.elegit;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.MenuBar;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import org.apache.commons.lang3.SystemUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.xml.XmlConfigurationFactory;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URI;
/**
 * The starting point for this JavaFX application.
 */
public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{

        // -----------------------Logging Initialization Start---------------------------
        System.setProperty("log4j.configurationFile", getClass().getResource("/elegit/config/log4j2.xml").getPath().toString());

        System.setProperty("logFolder", getClass().getResource("/elegit/logs").getPath().toString());


        final Logger logger = LogManager.getLogger();

        try {
            InputStream fis = getClass().getResourceAsStream("/elegit/config/log4j2.xml");

            XmlConfigurationFactory fc = new XmlConfigurationFactory( );
            fc.getConfiguration(  new ConfigurationSource( fis ) );

            URI configuration = getClass().getResource("/elegit/config/log4j2.xml").toURI();
            Configurator.initialize("config", null, configuration);

            org.apache.logging.log4j.core.LoggerContext ctx =
                    (org.apache.logging.log4j.core.LoggerContext) LogManager.getContext( true );
            ctx.reconfigure();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        // -----------------------Logging Initialization End-----------------------------

        logger.info("Starting up.");


        Pane root = FXMLLoader.load(getClass().getResource
                ("/elegit/fxml/MainView.fxml"));
        primaryStage.setTitle("Elegit");

        // sets the icon
        Image img = new Image(getClass().getResourceAsStream("/elegit/elegit_icon.png"));
        primaryStage.getIcons().add(img);
        // handles mac os dock icon
        if (SystemUtils.IS_OS_MAC) {
            java.awt.image.BufferedImage dock_img = ImageIO.read(
                    getClass().getResourceAsStream(
                    "/elegit/elegit_icon.png"
                )
            );
            com.apple.eawt.Application.getApplication()
                    .setDockIconImage(dock_img);
        }

        primaryStage.setOnCloseRequest(event -> logger.info("Closed"));

        BusyWindow.setParentWindow(primaryStage);

        Scene scene = new Scene(root, 1200, 650); // width, height

        // create the menu bar here
        MenuBar menuBar = MenuPopulator.getInstance().populate();
        // for now we'll only display menu on mac os
        // because it blocks repo dropdown menu on other platforms
        if (SystemUtils.IS_OS_MAC) {
            ((Pane) scene.getRoot()).getChildren().addAll(menuBar);
        }

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
