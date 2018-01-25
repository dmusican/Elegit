package elegitfx;

import com.jcraft.jsch.JSch;
import elegit.Main;
import elegit.controllers.BusyWindow;
import elegit.controllers.SessionController;
import elegit.exceptions.ExceptionAdapter;
import elegit.models.ClonedRepoHelper;
import elegit.models.ExistingRepoHelper;
import elegit.models.SessionModel;
import elegit.monitors.RepositoryMonitor;
import elegit.sshauthentication.ElegitUserInfoGUI;
import elegit.sshauthentication.ElegitUserInfoTest;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.text.Text;
import javafx.stage.Screen;
import javafx.stage.Stage;
import junit.framework.TestCase;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.sshd.common.config.keys.FilePasswordProvider;
import org.apache.sshd.common.keyprovider.KeyPairProvider;
import org.apache.sshd.common.keyprovider.MappedKeyPairProvider;
import org.apache.sshd.common.util.security.SecurityUtils;
import org.apache.sshd.git.pack.GitPackCommandFactory;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.password.PasswordAuthenticator;
import org.apache.sshd.server.auth.pubkey.KeySetPublickeyAuthenticator;
import org.apache.sshd.server.session.ServerSession;
import org.eclipse.jgit.revwalk.RevCommit;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.loadui.testfx.GuiTest;
import org.loadui.testfx.controls.impl.VisibleNodesMatcher;
import org.testfx.framework.junit.ApplicationTest;
import sharedrules.TestingLogPathRule;
import sharedrules.TestingRemoteAndLocalReposRule;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.prefs.Preferences;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;

public class SshPrivateKeyPasswordExistingFXTest extends ApplicationTest {

    @ClassRule
    public static final TestingLogPathRule testingLogPath = new TestingLogPathRule();

    @Rule
    public final TestingRemoteAndLocalReposRule testingRemoteAndLocalRepos =
            new TestingRemoteAndLocalReposRule(false);
    private Path directoryPath;

    private static final Logger console = LogManager.getLogger("briefconsolelogger");




    static {
        // -----------------------Logging Initialization Start---------------------------
        Path logPath = Paths.get("logs");
        String s = logPath.toAbsolutePath().toString();
        System.setProperty("logFolder", s);
    }

    private static final Logger logger = LogManager.getLogger("consolelogger");

    private static final Random random = new Random(90125);

    private SessionController sessionController;
    private static GuiTest testController;


    private Stage stage;

    @Rule
    public TestName testName = new TestName();

    @Before
    public void setup() throws Exception {
        logger.info("Unit test started");
        directoryPath = Files.createTempDirectory("unitTestRepos");
        directoryPath.toFile().deleteOnExit();
        initializeLogger();
        logger.info("Test name: " + testName.getMethodName());
        directoryPath = testingRemoteAndLocalRepos.getDirectoryPath();
    }


    // Helper method to avoid annoying traces from logger
    void initializeLogger() throws IOException {
        // Create a temp directory for the files to be placed in
        Path logPath = Files.createTempDirectory("elegitLogs");
        logPath.toFile().deleteOnExit();
        System.setProperty("logFolder", logPath.toString());
    }

    @After
    public void tearDown() {
        logger.info("Tearing down");
        TestCase.assertEquals(0, Main.getAssertionCount());
    }


    @Override
    public void start(Stage stage) throws Exception {
        Main.testMode = true;
        BusyWindow.setParentWindow(stage);

        Preferences prefs = Preferences.userNodeForPackage(this.getClass());
        prefs.removeNode();

        SessionModel.setPreferencesNodeClass(this.getClass());
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/elegit/fxml/MainView.fxml"));
        fxmlLoader.load();
        sessionController = fxmlLoader.getController();
        Parent root = fxmlLoader.getRoot();
        Rectangle2D primScreenBounds = Screen.getPrimary().getVisualBounds();
        Scene scene = new Scene(root, 800, 600);
        stage.setScene(scene);
        stage.setX(0);
        stage.setY(0);
        sessionController.setStageForNotifications(stage);
        stage.show();
        stage.toFront();
        // TODO: Remove this pause and keep test working; no good reason for it to be necessary
        RepositoryMonitor.pause();

        this.stage = stage;

    }


    // http://www.jcraft.com/jsch/examples/Logger.java.html
    public static class MyLogger implements com.jcraft.jsch.Logger {
        static Hashtable<Integer,String> name=new Hashtable<>();
        static{
            name.put(DEBUG, "DEBUG: ");
            name.put(INFO, "INFO: ");
            name.put(WARN, "WARN: ");
            name.put(ERROR, "ERROR: ");
            name.put(FATAL, "FATAL: ");
        }
        public boolean isEnabled(int level){
            return true;
        }
        public void log(int level, String message){
            System.err.print(name.get(level));
            System.err.println(message);
        }
    }


