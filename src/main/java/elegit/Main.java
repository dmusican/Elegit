package elegit;

import elegit.controllers.BusyWindow;
import elegit.controllers.SessionController;
import elegit.monitors.RepositoryMonitor;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * The starting point for this JavaFX application.
 */
public class Main extends Application {
    private static final Logger logger;

    static {
        // Initialize logging. This must be done as early as possible, since other static loggers in other classes
        // depend on this system property being set; hence done in this static initializer block.
        Path logPath = Paths.get("logs");
        String s = logPath.toAbsolutePath().toString();
        System.setProperty("logFolder", s);
        logger = LogManager.getLogger();
    }

    // boolean used to stop the service that moves cells in TreeLayout.
    // TODO: This is likely misplaced, but I can't really do much with it until I fix TreeLayout
    public final static AtomicBoolean isAppClosed = new AtomicBoolean();

    // Used to indicate when initialization is done; used to avoid misordering showing initial BusyWindow
    public final static AtomicBoolean initializationComplete = new AtomicBoolean(false);

    // Marker for sessionController; only used for unit testing
    public static SessionController sessionController;

    public static Stage primaryStage;

    // Location in Java API preferences hierarchy, to be used throughout code
    public static Preferences preferences = Preferences.userNodeForPackage(Main.class);

    // Set to true when unit testing for occasional differences in code
    public static boolean testMode = false;

    // This is used to keep track of a subscription for a RepositoryMonitor timer that seems to never
    // actually get used.
    // It's also a threading disaster. Is RepositoryMonitor changing this from a different thread?
    // TODO: Verify this is useful or get rid of it
    public static CompositeDisposable allSubscriptions = new CompositeDisposable();

    // Used for testing purposes
    public static final AtomicInteger assertionCount = new AtomicInteger(0);

    public static void main(String[] args) {
        // If this gets fancier, we should write a more robust command-line parser or use a library.
        // At the moment, there's only one possible option.
        if (args.length == 0) {
            launch(args);
        } else if (args[0].equals("clearprefs")) {
            clearPreferences();
            launch(args);
        } else {
            System.out.println("Invalid option.");
        }
    }

    @Override
    public void start(Stage primaryStage) throws Exception{

        // Handles some concurrency issues with gitStatus()
        RepositoryMonitor.pause();

        // Initialize the busy window
        BusyWindow.setParentWindow(primaryStage);

        // Load the fxml
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/elegit/fxml/MainView.fxml"));
        fxmlLoader.load();
        BorderPane root = fxmlLoader.getRoot();
        sessionController = fxmlLoader.getController();

        sessionController.loadLogging();
        Main.primaryStage = primaryStage;

        RepositoryMonitor.setSessionController(sessionController);
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
            RepositoryMonitor.disposeTimers();
            allSubscriptions.clear();
        });
        primaryStage.setTitle("Elegit");
        primaryStage.setScene(scene);
        sessionController.setStageForNotifications(primaryStage);
        primaryStage.show();

        // This code is not in a synchronization block because the updates for initializationComplete should ONLY
        // happen on FX thread
        if (!Main.initializationComplete.get()) {
            BusyWindow.show();
        }

    }

    private static void clearPreferences() {
        try {
            System.out.print("Are you sure you want to clear all preferences (yes/no)?");
            Scanner inp = new Scanner(System.in);
            String response = inp.next();
            if (response.equals("yes")) {
                preferences.removeNode();
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
            assertionCount.incrementAndGet();
            new Throwable().printStackTrace();
            assert(Platform.isFxApplicationThread());
        }
        assert(Platform.isFxApplicationThread());
    }


    public static void assertNotFxThread() {
        if (Platform.isFxApplicationThread()) {
            System.err.println(Thread.currentThread());
            assertionCount.incrementAndGet();
            new Throwable().printStackTrace();

        }
    }

    public static void assertAndLog(boolean condition, String message) {
        if (!condition) {
            assertionCount.incrementAndGet();
            logger.error(message, new AssertionError());
            assert(condition);
        }
    }

    public static int getAssertionCount() {
        return assertionCount.get();
    }

    public static void showPrimaryStage() {
        primaryStage.show();
    }

    public static void hidePrimaryStage() {
        primaryStage.hide();
    }

}
