package elegit;

import elegit.controllers.BusyWindow;
import elegit.controllers.SessionController;
import io.reactivex.disposables.CompositeDisposable;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.apache.commons.lang3.SystemUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * The starting point for this JavaFX application.
 */
public class Main extends Application {
    private Path logPath;

    // boolean used to stop the service that moves cells in TreeLayout.
    // TODO: This is likely misplaced, but I can't really do much with it until I fix TreeLayout
    public final static AtomicBoolean isAppClosed = new AtomicBoolean();

    // Latch used to indicate when start is nearly complete; used for unit testing
    public final static CountDownLatch startLatch = new CountDownLatch(1);

    // Marker for sessionController; only used for unit testing
    public static SessionController sessionController;

    // This is used to keep track of a subscription for a RepositoryMonitor timer that seems to never
    // actually get used.
    // It's also a threading disaster. Is RepositoryMonitor changing this from a different thread?
    // TODO: Verify this is useful or get rid of it
    public static CompositeDisposable allSubscriptions = new CompositeDisposable();

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
        // Handles some concurrency issues with gitStatus()
        // TODO: This can't be the right way to first interact with the repository monitor
        RepositoryMonitor.pause();

        // Initialize the busy window
        BusyWindow.setParentWindow(primaryStage);

        // Load the fxml
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/elegit/fxml/MainView.fxml"));
        fxmlLoader.load();
        BorderPane root = fxmlLoader.getRoot();
        sessionController = fxmlLoader.getController();

        sessionController.loadLogging();

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

        // creates the scene
        Rectangle2D primScreenBounds = Screen.getPrimary().getVisualBounds();
        int screenWidth = (int)primScreenBounds.getWidth();
        int screenHeight = (int)primScreenBounds.getHeight();
        Scene scene = new Scene(root, screenWidth*4/5, screenHeight*4/5);

        // setup and show the stage
        primaryStage.setOnCloseRequest(event -> {
            // On close, upload the logs and delete the log.
            logger.info("Closed");
            // used to stop the service that moves cells in TreeLayout
            isAppClosed.set(true);
            allSubscriptions.clear();
        });
        primaryStage.setTitle("Elegit");
        primaryStage.setScene(scene);
        sessionController.setStage(primaryStage);
        startLatch.countDown();


        primaryStage.show();

        // Handles some concurrency issues with gitStatus()
        // TODO: Again, manage RepositoryMonitor
        RepositoryMonitor.unpause();
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

    // This can't be easily done with a standard assert, because standard asserts are exceptions, which end
    // up getting ignored in other threads
    public static void assertFxThread() {
        if (!Platform.isFxApplicationThread()) {
            System.err.println("Not in FX thread");
            System.err.println(Thread.currentThread());
            new Throwable().printStackTrace();
        }
    }


    public static void assertNotFxThread() {
        if (Platform.isFxApplicationThread()) {
            System.err.println(Thread.currentThread());
            new Throwable().printStackTrace();

        }
    }

}
