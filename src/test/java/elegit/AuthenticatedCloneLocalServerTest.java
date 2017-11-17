package elegit;

import com.jcraft.jsch.JSch;
import elegit.exceptions.ExceptionAdapter;
import elegit.models.AuthMethod;
import elegit.models.ClonedRepoHelper;
import elegit.models.ExistingRepoHelper;
import elegit.models.LocalBranchHelper;
import elegit.sshauthentication.ElegitUserInfoTest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.sshd.common.config.keys.FilePasswordProvider;
import org.apache.sshd.common.keyprovider.KeyPairProvider;
import org.apache.sshd.common.keyprovider.MappedKeyPairProvider;
import org.apache.sshd.common.util.security.SecurityUtils;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.password.PasswordAuthenticator;
import org.apache.sshd.server.auth.pubkey.AcceptAllPublickeyAuthenticator;
import org.apache.sshd.server.auth.pubkey.PublickeyAuthenticator;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.session.ServerSession;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PushCommand;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Scanner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class AuthenticatedCloneLocalServerTest {

    private static Path logPath;

    static {
        try {
            logPath = Files.createTempDirectory("elegitLogs");
        } catch (IOException e) {
            throw new ExceptionAdapter(e);
        }

        logPath.toFile().deleteOnExit();

        System.setProperty("logFolder", logPath.toString());
    }

    private static final Logger console = LogManager.getLogger("briefconsolelogger");

    private Path directoryPath;

    @Before
    public void setUp() throws Exception {
        console.info("Unit test started");

        directoryPath = Files.createTempDirectory("unitTestRepos");
        directoryPath.toFile().deleteOnExit();
    }

    @After
    public void tearDown() throws Exception {
        removeAllFilesFromDirectory(this.logPath.toFile());
        removeAllFilesFromDirectory(directoryPath.toFile());
    }

    // Helper tear-down method:
    void removeAllFilesFromDirectory(File dir) {
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) removeAllFilesFromDirectory(file);
            file.delete();
        }
    }

    // http://www.jcraft.com/jsch/examples/Logger.java.html
    public static class MyLogger implements com.jcraft.jsch.Logger {
        static java.util.Hashtable<Integer,String> name=new java.util.Hashtable<>();
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
        JSch.setLogger(new AuthenticatedCloneTest.MyLogger());

        SshServer sshd = SshServer.setUpDefaultServer();

        String testFileLocation = System.getProperty("user.home") + File.separator +
                "elegitTests" + File.separator;
//        File passwordFile = new File(testFileLocation + "sshPrivateKeyPassword.txt");
        File passwordFile = new File(testFileLocation + "anotherpass.txt");
        File keyFile = new File(testFileLocation + "anotherkeyfile.txt");

        Scanner scanner = new Scanner(passwordFile);
        String passphrase = scanner.next();
        console.info("phrase is " + passphrase);

        scanner = new Scanner(keyFile);
        String keyFileName = scanner.next();

        InputStream inputStream = Files.newInputStream(Paths.get(keyFileName));
        FilePasswordProvider filePasswordProvider = FilePasswordProvider.of(passphrase);

        KeyPair kp = SecurityUtils.loadKeyPairIdentity("testkey", inputStream, filePasswordProvider);
        ArrayList<KeyPair> pairs = new ArrayList<>();
        pairs.add(kp);
        KeyPairProvider hostKeyProvider = new MappedKeyPairProvider(pairs);
        sshd.setKeyPairProvider(hostKeyProvider);

        // Need to use a non-standard port, as there may be an ssh server already running on this machine
        sshd.setPort(2222);



        sshd.setPasswordAuthenticator(new PasswordAuthenticator() {
            public boolean authenticate(String username, String password, ServerSession session) {
                fail("Tried to use password instead of public key authentication");
                return false;
            }
        });

        sshd.setPublickeyAuthenticator(AcceptAllPublickeyAuthenticator.INSTANCE);


        sshd.start();


        Path remote = directoryPath.resolve("remote");
        Path local = directoryPath.resolve("local");

        console.info("Remote path = " + remote);
        console.info("Local path = " + local);

        Git remoteHandle = Git.init().setDirectory(remote.toFile()).setBare(true).call();

        String remoteURL = "ssh://localhost:2222"+remote;
        console.info("Connecting to " + remoteURL);
        ClonedRepoHelper helper = new ClonedRepoHelper(local, remoteURL, passphrase,
                                                       new ElegitUserInfoTest(null, passphrase));
        helper.obtainRepository(remoteURL);
        assertEquals(helper.getCompatibleAuthentication(), AuthMethod.SSH);
        helper.fetch(false);

        Path fileLocation = local.resolve("README.md");

        FileWriter fw = new FileWriter(fileLocation.toString(), true);
        fw.write("start");
        fw.close();
        helper.addFilePathTest(fileLocation);
        helper.commit("Appended to file");


        PushCommand command = helper.prepareToPushAll();
        helper.pushAll(command);


    }
}
