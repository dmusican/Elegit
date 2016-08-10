package elegit;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.MenuBar;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import org.apache.commons.lang3.SystemUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * The starting point for this JavaFX application.
 */
public class Main extends Application {
    private Path logPath;

    // boolean used to stop the service that moves cells in TreeLayout
    public static boolean isAppClosed;

    // Latch used to indicate when start is nearly complete; used for unit testing
    public final static CountDownLatch startLatch = new CountDownLatch(1);

    public static SessionController sessionController;

    public static void main(String[] args) {
        // If this gets fancier, we should write a more robust command-line parser or use a library.
        // At the moment, there's only one possible option.
        if (args.length == 0) {
            launch(args);
        } else if (args[0].equals("clearprefs")) {
            clearPreferences();
        } else {
            System.out.println("Invalid option.");
        }
    }

    @Override
    public void start(Stage primaryStage) throws Exception{

        // -----------------------Logging Initialization Start---------------------------
        logPath = Paths.get("logs");
        String s = logPath.toAbsolutePath().toString();
        System.setProperty("logFolder", s);

        final Logger logger = LogManager.getLogger();
        // -----------------------Logging Initialization End-----------------------------

        logger.info("Starting up.");

        // Handles some concurrency issues with gitStatus()
        RepositoryMonitor.pause();

        BusyWindow.setParentWindow(primaryStage);

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/elegit/fxml/MainView.fxml"));
        fxmlLoader.load();
        Pane root = fxmlLoader.getRoot();
        sessionController = fxmlLoader.getController();

        primaryStage.setTitle("Elegit");

        // sets the icon
        Image img = new Image(getClass().getResourceAsStream("/elegit/images/elegit_icon.png"));
        primaryStage.getIcons().add(img);
        // handles mac os dock icon
        if (SystemUtils.IS_OS_MAC) {
            java.awt.image.BufferedImage dock_img = ImageIO.read(
                    getClass().getResourceAsStream(
                            "/elegit/images/elegit_icon.png"
                )
            );
            com.apple.eawt.Application.getApplication()
                    .setDockIconImage(dock_img);
        }

        primaryStage.setOnCloseRequest(event -> {
                // On close, upload the logs and delete the log.
                logger.info("Closed");});

        Scene scene = new Scene(root, 1200, 650); // width, height

        // create the menu bar here
        MenuBar menuBar = MenuPopulator.getInstance().populate();
        // for now we'll only display menu on mac os
        // because it blocks repo dropdown menu on other platforms
        if (SystemUtils.IS_OS_MAC) {
            ((Pane) scene.getRoot()).getChildren().addAll(menuBar);
        }

        primaryStage.setScene(scene);
        startLatch.countDown();
        primaryStage.show();

        // Handles some concurrency issues with gitStatus()
        RepositoryMonitor.unpause();

        // used to stop the service that moves cells in TreeLayout
        primaryStage.setOnCloseRequest(event -> isAppClosed = true);
    }

    private static void clearPreferences() {
        Preferences prefs = Preferences.userNodeForPackage(Main.class);
        try {
            System.out.print("Are you sure you want to clear all prefs (yes/no)?");
            Scanner inp = new Scanner(System.in);
            String response = inp.next();
            if (response.equals("yes")) {
                prefs.removeNode();
                System.out.println("Preferences cleared.");
            }
        } catch (BackingStoreException e) {
            System.out.println("Error: can't access preferences.");
        }
    }
}