    @Test
    public void testSshPrivateKey() throws Exception {

        // Set up remote repo
        Path remote = testingRemoteAndLocalRepos.getRemoteFull();
        Path remoteFilePath = remote.resolve("file.txt");
        Files.write(remoteFilePath, "testSshPassword".getBytes());
        ArrayList<Path> paths = new ArrayList<>();
        paths.add(remoteFilePath);
        ExistingRepoHelper helperServer = new ExistingRepoHelper(remote, null);
        helperServer.addFilePathsTest(paths);
        RevCommit firstCommit = helperServer.commit("Initial unit test commit");
        console.info("firstCommit name = " + firstCommit.getName());

        // Uncomment this to get detail SSH logging info, for debugging
        //JSch.setLogger(new MyLogger());

        // Set up test SSH server.
        try (SshServer sshd = SshServer.setUpDefaultServer()) {

            // Provide SSH server with public and private key info that client will be connecting with
            InputStream passwordFileStream = getClass().getResourceAsStream("/rsa_key1_passphrase.txt");
            Scanner scanner = new Scanner(passwordFileStream);
            String passphrase = scanner.next();
            console.info("phrase is " + passphrase);

            String privateKeyFileLocation = "/rsa_key1";
            InputStream privateKeyStream = getClass().getResourceAsStream(privateKeyFileLocation);
            FilePasswordProvider filePasswordProvider = FilePasswordProvider.of(passphrase);
            KeyPair kp = SecurityUtils.loadKeyPairIdentity("testkey", privateKeyStream, filePasswordProvider);
            ArrayList<KeyPair> pairs = new ArrayList<>();
            pairs.add(kp);
            KeyPairProvider hostKeyProvider = new MappedKeyPairProvider(pairs);
            sshd.setKeyPairProvider(hostKeyProvider);

            // Need to use a non-standard port, as there may be an ssh server already running on this machine
            sshd.setPort(2222);

            // Set up a fall-back password authenticator to help in diagnosing failed test
            sshd.setPasswordAuthenticator(new PasswordAuthenticator() {
                public boolean authenticate(String username, String password, ServerSession session) {
                    fail("Tried to use password instead of public key authentication");
                    return false;
                }
            });

            // This replaces the role of authorized_keys.
            Collection<PublicKey> allowedKeys = new ArrayList<>();
            allowedKeys.add(kp.getPublic());
            sshd.setPublickeyAuthenticator(new KeySetPublickeyAuthenticator(allowedKeys));

            // Amazingly useful Git command setup provided by Mina.
            sshd.setCommandFactory(new GitPackCommandFactory(directoryPath.toString()));

            // Start the SSH test server.
            sshd.start();

            // Create temporary known_hosts file.
            Path knownHostsFileLocation = directoryPath.resolve("testing_known_hosts");
            Files.createFile(knownHostsFileLocation);

            // Clone the bare repo, using the SSH connection, to the local.
            console.info("About to clone repo before bringing into Elegit");
            String remoteURL = "ssh://localhost:2222/" + testingRemoteAndLocalRepos.getRemoteBrief();
            console.info("Connecting to " + remoteURL);
            Path local = testingRemoteAndLocalRepos.getLocalFull();
            ClonedRepoHelper helper =
                    new ClonedRepoHelper(local, "",
                                         new ElegitUserInfoTest(null, passphrase),
                                         getClass().getResource(privateKeyFileLocation).getFile(),
                                         directoryPath.resolve("testing_known_hosts").toString());
            helper.obtainRepository(remoteURL);
            console.info("Repo cloned");

            // Make sure initial status is correct
            Text branchStatusText = lookup("#branchStatusText").query();
            assertEquals("",branchStatusText.getText());

            Text needToFetch = lookup("#needToFetch").query();
            assertEquals("",needToFetch.getText());

            // Open as an existing repo
            clickOn("#loadNewRepoButton")
                    .clickOn("#loadExistingRepoOption")
                    .clickOn("#repoInputDialog")
                    .write(local.toString() + "\n");


            // THIS IS WHERE TEST NEEDS TO BE FIXED; ON EXISTING REPO, NEED TO GET PRIVATE KEY IN THERE
            // Enter in private key location
            clickOn("#repoInputDialog")
                    .write(getClass().getResource(privateKeyFileLocation).getFile())
                    .clickOn("#repoInputDialogOK");

            // Enter in known hosts location
            clickOn("#repoInputDialog")
                    .write(knownHostsFileLocation.toString())
                    .clickOn("#repoInputDialogOK");


            // Since remote checking should still be off, make sure status is empty
            branchStatusText = lookup("#branchStatusText").query();
            assertEquals("",branchStatusText.getText());

            // Make sure no errors occurred
            assertNotEquals(null, RepositoryMonitor.getSessionController());
            assertEquals(0, ExceptionAdapter.getWrappedCount());
            assertEquals(0, sessionController.getNotificationPaneController().getNotificationNum());


//            // Wait for known hosts confirmation dialog, then click
//            GuiTest.waitUntil("Yes", Matchers.is(VisibleNodesMatcher.visible()));
//            clickOn("Yes");

            // Enter passphrase
            clickOn("#sshprompt")
                    .write(passphrase)
                    .write("\n");

            // Wait a while, to make sure that RepositoryMonitor has kicked in and is happy
            Thread.sleep(10000);

            // Shut down test SSH server
            assertEquals(0, ExceptionAdapter.getWrappedCount());
            sshd.stop();
        }
    }

}