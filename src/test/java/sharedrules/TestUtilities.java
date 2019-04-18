package sharedrules;

import elegit.Main;
import elegit.controllers.BusyWindow;
import elegit.controllers.SessionController;
import elegit.exceptions.CancelledAuthorizationException;
import elegit.exceptions.ExceptionAdapter;
import elegit.exceptions.MissingRepoException;
import elegit.exceptions.NoCommitsToPushException;
import elegit.exceptions.PushToAheadRemoteError;
import elegit.models.ExistingRepoHelper;
import elegit.monitors.RepositoryMonitor;
import elegit.sshauthentication.ElegitUserInfoTest;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.sshd.common.config.keys.FilePasswordProvider;
import org.apache.sshd.common.keyprovider.KeyPairProvider;
import org.apache.sshd.common.keyprovider.MappedKeyPairProvider;
import org.apache.sshd.common.util.security.SecurityUtils;
import org.apache.sshd.git.pack.GitPackCommandFactory;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.pubkey.KeySetPublickeyAuthenticator;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.testfx.util.WaitForAsyncUtils;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static org.junit.Assert.fail;

public class TestUtilities {

    private static final Logger console = LogManager.getLogger("briefconsolelogger");
    private static final String testPassword = "a_test_password";
    public static final CountDownLatch startComplete = new CountDownLatch(1);

    private static Path tempPrefsPath;

    public static String setUpTestSshServer(SshServer sshd,
                                            Path serverDirectory,
                                            Path remoteRepoDirectoryFull,
                                            Path remoteRepoDirectoryBrief)
            throws IOException, GitAPIException,
            CancelledAuthorizationException,
            MissingRepoException, GeneralSecurityException {

        console.info("Setting up ssh server");

        // Set up remote repo
        Path remoteFilePath = remoteRepoDirectoryFull.resolve("file.txt");
        Files.write(remoteFilePath, "testSshPassword".getBytes());
        //ArrayList<Path> paths = new ArrayList<>();
        //paths.add(remoteFilePath);
        ExistingRepoHelper helperServer = new ExistingRepoHelper(remoteRepoDirectoryFull, null);
        helperServer.addFilePathTest(remoteFilePath);
        helperServer.commit("Initial unit test commit");

        // All of this key set up is gratuitous, but it's the only way that I was able to get sshd to start up.
        // In the end, it is ignore, and the password is used.

        // Provide SSH server with public and private key info that client will be connecting with
        InputStream passwordFileStream = TestUtilities.class.getResourceAsStream("/rsa_key1_passphrase.txt");
        Scanner scanner = new Scanner(passwordFileStream);
        String passphrase = scanner.next();

        String privateKeyFileLocation = "/rsa_key1";
        InputStream privateKeyStream = TestUtilities.class.getResourceAsStream(privateKeyFileLocation);
        FilePasswordProvider filePasswordProvider = FilePasswordProvider.of(passphrase);
        KeyPair kp = SecurityUtils.loadKeyPairIdentity("testkey", privateKeyStream, filePasswordProvider);
        ArrayList<KeyPair> pairs = new ArrayList<>();
        pairs.add(kp);
        KeyPairProvider hostKeyProvider = new MappedKeyPairProvider(pairs);
        sshd.setKeyPairProvider(hostKeyProvider);

        // Need to use a non-standard port, as there may be an ssh server already running on this machine
        // Setting a port to 0 automatically picks an unused port. This is undocumented, but apparently works just
        // like new ServerSocket(0), which is documented
        sshd.setPort(0);


        // Set up a fall-back password authenticator to help in diagnosing failed test
        sshd.setPasswordAuthenticator(
                (username, password, session) -> {
                    if (password.equals(testPassword)) {
                        return true;
                    } else {
                        fail("Tried to use password instead of public key authentication");
                        return false;
                    }
                });


        // This replaces the role of authorized_keys.
        Collection<PublicKey> allowedKeys = new ArrayList<>();
        allowedKeys.add(kp.getPublic());
        sshd.setPublickeyAuthenticator(new KeySetPublickeyAuthenticator(allowedKeys));

        // Amazingly useful Git command setup provided by Mina.
        sshd.setCommandFactory(new GitPackCommandFactory(serverDirectory.toString()));

        // Start the SSH test server.
        sshd.start();

        // Create temporary known_hosts file.
        Path knownHostsFileLocation = serverDirectory.resolve("testing_known_hosts");
        Files.createFile(knownHostsFileLocation);

        // Clone the bare repo, using the SSH connection, to the local.
        return "ssh://localhost:" +  sshd.getPort() + "/"+remoteRepoDirectoryBrief;
    }

    public static SessionController commonTestFxStart(Stage stage) throws Exception {
        Main.assertFxThread();
        setupTestEnvironment();

        return startupFxApp(stage);

    }

    public static SessionController startupFxApp(Stage stage) throws IOException {
        Main.assertFxThread();
        BusyWindow.setParentWindow(stage);

        FXMLLoader fxmlLoader = new FXMLLoader(TestUtilities.class.getResource("/elegit/fxml/MainView.fxml"));
        fxmlLoader.load();
        SessionController sessionController = fxmlLoader.getController();
        Parent root = fxmlLoader.getRoot();
        Rectangle2D primScreenBounds = Screen.getPrimary().getVisualBounds();
        Scene scene = new Scene(root, 1100, 600);
        stage.setX(100);
        stage.setY(100);
        stage.setScene(scene);
        sessionController.setStageForNotifications(stage);
        stage.show();
        stage.toFront();
        // TODO: Remove this pause and keep test working; no good reason for it to be necessary
        RepositoryMonitor.pause();

        startComplete.countDown();
        return sessionController;
    }

    public static void setupTestEnvironment() {
        Main.testMode = true;
        Main.preferences = Preferences.userNodeForPackage(TestUtilities.class).node("test" + (new Random().nextLong()));
    }

    public static void cleanupTestEnvironment() {
        try {
            Main.preferences.removeNode();
        } catch (BackingStoreException e) {
            throw new ExceptionAdapter(e);
        }
    }

    public static void cleanupTestFXEnvironment() {
        try {
            WaitForAsyncUtils.waitFor(20, TimeUnit.SECONDS,
                    () -> !BusyWindow.window.isShowing());
            WaitForAsyncUtils.waitForFxEvents();

            cleanupTestEnvironment();
        } catch (TimeoutException e) {
            throw new ExceptionAdapter(e);
        }
    }

    public static void commonStartupOffFXThread() {
        try {
            startComplete.await();
            WaitForAsyncUtils.waitFor(20, TimeUnit.SECONDS,
                                      () -> !BusyWindow.window.isShowing());
            WaitForAsyncUtils.waitForFxEvents();
        } catch (InterruptedException e) {
            throw new ExceptionAdapter(e);
        } catch (TimeoutException e) {
            throw new ExceptionAdapter(e);
        }

    }


    public static List<RevCommit> makeTestRepo(Path remote, Path local, int numFiles, int numCommits,
                                               boolean lastCommitUndone) throws GitAPIException,
            IOException, CancelledAuthorizationException, MissingRepoException {
        Git.init().setDirectory(remote.toFile()).setBare(true).call();
        Git.cloneRepository().setDirectory(local.toFile()).setURI("file://" + remote).call();

        ExistingRepoHelper helper = new ExistingRepoHelper(local, new ElegitUserInfoTest());

        final Random random = new Random(90125);


        for (int fileNum = 0; fileNum < numFiles; fileNum++) {
            Path thisFileLocation = local.resolve("file" + fileNum);
            FileWriter fw = new FileWriter(thisFileLocation.toString(), true);
            fw.write("start"+random.nextInt()); // need this to make sure each repo comes out with different hashes
            fw.close();
            helper.addFilePathTest(thisFileLocation);
        }

        ArrayList<RevCommit> commitsMade = new ArrayList<>();
        RevCommit firstCommit = helper.commit("Appended to file");
        commitsMade.add(firstCommit);

        for (int i = 0; i < numCommits; i++) {
            if (i % 100 == 0) {
                console.info("commit num = " + i);
            }
            for (int fileNum = 0; fileNum < numFiles; fileNum++) {
                Path thisFileLocation = local.resolve("file" + fileNum);
                FileWriter fw = new FileWriter(thisFileLocation.toString(), true);
                fw.write("" + i);
                fw.close();
                helper.addFilePathTest(thisFileLocation);
            }

            // Commit all but last one (if specified), to leave something behind to actually commit in GUI
            if (i < numCommits - 1 || (i == numCommits-1 && !lastCommitUndone)) {
                RevCommit commit = helper.commit("Appended to file");
                commitsMade.add(commit);
            }
        }

        return commitsMade;
    }

}